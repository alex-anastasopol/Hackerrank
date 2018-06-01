package ro.cst.tsearch.bean;

import static org.apache.commons.lang.StringEscapeUtils.escapeXml;
import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mailtemplate.MailTemplateUtils;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.DataSources;
import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.community.CommunityProducts;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.data.CountyCommunityManager;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.GenericCounty;
import ro.cst.tsearch.data.GenericState;
import ro.cst.tsearch.data.StateCountyManager;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.database.rowmapper.ProductsMapper;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.propertyInformation.Address;
import ro.cst.tsearch.propertyInformation.Family;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.search.address.Normalize;
import ro.cst.tsearch.search.address.StandardAddress;
import ro.cst.tsearch.search.name.NameNormalizer;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.types.CertificationDateDS;
import ro.cst.tsearch.servers.types.CertificationDateManager;
import ro.cst.tsearch.servers.types.GenericDASLNDB;
import ro.cst.tsearch.servers.types.GenericPI;
import ro.cst.tsearch.servers.types.GenericSKLD;
import ro.cst.tsearch.servers.types.GenericServerADI;
import ro.cst.tsearch.servers.types.TNGenericMSServiceCT;
import ro.cst.tsearch.titledocument.abstracts.FormatSa;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.RomanNumeral;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.address.AddressI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.TransferI;
import com.stewart.ats.base.document.sort.RecordedDateComparator;
import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.Subdivision;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShip;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.name.NameFormaterI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.parties.Party;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.Pin;
import com.stewart.ats.base.property.PinI;
import com.stewart.ats.base.property.Property;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.base.search.SearchAttributesI;
import com.stewart.ats.tsrindex.client.CertificationDate;
import com.stewart.ats.tsrindex.client.ReviewChecker;
import com.stewart.ats.tsrindex.client.ReviewChecker.ReviewFlag;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;
import com.stewart.ats.tsrindex.client.ocrelements.LegalDescription;
import com.stewart.ats.tsrindex.client.ocrelements.LegalDescriptionI;
import com.stewart.ats.tsrindex.client.ocrelements.OcredEditableElementI;
import com.stewart.ats.tsrindex.client.ocrelements.VestingInfo;
import com.stewart.ats.tsrindex.client.ocrelements.VestingInfoI;

/**
 * @author nae
 */
public class SearchAttributes implements Serializable, Cloneable,
		SearchAttributesI {

	static final long serialVersionUID = 10000001;

	private static final Category logger = Logger
			.getLogger(SearchAttributes.class);

	private static final Category loggerDetails = Logger
			.getLogger(Log.DETAILS_PREFIX + SearchAttributes.class.getName());

	public static final String IS_CONDO = "IS_CONDO";

	public static final String IS_UPDATED_ONCE = "IS_UPDATED_ONCE";
	
	public static final String IS_PRODUCT_CHANGED_ONCE = "IS_PRODUCT_CHANGED_ONCE";
	
	public static final String IS_UPDATE_OF_A_REFINANCE = "IS_UPDATE_OF_A_REFINANCE";
	
	public static final String ORIGINAL_SEARCH_OLDER_THAN_ONE_YEAR = "ORIGINAL_SEARCH_OLDER_THAN_ONE_YEAR";

	public static final String IS_PLATED = "IS_PLATED";

	public static final String PROPERTY_TYPE = "PROPERTY_TYPE";

	public static final String SEARCH_WARNING = "SEARCH_WARNING";

	public static final String NO_KEY = "NO_KEY";

	public static final String BLANK_ATTR = "BLANK_ATTR";

	public static final String OWNER_OBJECT = "OWNER_OBJECT";

	public static final String BUYER_OBJECT = "BUYER_OBJECT";

	public static final String GB_MANAGER_OBJECT = "GB_MANAGER_OBJECT";

	public static final String OWNERS_LIST = "OWNERS_LIST";

	public static final String BUYERS_LIST = "BUYERS_LIST";

	public static final String INSTR_LIST = "INSTR_LIST";

	public static final String CERTIFICATION_DATE_ON_SEARCH = "CERTIFICATION_DATE_ON_SEARCH";
	
	public static final String DATA_SOURCES_ON_SEARCH = "DATA_SOURCES_ON_SEARCH";
	
	public static final String REVIEW_CHECKER_LIST = "REVIEW_CHECKER_LIST";
	
	public static final String ORDER_COUNT_CACHE = "ORDER_COUNT_CACHE";

	public static final String SEARCH_PAGE_MANUAL_OWNERS_LIST = "SEARCH_PAGE_MANUAL_OWNERS_LIST";
	public static final String SEARCH_PAGE_MANUAL_BUYERS_LIST = "SEARCH_PAGE_MANUAL_BUYERS_LIST";
	public static final String SEARCH_PAGE_MANUAL_FIELDS = "SEARCH_PAGE_MANUAL_FIELDS";

	// boolean array[4]. 0-owner, 1-co-owner, 2-buyer, 3- co-buyer
	// 1 null and 2 false means search was canceled.
	public static final String INITIAL_SEARCH_MIDDLE_NAME_MISSING = "INITIAL_SEARCH_MIDDLE_NAME_MISSING";

	// HashSet that keeps all site types that have name search skipped
	public static final String SITES_WITH_NAME_SEARCH_SKIPPED = "SITES_WITH_NAME_SEARCH_SKIPPED";
	// Poor search data from order search! (B2527)

	public static final String POOR_SEARCH_DATA = "POOR_SEARCH_DATA";

	/**
	 * Please use GenericInstrumentIterator<br><br>
	 * used to store crossref info parsed from the official document (like from
	 * comments) (any book/page or instrumentNo without a link so ATS will treat
	 * them like crossreferences)
	 */
	@Deprecated
	public static final String RO_CROSS_REF_INSTR_LIST = "RO_CROSS_REF_INSTR_LIST";

	public static final String SEARCHFINISH = "SEARCHFINISH";

	public static final String INITDATE = "Jan 1, 1810";

	public static final String START_HISTORY_DATE = "STARTHISTORYDATE";
	public static final String START_HISTORY_DATE_MM_SLASH_DD_SLASH_YYYY = "START_HISTORY_DATE_MM_SLASH_DD_SLASH_YYYY";
	public static final String FROMDATE = "FROMDATE";
	public static final String TODATE = "TODATE";
	public static final String FROMDATE_MM_SLASH_DD_SLASH_YYYY = "FROMDATE_MM_SLASH_DD_SLASH_YYYY";
	public static final String TODATE_MM_SLASH_DD_SLASH_YYYY = "TODATE_MM_SLASH_DD_SLASH_YYYY";
	public static final String FROMDATE_MM_DD_YYYY = "FROMDATE_MM_DD_YYYY";
	public static final String TODATE_MM_DD_YYYY = "TODATE_MM_DD_YYYY";
	public static final String FROMDATE_MMM_DD_YYYY = "FROMDATE_MMM_DD_YYYY";
	public static final String TODATE_MMM_DD_YYYY = "TODATE_MMM_DD_YYYY";
	public static final String FROMDATE_MM_DD_YY = "FROMDATE_MM_DD_YY";
	public static final String TODATE_MM_DD_YY = "TODATE_MM_DD_YY";
	public static final String FROMDATE_YYYY_MM_DD = "FROMDATE_YYYY_MM_DD";
	public static final String TODATE_YYYY_MM_DD = "TODATE_YYYY_MM_DD";

	public static final String FROMDATE_YEAR = "FROMDATE_YEAR";
	public static final String TODATE_YEAR = "TODATE_YEAR";
	public static final String FROMDATE_MMDD = "FROMDATE_MMDD";
	public static final String TODATE_MMDD = "TODATE_MMDD";
	public static final String FROMDATE_MM = "FROMDATE_MM";
	public static final String TODATE_MM = "TODATE_MM";
	public static final String FROMDATE_DD = "FROMDATE_DD";
	public static final String TODATE_DD = "TODATE_DD";
	public static final String CURRENTDATE_MMDD = "CURRENTDATE_MMDD";
	public static final String CURRENTDATE_MM = "CURRENTDATE_MM";
	public static final String CURRENTDATE_DD = "CURRENTDATE_DD";
	public static final String CURRENTDATE_YYYY = "CURRENTDATE_YYYY";
	public static final String LAST_REAL_TRANSFER_DATE_MMDD = "LAST_REAL_TRANSFER_DATE_MMDD";
	public static final String LAST_REAL_TRANSFER_DATE_YYYY = "LAST_REAL_TRANSFER_DATE_YYYY";
	public static final String LAST_TRANSFER_DATE_MMDDYYYY = "LAST_TRANSFER_DATE_MMDDYYYY";
	public static final String LIEN_DATE_MMDD = "LIEN_DATE_MMDD";
	public static final String LIEN_DATE_YYYY = "LIEN_DATE_YYYY";

	public static final String SEARCHUPDATE = "SEARCHUPDATE";
	
	public static final String FVS_UPDATE	= "FVS_UPDATE";

	public static final String LAST_SCHEDULED_FVS_UPDATE = "LAST_SCHEDULED_FVS_UPDATE";
	
	public static final String FVS_UPDATE_AUTO_LAUNCHED = "FVS_UPDATE_AUTO_LAUNCHED";
	
	public static final String ATS_MULTIPLE_LEGALS_FOUND = "ATS_MULTIPLE_LEGALS_FOUND";

	public static final String ATS_MULTIPLE_LEGAL_INSTRUMENTS = "ATS_MULTIPLE_LEGAL_INSTRUMENTS";

	// table 1) Property Adress
	public static final String P_STREETNO = "P_STREETNO";

	public static final String P_STREETDIRECTION = "P_STREETDIRECTION";

	public static final String P_STREET_POST_DIRECTION = "P_STREET_POST_DIRECTION";

	public static final String P_STREETDIRECTION_ABBREV = "P_STREETDIRECTION_ABBREV";

	public static final String P_STREETNAME = "P_STREETNAME";

	public static final String P_ORDER_STREETNAME = "P_ORDER_STREETNAME";

	public static final String P_STREET_NO_NAME = "P_STREET_NO_NAME";

	public static final String P_STREET_NO_NAME_NO_SPACE = "P_STREET_NO_NAME_NO_SPACE";
	
	public static final String P_STREET_NO_DIR_NAME_POSTDIR = "P_STREET_NO_DIR_NAME_POSTDIR";

	public static final String P_STREETSUFIX = "P_STREETSUFIX";
	
	public static final String P_STREETSUFIX_ABBREV = "P_STREETSUFIX_ABBREV";

	public static final String P_STREETUNIT = "P_STREETUNIT";

	public static final String P_STREETUNIT_CLEANED = "P_STREETUNIT_CLEANED";

	public static final String P_CITY = "P_CITY";

	public static final String P_MUNICIPALITY = "P_MUNICIPALITY";

	public static final String P_IDENTIFIER_TYPE = "P_IDENTIFIER_TYPE";

	public static final String TSOPCODE = "TS_OP_CODE";

	public static final String P_STATE = "P_STATE";
	public static final String P_STATE_ABREV = "P_STATE_ABREV";

	public static final String P_COUNTY = "P_COUNTY";
	public static final String P_COUNTY_FIPS = "P_COUNTY_FIPS";
	public static final String P_COUNTY_NAME = "P_COUNTY_NAME";

	public static final String P_ZIP = "P_ZIP";

	public static final String P_STREET_FULL_NAME = "P_STREET_FULL_NAME";
	
	/**
	 * No + Dir + Name + Suffix
	 */
	public static final String P_FULL_ADDRESS_N_D_N_S = "P_FULL_ADDRESS_N_D_N_S";

	public static final String P_STREET_FULL_NAME_EX = "P_STREET_FULL_NAME_EX";

	public static final String P_STREETNAME_SUFFIX_UNIT_NO = "P_STREETNAME_SUFFIX_UNIT_NO";

	public static final String P_STREET_FULL_NAME_NO_SUFFIX = "P_STREET_FULL_NAME_NO_SUFFIX";

	public static final String P_STREET_NO_NA_SU = "P_STREET_NO_NA_SU";

	public static final String P_STREET_NA_SU_NO = "P_STREET_NA_SU_NO";

	public static final String P_STREET_NO_STAR_NA_STAR = "P_STREET_NO_STAR_NA_STAR";

	// table 2) Legal Description
	public static final String LD_INSTRNO = "LD_INSTRNO";

	public static final String LD_BOOKNO = "LD_BOOKNO";

	public static final String LD_PAGENO = "LD_PAGENO";

	public static final String LD_BOOKNO_1 = "LD_BOOKNO_1";

	public static final String LD_PAGENO_1 = "LD_PAGENO_1";

	public static final String LD_BOOKPAGE = "LD_BOOKPAGE";

	public static final String LD_SUBDIVISION = "LD_SUBDIVISION";

	public static final String LD_LOTNO = "LD_LOTNO";
	
	public static final String LD_SUBLOT = "LD_SUBLOTNO";

	public static final String WEEK = "WEEK";

	public static final String BUILDING = "BUILDING";

	public static final String AGENT_USER = "AGENT_USER";

	public static final String AGENT_PASSWORD = "AGENT_PASSWORD";

	public static final String LD_SUBDIV_NAME = "LD_SUBDIV_NAME";

	public static final String LD_ADDITION = "LD_ADDITION";

	public static final String LD_SUBDIV_PARCEL = "LD_SUBDIV_PARCEL";

	public static final String ARB = "ARB";

	public static final String ARB_LOT = "ARB_LOT";
	
	public static final String ARB_BLOCK = "ARB_BLOCK";
	
	public static final String ARB_BOOK = "ARB_BOOK";
	
	public static final String ARB_PAGE = "ARB_PAGE";
	
	public static final String LD_AREA = "LD_AREA";
	
	public static final String LD_PI_AREA = "LD_PI_AREA";
	public static final String LD_PI_SEC = "LD_PI_SEC";
	public static final String LD_PI_BLOCK = "LD_PI_BLOCK";
	public static final String LD_PI_PARCEL = "LD_PI_PARCEL";
	public static final String LD_PI_UNIT = "LD_PI_UNIT";
	
	public static final String LD_PI_MAP_BOOK = "LD_PI_MAP_BOOK";
	public static final String LD_PI_MAP_PAGE = "LD_PI_MAP_PAGE";
	public static final String LD_PI_LOT = "LD_PI_LOT";
	public static final String LD_PI_TRACT = "LD_PI_TRACT";
	public static final String LD_PI_MAP_CODE = "LD_PI_MAP_CODE";
	public static final String LD_PI_MAJ_LEGAL_NAME = "LD_PI_MAJ_LEGAL_NAME";
	
	public static final String LD_IL_WILL_AO_COMP_CODE = "LD_IL_WILL_AO_COMP_CODE";
	public static final String LD_IL_WILL_AO_TWN = "LD_IL_WILL_AO_TWN";
	public static final String LD_IL_WILL_AO_SEC = "LD_IL_WILL_AO_SEC";
	public static final String LD_IL_WILL_AO_BLOCK = "LD_IL_WILL_AO_BLOCK";
	public static final String LD_IL_WILL_AO_LOT = "LD_IL_WILL_AO_LOT";
	public static final String LD_IL_WILL_AO_MISC = "LD_IL_WILL_AO_MISC";
	
	public static final String LD_SUBDIV_NAME_MOJACKSONRO = "LD_SUBDIV_NAME_MOJACKSONRO"; // bug #531: save subdivision
																							// name taken from MO Jackson RO

	public static final String LD_SUBDIV_NAME_AND_PHASE = "LD_SUBDIV_NAME_AND_PHASE";

	public static final String LD_SUBDIV_NAME_AND_PHASE_ROMAN = "LD_SUBDIV_NAME_AND_PHASE_ROMAN";

	public static final String LD_SUBDIV_BLOCK = "LD_SUBDIV_BLOCK";

	public static final String LD_SUBDIV_PHASE = "LD_SUBDIV_PHASE";

	public static final String LD_SUBDIV_TRACT = "LD_SUBDIV_TRACT";

	/**
	 * This <b>Section</b> is related to subdivision and has <b>NO</b>
	 * connection to township<br>
	 * Be careful when parsing since this is not to be confused with the
	 * township <i>Section (LD_SUBDIV_SEC)</i>
	 */
	public static final String LD_SECTION = "LD_SECTION";

	/**
	 * This <b>Section</b> is related to township and has to be paired with
	 * <i>LD_SUBDIV_TWN</i> and <i>LD_SUBDIV_RNG</i><br>
	 * Be careful when parsing since this is not to be confused with the
	 * subdivision <i>Section (LD_SECTION)</i>
	 */
	public static final String LD_SUBDIV_SEC = "LD_SUBDIV_SEC";

	public static final String LD_SUBDIV_TWN = "LD_SUBDIV_TWN";

	public static final String LD_SUBDIV_RNG = "LD_SUBDIV_RNG";

	public static final String LD_SUBDIV_CODE = "LD_SUBDIV_CODE";

	public static final String QUARTER_ORDER = "QUARTER_ORDER";

	public static final String QUARTER_VALUE = "QUARTER_VALUE";

	public static final String LD_ACRES = "LD_ACRES";
	
	// composed attribute from lot number and subdivision
	public static final String LD_LOT_SUBDIVISION = "LD_LOT_SUBDIVISION";

	public static final String LD_PARCELNO = "LD_PARCELNO";

	public static final String LD_PARCELNO_RANGE = "LD_PARCELNO_RANGE";

	public static final String LD_PARCELNO_TOWNSHIP = "LD_PARCELNO_TOWNSHIP";

	public static final String LD_PARCELNO2 = "LD_PARCELNO2"; // now used just
																// with DASL RV

	public static final String LD_PARCELNONDB = "LD_PARCELNONDB";

	public static final String LD_PARCELNO2_ALTERNATE = "LD_PARCELNO2_ALTERNATE"; // now used just with DASL RV

	public static final String LD_PARCELNO3 = "LD_PARCELNO3"; // now used just with DASL DT , we can ahe
																// counties that has LD_PARCELNO,LD_PARCELNO2,LD_PARCELNO3

	public static final String STEWARTORDERS_ORDER_ID = "";

	public static final String SURECLOSE_FILE_ID = "SURECLOSE_FILE_ID";

	public static final String STEWARTORDERS_ORDER_PRODUCT_ID = "STEWARTORDERS_ORDER_PRODUCT_ID";

	public static final String STEWARTORDERS_CUSTOMER_GUID = "STEWARTORDERS_CUSTOMER_GUID";
	
	public static final String STEWARTORDERS_PARENT_ORDER_GUID = "STEWARTORDERS_PARENT_ORDER_GUID";
	public static final String STEWARTORDERS_TO_UPDATE_ORDER_GUID = "STEWARTORDERS_TO_UPDATE_ORDER_GUID";
	
	public static final String ATIDS_FILE_REFERENCE_ID = "ATIDS_FILE_REFERENCE_ID";
	public static final String ATIDS_FILE_REFERENCE_CREATED = "ATIDS_FILE_REFERENCE_CREATED";

	public static final String LD_PARCELNO_PREFIX = "LD_PARCELNO_PREFIX"; // used to store the prefix
																			// of PID on KYJeffersonTR

	public static final String LD_PARCELNO_FULL = "LD_PARCELNO_FULL"; // used to construct the full PID
																		// on KYJeffersonTR (AO PID + prefix)

	public static final String LD_SUBDIV_UNIT = "LD_SUBDIV_UNIT";

	public static final String LD_PARCELNO_MAP = "LD_PARCELNO_MAP";
	
	public static final String LD_PARCELNO_CTRL_MAP = "LD_PARCELNO_CTRL_MAP";

	public static final String LD_PARCELNO_GROUP = "LD_PARCELNO_GROUP";

	public static final String LD_PARCELNO_PARCEL = "LD_PARCELNO_PARCEL";

	public static final String LD_TN_WILLIAMSON_Ctl1 = "LD_TN_WILLIAMSON_Ctl1";

	public static final String LD_TN_WILLIAMSON_Ctl2 = "LD_TN_WILLIAMSON_Ctl2";

	public static final String LD_TN_WILLIAMSON_GROUP = "LD_TN_WILLIAMSON_GROUP";

	public static final String LD_TN_WILLIAMSON_PARCEL = "LD_TN_WILLIAMSON_PARCEL";

	public static final String LD_TN_WILLIAMSON_ID = "LD_TN_WILLIAMSON_ID";

	public static final String LD_TN_WILLIAMSON_SI = "LD_TN_WILLIAMSON_SI";
	
	public static final String LD_TN_WILLIAMSON_YC_Ctl1 = "LD_TN_WILLIAMSON_YC_Ctl1";

	public static final String LD_TN_WILLIAMSON_YC_Ctl2 = "LD_TN_WILLIAMSON_YC_Ctl2";

	public static final String LD_TN_WILLIAMSON_YC_Group = "LD_TN_WILLIAMSON_YC_Group";

	public static final String LD_TN_WILLIAMSON_YC_Parcel = "LD_TN_WILLIAMSON_YC_Parcel";

	public static final String LD_TN_WILLIAMSON_YC_Id = "LD_TN_WILLIAMSON_YC_Id";

	public static final String LD_TN_WILLIAMSON_YC_Si = "LD_TN_WILLIAMSON_YC_Si";
	
	public static final String LD_TN_WILLIAMSON_YB_Ctl1 = "LD_TN_WILLIAMSON_YB_Ctl1";
	
	public static final String LD_TN_WILLIAMSON_YB_Ctl2 = "LD_TN_WILLIAMSON_YB_Ctl2";

	public static final String LD_TN_WILLIAMSON_YB_Group = "LD_TN_WILLIAMSON_YB_Group";

	public static final String LD_TN_WILLIAMSON_YB_Parcel = "LD_TN_WILLIAMSON_YB_Parcel";

	public static final String LD_TN_WILLIAMSON_YB_Id = "LD_TN_WILLIAMSON_YB_Id";

	public static final String LD_TN_WILLIAMSON_YB_Si = "LD_TN_WILLIAMSON_YB_Si";

	public static final String LD_NCB_NO = "LD_NCB_NO";

	public static final String LD_ABS_NO = "LD_ABS_NO";
	
	public static final String LD_DISTRICT = "LD_DISTRICT";

	public static final String LD_PARCELNO_CONDO = "LD_PARCELNO_CONDO";

	public static final String LD_PARCELNO_PARCEL_CONDO = "LD_PARCELNO_PARCEL_CONDO";

	public static final String LD_FL_PALM_BEACH_SECTION = "LD_FL_PALM_BEACH_SECTION";

	public static final String LD_FL_PALM_BEACH_TOWNSHIP = "LD_FL_PALM_BEACH_TOWNSHIP";

	public static final String LD_FL_PALM_BEACH_RANGE = "LD_FL_PALM_BEACH_RANGE";

	public static final String LD_FL_PALM_BEACH_SUBDIVISION = "LD_FL_PALM_BEACH_SUBDIVISION";

	public static final String LD_FL_PALM_BEACH_BLOCK = "LD_FL_PALM_BEACH_BLOCK";

	public static final String LD_FL_PALM_BEACH_LOT = "LD_FL_PALM_BEACH_LOT";

	public static final String LD_FL_PALM_BEACH_CITY = "LD_FL_PALM_BEACH_CITY";

	public static final String LD_FL_PINELLAS_SECTION = "LD_FL_PINELLAS_SECTION";

	public static final String LD_FL_PINELLAS_TOWNSHIP = "LD_FL_PINELLAS_TOWNSHIP";

	public static final String LD_FL_PINELLAS_RANGE = "LD_FL_PINELLAS_RANGE";

	public static final String LD_FL_PINELLAS_SUBDIVISION = "LD_FL_PINELLAS_SUBDIVISION";

	public static final String LD_FL_PINELLAS_BLOCK = "LD_FL_PINELLAS_BLOCK";

	public static final String LD_FL_PINELLAS_LOT = "LD_FL_PINELLAS_LOT";

	public static final String LD_FL_PINELLAS_CITY = "LD_FL_PINELLAS_CITY";

	public static final String LD_FL_ORANGE_SECTION = "LD_FL_ORANGE_SECTION";

	public static final String LD_FL_ORANGE_TOWNSHIP = "LD_FL_ORANGE_TOWNSHIP";

	public static final String LD_FL_ORANGE_RANGE = "LD_FL_ORANGE_RANGE";

	public static final String LD_FL_ORANGE_SUBDIVISION = "LD_FL_ORANGE_SUBDIVISION";

	public static final String LD_FL_ORANGE_BLOCK = "LD_FL_ORANGE_BLOCK";

	public static final String LD_FL_ORANGE_LOT = "LD_FL_ORANGE_LOT";

	public static final String LD_GEO_NUMBER = "LD_GEO_NUMBER";

	public static final String LD_PARCELNO_GENERIC_TR = "LD_PARCELNO_GENERIC_TR";
	public static final String LD_PARCELNO_GENERIC_AO = "LD_PARCELNO_GENERIC_AO";
	public static final String LD_PARCELNO_GENERIC_NDB = "LD_PARCELNO_GENERIC_NDB";
	public static final String LD_PARCELNO_GENERIC_RO = "LD_PARCELNO_GENERIC_RO";
	public static final String LD_PARCELNO_GENERIC_PRI = "LD_PARCELNO_GENERIC_PRI";

	public static final String LD_FL_HERNANDO_SECTION = "LD_FL_HERNANDO_SECTION";
	public static final String LD_FL_HERNANDO_BOOK = "LD_FL_HERNANDO_BOOK";
	public static final String LD_FL_HERNANDO_TOWNSHIP = "LD_FL_HERNANDO_TOWNSHIP";
	public static final String LD_FL_HERNANDO_RANGE = "LD_FL_HERNANDO_RANGE";
	public static final String LD_FL_HERNANDO_SUBDIVIZION = "LD_FL_HERNANDO_SUBDIVIZION";
	public static final String LD_FL_HERNANDO_BLOCK = "LD_FL_HERNANDO_BLOCK";
	public static final String LD_FL_HERNANDO_LOT = "LD_FL_HERNANDO_LOT";
	
	public static final String LD_TN_SHELBY_WARD = "LD_TN_SHELBY_WARD";
	public static final String LD_TN_SHELBY_BLOCK = "LD_TN_SHELBY_BLOCK";
	public static final String LD_TN_SHELBY_SUB = "LD_TN_SHELBY_SUB";
	public static final String LD_TN_SHELBY_PARCEL = "LD_TN_SHELBY_PARCEL";
	public static final String LD_TN_SHELBY_TAG = "LD_TN_SHELBY_TAG";

	public static final String LD_AO_FRANKLIN_TAX_DISTRICT = "LD_AO_FRANKLIN_TAX_DISTRICT";
	public static final String LD_AO_FRANKLIN_PARCEL_NO = "LD_AO_FRANKLIN_PARCEL_NO";

	public static final String LD_CO_PARCEL_NO = "LD_CO_PARCEL_NO";
	public static final String LD_CO_ACCOUNT_NO = "LD_CO_ACCOUNT_NO";

	public static final String LD_SK_SUBDIVISION_NAME = "LD_SK_SUBDIVISION_NAME";
	public static final String LD_SK_MAPID_BOOK = "LD_SK_MAPID_BOOK";
	public static final String LD_SK_MAPID_PAGE = "LD_SK_MAPID_PAGE";
	public static final String LD_SK_LOT_LOW = "LD_SK_LOT_LOW";
	public static final String LD_SK_LOT_HIGH = "LD_SK_LOT_HIGH";
	public static final String LD_SK_BLOCK_LOW = "LD_SK_BLOCK_LOW";
	public static final String LD_SK_BLOCK_HIGH = "LD_SK_BLOCK_HIGH";
	
	public static final String LD_TAD_SUBDIVISION_OR_ACREAGE = "LD_TAD_SUBDIVISION_OR_ACREAGE";
	public static final String LD_TAD_PLAT_BOOK_PAGE = "LD_TAD_PLAT_BOOK_PAGE";

	public static final String LD_TS_SUBDIV_NAME = "LD_TS_SUBDIV_NAME";
	public static final String LD_TS_PLAT_BOOK = "LD_TS_PLAT_BOOK";
	public static final String LD_TS_PLAT_PAGE = "LD_TS_PLAT_PAGE";
	public static final String LD_TS_LOT = "LD_TS_LOT";
	public static final String LD_TS_BLOCK = "LD_TS_BLOCK";

	public static final String TN_MONTGOMERY_EP_PID = "TN_MONTGOMERY_EP_PID";
	
	public static final String LD_PARCELNO_MAP_GENERIC_TR = "LD_PARCELNO_MAP_GENERIC_TR";
	public static final String LD_PARCELNO_CTRL_MAP_GENERIC_TR = "LD_PARCELNO_CTRL_MAP_GENERIC_TR";
	public static final String LD_PARCELNO_GROUP_GENERIC_TR = "LD_PARCELNO_GROUP_GENERIC_TR";
	public static final String LD_PARCELNO_PARCEL_GENERIC_TR = "LD_PARCELNO_PARCEL_GENERIC_TR";
	
	public static final String LD_MO_PLATTE_TWN = "LD_MO_PLATTE_TWN";
	public static final String LD_MO_PLATTE_AREA = "LD_MO_PLATTE_AREA";
	public static final String LD_MO_PLATTE_SECT = "LD_MO_PLATTE_SECT";
	public static final String LD_MO_PLATTE_QTRSECT = "LD_MO_PLATTE_QTRSECT";
	public static final String LD_MO_PLATTE_BLOCK = "LD_MO_PLATTE_BLOCK";
	public static final String LD_MO_PLATTE_PARCEL = "LD_MO_PLATTE_PARCEL";
	
	public static final String LD_MD_DISTRICT = "LD_MD_DISTRICT";
	public static final String LD_MD_ACCOUNT = "LD_MD_ACCOUNT";
	
	// table 3) Property Owner
	/** company type constants */
	public static int COMP_INDIVIDUAL = 0;

	public static int COMP_TRUST = 1;

	public static int COMP_CORPORATION = 2;

	public static int COMP_PART = 3;

	public static int COMP_PARTNERSHIP = 4;

	public static int COMP_FEDGOV = 5;

	public static int COMP_STATEGOV = 6;

	public static int COMP_FEDBANK = 7;

	public static int COMP_STATEBANK = 8;

	public static int COMP_LLC = 9;

	/** end company constants */
	public static final String OWNER_FNAME = "OWNER_FNAME";

	public static final String OWNER_MNAME = "OWNER_MNAME";

	public static final String OWNER_MINITIAL = "OWNER_MINITIAL";
	
	public static final String OWNER_GUID = "OWNER_GUID";

	public static final String BUYER_GUID = "BUYER_GUID";
	
	public static final String OWNER_LNAME = "OWNER_LNAME";

	public static final String OWNER_LF_NAME = "OWNER_LF_NAME";

	public static final String OWNER_LFM_NAME = "OWNER_LFM_NAME";

	public static final String OWNER_FML_NAME = "OWNER_FML_NAME";
	
	
	public static final String OWNER_LAST_NAME = "OWNER_LAST_NAME";
	public static final String OWNER_FIRST_NAME = "OWNER_FIRST_NAME";
	public static final String OWNER_MIDDLE_NAME = "OWNER_MIDDLE_NAME";
	public static final String OWNER_COMPANY_NAME = "OWNER_COMPANY_NAME";

	public static final String OWNER_NAME_KEY = "OWNER_NAME_KEY";

	public static final String OWNER_NAME_SUFFIX = "OWN_NM_SUF";

	public static final String OWNER_COMPTYPE = "OWNER_COMPTYPE";

	public static final String OWNER_ZIP = "OWNER_ZIP";

	public static final String OWNER_FULL_NAME = "O_FULL_NAME";

	public static final String OWNER_LCF_NAME = "OWNER_LCF_NAME";

	public static final String OWNER_LCFM_NAME = "OWNER_LCFM_NAME";

	public static final String BUYER_FULL_NAME = "B_FULL_NAME";

	public static final String IGNORE_MNAME = "IGNORE_MNAME";

	public static final String IGNORE_MNAME_BUYER = "IGNORE_MNAME_BUYER";

	// table 4) Buyer
	public static final String BUYER_FNAME = "BUYER_FNAME";

	public static final String BUYER_MNAME = "BUYER_MNAME";

	public static final String BUYER_LNAME = "BUYER_LNAME";

	public static final String BUYER_COMPTYPE = "BUYER_COMPTYPE";

	public static final String BUYER_NAME_KEY = "BUYER_NAME_KEY";

	public static final String BUYER_NAME_SUFFIX = "BUY_NM_SUF";

	// table 5) Order By
	public static final String ORDERBY_ID = "ORDERBY_ID";

	public static final String ORDERBY_FNAME = "ORDERBY_FNAME";

	public static final String ORDERBY_MNAME = "ORDERBY_MNAME";

	public static final String ORDERBY_LNAME = "ORDERBY_LNAME";

	public static final String ORDERBY_FILENO = "ORDERBY_FILENO";

	// table 6) Abstractor
	public static final String ABSTRACTOR_OBJECT = "ABSTRACTOR_OBJECT";

	public static final String ABSTRACTOR_EMAIL = "ABSTRACTOR_EMAIL";

	public static final String ABSTRACTOR_FILENO = "ABSTRACTOR_FILENO";

	// table 7) 1st Borrower Mortgage
	public static final String BM1_LENDERNAME = "BM1_LENDERNAME";

	public static final String BM1_LOADACCOUNTNO = "BM1_LOADACCOUNTNO";

	// table 8) 2nd Borrower Mortgage
	public static final String BM2_LENDERNAME = "BM2_LENDERNAME";

	public static final String BM2_LOADACCOUNTNO = "BM2_LOADACCOUNTNO";

	public static final String ADDITIONAL_INFORMATION = "ADDITIONAL_INFORMATION";

	public static final String ADDITIONAL_REQUIREMENTS = "ADDITIONAL_REQUIREMENTS";

	public static final String ADDITIONAL_EXCEPTIONS = "ADDITIONAL_EXCEPTIONS";

	public static final String LEGAL_DESCRIPTION = "LEGAL_DESCRIPTION";

	public static final String LEGAL_DESCRIPTION_STATUS = "LEGAL_DESCRIPTION_STATUS";

	@Deprecated
	public static final String CERTICICATION_DATE = "CERTICICATION_DATE";

	public static final String ATS_DEFAULT_CERTIFICATION_DATE = "ATS_DEFAULT_CERTIFICATION_DATE";

	public static final String SEARCH_PRODUCT = "SEARCH_PRODUCT";

	public static final String PAYRATE_NEW_VALUE = "PAYRATE_NEW_VALUE";

	public static final String ASSESSED_VALUE = "ASSESSED_VALUE";

	public static final String SEARCH_ORIGIN = "SEARCH_ORIGIN";

	public static final String SEARCH_STEWART_TYPE = "SEARCH_STEWART_TYPE";

	public static final String TITLEDESK_ORDER_ID = "TITLEDESK_ORDER_ID";

	public static final String ECORE_AGENT_ID = "ECORE_AGENT_ID";

	public static final String TITLE_UNIT = "TITLE_UNIT";

	public static final String ADDITIONAL_LENDER_LANGUAGE = "ADDITIONAL_LENDER_LANGUAGE";

	public static final String CURRENT_TAX_YEAR = "CURRENT_TAX_YEAR";

	public static final String ADDITIONAL_SEARCH_TYPE = "ADDITIONAL_SEARCH_TYPE";

	// public static final String STATEMENTS_FOR_SSF = "STATEMENTS_FOR_SSF";
	
	/**
	 * Before this flag the log was written in a file locally and then stored in the database (<b>value 0</b>)<br>
	 * After this flag it is possible to write the log directly in the database statement by statement (<b>value ServerConfig.getLogInTableVersion()</b>)<br>
	 */
	public static final String INTERNAL_LOG_ORIGINAL_LOCATION = "INTERNAL_LOG_ORIGINAL_LOCATION";

	public static final int SEARCH_PROD_FULL = 1;

	public static final int SEARCH_PROD_CURRENT_OWN = 2;

	public static final int SEARCH_PROD_CONSTRUCTION = 3;

	public static final int SEARCH_PROD_COMMERCIAL = 4;

	public static final int SEARCH_PROD_REFINANCE = 5;

	public static final int SEARCH_PROD_OE = 6;

	public static final int SEARCH_PROD_LIENS = 7;

	public static final int SEARCH_PROD_ACREAGE = 8;

	public static final int SEARCH_PROD_SUBLOT = 9;

	public static final int SEARCH_PROD_UPDATE = 10;

	public static final String DATE_DOWN = "DATE_DOWN";
	public static final String DATA_SOURCE = "DATA_SOURCE";

	public static final String OTHER_RESULTS = "OTHER_RESULTS";

	private boolean isSet = false;

	private boolean reopenSearch;

	private volatile VestingInfoI vestingInfoGrantee = new VestingInfo();

	private volatile LegalDescriptionI legalDescription = new LegalDescription();

	private volatile Map<String, LinkedHashSet<String>> statementsForSSF = new HashMap<String, LinkedHashSet<String>>();

	private volatile CertificationDate certification = new CertificationDate();

	private volatile CertificationDate effectiveStartDate = null;

	public static final String[] PRODUCT_NAMES = Products.getProductListNames();

	public static final int YEARS_BACK = 45; // for how many years search back
												// in time

	private String abstrFileName = "";

	private HashMap hashSearch;

	private HashMap extraHashSearch;
	private transient AtomicInteger fileUploadInProgressCount = new AtomicInteger(
			0);
	private transient HashSet<InstrumentI> invalidatedInstruments = new LinkedHashSet<InstrumentI>();

	// we do not want duplicates
	@SuppressWarnings("unused")
	@Deprecated
	private transient Set<Family> ownerSearchForGBList = null;

	long searchId = -1;

	long originalSearchId = -1;

	private int commId = -1;
	private String searchIdSKLD = null;

	private Map<Long, List<NameI>> forUpdateSearchGrantorNames = null;
	private Map<Long, List<NameI>> forUpdateSearchGranteeNames = null;
	private Map<Long, List<AddressI>> forUpdateSearchAddresses = null;
	private Map<Long, List<LegalI>> forUpdateSearchLegals = null;

	private PropertyI orderProperty = new Property();
	private PropertyI validatedProperty = null;

	private static final String DATE_FORMAT = "MMM d, yyyy";
	public static final java.text.SimpleDateFormat DEFAULT_DATE_PARSER = new java.text.SimpleDateFormat(
			DATE_FORMAT);

	private static final String DATE_FORMAT_MM_dd_yyy = "MM/dd/yyyy";
	public static final java.text.SimpleDateFormat DATE_FORMAT_MM_dd_yyy_PARSER = new java.text.SimpleDateFormat(
			DATE_FORMAT_MM_dd_yyy);

	/**
	 * Be careful when you add a new Search attribute in hashSearch. You should
	 * check that old searches are ok. Pay attention to
	 * {@link IllegalArgumentException} thrown in @see
	 * {@link #getAtribute(String)} and @see #setAtribute(String, String). You
	 * should add another "if" in order to avoid this exception.
	 * 
	 * @param searchId
	 */
	@SuppressWarnings("unchecked")
	public SearchAttributes(long searchId) {
		this.searchId = searchId;
		hashSearch = new HashMap();
		extraHashSearch = new HashMap();

		hashSearch.put(SEARCHFINISH, "true");
		setDefaultFromDate();
		setDefaultToDate();

		hashSearch.put(SEARCHUPDATE, "false");
		hashSearch.put(SEARCH_PRODUCT, "");
		hashSearch.put(PAYRATE_NEW_VALUE, "");
		hashSearch.put(FVS_UPDATE, "false");
		hashSearch.put(LAST_SCHEDULED_FVS_UPDATE, "false");
		hashSearch.put(FVS_UPDATE_AUTO_LAUNCHED, "false");

		hashSearch.put(IS_CONDO, "false");
		hashSearch.put(IS_UPDATED_ONCE, "");
		hashSearch.put(IS_PLATED, "");
		hashSearch.put(PROPERTY_TYPE, "");
		hashSearch.put(SEARCH_WARNING, "");
		hashSearch.put(IS_PRODUCT_CHANGED_ONCE, "");
		hashSearch.put(IS_UPDATE_OF_A_REFINANCE, "false");
		hashSearch.put(ORIGINAL_SEARCH_OLDER_THAN_ONE_YEAR, "false");

		// 1) Property Adress
		hashSearch.put(P_STREETNO, "");
		hashSearch.put(P_STREETDIRECTION, "");
		hashSearch.put(P_STREET_POST_DIRECTION, "");
		hashSearch.put(P_STREETDIRECTION_ABBREV, "");
		hashSearch.put(P_STREETNAME, "");
		hashSearch.put(P_ORDER_STREETNAME, "");
		hashSearch.put(P_STREETSUFIX, "");
		hashSearch.put(P_STREETSUFIX_ABBREV, "");
		hashSearch.put(P_STREETUNIT, "");
		hashSearch.put(P_STREETUNIT_CLEANED, "");
		hashSearch.put(P_CITY, "");
		hashSearch.put(P_MUNICIPALITY, "");
		hashSearch.put(P_STATE, "");
		hashSearch.put(P_COUNTY, "");
		hashSearch.put(P_ZIP, "");
		hashSearch.put(P_IDENTIFIER_TYPE, "");

		// 2) Legal Description
		hashSearch.put(LD_INSTRNO, "");
		hashSearch.put(LD_BOOKNO, "");
		hashSearch.put(LD_PAGENO, "");
		hashSearch.put(LD_BOOKPAGE, "");
		hashSearch.put(LD_SUBDIVISION, "");
		hashSearch.put(LD_LOTNO, "");
		hashSearch.put(LD_SUBLOT, "");
		hashSearch.put(WEEK, "");
		hashSearch.put(BUILDING, "");
		hashSearch.put(AGENT_USER, "");
		hashSearch.put(AGENT_PASSWORD, "");
		hashSearch.put(LD_SUBDIV_NAME, "");
		hashSearch.put(LD_SUBDIV_PARCEL, "");
		hashSearch.put(ARB, "");
		hashSearch.put(ARB_LOT, "");
		hashSearch.put(ARB_BLOCK, "");
		hashSearch.put(ARB_BOOK, "");
		hashSearch.put(ARB_PAGE, "");
		hashSearch.put(LD_ADDITION, "");
		hashSearch.put(LD_SUBDIV_NAME_MOJACKSONRO, "");
		hashSearch.put(LD_LOT_SUBDIVISION, "");
		hashSearch.put(LD_SUBDIV_BLOCK, "");
		hashSearch.put(LD_SUBDIV_TRACT, "");
		hashSearch.put(LD_SUBDIV_PHASE, "");
		hashSearch.put(LD_SUBDIV_SEC, "");
		hashSearch.put(LD_SECTION, "");
		hashSearch.put(LD_SUBDIV_TWN, "");
		hashSearch.put(LD_SUBDIV_RNG, "");
		hashSearch.put(LD_SUBDIV_CODE, "");
		hashSearch.put(LD_NCB_NO, "");
		hashSearch.put(LD_ABS_NO, "");
		hashSearch.put(LD_DISTRICT, "");
		hashSearch.put(QUARTER_ORDER, "");
		hashSearch.put(QUARTER_VALUE, "");
		hashSearch.put(LD_AREA, "");
		hashSearch.put(LD_PI_AREA, "");
		hashSearch.put(LD_PI_BLOCK, "");
		hashSearch.put(LD_PI_PARCEL, "");
		hashSearch.put(LD_PI_SEC, "");
		hashSearch.put(LD_PI_UNIT, "");
		
		hashSearch.put(LD_IL_WILL_AO_BLOCK, "");
		hashSearch.put(LD_IL_WILL_AO_COMP_CODE, "");
		hashSearch.put(LD_IL_WILL_AO_LOT, "");
		hashSearch.put(LD_IL_WILL_AO_MISC, "");
		hashSearch.put(LD_IL_WILL_AO_SEC, "");
		hashSearch.put(LD_IL_WILL_AO_TWN, "");

		hashSearch.put(LD_TS_SUBDIV_NAME, "");
		hashSearch.put(LD_TS_PLAT_BOOK, "");
		hashSearch.put(LD_TS_PLAT_PAGE, "");
		hashSearch.put(LD_TS_LOT, "");
		hashSearch.put(LD_TS_BLOCK, "");
		hashSearch.put(OWNER_GUID, "");
		hashSearch.put(BUYER_GUID, "");
		hashSearch.put(LD_PARCELNO, "");
		hashSearch.put(LD_PARCELNO2, "");
		hashSearch.put(LD_PARCELNONDB, "");
		hashSearch.put(LD_PARCELNO3, "");
		hashSearch.put(STEWARTORDERS_ORDER_ID, "");
		hashSearch.put(ATIDS_FILE_REFERENCE_ID, "");
		hashSearch.put(ATIDS_FILE_REFERENCE_CREATED, "");
		hashSearch.put(SURECLOSE_FILE_ID, "");
		hashSearch.put(STEWARTORDERS_ORDER_PRODUCT_ID, "");
		hashSearch.put(STEWARTORDERS_CUSTOMER_GUID, "");
		hashSearch.put(STEWARTORDERS_PARENT_ORDER_GUID, "");
		hashSearch.put(STEWARTORDERS_TO_UPDATE_ORDER_GUID, "");
		hashSearch.put(LD_PARCELNO2_ALTERNATE, "");
		hashSearch.put(LD_GEO_NUMBER, "");
		hashSearch.put(ATS_MULTIPLE_LEGALS_FOUND, "");
		hashSearch.put(ATS_MULTIPLE_LEGAL_INSTRUMENTS, "");
		hashSearch.put(LD_BOOKNO_1, "");
		hashSearch.put(LD_PAGENO_1, "");

		hashSearch.put(LD_PARCELNO_RANGE, "");
		hashSearch.put(LD_PARCELNO_TOWNSHIP, "");

		hashSearch.put(LD_PARCELNO_PREFIX, "");
		hashSearch.put(LD_PARCELNO_PARCEL, "");
		hashSearch.put(LD_PARCELNO_CONDO, "");
		hashSearch.put(LD_PARCELNO_MAP, "");
		hashSearch.put(LD_PARCELNO_CTRL_MAP, "");
		hashSearch.put(LD_PARCELNO_GROUP, "");
		hashSearch.put(LD_SUBDIV_UNIT, "");
		
		// 3) Property Owner
		hashSearch.put(OWNER_ZIP, "");
		hashSearch.put(IGNORE_MNAME, "false");

		// 4) Buyer
		hashSearch.put(IGNORE_MNAME_BUYER, "false");

		// 5) Order By
		hashSearch.put(ORDERBY_ID, "0");
		hashSearch.put(ORDERBY_FNAME, "");
		hashSearch.put(ORDERBY_MNAME, "");
		hashSearch.put(ORDERBY_LNAME, "");
		hashSearch.put(ORDERBY_FILENO, "");

		// 6) Abstractor
		hashSearch.put(ABSTRACTOR_FILENO, "");

		// 7) 1st Borrower Mortgage
		hashSearch.put(BM1_LENDERNAME, "");
		hashSearch.put(BM1_LOADACCOUNTNO, "");

		// 8) 2nd Borrower Mortgage
		hashSearch.put(BM2_LENDERNAME, "");
		hashSearch.put(BM2_LOADACCOUNTNO, "");
		hashSearch.put(CERTICICATION_DATE, "");
		hashSearch.put(ATS_DEFAULT_CERTIFICATION_DATE, "");

		// for costin purpose
		hashSearch.put(ADDITIONAL_INFORMATION, "");
		hashSearch.put(ADDITIONAL_REQUIREMENTS, "");
		hashSearch.put(ADDITIONAL_EXCEPTIONS, "");
		hashSearch.put(LEGAL_DESCRIPTION, "");
		hashSearch.put(LEGAL_DESCRIPTION_STATUS, "0");

		hashSearch.put(ASSESSED_VALUE, "0");

		hashSearch.put(SEARCH_ORIGIN, "");
		hashSearch.put(SEARCH_STEWART_TYPE, "");
		hashSearch.put(TITLEDESK_ORDER_ID, "");
		hashSearch.put(ECORE_AGENT_ID, "");
		hashSearch.put(TITLE_UNIT, "");
		hashSearch.put(ADDITIONAL_LENDER_LANGUAGE, "");
		hashSearch.put(ADDITIONAL_SEARCH_TYPE, "");
		hashSearch.put(OTHER_RESULTS, "");

		hashSearch.put(POOR_SEARCH_DATA, "false");
		hashSearch.put(TSOPCODE, "-1");

		extraHashSearch.put(INSTR_LIST, new ArrayList());
		extraHashSearch.put(RO_CROSS_REF_INSTR_LIST,
				new ArrayList<Instrument>());
		extraHashSearch.put(GB_MANAGER_OBJECT, new GBManager());
		extraHashSearch.put(INITIAL_SEARCH_MIDDLE_NAME_MISSING, new Boolean[] {
				null, null, null, null });
		extraHashSearch.put(SITES_WITH_NAME_SEARCH_SKIPPED, null);
		extraHashSearch.put(OWNERS_LIST, new Party(PType.GRANTOR));
		extraHashSearch.put(SEARCH_PAGE_MANUAL_OWNERS_LIST, new Party(
				PType.GRANTOR));
		extraHashSearch.put(SEARCH_PAGE_MANUAL_BUYERS_LIST, new Party(
				PType.GRANTEE));
		extraHashSearch.put(SEARCH_PAGE_MANUAL_FIELDS,
				new Hashtable<String, String>());
		extraHashSearch.put(BUYERS_LIST, new Party(PType.GRANTEE));
		extraHashSearch.put(CERTIFICATION_DATE_ON_SEARCH, new ArrayList<CertificationDateDS>());
		extraHashSearch.put(DATA_SOURCES_ON_SEARCH, new HashSet<DataSources>());
		extraHashSearch.put(REVIEW_CHECKER_LIST, new LinkedHashMap<String, ReviewChecker>());
		extraHashSearch.put(ORDER_COUNT_CACHE, new HashMap<String, Set<Integer>>());
		extraHashSearch.put(INTERNAL_LOG_ORIGINAL_LOCATION, 
				(ServerConfig.isEnableLogInSamba())?
						new Integer(ServerConfig.getLogInTableVersion()):
						new Integer(0));
		
		//add the key only if needed
		//extraHashSearch.put(TDI_NO_OF_EXPECTED_RESULTS, new AtomicInteger(-1));

		//int commId = getCommId();

		if (URLMaping.INSTANCE_DIR.startsWith("local")) {
			setAtribute("ABSTRACTOR_FILENO", "testATS");
			//Calendar cal = Calendar.getInstance();
			/*
			 * setAtribute("ORDERBY_FILENO", "Test~" +
			 * Calendar.getInstance().getTime().getDate() + "~");
			 */
		}
	}

	public Date setDefaultFromDate(){

		Calendar calendar = Calendar.getInstance();
		int yearsBackValue = YEARS_BACK;
		// LTD , PTD will not be functional yet for "Search From"
		try {
			if (hashSearch.containsKey(P_COUNTY)){
				String searchFrom = Products.getSearchFrom(getCommId(), getProductId());
				if (Products.SearchFromOptions.DEF.toString().equals(searchFrom)
						|| Products.SearchFromOptions.LTD.toString().equals(searchFrom)
						|| Products.SearchFromOptions.PTD.toString().equals(searchFrom)){
					
					yearsBackValue = CountyCommunityManager.getInstance()
												.getCountyCommunityMapper(Integer.parseInt(getCountyId()), getCommId()).getDefaultStartDateOffset();
				} else if (Products.SearchFromOptions.CD.toString().equals(searchFrom) && Products.isOneOfUpdateProductType(getProductId())){// only for Update

					try {
						int daysBackValue = CountyCommunityManager.getInstance()
													.getCountyCommunityMapper(Integer.parseInt(getCountyId()), getCommId()).getDefaultCertificationDateOffset();
						if (daysBackValue == CertificationDateManager.CERTIFICATION_DATE_OFFSET_FOR_EMPTY_INPUT){
							if (certification.getDate() != null){
								return certification.getDate();
							}
						} else{
							Calendar now = Calendar.getInstance();
							now.add(Calendar.DATE, -daysBackValue);
							hashSearch.put(FROMDATE, DEFAULT_DATE_PARSER.format(now.getTime()));
							return now.getTime();
						}
					} catch (Exception e){
						e.printStackTrace();
					}

				} else if (NumberUtils.isDigits(searchFrom)){
					try {
						int years = Integer.parseInt(searchFrom);
						Calendar now = Calendar.getInstance();
						now.add(Calendar.YEAR, -years);
						hashSearch.put(FROMDATE, DEFAULT_DATE_PARSER.format(now.getTime()));
						return now.getTime();
					} catch (Exception e){
						e.printStackTrace();
					}
				}
			}
		} catch (Exception ignored){
		}

		calendar.add(Calendar.YEAR, -yearsBackValue); // go back N years

		hashSearch.put(FROMDATE, DEFAULT_DATE_PARSER.format(calendar.getTime()));
		return calendar.getTime();
	}

	public void setDefaultToDate() {
		hashSearch.put(TODATE, DEFAULT_DATE_PARSER.format(Calendar.getInstance().getTime()));
	}

	public SearchAttributes(SearchAttributes sa, long searchId) {
		this.searchId = searchId;
		this.hashSearch = new HashMap();

		for (Iterator iter = sa.hashSearch.keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			this.hashSearch.put(key, sa.hashSearch.get(key));
		}

		this.extraHashSearch = new HashMap();

		for (Iterator iter = sa.extraHashSearch.keySet().iterator(); iter
				.hasNext();) {
			String key = (String) iter.next();
			this.extraHashSearch.put(key, sa.extraHashSearch.get(key));
		}
	}

	public Map getAttributes() {
		return hashSearch;
	}

	public String cleanText(String text) {

		text = text.replaceAll("\\bLOTS?\\b", " ");
		text = text.replaceAll("\\bBLKS?\\b", " ");
		text = text.replaceAll("\\bTRA?C?T?S?\\b", " ");
		text = text.replaceAll("\\bSEC\\b", " ");
		text = text.replaceAll("\\bSUB\\b", " ");
		text = text.replaceAll("\\bOF\\b", " ");
		text = text.replaceAll("\\bTRACT\\b", " ");
		text = text.replaceAll("\\bUNITS?\\b", " ");
		text = text.replaceAll("\\bPHASE\\b", " ");
		text = text.replaceAll("\\bPH\\b", " ");
		text = text.replaceAll(",", " ");
		text = text.replaceAll("\\.", " ");
		text = text.replaceAll("\\&", " ");
		text = text.replaceAll("\\\\", " ");
		text = text.replaceAll("\\-", " ");

		return " " + text.replaceAll("\\s+", " ");
	}

	private static final Map<String, String> dirAbbreviations = new HashMap<String, String>();
	static {
		dirAbbreviations.put("SOUTHEAST", "SE");
		dirAbbreviations.put("SOUTHWEST", "SW");
		dirAbbreviations.put("NORTHEAST", "NE");
		dirAbbreviations.put("NORTHWEST", "NW");
		dirAbbreviations.put("SE", "SE");
		dirAbbreviations.put("SW", "SW");
		dirAbbreviations.put("NE", "NE");
		dirAbbreviations.put("NW", "NW");
	}

	public String getAtribute(String key) {
		if (key.equals(LD_IL_WILL_AO_COMP_CODE)) {
			String pin = getAtribute(SearchAttributes.LD_PARCELNO);
			pin = pin.replaceAll("(?is)\\p{Punct}", "");
			if (pin.length() == 16) {
				return pin.substring(0, 2);
			}
		} else if (key.equals(LD_IL_WILL_AO_TWN)) {
			String pin = getAtribute(SearchAttributes.LD_PARCELNO);
			pin = pin.replaceAll("(?is)\\p{Punct}", "");
			if (pin.length() == 16) {
				return pin.substring(2, 4);
			}
		} else if (key.equals(LD_IL_WILL_AO_SEC)) {
			String pin = getAtribute(SearchAttributes.LD_PARCELNO);
			pin = pin.replaceAll("(?is)\\p{Punct}", "");
			if (pin.length() == 16) {
				return pin.substring(4, 6);
			}
		} else if (key.equals(LD_IL_WILL_AO_BLOCK)) {
			String pin = getAtribute(SearchAttributes.LD_PARCELNO);
			pin = pin.replaceAll("(?is)\\p{Punct}", "");
			if (pin.length() == 16) {
				return pin.substring(6, 9);
			}

		} else if (key.equals(LD_IL_WILL_AO_LOT)) {
			String pin = getAtribute(SearchAttributes.LD_PARCELNO);
			pin = pin.replaceAll("(?is)\\p{Punct}", "");
			if (pin.length() == 16) {
				return pin.substring(9, 12);
			}
		} else if (key.equals(LD_IL_WILL_AO_MISC)) {
			String pin = getAtribute(SearchAttributes.LD_PARCELNO);
			pin = pin.replaceAll("(?is)\\p{Punct}", "");
			if (pin.length() == 16) {
				String misc = pin.substring(12, 16);
				
				return misc;
			} else{
				return "";
			}
		} else if (key.equals(LD_PI_AREA)) {
			String pin = getAtribute(SearchAttributes.LD_PARCELNO);
			String stateCounty = getStateCounty();
			pin = pin.replaceAll("(?is)\\p{Punct}", "");
			if ("ILWill".equals(stateCounty)){
				if (pin.length() == 16) {
					return org.apache.commons.lang.StringUtils.stripStart(pin.substring(2, 4), "0");
				}
			} else {
				if (pin.length() > 2) {
					return org.apache.commons.lang.StringUtils.stripStart(pin.substring(0, 2), "0");
				}
			}
		} else if (key.equals(LD_PI_SEC)) {
			String pin = getAtribute(SearchAttributes.LD_PARCELNO);
			String stateCounty = getStateCounty();
			pin = pin.replaceAll("(?is)\\p{Punct}", "");
			if ("ILWill".equals(stateCounty)){
				if (pin.length() == 16) {
					return org.apache.commons.lang.StringUtils.stripStart(pin.substring(4, 6), "0");
				}
			} else {
				if (pin.length() > 4) {
					return org.apache.commons.lang.StringUtils.stripStart(pin.substring(2, 4), "0");
				}
			}
		} else if (key.equals(LD_PI_BLOCK)) {
			String pin = getAtribute(SearchAttributes.LD_PARCELNO);
			String stateCounty = getStateCounty();
			pin = pin.replaceAll("(?is)\\p{Punct}", "");
			if ("ILWill".equals(stateCounty)){
				if (pin.length() >= 9) {
					return org.apache.commons.lang.StringUtils.stripStart(pin.substring(6, 9), "0");
				}
			} else {
				if (pin.length() >= 7) {
					return org.apache.commons.lang.StringUtils.stripStart(pin.substring(4, 7), "0");
				}
			}
		} else if (key.equals(LD_PI_PARCEL)) {
			String pin = getAtribute(SearchAttributes.LD_PARCELNO);
			String stateCounty = getStateCounty();
			pin = pin.replaceAll("(?is)\\p{Punct}", "");
			if ("ILWill".equals(stateCounty)){
				if (pin.length() >= 12) {
					return org.apache.commons.lang.StringUtils.stripStart(pin.substring(9, 12), "0");
				}
			} else {
				if (pin.length() >= 10) {
					return org.apache.commons.lang.StringUtils.stripStart(pin.substring(7, 10), "0");
				}
			}
		} else if (key.equals(LD_PI_UNIT)) {
			String pin = getAtribute(SearchAttributes.LD_PARCELNO);
			String stateCounty = getStateCounty();
			pin = pin.replaceAll("(?is)\\p{Punct}", "");
			if ("ILWill".equals(stateCounty)){
				if (pin.length() == 16) {
					return org.apache.commons.lang.StringUtils.stripStart(pin.substring(12, 16), "0");
				}
			} else {
				if (pin.length() == 14) {
					return org.apache.commons.lang.StringUtils.stripStart(pin.substring(10, 14), "0");
				}
			}
		} else if (key.equals(FROMDATE_YEAR)) {
			String fromDateStr = getAtribute(SearchAttributes.FROMDATE);
			Date fromDate = Util.dateParser3(fromDateStr);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
			return String.valueOf(sdf.format(fromDate));
		} else if (key.equals(TODATE_YEAR)) {
			String fromDateStr = getAtribute(SearchAttributes.TODATE);
			Date fromDate = Util.dateParser3(fromDateStr);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
			return String.valueOf(sdf.format(fromDate));
		} else if (key.equals(FROMDATE_MMDD)) {
			String fromDateStr = getAtribute(SearchAttributes.FROMDATE);
			Date fromDate = Util.dateParser3(fromDateStr);
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd");
			return String.valueOf(sdf.format(fromDate));
		} else if (key.equals(TODATE_MMDD)) {
			String fromDateStr = getAtribute(SearchAttributes.TODATE);
			Date fromDate = Util.dateParser3(fromDateStr);
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd");
			return String.valueOf(sdf.format(fromDate));
		} else if (key.equals(FROMDATE_MM)) {
			String fromDateStr = getAtribute(SearchAttributes.FROMDATE);
			Date fromDate = Util.dateParser3(fromDateStr);
			SimpleDateFormat sdf = new SimpleDateFormat("MM");
			return String.valueOf(sdf.format(fromDate));
		} else if (key.equals(TODATE_MM)) {
			String fromDateStr = getAtribute(SearchAttributes.TODATE);
			Date fromDate = Util.dateParser3(fromDateStr);
			SimpleDateFormat sdf = new SimpleDateFormat("MM");
			return String.valueOf(sdf.format(fromDate));
		} else if (key.equals(FROMDATE_DD)) {
			String fromDateStr = getAtribute(SearchAttributes.FROMDATE);
			Date fromDate = Util.dateParser3(fromDateStr);
			SimpleDateFormat sdf = new SimpleDateFormat("dd");
			return String.valueOf(sdf.format(fromDate));
		} else if (key.equals(TODATE_DD)) {
			String fromDateStr = getAtribute(SearchAttributes.TODATE);
			Date fromDate = Util.dateParser3(fromDateStr);
			SimpleDateFormat sdf = new SimpleDateFormat("dd");
			return String.valueOf(sdf.format(fromDate));

		} else if (key.equals(FROMDATE_MM_SLASH_DD_SLASH_YYYY)) {
			return new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY).format(Util
					.dateParser3(getAtribute(SearchAttributes.FROMDATE)));
		} else if (key.equals(TODATE_MM_SLASH_DD_SLASH_YYYY)) {
			return new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY).format(Util
					.dateParser3(getAtribute(SearchAttributes.TODATE)));
		} else if (key.equals(FROMDATE_MM_DD_YYYY)) {
			return FormatDate.getDateFormat(FormatDate.PATTERN_MMddyyyy).format(Util
					.dateParser3(getAtribute(SearchAttributes.FROMDATE)));
		} else if (key.equals(TODATE_MM_DD_YYYY)) {
			return FormatDate.getDateFormat(FormatDate.PATTERN_MMddyyyy).format(Util
					.dateParser3(getAtribute(SearchAttributes.TODATE)));
		} else if (key.equals(FROMDATE_MMM_DD_YYYY)) {
			return FormatDate.getDateFormat(FormatDate.PATTERN_MMMddcyyyy).format(Util
					.dateParser3(getAtribute(SearchAttributes.FROMDATE)));
		} else if (key.equals(TODATE_MMM_DD_YYYY)) {
			return FormatDate.getDateFormat(FormatDate.PATTERN_MMMddcyyyy).format(Util
					.dateParser3(getAtribute(SearchAttributes.TODATE)));
		} else if (key.equals(FROMDATE_MM_DD_YY)) {
			return FormatDate.getDateFormat(FormatDate.PATTERN_MMddyy).format(Util
					.dateParser3(getAtribute(SearchAttributes.FROMDATE)));
		} else if (key.equals(TODATE_MM_DD_YY)) {
			return FormatDate.getDateFormat(FormatDate.PATTERN_MMddyy).format(Util
					.dateParser3(getAtribute(SearchAttributes.TODATE)));
		} else if (key.equals(FROMDATE_YYYY_MM_DD)) {
			return FormatDate.getDateFormat(FormatDate.PATTERN_yyyyMMddDash).format(Util
					.dateParser3(getAtribute(SearchAttributes.FROMDATE)));
		} else if (key.equals(TODATE_YYYY_MM_DD)) {
			return FormatDate.getDateFormat(FormatDate.PATTERN_yyyyMMddDash).format(Util
					.dateParser3(getAtribute(SearchAttributes.TODATE)));
		} else if (key.equals(CURRENTDATE_MMDD)) {
			Date fromDate = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd");
			return String.valueOf(sdf.format(fromDate));
		} else if (key.equals(CURRENTDATE_MM)) {
			Date fromDate = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("MM");
			return String.valueOf(sdf.format(fromDate));
		} else if (key.equals(CURRENTDATE_DD)) {
			Date fromDate = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("dd");
			return String.valueOf(sdf.format(fromDate));
		} else if (key.equals(CURRENTDATE_YYYY)) {
			Date fromDate = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
			return String.valueOf(sdf.format(fromDate));
		} else if (key.equals(LIEN_DATE_MMDD)) {
			Search search = InstanceManager.getManager()
					.getCurrentInstance(searchId).getCrtSearchContext();
			Date fromDate = search
					.getTsrViewFilterForProduct(Products.LIENS_PRODUCT);
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd");
			System.err.println("LIEN_DATE_MMDD" + fromDate);
			return String.valueOf(sdf.format(fromDate));
		} else if (key.equals(LIEN_DATE_YYYY)) {
			Search search = InstanceManager.getManager()
					.getCurrentInstance(searchId).getCrtSearchContext();
			Date fromDate = search
					.getTsrViewFilterForProduct(Products.LIENS_PRODUCT);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
			System.err.println("LIEN_DATE_YYYY" + fromDate);
			return String.valueOf(sdf.format(fromDate));
		} else if (key.equals(LAST_REAL_TRANSFER_DATE_MMDD)) {
			Search search = InstanceManager.getManager()
					.getCurrentInstance(searchId).getCrtSearchContext();
			DocumentsManagerI documentsManagerI = search.getDocManager();
			Date fromDate = null;
			try {
				documentsManagerI.getAccess();
				TransferI transfer = documentsManagerI.getLastRealTransfer();
				if (transfer != null) {
					fromDate = transfer.getRecordedDate();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				documentsManagerI.releaseAccess();
			}
			if (fromDate == null) {
				String fromDateStr = getAtribute(SearchAttributes.FROMDATE);
				fromDate = Util.dateParser3(fromDateStr);
			}
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd");
			System.err.println("LAST_REAL_TRANSFER_DATE_MMDD" + fromDate);
			return String.valueOf(sdf.format(fromDate));
		} else if (key.equals(LAST_REAL_TRANSFER_DATE_YYYY)) {
			Search search = InstanceManager.getManager()
					.getCurrentInstance(searchId).getCrtSearchContext();
			DocumentsManagerI documentsManagerI = search.getDocManager();
			Date fromDate = null;
			try {
				documentsManagerI.getAccess();
				TransferI transfer = documentsManagerI.getLastRealTransfer();
				if (transfer != null) {
					fromDate = transfer.getRecordedDate();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				documentsManagerI.releaseAccess();
			}
			if (fromDate == null) {
				String fromDateStr = getAtribute(SearchAttributes.FROMDATE);
				fromDate = Util.dateParser3(fromDateStr);
			}
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
			System.err.println("LAST_REAL_TRANSFER_DATE_YYYY" + fromDate);
			return String.valueOf(sdf.format(fromDate));
		} else if (key.equals(LAST_TRANSFER_DATE_MMDDYYYY)) {
			Search search = InstanceManager.getManager()
					.getCurrentInstance(searchId).getCrtSearchContext();
			DocumentsManagerI documentsManagerI = search.getDocManager();
			Date fromDate = null;
			try {
				documentsManagerI.getAccess();
				TransferI transfer = documentsManagerI.getLastTransfer();
				if (transfer != null) {
					fromDate = transfer.getRecordedDate();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				documentsManagerI.releaseAccess();
			}
			if (fromDate == null) {
				String fromDateStr = getAtribute(SearchAttributes.FROMDATE);
				fromDate = Util.dateParser3(fromDateStr);
			}
			System.err.println("LAST_TRANSFER_DATE_MMDDYYYY" + fromDate);
			return String.valueOf(DATE_FORMAT_MMddyyyy.format(fromDate));
		} else if (key.equals(START_HISTORY_DATE_MM_SLASH_DD_SLASH_YYYY)) {
			return new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY).format(Util
					.dateParser3(getAtribute(SearchAttributes.START_HISTORY_DATE)));
			
		} else if (key.equals(LD_PARCELNO_GENERIC_AO)) {
			
			return getParcelNoGenericAO();

		} else if (key.equals(LD_PARCELNO_GENERIC_TR)) {
			
			return getParcelNoGenericTR();

		} else if (key.equals(LD_PARCELNO_GENERIC_NDB)) {
			return GenericDASLNDB.formatPID(getAtribute(LD_PARCELNO), getCountyId());
			
		} else if (key.equals(LD_PARCELNO_GENERIC_RO)) {
			String pid = getAtribute(LD_PARCELNO);
		    if (getStateCounty().equals("NVClark")) {
		    	pid = pid.replaceAll("[-\\s]", "");
				if (pid.length() == 11) {
					pid = pid.replaceAll(
							"(?is)(\\d{3})(\\d{2})(\\d{3})(\\d{3})",
							"$1-$2-$3-$4");
				}
				return pid;
		    } else if (getStateCounty().equals("OHFranklin")) {
		    	//for RO
				try {
					pid = pid.replaceAll("-", "");
					if (pid.length() > 8) {
						return (pid.substring(0, 3) + "-" + pid.substring(3, 9));
					} else {
						return pid;
					}
				} catch (Exception e) {
					return "";
				}
			}
		} else if (key.equals(LD_PARCELNO_GENERIC_PRI)) {
			
			return getParcelNoGenericPRI();
			
		}else if (key.equals(CURRENT_TAX_YEAR)) {
			Calendar cal = Calendar.getInstance();
			try {
				cal = InstanceManager.getManager().getCurrentInstance(searchId)
						.getCrtSearchContext().getCurrentTaxCalendar();
			} catch (Exception e) {
				logger.error("Cannot get CURRENT_TAX_YEAR for searchId " + searchId, e);
			}
			return cal.get(Calendar.YEAR) + "";
		} else if (key.equals(P_STATE_ABREV)) {
			return FormatSa.getStateNameAbbrev(getAtribute(P_STATE));
		} else if (key.equals(P_COUNTY_FIPS)) {
			return StateCountyManager.getInstance().getCountyFipsForCountyId(
					Long.parseLong(getAtribute(P_STATE)),
					Long.parseLong(getAtribute(P_COUNTY)));
		} else if (key.equals(P_COUNTY_NAME)) {
			return getCountyName();
		} else if (key.equals(LD_PARCELNO_MAP_GENERIC_TR)) {
			String stateCounty = getStateCounty();
			if (stateCounty.equals("TNSumner")) {
				String map = getAtribute(LD_PARCELNO_MAP);
				map = map.replaceAll("^0+", "");
				return map;
			} else if (stateCounty.equals("TNMontgomery")) {
				String map = getAtribute(LD_PARCELNO_MAP);
				map = map.replaceAll("^0+", "");
				return map;
			} else if (stateCounty.equals("TNWarren")) {
				String map = getAtribute(LD_PARCELNO_MAP);
				map = map.replaceAll("^0+", "");
				return map;
			} else if (stateCounty.equals("TNMadison")) {
				String map = getAtribute(LD_PARCELNO_MAP);
				map = map.replaceAll("^0+", "");
				return map;
			} else if (stateCounty.equals("TNSullivan")) {
				String map = getAtribute(LD_PARCELNO_MAP);
				return map;
			}else if (stateCounty.equals("TNWilson")) {
				String map = getAtribute(LD_PARCELNO_MAP);
				return map;
			}
		} else if (key.equals(LD_PARCELNO_CTRL_MAP_GENERIC_TR)){
			String stateCounty = getStateCounty();
			 if (stateCounty.equals("TNSullivan")) {
				 String pin = getAtribute(LD_PARCELNO);
				 String [] parts = pin.split("-");
			    	if (parts.length > 3){
			    		return parts[2];
			    	}
			    return	getAtribute(LD_PARCELNO_MAP);
			}
		}else if (key.equals(LD_PARCELNO_GROUP_GENERIC_TR)){
			String stateCounty = getStateCounty();
			if (stateCounty.equals("TNSumner")) {
				String map = getAtribute(LD_PARCELNO_GROUP);
				map = map.replaceAll("^0+", "");
				return map;
			} else if (stateCounty.equals("TNMontgomery")) {
				String group = getAtribute(LD_PARCELNO_GROUP);
				group = group.replaceAll("^0+", "");
				return group;
			} else if (stateCounty.equals("TNWarren")) {
				String group = getAtribute(LD_PARCELNO_GROUP);
				group = group.replaceAll("^0+", "");
				return group;
			} else if (stateCounty.equals("TNMadison")) {
				String group = getAtribute(LD_PARCELNO_GROUP);
				group = group.replaceAll("^0+", "");
				return group;
			} else if (stateCounty.equals("TNWilson")) {
				String group = getAtribute(LD_PARCELNO_GROUP);
				group = group.replaceAll("^0+", "");
				return group;
			} else if (stateCounty.equals("TNSullivan")){
				String pin = getAtribute(LD_PARCELNO);
				String [] parts = pin.split("-");
					if (parts.length > 3){
			    		return parts[1];
			    	}
				return getAtribute(LD_PARCELNO_GROUP);
			}
		} else if (key.equals(LD_PARCELNO_PARCEL_GENERIC_TR)) {
			String stateCounty = getStateCounty();
			if (stateCounty.equals("TNSumner")) {
				String map = getAtribute(LD_PARCELNO_PARCEL);
				map = map.replaceAll("^0+", "");
				return map;
			} else if (stateCounty.equals("TNMontgomery")) {
				String parcel = getAtribute(LD_PARCELNO_PARCEL);
				parcel = parcel.replaceAll("^0+", "");
				return parcel;
			} else if (stateCounty.equals("TNWilson")) {
				String parcel = getAtribute(LD_PARCELNO_PARCEL);
				parcel = parcel.replaceAll("^0+", "");
				return parcel;
			} else if (stateCounty.equals("TNWarren")) {
				String parcel = getAtribute(LD_PARCELNO_PARCEL);
				parcel = parcel.replaceAll("^0+", "");
				return parcel;
			} else if (stateCounty.equals("TNMadison")) {
				String parcel = getAtribute(LD_PARCELNO_PARCEL);
				parcel = parcel.replaceAll("^0+", "");
				return parcel;
			} else if (stateCounty.equals("TNSullivan")){
				String pin = getAtribute(LD_PARCELNO);
				String [] parts = pin.split("-");
			    	if (parts.length > 3){
			    		return parts[3];
			    	}
			    return	getAtribute(LD_PARCELNO_PARCEL);
			}
		} else if (key.equals(LD_MO_PLATTE_TWN)){
			String pin = getAtribute(LD_PARCELNO);
			pin = pin.replaceAll("(?is)\\p{Punct}", "");
			
	    	if (pin.length() > 15){
	    		return pin.substring(0, 2);
	    	} else{
	    		return pin;
	    	}
		} else if (key.equals(LD_MO_PLATTE_AREA)){
			String pin = getAtribute(LD_PARCELNO);
			pin = pin.replaceAll("(?is)\\p{Punct}", "");
			
	    	if (pin.length() > 15){
	    		return pin.substring(2, 4).replaceAll("(\\d)(\\d)", "$1.$2");
	    	} else{
	    		return pin;
	    	}
		} else if (key.equals(LD_MO_PLATTE_SECT)){
			String pin = getAtribute(LD_PARCELNO);
			pin = pin.replaceAll("(?is)\\p{Punct}", "");
			
	    	if (pin.length() > 15){
	    		return pin.substring(4, 6);
	    	} else{
	    		return pin;
	    	}
		} else if (key.equals(LD_MO_PLATTE_QTRSECT)){
			String pin = getAtribute(LD_PARCELNO);
			pin = pin.replaceAll("(?is)\\p{Punct}", "");
			
	    	if (pin.length() > 15){
	    		return pin.substring(6, 9);
	    	} else{
	    		return pin;
	    	}
		} else if (key.equals(LD_MO_PLATTE_BLOCK)){
			String pin = getAtribute(LD_PARCELNO);
			pin = pin.replaceAll("(?is)\\p{Punct}", "");
			
	    	if (pin.length() > 15){
	    		return pin.substring(9, 12);
	    	} else{
	    		return pin;
	    	}
		} else if (key.equals(LD_MO_PLATTE_PARCEL)){
			String pin = getAtribute(LD_PARCELNO);
			pin = pin.replaceAll("(?is)\\p{Punct}", "");
			
	    	if (pin.length() > 17){
	    		return pin.substring(12, 18).replaceAll("(\\d{3})(\\d{3})", "$1.$2");
	    	} else{
	    		return pin;
	    	}
		}

		// 1234-567-8901-234/5
		if (key.equals(TN_MONTGOMERY_EP_PID)) {
			String pid = getAtribute(LD_PARCELNO);
			return TNGenericMSServiceCT.convertToEpPid(pid);

		}
		if (key.equals(LD_FL_HERNANDO_SECTION)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid.length() == 19) {
				return pid.substring(0, 2);
			} else {
				return "";
			}

		}
		if (key.equals(LD_FL_HERNANDO_BOOK)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid.length() == 19) {
				return pid.substring(2, 3);
			} else {
				return "";
			}

		}
		if (key.equals(LD_FL_HERNANDO_TOWNSHIP)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid.length() == 19) {
				return pid.substring(3, 5);
			} else {
				return "";
			}

		}
		if (key.equals(LD_FL_HERNANDO_RANGE)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid.length() == 19) {
				return pid.substring(5, 7);
			} else {
				return "";
			}

		}
		if (key.equals(LD_FL_HERNANDO_SUBDIVIZION)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid.length() == 19) {
				return pid.substring(7, 11);
			} else {
				return "";
			}

		}
		if (key.equals(LD_FL_HERNANDO_BLOCK)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid.length() == 19) {
				return pid.substring(11, 15);
			} else {
				return "";
			}

		}
		if (key.equals(LD_FL_HERNANDO_LOT)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid.length() == 19) {
				return pid.substring(15, 19);
			} else {
				return "";
			}

		}
		if (key.equals(LD_TN_SHELBY_WARD)) {
			try {
				String pid = TNShelbyTRAOtoTRPID();
				if (pid.length() != 14) {
					return "";
				} else {
					return pid.substring(0, 3);
				}
			} catch (Exception e) {
				return "";
			}
		}
		if (key.equals(LD_TN_SHELBY_BLOCK)) {
			try {
				String pid = TNShelbyTRAOtoTRPID();
				if (pid.length() != 14) {
					return "";
				} else {
					return pid.substring(3, 7);
				}
			} catch (Exception e) {
				return "";
			}
		}
		if (key.equals(LD_TN_SHELBY_SUB)) {
			try {
				String pid = TNShelbyTRAOtoTRPID();
				if (pid.length() != 14) {
					return "";
				} else {
					return pid.substring(7, 8);
				}
			} catch (Exception e) {
				return "";
			}
		}
		if (key.equals(LD_TN_SHELBY_PARCEL)) {
			try {
				String pid = TNShelbyTRAOtoTRPID();
				if (pid.length() != 14) {
					return "";
				} else {
					return pid.substring(8, 13);
				}
			} catch (Exception e) {
				return "";
			}
		}
		if (key.equals(LD_TN_SHELBY_TAG)) {
			try {
				String pid = TNShelbyTRAOtoTRPID();
				if (pid.length() != 14) {
					return "";
				} else {
					return pid.substring(13, 14);
				}
			} catch (Exception e) {
				return "";
			}
		}
		if (key.equals(LD_TN_WILLIAMSON_Ctl1)) {
			try {
				String pid = getAtribute(LD_PARCELNO).replaceAll("-", "");
				boolean hasShortFormat = false;
				if (pid.matches("(?is)[\\dA-Z]{6,8}\\.\\d{2}(?:[A-Z]?\\d{3})?")) {
					hasShortFormat = true;
				}
				
				if (pid.contains(" ") || hasShortFormat) {
					return ro.cst.tsearch.servers.types.TNWilliamsonTR.splitPin(pid)[0];
				} else if (!pid.contains(" ") && pid.length() > 6) {
					//return pid.substring(0, 3).replaceFirst("^0+", "");
					return pid.substring(0, 3);
				} else {
					return "";
				}
			} catch (Exception e) {
				return "";
			}
		}
		if (key.equals(LD_TN_WILLIAMSON_Ctl2)) {
			try {
				String pid = getAtribute(LD_PARCELNO).replaceAll("-", "");
				boolean hasShortFormat = false;
				if (pid.matches("(?is)[\\dA-Z]{6,8}\\.\\d{2}(?:[A-Z]?\\d{3})?")) {
					hasShortFormat = true;
				}
				
				if (pid.contains(" ") || hasShortFormat)
					return ro.cst.tsearch.servers.types.TNWilliamsonTR.splitPin(pid)[1];
				if (pid.matches("(?is)\\d+\\s*[A-Z].*")) {
					pid = pid.replaceAll("(?is)\\A\\d+\\s*([A-Z]).*", "$1");
					return pid;
				} else {
					return "";
				}
			} catch (Exception e) {
				return "";
			}
		}
		if (key.equals(LD_TN_WILLIAMSON_GROUP)) {
			try {
				String pid = getAtribute(LD_PARCELNO).replaceAll("-", "");
				boolean hasShortFormat = false;
				if (pid.matches("(?is)[\\dA-Z]{6,8}\\.\\d{2}(?:[A-Z]?\\d{3})?")) {
					hasShortFormat = true;
				}
				
				if (pid.contains(" ") || hasShortFormat)
					return ro.cst.tsearch.servers.types.TNWilliamsonTR.splitPin(pid)[2];
				if (pid.matches("(?is)\\d+\\s*[A-Z]\\s*[A-Z].*")) {
					pid = pid.replaceAll("(?is)\\A\\d+\\s*[A-Z]\\s*([A-Z]).*", "$1");
					return pid;
				} else {
					return "";
				}
			} catch (Exception e) {
				return "";
			}
		}
		if (key.equals(LD_TN_WILLIAMSON_PARCEL)) {
			try {
				String pid = getAtribute(LD_PARCELNO).replaceAll("-", "");
				boolean hasShortFormat = false;
				if (pid.matches("(?is)[\\dA-Z]{6,8}\\.\\d{2}(?:[A-Z]?\\d{3})?")) {
					hasShortFormat = true;
				}
				
				if (pid.contains(" ") || hasShortFormat) {
					return ro.cst.tsearch.servers.types.TNWilliamsonTR.splitPin(pid)[3];
				} else if (!pid.contains(" ")) {
					if (pid.matches("(?is)\\d+[A-Z][A-Z].*")) {
						pid = pid.replaceAll("(?is)\\A\\d+[A-Z][A-Z](\\d{3}).(\\d{2}).*", "$1.$2").replaceFirst("^0+", "");
						return pid;
					} else {
						if (pid.length() > 8) {
							if (!pid.contains(".")) {
								return pid.substring(4, 9).replaceAll("(?is)(\\d{3})(\\d{2})", "$1.$2");
							} else {
								return pid.substring(3, 9).replaceAll("(?is)(\\d{3})(\\d{2})", "$1.$2");
							}
							
						} else if (pid.length() == 8) {
							return pid.substring(3, pid.length()).replaceAll("(?is)(\\d{3})(\\d{2})", "$1.$2");
						} else {
							return "";
						}
					}
				}
			} catch (Exception e) {
				return "";
			}
			return "";
		}
		if (key.equals(LD_TN_WILLIAMSON_ID)) {
			try {
				String pid = getAtribute(LD_PARCELNO).replaceAll("-", "");
				boolean hasShortFormat = false;
				if (pid.matches("(?is)[\\dA-Z]{6,8}\\.\\d{2}(?:[A-Z]?\\d{3})?")) {
					hasShortFormat = true;
				}
				
				if (pid.contains(" ") || hasShortFormat) 
					return ro.cst.tsearch.servers.types.TNWilliamsonTR.splitPin(pid)[4];
				else if (pid.matches("(?is)\\d+\\s*(?:[A-Z]\\s*[A-Z]\\s*)[\\d\\.]+[A-Z].*")) {
					pid = pid.replaceAll("(?is)\\d+\\s*(?:[A-Z]\\s*[A-Z]\\s*)[\\d\\.]+([A-Z]).*", "$1");
					return pid;
				} else {
					return "";
				}
			} catch (Exception e) {
				return "";
			}
		}
		if (key.equals(LD_TN_WILLIAMSON_SI)) {
			try {
				String pid = getAtribute(LD_PARCELNO).replaceAll("-", "");
				boolean hasShortFormat = false;
				if (pid.matches("(?is)[\\dA-Z]{6,8}\\.\\d{2}(?:[A-Z]?\\d{3})?")) {
					hasShortFormat = true;
				}
				
				if (pid.contains(" ") || hasShortFormat) {
					return ro.cst.tsearch.servers.types.TNWilliamsonTR.splitPin(pid)[5];
				} else if (!pid.contains(" ")) {
					if (pid.matches("(?is)\\d+[A-Z][A-Z].*(?:[A-Z])?.*")) {
						pid = pid.replaceAll("(?is)\\d+[A-Z][A-Z]\\d{1,3}.\\d{2}(\\d|[A-Z])?(\\d+)?", "$2");
						if ("".equals(pid)) {
							pid = "0";
						}
						return pid;
					} else {
						if (pid.length() > 10) {
							return pid.substring(10, pid.length());
						}
					}
				}
			} catch (Exception e) {
				logger.error("Error getting LD_TN_WILLIAMSON_SI", e);
			}
			return "";
		}
		
		if (key.equals(LD_TN_WILLIAMSON_YB_Ctl1)) {
			try {
				String pid = getAtribute(LD_PARCELNO);
				if (pid.contains(" ") || pid.contains("-")) {
						return ro.cst.tsearch.servers.types.TNWilliamsonYB.splitAPN(pid)[0];
				} else if (!pid.contains(" ")) {
					return pid.substring(0, 3);
				} else {
					return "";
				}
			} catch (Exception e) {
				return "";
			}
		}
		if (key.equals(LD_TN_WILLIAMSON_YB_Ctl2)) {
			try {
				String pid = getAtribute(LD_PARCELNO);
				if (pid.contains(" ") || pid.contains("-")) {
						return ro.cst.tsearch.servers.types.TNWilliamsonYB.splitAPN(pid)[1];
				} else {
					return "";
				}
			} catch (Exception e) {
				return "";
			}
		}
		if (key.equals(LD_TN_WILLIAMSON_YB_Group)) {
			try {
				String pid = getAtribute(LD_PARCELNO);
				if (pid.contains(" ") || pid.contains("-")) {
						return ro.cst.tsearch.servers.types.TNWilliamsonYB.splitAPN(pid)[2];
				} else {
					return "";
				}
			} catch (Exception e) {
				return "";
			}
		}
		if (key.equals(LD_TN_WILLIAMSON_YB_Parcel)) {
			try {
				String pid = getAtribute(LD_PARCELNO);
				if (pid.contains(" ") || pid.contains("-")) {
						return ro.cst.tsearch.servers.types.TNWilliamsonYB.splitAPN(pid)[3];
				} else {
					return "";
				}
			} catch (Exception e) {
				return "";
			}
		}
		if (key.equals(LD_TN_WILLIAMSON_YB_Id)) {
			try {
				String pid = getAtribute(LD_PARCELNO);
				if (pid.contains(" ") || pid.contains("-")) {
						return ro.cst.tsearch.servers.types.TNWilliamsonYB.splitAPN(pid)[4];
				} else {
					return "";
				}
			} catch (Exception e) {
				return "";
			}
		}
		if (key.equals(LD_TN_WILLIAMSON_YB_Si)) {
			try {
				String pid = getAtribute(LD_PARCELNO);
				if (pid.contains(" ") || pid.contains("-")) {
						return ro.cst.tsearch.servers.types.TNWilliamsonYB.splitAPN(pid)[5];
				} 
			} catch (Exception e) {
				logger.error("Error getting LD_TN_WILLIAMSON_SI", e);
			}
			return "";
		}		
		
		if (key.equals(LD_TN_WILLIAMSON_YC_Ctl1)) {
			try {
				String pid = getAtribute(LD_PARCELNO);
				if (pid.contains(" ") || pid.contains("-")) {
						return ro.cst.tsearch.servers.types.TNWilliamsonYC.splitPin(pid)[0];
				} else if (!pid.contains(" ")) {
					return pid.substring(0, 3);
				} else {
					return "";
				}
			} catch (Exception e) {
				return "";
			}
		}
		if (key.equals(LD_TN_WILLIAMSON_YC_Ctl2)) {
			try {
				String pid = getAtribute(LD_PARCELNO);
				if (pid.contains(" ") || pid.contains("-")) {
						return ro.cst.tsearch.servers.types.TNWilliamsonYC.splitPin(pid)[1];
				} else {
					return "";
				}
			} catch (Exception e) {
				return "";
			}
		}
		if (key.equals(LD_TN_WILLIAMSON_YC_Group)) {
			try {
				String pid = getAtribute(LD_PARCELNO);
				if (pid.contains(" ") || pid.contains("-")) {
						return ro.cst.tsearch.servers.types.TNWilliamsonYC.splitPin(pid)[2];
				} else {
					return "";
				}
			} catch (Exception e) {
				return "";
			}
		}
		if (key.equals(LD_TN_WILLIAMSON_YC_Parcel)) {
			try {
				String pid = getAtribute(LD_PARCELNO);
				if (pid.contains(" ") || pid.contains("-")) {
						return ro.cst.tsearch.servers.types.TNWilliamsonYC.splitPin(pid)[3];
				} else {
					return "";
				}
			} catch (Exception e) {
				return "";
			}
		}
		if (key.equals(LD_TN_WILLIAMSON_YC_Id)) {
			try {
				String pid = getAtribute(LD_PARCELNO);
				if (pid.contains(" ") || pid.contains("-")) {
						return ro.cst.tsearch.servers.types.TNWilliamsonYC.splitPin(pid)[4];
				} else {
					return "";
				}
			} catch (Exception e) {
				return "";
			}
		}
		if (key.equals(LD_TN_WILLIAMSON_YC_Si)) {
			try {
				String pid = getAtribute(LD_PARCELNO);
				if (pid.contains(" ") || pid.contains("-")) {
						return ro.cst.tsearch.servers.types.TNWilliamsonYC.splitPin(pid)[5];
				} 
			} catch (Exception e) {
				logger.error("Error getting LD_TN_WILLIAMSON_SI", e);
			}
			return "";
		}
		
		if (key.equals(LD_MD_DISTRICT)) {
			try {
				String pid = getAtribute(LD_PARCELNO);
				if (pid.length() == 8) {
					return pid.substring(0, 2);
				} else {
					return "";
				}
			} catch (Exception e) {
				return "";
			}
		}
		if (key.equals(LD_MD_ACCOUNT)) {
			try {
				String pid = getAtribute(LD_PARCELNO);
				if (pid.length() == 8) {
					return pid.substring(2, 8);
				} else {
					return "";
				}
			} catch (Exception e) {
				return "";
			}
		}
		
		if (key.equals(LD_AO_FRANKLIN_TAX_DISTRICT)) {
			try {
				String pid = getAtribute(LD_PARCELNO);
				pid = pid.replaceAll("-", "");
				if (pid.length() < 11) {
					if (pid.length() == 9){
						return pid.substring(0, 3);
					}
					return "";
				} else {
					return pid.substring(0, 3);
				}
			} catch (Exception e) {
				return "";
			}
		}
		if (key.equals(LD_AO_FRANKLIN_PARCEL_NO)) {
			try {
				String pid = getAtribute(LD_PARCELNO);
				pid = pid.replaceAll("-", "");
				if (pid.length() < 11) {
					if (pid.length() == 9){
						return pid.substring(3, 9);
					}
					return "";
				} else {
					return pid.substring(3, 9);
				}
			} catch (Exception e) {
				return "";
			}
		}

		// LD_FL_HERNANDO_SECTION
		// LD_FL_HERNANDO_BOOK
		// LD_FL_HERNANDO_TOWNSHIP
		// LD_FL_HERNANDO_RANGE
		// LD_FL_HERNANDO_SUBDIVIZION
		// LD_FL_HERNANDO_BLOCK
		// LD_FL_HERNANDO_LOT

		if (key.equals(POOR_SEARCH_DATA)) {
			String value = (String) hashSearch.get("POOR_SEARCH_DATA");
			return (value != null) ? value : "";
		}

		if (key.equals(P_STREET_POST_DIRECTION)) {
			String value = (String) hashSearch.get(P_STREET_POST_DIRECTION);
			return (value != null) ? value : "";
		}

		if (key.equals(LD_NCB_NO)) {
			String value = (String) hashSearch.get(LD_NCB_NO);
			return (value != null) ? value : "";
		}

		if (key.equals(LD_ABS_NO)) {
			String value = (String) hashSearch.get(LD_ABS_NO);
			return (value != null) ? value : "";
		}
		
		if (key.equals(LD_DISTRICT)) {
			String value = (String) hashSearch.get(LD_DISTRICT);
			return (value != null) ? value : "";
		}

		if (key.equals(SEARCH_ORIGIN)) {
			String value = (String) hashSearch.get("SEARCH_ORIGIN");
			return (value != null) ? value : "";
		}
		if (key.equals(SEARCH_STEWART_TYPE)) {
			String value = (String) hashSearch.get("SEARCH_STEWART_TYPE");
			return (value != null) ? value : "";
		}
		if (key.equals(ADDITIONAL_SEARCH_TYPE)) {
			String value = (String) hashSearch.get("ADDITIONAL_SEARCH_TYPE");
			return (value != null) ? value : "";
		}
		if (key.equals(OTHER_RESULTS)) {
			String value = (String) hashSearch.get(OTHER_RESULTS);
			return (value != null) ? value : "";
		}
		if (key.equals(TITLEDESK_ORDER_ID)) {
			String value = (String) hashSearch.get("TITLEDESK_ORDER_ID");
			return (value != null) ? value : "";
		}
		if (key.equals(ECORE_AGENT_ID)) {
			String value = (String) hashSearch.get(ECORE_AGENT_ID);
			return (value != null) ? value : "";
		}
		if (key.equals(TITLE_UNIT)) {
			String value = (String) hashSearch.get(TITLE_UNIT);
			return (value != null) ? value : "";
		}
		if (key.equals(ADDITIONAL_LENDER_LANGUAGE)) {
			String value = (String) hashSearch.get(ADDITIONAL_LENDER_LANGUAGE);
			return (value != null) ? value : "";
		}
		if (key.equals(P_STREETUNIT_CLEANED)) {
			String value = (String) hashSearch.get(P_STREETUNIT);
			value = value.trim().toUpperCase();

			// remove CONDOMINIUM, TOWNHOUSE, UNIT, # and all words of > 3
			// letters
			// value = value.replaceAll("COND?O?M?I?N?I?U?M?", "");
			// value = value.replaceAll("TOWN?H?O?M?E", "");
			// value = value.replaceAll("UNIT", "");

			value = value.replaceAll("[A-Z]{3,}", "");
			value = value.replaceAll("\\s{2,}", " ");
			value = value.replaceAll("#", "");

			value = value.trim();
			return value;
		}
		if (key.equals(LD_FL_PALM_BEACH_CITY)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid == null || pid.length() == 0)
				return "";
			pid = pid.replaceAll("-", "");
			if (pid.length() != 17)
				return "";
			return pid.substring(0, 2);
		}
		if (key.equals(LD_FL_PALM_BEACH_RANGE)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid == null || pid.length() == 0)
				return "";
			pid = pid.replaceAll("-", "");
			if (pid.length() != 17)
				return "";
			return pid.substring(2, 4);
		}

		if (key.equals(LD_FL_PALM_BEACH_TOWNSHIP)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid == null || pid.length() == 0)
				return "";
			pid = pid.replaceAll("-", "");
			if (pid.length() != 17)
				return "";
			return pid.substring(4, 6);
		}

		if (key.equals(LD_FL_PALM_BEACH_SECTION)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid == null || pid.length() == 0)
				return "";
			pid = pid.replaceAll("-", "");
			if (pid.length() != 17)
				return "";
			return pid.substring(6, 8);
		}

		if (key.equals(LD_FL_PALM_BEACH_SUBDIVISION)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid == null || pid.length() == 0)
				return "";
			pid = pid.replaceAll("-", "");
			if (pid.length() != 17)
				return "";
			return pid.substring(8, 10);
		}
		if (key.equals(LD_FL_PALM_BEACH_BLOCK)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid == null || pid.length() == 0)
				return "";
			pid = pid.replaceAll("-", "");
			if (pid.length() != 17)
				return "";
			return pid.substring(10, 13);
		}
		if (key.equals(LD_FL_PALM_BEACH_LOT)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid == null || pid.length() == 0)
				return "";
			pid = pid.replaceAll("-", "");
			if (pid.length() != 17)
				return "";
			return pid.substring(13);
		}

		if (key.equals(LD_FL_PINELLAS_SECTION)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid == null || pid.length() == 0)
				return "";
			pid = pid.replaceAll("-", "");
			if (pid.length() != 18)
				return "";
			return pid.substring(0, 2);
		}
		if (key.equals(LD_FL_PINELLAS_TOWNSHIP)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid == null || pid.length() == 0)
				return "";
			pid = pid.replaceAll("-", "");
			if (pid.length() != 18)
				return "";
			return pid.substring(2, 4);
		}

		if (key.equals(LD_FL_PINELLAS_RANGE)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid == null || pid.length() == 0)
				return "";
			pid = pid.replaceAll("-", "");
			if (pid.length() != 18)
				return "";
			return pid.substring(4, 6);
		}

		if (key.equals(LD_FL_PINELLAS_SUBDIVISION)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid == null || pid.length() == 0)
				return "";
			pid = pid.replaceAll("-", "");
			if (pid.length() != 18)
				return "";
			return pid.substring(6, 11);
		}

		if (key.equals(LD_FL_PINELLAS_BLOCK)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid == null || pid.length() == 0)
				return "";
			pid = pid.replaceAll("-", "");
			if (pid.length() != 18)
				return "";
			return pid.substring(11, 14);
		}
		if (key.equals(LD_FL_PINELLAS_LOT)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid == null || pid.length() == 0)
				return "";
			pid = pid.replaceAll("-", "");
			if (pid.length() != 18)
				return "";
			return pid.substring(14, 18);
		}
		if (key.equals(LD_FL_PINELLAS_LOT)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid == null || pid.length() == 0)
				return "";
			pid = pid.replaceAll("-", "");
			if (pid.length() != 17)
				return "";
			return pid.substring(13);
		}
		if (key.equals(LD_CO_PARCEL_NO)) {
			String pin = getAtribute(LD_PARCELNO);
			String state = getAtribute(P_STATE);
			String county = getAtribute(P_COUNTY);
			if (pin == null || pin.length() == 0)
				return "";
			if (pin.length() == 12 && "3407".equals(county)
					&& "6".equals(state)) {// COAdamsTR
				pin = "0" + pin;
			}
			if (pin.length() != 13) {
				if (!pin.matches("\\d{8}"))
					return "";
			}

			return pin;
		} else if (key.equals(LD_CO_ACCOUNT_NO)) {
			String pin = getAtribute(LD_PARCELNO);
			if (pin == null || pin.length() == 0)
				return "";
			if (pin.length() != 8)
				return "";
			return pin;
		}

		if (key.equals(LD_FL_ORANGE_SECTION)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid == null || pid.length() == 0)
				return "";
			String[] array = pid.split("-");
			if (array.length > 0)
				return array[0].trim();
			return "";
		} else if (key.equals(LD_FL_ORANGE_TOWNSHIP)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid == null || pid.length() == 0)
				return "";
			String[] array = pid.split("-");
			if (array.length > 1)
				return array[1].trim();
			return "";
		} else if (key.equals(LD_FL_ORANGE_RANGE)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid == null || pid.length() == 0)
				return "";
			String[] array = pid.split("-");
			if (array.length > 2)
				return array[2].trim();
			return "";
		} else if (key.equals(LD_FL_ORANGE_SUBDIVISION)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid == null || pid.length() == 0)
				return "";
			String[] array = pid.split("-");
			if (array.length > 3)
				return array[3].trim();
			return "";
		} else if (key.equals(LD_FL_ORANGE_BLOCK)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid == null || pid.length() == 0)
				return "";
			String[] array = pid.split("-");
			if (array.length > 4) {
				if (array.length > 5)
					return array[4].trim();
				String temp = array[4].trim();
				if (temp.length() >= 2)
					return temp.substring(0, 2);
				else
					return temp;
			}
			return "";
		} else if (key.equals(LD_FL_ORANGE_LOT)) {
			String pid = getAtribute(LD_PARCELNO);
			if (pid == null || pid.length() == 0)
				return "";
			String[] array = pid.split("-");
			if (array.length > 4) {
				if (array.length > 5)
					return array[5].trim();
				String temp = array[4].trim();
				if (temp.length() >= 2)
					return temp.substring(2);
			}
			return "";
		}

		if (key.equals(P_STREETDIRECTION_ABBREV)) {
			String dir = (String) hashSearch.get(P_STREETDIRECTION);
			if (dir == null) {
				return "";
			}
			dir = dir.replaceAll("\\s+", "").toUpperCase();
			if ("".equals(dir)) {
				return "";
			}
			String retVal = dirAbbreviations.get(dir);
			if (retVal == null) {
				retVal = dir.substring(0, 1);
			}
			return retVal;
		}
		if (key.equals(P_STREETSUFIX_ABBREV)) {
			String streetSuffix = (String) hashSearch.get(P_STREETSUFIX);
			if (streetSuffix!=null) {
				String stateCounty = getStateCounty();
				if (stateCounty.equals("FLMiami-Dade")) {
					String ret = Normalize.translateSuffix(streetSuffix.toUpperCase());
					if (ret!=null) {
						return ret;
					}
				} 
				return streetSuffix;
			}
			return ""; 
		}
		if (key.equals(P_STREET_NO_NAME)) {
			String no = (String) hashSearch.get(P_STREETNO);
			String name = (String) hashSearch.get(P_STREETNAME);
			String val = "";
			if (!"".equals(no)) {
				val = no + " ";
			}
			if (!"".equals(name)) {
				val += name;
			}
			val = val.trim();
			return val;
		} else if (key.equals(P_STREET_NO_NAME_NO_SPACE)) {
			String no = (String) hashSearch.get(P_STREETNO);
			String name = (String) hashSearch.get(P_STREETNAME);
			String val = "";
			if (!"".equals(no)) {
				val = no;
			}
			if (!"".equals(name)) {
				val += name.replaceAll("[\\s-]", "");
			}
			val = val.trim();
			return val;
		} else if (key.equals(P_STREET_NO_DIR_NAME_POSTDIR)) {
			String no = (String) hashSearch.get(P_STREETNO);
			String dir = (String) hashSearch.get(P_STREETDIRECTION);
			String name = (String) hashSearch.get(P_STREETNAME);
			String postdir = (String) hashSearch.get(P_STREET_POST_DIRECTION);
			String val = "";
			if (!"".equals(no)) {
				val = no + " ";
			}
			if (!"".equals(dir)) {
				val += dir + " ";
			}
			if (!"".equals(name)) {
				val += name + " ";
			}
			if (!"".equals(postdir)) {
				val += postdir;
			}
			val = val.trim();
			return val;
		} else if (key.equals(P_STREET_NA_SU_NO)) {
			String no = (String) hashSearch.get(P_STREETNO);
			String name = (String) hashSearch.get(P_STREETNAME);
			String suffix = (String) hashSearch.get(P_STREETSUFIX);
			String val = "";
			if (!"".equals(name)) {
				val = name;
			}
			if (!"".equals(suffix)) {
				val += " " + Normalize.translateSuffix(suffix.toUpperCase());
			}
			if (!"".equals(no)) {
				val += " " + no;
			}
			val = val.trim();
			return val;
		}

		// bug #531
		if (key.equals("LD_SUBDIV_NAME_MOJACKSONRO")) {
			String moJacksonROsubdiv = (String) hashSearch
					.get(LD_SUBDIV_NAME_MOJACKSONRO);
			if (!"".equals(moJacksonROsubdiv)) {
				return moJacksonROsubdiv;
			} else {
				return (String) hashSearch.get(LD_SUBDIV_NAME);
			}
		}

		/*
		 * if trying to get the SEARCHUPDATE, then compute it from
		 * SEARCH_PRODUCT
		 */
		if (key.equals(LD_PARCELNO_FULL)) {
			return getAtribute(LD_PARCELNO_PREFIX) + getAtribute(LD_PARCELNO);
		}

		if (key.equals(SEARCHUPDATE)) {
			try {
				if (Integer.parseInt(getAtribute(SEARCH_PRODUCT)) == SEARCH_PROD_UPDATE) {
					return "true";
				} else {
					return "false";
				}
			} catch (Exception e) {
			}
		} else if (key.equals(FVS_UPDATE)) {
			String value = (String) hashSearch.get(FVS_UPDATE);
			return (value != null) ? value : "false";
		} else if (key.equals(LAST_SCHEDULED_FVS_UPDATE)) {
			String value = (String) hashSearch.get(LAST_SCHEDULED_FVS_UPDATE);
			return (value != null) ? value : "false";
		} else if (key.equals(FVS_UPDATE_AUTO_LAUNCHED)){
			String value = (String) hashSearch.get(FVS_UPDATE_AUTO_LAUNCHED);
			return (value != null) ? value : "false";
		} else if (key.equals(START_HISTORY_DATE)) {
			return INITDATE;
		} else if (key.equals(LD_LOT_SUBDIVISION)) {
			return ((String) hashSearch.get(LD_LOTNO)) + " "
					+ ((String) hashSearch.get(LD_SUBDIV_BLOCK)) + " "
					+ ((String) hashSearch.get(LD_SUBDIV_NAME));
		} else if (key.equals(BLANK_ATTR)) {
			return "";
		} else if (GenericSKLD.mapIdModuleSaKeys.contains(key)) {
			Search search = InstanceManager.getManager()
					.getCurrentInstance(searchId).getCrtSearchContext();
			return org.apache.commons.lang.StringUtils
					.defaultString((String) search.getAdditionalInfo(key));
		} else if (GenericServerADI.mapIdModuleSaKeys.contains(key)) {
			Search search = InstanceManager.getManager()
					.getCurrentInstance(searchId).getCrtSearchContext();
			return org.apache.commons.lang.StringUtils
					.defaultString((String) search.getAdditionalInfo(key));
		} else if (GenericPI.mapIdModuleSaKeys.contains(key)) {
			Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			return org.apache.commons.lang.StringUtils.defaultString((String) search.getAdditionalInfo(key));
		}

		if (!hashSearch.containsKey(key)) {
			
			switch (key) {
			case POOR_SEARCH_DATA:
			case ADDITIONAL_REQUIREMENTS:
			case LEGAL_DESCRIPTION:
			case ADDITIONAL_EXCEPTIONS:
			case LD_PARCELNO_MAP:
			case LD_PARCELNO_GROUP:
			case LD_PARCELNO_PARCEL:
			case LD_PARCELNO_CTRL_MAP:
			case LD_PARCELNO_CONDO:
			case LD_PARCELNO_PREFIX:
			case P_MUNICIPALITY:
			case LD_SUBDIV_TWN:
			case LD_SUBDIV_RNG:
			case LD_SECTION:
			case SEARCH_WARNING:
			case OWNER_GUID:
			case BUYER_GUID:
			{
				return "";
			}
			case P_STREET_NO_NA_SU:
				return ((String) hashSearch.get(P_STREETNO)) + " "
						+ ((String) hashSearch.get(P_STREETNAME));
			case P_STREET_FULL_NAME:
				return ((String) hashSearch.get(P_STREETDIRECTION) + " "
						+ (String) hashSearch.get(P_STREETNAME) + " " 
						+ (String) hashSearch.get(P_STREETSUFIX)).trim();
			case P_FULL_ADDRESS_N_D_N_S:
				String result = (String)hashSearch.get(P_STREETNO) + " "
						+ (String)hashSearch.get(P_STREETDIRECTION) + " "
						+ (String)hashSearch.get(P_STREETNAME) + " "
						+ (String)hashSearch.get(P_STREETSUFIX);
				
				return result.replaceAll("\\s{2,}", " ").trim();
			case P_STREET_FULL_NAME_NO_SUFFIX:
				return ((String) hashSearch.get(P_STREETDIRECTION) + " " 
						+ (String) hashSearch.get(P_STREETNAME)).trim();
			case P_STREET_NO_STAR_NA_STAR:
				String streetName = ((String) hashSearch.get(P_STREETNAME)).trim();
				if (!streetName.equals("")) {
					streetName = " *" + streetName + "*";
				}
				return ((String) hashSearch.get(P_STREETNO) + streetName).trim();
			case ABSTRACTOR_EMAIL:
				return getAbstractorObject().getEMAIL();
			case LD_LOT_SUBDIVISION:
				return ((String) hashSearch.get(LD_LOTNO)) + " "
						+ ((String) hashSearch.get(LD_SUBDIV_BLOCK)) + " "
						+ ((String) hashSearch.get(LD_SUBDIV_NAME));
			case P_STREET_FULL_NAME_EX:
				return ((String) hashSearch.get(P_STREETNO) + " "
						+ (String) hashSearch.get(P_STREETDIRECTION) + " " 
						+ (String) hashSearch.get(P_STREETNAME)).trim().replaceAll("\\s\\s", " ");
			case P_STREETNAME_SUFFIX_UNIT_NO:
				return (((String) hashSearch.get(P_STREETDIRECTION) + " " 
						+ (String) hashSearch.get(P_STREETNAME)).trim() + " "
						+ (String) hashSearch.get(P_STREETSUFIX) + " "
						+ (String) hashSearch.get(P_STREETUNIT) + " " 
						+ (String) hashSearch.get(P_STREETNO)).replaceAll("\\s+", " ").trim();
			case LD_PARCELNO_PARCEL_CONDO:
				return (String) hashSearch.get(LD_PARCELNO_PARCEL) + "   "
						+ (String) hashSearch.get(LD_PARCELNO_CONDO);
			
			case OWNER_LAST_NAME:
			case OWNER_FIRST_NAME:
			case OWNER_MIDDLE_NAME:
			case OWNER_COMPANY_NAME:
				
			case OWNER_LNAME:
			case OWNER_FNAME:
			case OWNER_MNAME:
			case OWNER_MINITIAL:
			case BUYER_LNAME:
			case BUYER_FNAME:
			case BUYER_MNAME:
				
			case OWNER_LF_NAME:
			case OWNER_FULL_NAME:
			case BUYER_FULL_NAME:
			case OWNER_LCFM_NAME:
			case OWNER_LCF_NAME: 
			case OWNER_LFM_NAME:
			case OWNER_FML_NAME:
			{
				return getNameTokenFromOwnersList(key);
			}
			case LEGAL_DESCRIPTION_STATUS:
			case ASSESSED_VALUE:
				return "0";
			default:
				break;
			}
			
			if (key.endsWith(LD_PARCELNO3) || key.endsWith(LD_BOOKNO_1)
					|| key.endsWith(LD_PAGENO_1) || key.endsWith(QUARTER_ORDER)
					|| key.endsWith(QUARTER_VALUE)
					|| key.endsWith(LD_PARCELNO2)
					|| key.endsWith(LD_PARCELNONDB)
					|| key.endsWith(LD_PARCELNO2_ALTERNATE)
					|| key.equals(LD_GEO_NUMBER) || key.equals(IS_CONDO)
					|| key.equals(IS_UPDATED_ONCE) || key.equals(IS_PLATED) || key.equals(IS_PRODUCT_CHANGED_ONCE)
					|| key.equals(IS_UPDATE_OF_A_REFINANCE) || key.equals(ORIGINAL_SEARCH_OLDER_THAN_ONE_YEAR)
					|| key.equals(LD_SUBDIV_PARCEL) || key.equals(ARB)
					|| key.equals(ARB_LOT)
					|| key.equals(ARB_BLOCK)
					|| key.equals(ARB_BOOK)
					|| key.equals(ARB_PAGE) || key.equals(LD_AREA)
					|| key.equals(LD_PI_AREA) || key.equals(LD_PI_BLOCK) || key.equals(LD_PI_PARCEL) 
					|| key.equals(LD_PI_SEC) || key.equals(LD_PI_UNIT)
					|| key.equals(LD_IL_WILL_AO_BLOCK) || key.equals(LD_IL_WILL_AO_COMP_CODE) || key.equals(LD_IL_WILL_AO_LOT)
					|| key.equals(LD_IL_WILL_AO_MISC) || key.equals(LD_IL_WILL_AO_SEC) || key.equals(LD_IL_WILL_AO_TWN)
					|| key.equals(LD_ADDITION) || key.equals(WEEK)
					|| key.equals(P_IDENTIFIER_TYPE)
					|| key.equals(P_ORDER_STREETNAME) || key.equals(BUILDING)
					|| key.equals(AGENT_PASSWORD) || key.equals(AGENT_USER)
					|| key.equals(ATS_MULTIPLE_LEGALS_FOUND)
					|| key.equals(ATS_MULTIPLE_LEGAL_INSTRUMENTS)
					|| key.equals(STEWARTORDERS_ORDER_ID)
					|| key.equals(SURECLOSE_FILE_ID)
					|| key.equals(STEWARTORDERS_ORDER_PRODUCT_ID)
					|| key.equals(STEWARTORDERS_CUSTOMER_GUID)
					|| key.equals(STEWARTORDERS_PARENT_ORDER_GUID)
					|| key.equals(STEWARTORDERS_TO_UPDATE_ORDER_GUID)
					|| key.equals(LD_TS_SUBDIV_NAME)
					|| key.equals(LD_TS_PLAT_BOOK)
					|| key.equals(LD_TS_PLAT_PAGE) || key.equals(LD_TS_LOT)
					|| key.equals(LD_TS_BLOCK)) {
				return "";
			}
			else if(key.equals(ATIDS_FILE_REFERENCE_ID)) {
				hashSearch.put(ATIDS_FILE_REFERENCE_ID, "");
				return "";
			}
			else if(key.equals(ATIDS_FILE_REFERENCE_CREATED)) {
				hashSearch.put(ATIDS_FILE_REFERENCE_CREATED, "");
				return "";
			}
			else if (key.endsWith(LD_SUBDIV_NAME_AND_PHASE)) {
				String phase = getAtribute(LD_SUBDIV_PHASE);
				String subdiv = getAtribute(LD_SUBDIV_NAME);
				if (phase != null && phase.length() > 0) {
					return subdiv + " PH " + phase;
				} else {
					return subdiv;
				}
			} else if (key.endsWith(LD_SUBDIV_NAME_AND_PHASE_ROMAN)) {
				String phase = getAtribute(LD_SUBDIV_PHASE);
				String subdiv = getAtribute(LD_SUBDIV_NAME);
				if (phase != null && phase.length() > 0) {
					String result = subdiv;
					try {
						RomanNumeral roman = new RomanNumeral(
								Integer.parseInt(phase));
						result += " PH " + roman.toString();
					} catch (Exception e) {
						loggerDetails
								.error("Exception when parsing number from arabic to roman");
					}
					return result;
				} else {
					return subdiv;
				}
			} else if (key.equals(IGNORE_MNAME)) {
				return "false";
			} else if (key.equals(IGNORE_MNAME_BUYER)) {
				return "false";
			} else if ("".equals(key)) {
				return "";
			} else if (key.equals(SearchAttributes.LD_ACRES)) {
				return "-1";
			} else if (key.equals(SearchAttributes.PROPERTY_TYPE)) {
				return "";
			} else if (key.equals(SearchAttributes.P_IDENTIFIER_TYPE)) {
				return "";
			} else if (key.equals(SearchAttributes.P_ORDER_STREETNAME)) {
				return "";
			} else if (key.equals(SearchAttributes.LD_SUBLOT)) {
					return ""; //old searches do't contain this key
			} else if (HASH_SEARCH_ATTRIBUTES_TO_IGNORE.contains(key)) {
				return "";
			} else {
//				throw new IllegalArgumentException(
//						"Unknown Search atribute!...." + key);
				Log.sendExceptionViaEmail(
						MailConfig.getMailLoggerToEmailAddress(), 
						"GetAtribute with key " + key, 
						null, 
						"SearchId used: " + searchId + ", Key used: " + key);
				return "";
			}
		}
		return (String) hashSearch.get(key);
	}

	protected String getParcelNoGenericPRI() {
		String pid = getAtribute(LD_PARCELNO).trim();
		String countyId = getCountyId();
		if(countyId.equals(CountyConstants.CA_Alameda_STRING)) {
			return getAtribute(LD_PARCELNO_GENERIC_NDB);
		} else if(countyId.equals(CountyConstants.CA_Kern_STRING)) {
			return getAtribute(LD_PARCELNO_GENERIC_NDB);
		} else if(countyId.equals(CountyConstants.CA_San_Bernardino_STRING)) {
			pid = pid.replaceFirst("^(\\d{4}-\\d{3}-\\d{2})-(\\d)-(\\d{3})$", "$1-$2$3");
			return pid;
		} else if(countyId.equals(CountyConstants.CA_San_Francisco_STRING)) {
			return getAtribute(LD_PARCELNO_GENERIC_NDB);
		} else if(countyId.equals(CountyConstants.CA_Siskiyou_STRING)) {
			return getAtribute(LD_PARCELNO_GENERIC_NDB);
		} else if (countyId.equals(CountyConstants.FL_Hendry_STRING)) {
			pid = pid.replaceFirst("^(\\d{7})-([A-Z]\\d{10})$", "$1$2");
			return pid;
		} else if (countyId.equals(CountyConstants.FL_Hernando_STRING)) {
			return getAtribute(LD_PARCELNO_GENERIC_NDB);
		} else if (countyId.equals(CountyConstants.FL_Polk_STRING)) {
			return getAtribute(LD_PARCELNO_GENERIC_NDB);
		} else if(countyId.equals(CountyConstants.KS_Johnson_STRING)) {
			pid = pid.replaceAll("\\s", "");
			return pid;
		} else if(countyId.equals(CountyConstants.TN_Benton_STRING)||
				  countyId.equals(CountyConstants.TN_Chester_STRING)||
				  countyId.equals(CountyConstants.TN_Crockett_STRING)||
				  countyId.equals(CountyConstants.TN_Dyer_STRING)||
				  countyId.equals(CountyConstants.TN_Lake_STRING)) {
			pid = pid.replaceFirst("^(\\d+)--\\d+-([\\d.]+)--\\d+$", "$1-$2");
			pid = pid.replaceFirst("^(\\d+)\\s{2}(\\d{3})(\\d{2})\\s\\d+$", "$1-$2.$3");
			return pid;
		} else if(countyId.equals(CountyConstants.TN_Cocke_STRING)) {
			return getAtribute(LD_PARCELNO_GENERIC_NDB);
		} else if(countyId.equals(CountyConstants.TN_Hardeman_STRING)||
				  countyId.equals(CountyConstants.TN_Obion_STRING)||
				  countyId.equals(CountyConstants.TN_Robertson_STRING)) {
			if (countyId.equals(CountyConstants.TN_Robertson_STRING)) {
				if (pid.matches("(?i)(\\d{3}[A-Z])([A-Z])(\\d{3}\\.\\d{2})")) {
					pid = pid.replaceFirst("(?i)(\\d{3}[A-Z])([A-Z])(\\d{3}\\.\\d{2})", "$1-$2-$3");
					return pid;
				}
			}
			pid = pid.replaceFirst("(?i)(\\d+[A-Z])-([A-Z])-\\d+[A-Z]-([\\d.]+)--\\d+", "$1-$2-$3");
			return pid;
		} else if(countyId.equals(CountyConstants.TN_Sumner_STRING)) {
			pid = pid.replaceFirst("(?i)(\\d+[A-Z])-([A-Z])-\\1-([\\d.]+)--\\d+", "$1-$2-$3");
			pid = pid.replaceFirst("(?i)(\\d+)--\\1-([\\d.]+)--\\d+", "$1-$2");
		} else if(countyId.equals(CountyConstants.TN_Williamson_STRING)) {
			return getAtribute(LD_PARCELNO_GENERIC_NDB);
		}
		return pid;
	}

	protected String getParcelNoGenericTR() {
		String pid = getAtribute(LD_PARCELNO).trim();
		String stateCounty = getStateCounty();
		String countyId = getCountyId();
		if (pid.isEmpty()) {
			return pid;
		}
		
		switch (countyId) {
		case CountyConstants.AL_Montgomery_STRING:
			String resPid = pid.replaceAll("[^\\d]", "");
			
			if(resPid.length() == 17 && (pid.matches("\\d{2}-\\d{2}-\\d{2}-0\\d-\\d{3}-\\d{3}\\.\\d{3}") || pid.matches("\\d{2}\\d{2}\\d{2}0\\d\\d{3}\\d{3}\\.\\d{3}"))){
				resPid = pid.replaceAll("(\\d{2}-\\d{2}-\\d{2}-)0(\\d-\\d{3}-\\d{3}\\.\\d{3})", "$1$2")
						.replaceAll("(\\d{2}\\d{2}\\d{2})0(\\d\\d{3}\\d{3}\\.\\d{3})", "$1$2").replaceAll("[^\\d]", "");
			}
			
			return resPid;
		case CountyConstants.AR_Washington_STRING:
			if (pid.length() == 11) {
				pid = pid.replaceAll("(\\d{3})(\\d{5})(\\d{3})", "$1-$2-$3");
			} 
			break;
		case CountyConstants.CA_San_Bernardino_STRING:
			String patt1 = "^(\\d{4}-\\d{3}-\\d{2})-\\d{4}$";
			String patt2 = "^(\\d{4}-\\d{3}-\\d{2})-\\d-\\d{3}$";
			if (pid.matches(patt1)) {
				pid = pid.replaceFirst(patt1, "$1");
			} else if (pid.matches(patt2)) {
				pid = pid.replaceFirst(patt2, "$1");
			}
			break;
		case CountyConstants.CA_Santa_Clara_STRING:
		case CountyConstants.CO_El_Paso_STRING:
			return pid.replaceAll("[^\\d]", "");
		case CountyConstants.CA_Santa_Cruz_STRING:
			String CASantaCruzPattern = "(\\d{3}-?\\d{3}-?\\d{2})-?000";
			if (pid.matches(CASantaCruzPattern)) {
				// e.g. 045-193-14-000 from PRI will be 045-193-14; but for e.g. 019-273-01-310 will not be changed
				pid = pid.replaceFirst(CASantaCruzPattern, "$1");
			}
			return pid;
		case CountyConstants.CO_Arapahoe_STRING:
			if (!pid.matches("\\d{4}-\\d{2}-\\d-\\d{2}-\\d{3}")) {
				pid = pid.replaceAll("-", "");
				if (pid.matches("\\d{12}")) {
					return pid.substring(0, 4) + "-" + pid.substring(4, 6) + "-"
							+ pid.substring(6, 7) + "-" + pid.substring(7, 9)
							+ "-" + pid.substring(9);
					
				} 
			}
			break;
		case CountyConstants.CO_Mesa_STRING:
			return pid.replaceAll("[-\\s\\.]", "");

		case CountyConstants.KY_Jefferson_STRING:
			String group = (String) hashSearch.get(LD_PARCELNO_GROUP);
			if (org.apache.commons.lang.StringUtils.isNotBlank(group) && pid.length() < 14) {
				return group + pid;
			}
			break;
			
		case CountyConstants.FL_Gilchrist_STRING:
			// e.g. replace 05-07-15-0031-0000-0030 with 050715-00310000-0030
			String correctPid = "";
			if ((correctPid = pid.replaceAll("[\\s-]+", "")).matches("\\d{8,}")) {
				if (correctPid.length() == 18) {
					correctPid = correctPid.replaceFirst(ro.cst.tsearch.servers.types.FLGilchristTR.LARGE_PIN_FORMAT, "$1-$2-$3");
				} else if (correctPid.length() == 8) {
					correctPid = correctPid.replaceFirst(ro.cst.tsearch.servers.types.FLGilchristTR.SMALL_PIN_FORMAT, "$1-$2");
				}
			}
			return correctPid;
			
		case CountyConstants.FL_Hernando_STRING:
			pid = pid.replaceFirst("^R", "");
			if (pid.length() == 19 || pid.replaceAll("\\s", "").length() == 19) {
				pid = pid.replaceAll("\\s", "");
				return "R" + pid.substring(0, 2) + " " + pid.substring(2, 5)
						+ " " + pid.substring(5, 7) + " "
						+ pid.substring(7, 11) + " " + pid.substring(11, 15)
						+ " " + pid.substring(15, 19);
			} 
			break;
		case CountyConstants.FL_Sumter_STRING:
			pid = pid.replaceAll("=", "-");
			if (pid.length() == 6)
				pid = pid.replaceAll("([A-Z]{1}\\d{2})(\\d{3})", "$1-$2");
			if (pid.length() != 7 && pid.length() != 8)
				return "";
			break;
		default:
			break;
		}
		
		
		if (stateCounty.equals("FLAlachua")) {
			pid = pid.replaceAll("\\p{Punct}", "").replaceAll("\\s+", "");
			pid = pid.replaceFirst("(\\d{5})(\\d{3})(\\d{3})", "$1 $2 $3");
			return pid;
		
		} else if (countyId.equals(CountyConstants.FL_Baker_STRING)) {
			pid = pid.replaceAll("-", "");
			return pid;
		
		} else if (countyId.equals(CountyConstants.FL_Charlotte_STRING)) {
			pid = pid.replaceAll("-", "");
			return pid;
		
		} else if (stateCounty.equals("FLCitrus")){
			final int pidLength = pid.length();

			if (pidLength == 21){
				if ("0000".equals(pid.substring(8, 12))){ // RRrTTtSS0000xxxxxxxxx -> RRTTSSxxxxxxxxx
					pid = pid.substring(0, 8) + " " + pid.substring(12);
				} else if ("00000".equals(pid.substring(12, 17))) { // RRrTTtSSxxxx00000xxxx  ->  RRTTSSxxxxxxxx
					pid = pid.substring(0, 12) + " " + pid.substring(17);
				}
			}
			if (pidLength == 17){
				if ("0000".equals(pid.substring(8, 12))){ // RRrTTtSS0000xxxxx -> RRTTSSxxxxx
					pid = pid.substring(0, 8) + " " + pid.substring(12);
				}
			}
			return pid.substring(0, 2) + pid.substring(3, 5) + pid.substring(6); // RRrTTtSSxxxxxxxxxxxxx -> RRTTSSxxxxxxxxxxxxx
			
		} else if (stateCounty.equals("FLColumbia")) {
			pid = pid.replaceAll("\\p{Punct}", "");
			pid = pid.replaceFirst("(?is)[a-z\\d]{6}(\\d{5})(\\d{3})$", "R$1-$2");
			return pid;
			
		} else if (stateCounty.equals("FLDeSoto")){
			if (!pid.contains("-")) {
				pid = pid.replaceAll("(?is)(\\d{2})(\\d{2})(\\d{2})(\\d{4})([A-Z\\d]{4})(\\d{4})", "$1-$2-$3-$4-$5-$6");
			}
			return pid;
			
		} else if (stateCounty.equals("FLHendry")){
			String pidPRI = getAtribute(LD_PARCELNO_GENERIC_PRI).trim();
			
			if (StringUtils.isNotEmpty(pidPRI)) {
				//if (pidPRI.matches("(\\d{1})-(\\d{2})-(\\d{2})-(\\d{2})-(\\d{3})-(\\d{4})-(\\d{4})")) {
				if (pidPRI.matches("(\\d{1})-(\\d{2})-(\\d{2})-(\\d{2})-([A-Z\\d]{3})-([A-Z\\d]{4})-([A-Z\\d]{4})")) {
					pid = pid.replaceFirst("(\\d{1})-(\\d{2})-(\\d{2})-(\\d{2})-([A-Z\\d]{3})-([A-Z\\d]{4})-([A-Z\\d]{4})", "$1" + "$4" + "$3" + "$2" + "-" + "$5" + "$6" + "$7");
					
				} else if (pidPRI.matches("(\\d{7})([A-Z]\\d{10})")) {
					pid = pid.replaceFirst("(\\d{7})([A-Z]\\d{10})", "$1-$2");
					
				} else if (pidPRI.equals(pid) && pid.matches("\\d{18}")) {
					pid = pid.replaceFirst("(\\d{7})(\\d{11})", "$1" + "-" + "$2");
				}
			}
								
			return pid;

		} else if (stateCounty.equals("FLIndian River")){
			return pid.replace(".", "");
			
		} else if (stateCounty.equals("FLJackson")){
			if (pid.length() == 18)
				pid = pid.replaceAll("(\\d{2})(\\d[A-Z])(\\d{2})(\\d{4})([\\dA-Z]{4})(\\d{4})", "$1-$2-$3-$4-$5-$6");
			return pid;
			
		} else if (stateCounty.equals("FLLake")){
			if (pid.length() == 16) {
				return pid + "00";
			} else if (pid.length() == 19) {
				return pid.replace(".", "");
			}
		} else if (stateCounty.equals("FLLeon")){
			
			pid = pid.replaceAll("\\p{Punct}", "");
			pid = pid.replaceAll("\\s+", "");
			
			return pid.replaceAll("(?is)(\\d+)([A-Z][A-Z])", "$1 $2").replaceAll("(?is)(\\d+)([A-Z])", "$1  $2");
			
		} else if (stateCounty.equals("FLLevy")){
			int len = pid.length();
			if (len > 10 && !pid.contains("-")) {	//if the PIN from NB is longer, e.g. 2915130878700100 
				pid = pid.substring(len-10);		//take the last 10 characters from it, e.g. 0878700100
			}
			return pid;
			
		} else if (stateCounty.equals("FLMartin")){
			return org.apache.commons.lang.StringUtils.rightPad(pid.replaceAll("[^\\d]",""), 22, "0");
			
		} else if (stateCounty.equals("FLOrange")){
			if (pid.length() == 15 && pid.matches("[0-9]+")) {
				return pid.substring(4, 6) + "-" + pid.substring(2, 4)
						+ "-" + pid.substring(0, 2) + "-"
						+ pid.substring(6, 10) + "-" + pid.substring(10);
			} else if(pid.contains("-")){
				if(pid.length() == 19 && pid.lastIndexOf("-") == 13) {
					return pid;
				} else {
					return pid.replaceAll("-(\\d+)$", "$1");
				}
						
			}
		} else if (stateCounty.equals("FLPasco")){
			pid = pid.replace("-", "");
			if (pid.contains(".")){
				pid = pid.replace(".", "");
				if (pid.length() == 19) {
					return pid.substring(4, 6) + pid.substring(2, 4) + pid.substring(0, 2) + pid.substring(6);
				}
			}
			
		} else if (stateCounty.equals("FLPolk")) {
			if (pid.matches("\\d+-\\d+-\\d+-\\d+-\\d+")) {
				pid = pid.replaceFirst("(\\d+)-(\\d+)-(\\d+)-(\\d+)-(\\d+)", "$1" + "$2" + "$3" + "-$4-$5");
			} else if (pid.matches("\\d{18}")) {
				pid = pid.replaceFirst("(\\d+{6})(\\d{6})(\\d{6})", "$1" + "-$2" + "-$3");
			} else if (pid.matches("\\d{2}-\\d{2}-\\d{2}-\\d{6}-\\d{6}")) {
				pid = pid.replaceFirst("(\\d{2})-(\\d{2})-(\\d{2}-\\d{6}-\\d{6})", "$1$2$3");
			}
			
			return pid;
			
		} else if (countyId.equals(CountyConstants.FL_Sarasota_STRING)){
			return pid.replaceAll("-", "");	
		} else if(stateCounty.equals("FLSeminole")){
			pid = pid.replace("-", "");
			
		} else if (stateCounty.equals("FLWakulla")){
			pid = pid.replaceAll("-", "");
			if (pid.matches("[A-Z0-9]{18}")){
				pid = pid.replaceFirst("(\\w{2})(\\w{2})(\\w{3})(\\w{3})(\\w{5})(\\w{3})", "$1-$2-$3-$4-$5-$6");
			} else{
				pid = "";
			}
			return pid;
			
		} else if (stateCounty.equals("FLWashington")){
			if (!pid.contains("-") && pid.length() <= 18) {
				pid = org.apache.commons.lang.StringUtils.leftPad(pid, 18, "0");
				return pid.substring(0, 8) + "-" + pid.substring(8, 10)
						+ "-" + pid.substring(10, 14) + "-" + pid.substring(14);
			}
		} else if (stateCounty.equals("FLVolusia")){
			return pid.replaceAll("\\p{Punct}", "");
			
		}else if (stateCounty.equals("CODenver")) {
			return getAtribute(LD_PARCELNO_GENERIC_AO);
			
		} else if (stateCounty.equals("ILDu Page")){
			return pid.replaceAll("[^\\d]", "");
			
		} else if (stateCounty.equals("ILKendall")) {
			pid = pid.replaceFirst("^(\\d{2})(\\d{2})(\\d{3})(\\d{3})$", "$1-$2-$3-$4");
			return pid;
		} else if (stateCounty.equals("ILLake")){
			if (pid.length() == 10)
				pid = pid.replaceAll("(\\d{2})(\\d{2})(\\d{3})(\\d{3})", "$1-$2-$3-$4");
			return pid;
			
		} else if (stateCounty.equals("ILWill")){
			return pid.replaceAll("\\p{Punct}", "");
			
		} else if (countyId.equals(CountyConstants.KS_Johnson_STRING)) {
			return getAtribute(LD_PARCELNO_GENERIC_AO);
		} else if (stateCounty.equals("MDBaltimore")) {				
			pid = pid.replaceAll("\\p{Punct}", "");
			if (pid.length() == 12) {
				pid = pid.substring(2);
			}
			return pid;
			
		} else if(stateCounty.equals("MDCharles")) {
			pid = pid.replaceAll("\\s*-\\s*", "");
			return pid;
			
		} else if (stateCounty.equals("MDHarford")) {				
			pid = pid.replaceAll("\\s*-\\s*", "");
			return pid;
			
		} else if(stateCounty.equals("MDMontgomery")) {
			if (pid.length() >= 8) {
				pid = pid.substring(pid.length() - 8);
			}
		} else if(stateCounty.equals("MDPrince George's")) {
			if (pid.length() >= 7) {
				pid = pid.substring(pid.length() - 7);
			}
		} else if(stateCounty.equals("MDHoward")) {
			pid = pid.replace("-", "");
		} else if(stateCounty.equals("MDFrederick")) {
			pid = pid.replace("-", "");
		} else if(stateCounty.equals("MDAnne Arundel")) {
			pid = pid.replace("-", "");
		} else if (stateCounty.equals("MOClay")){
			return pid.replaceAll("[-\\.]+", "");
			
		} else if (stateCounty.equals("MOSt. Louis")) {
			if (pid.matches("[\\dA-Z]+-[\\dA-Z]+-[\\dA-Z]+")) {
				pid = pid.replaceAll("-", "");
			}
			return pid;
				
		} else if (stateCounty.equals("NVClark")){
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 11){
				pid = pid.replaceAll("(\\d{3})(\\d{2})(\\d{3})(\\d{3})", "$1-$2-$3-$4");
				return pid;
			}
		} else if (stateCounty.equals("OHCuyahoga")) {
			pid = pid.replaceAll("[-]+", "");					
			return pid;
		} else if (stateCounty.equals("OHDelaware")){
			return pid.replaceAll("[^\\d]", "");
		} else if (countyId.equals(CountyConstants.OH_Fairfield_STRING)) {
			pid = pid.replaceAll("-", "");
			return pid;
		} else if (stateCounty.equals("OHSummit")){
			return pid.replaceAll("[^\\d]", "");
		} else if (stateCounty.equals("SCGreenville")) {
			String regEx = "(?is)\\d{4,4}\\.\\d{2,2}-\\d{2,2}-\\d{3,3}\\.\\d{2,2}";
			if (pid.matches(regEx)) {
				pid = pid.replaceAll("\\.|\\-", "");
			}
			regEx = "(?is)(0)(\\d{4,4})(\\d{2,2})(\\d{2,2})(\\d{3,3})(\\d{2,2})";
			if (pid.matches(regEx)) {
				pid = pid.replaceAll(regEx, "$2$3$4$5$6");
			}

		} else if (stateCounty.equals("TNDavidson")){
			String cleanRegEx = "\\d{3,3}-\\d{2,2}-\\w{2,2}-\\d{3,3}\\.\\d{2,2}-\\w{1,2}";
			if (pid.matches(cleanRegEx)) {
				pid = pid.replaceAll("[\\.-]", "");
			}
		} else if (stateCounty.equals("TNWilliamson")) {
			if (pid.length() == 9 || pid.length() == 11) {
				pid = pid + "000";
			}
			return pid.replace(".", "");

		} else if (stateCounty.equals("TXCollin")) {
			// R047800302001
			pid = pid.replaceAll("[-\\s]", "");
			return pid;
		} else if (stateCounty.equals("TXDenton")) {
			pid = pid.replaceAll("[-\\s]", "");
			return pid;
		} else if (stateCounty.equals("TXEllis")) {
			pid = pid.replaceAll("[-\\s\\.]", "");
			return pid;
		} else if (stateCounty.equals("TXParker")){
			pid = pid.replaceAll("(?is)\\p{Punct}", "");
			if (pid.length() == 13) {
				pid = pid.replaceAll("(\\w{5})(\\w{3})(\\w{3})(\\w{2})", "$1.$2.$3.$4");
			}
		} else if (stateCounty.equals("TXWise")) {
			if (pid.matches("(?is)((\\d|[A-Z]){5})\\.(\\d{4})\\.(\\d{2})")) {
				return pid.replace(".", "-");
			}
		}
		//states and counties in alphabetical order
 
		return pid;
	}

	protected String getParcelNoGenericAO() {
		//states and counties in alphabetical order
		
		String pid = getAtribute(LD_PARCELNO).trim();
		if (pid.isEmpty()) {
			return pid;
		}

		
		String countyId = getCountyId();
		
		
		switch (countyId) {
		case CountyConstants.CO_Denver_STRING:
			pid = pid.replaceAll("-", "");
			if (pid.length() != 13) {
				if (pid.length() <= 10) {
					pid += "000";
				}
				return org.apache.commons.lang.StringUtils.leftPad(pid, 13, "0");
			}			
			break;
		case CountyConstants.CO_El_Paso_STRING:
			return pid.replaceAll("[-\\s]", "");
		case CountyConstants.CO_Fremont_STRING:
			if (pid.length() < 12) {
				return org.apache.commons.lang.StringUtils.leftPad(pid, 12, "0");
			}
			break;
		case CountyConstants.FL_Hillsborough_STRING:
			if (pid.matches("\\d+-\\d+")) {
				return pid;
			} else {
				if(pid.length() == 10){
					return pid.substring(0,6)+"-"+pid.substring(6);
				}
			}
			break;
		case CountyConstants.FL_Leon_STRING:
		case CountyConstants.FL_Washington_STRING:
			return getParcelNoGenericTR();
		case CountyConstants.FL_Martin_STRING:
			// 0138400010000017.070000
			pid = pid.replaceAll("\\p{Punct}", "");
			pid = pid.replaceFirst("(\\d{2})(\\d{2})(\\d{2})(\\d{3})(\\d{3})(\\d{5})(\\d{1})(\\d*)", "$1-$2-$3-$4-$5-$6-$7");
			return pid;
		case CountyConstants.FL_Orange_STRING:
			if (pid.matches("\\d+-\\d+-\\d+-\\d+-\\d+-\\d+")) {
				return pid;
			} else {
				if(pid.length() == 15){
					return pid.replaceAll("(?is)(\\d{2})(\\d{2})(\\d{2})(\\d{4})(\\d{2})(\\d{3})", "$3-$2-$1-$4-$5-$6");
				}
			}
			break;
		case CountyConstants.FL_Wakulla_STRING:
			if (!pid.contains("-")) {
				return pid
						.replaceAll(
								"(\\w{2,2})(\\w{2,2})(\\w{3,3})(\\w{3,3})(\\w{5,5})(\\w{3,3})",
								"$1-$2-$3-$4-$5-$6");
			}
			break;
		case CountyConstants.KS_Johnson_STRING:
			// DP36980000 0001
			if (pid.contains("-") || pid.contains(".")) {
				pid = pid.replaceAll("\\p{Punct}", " ");
			} else {
				if (pid.length() == 14 || pid.length() == 15) {
					pid = pid.replaceFirst("(?is)([A-Z\\d]{10})([A-Z\\d]+)", "$1 $2");
				}
			}
			break;
		case CountyConstants.KY_Jefferson_STRING:
			if (pid.length() > 12) {
				return pid.substring(pid.length() - 12);
			}
			break;
		case CountyConstants.MO_Cass_STRING:
			if (pid.matches("\\d{18}")) {
				return pid;
			} else {
				String pid2 = getAtribute(LD_PARCELNO2).trim();
				if (!StringUtils.isEmpty(pid2)) {
					if (pid2.matches("\\d{18}")) {
						return pid2;
					}
				}
			}
			return "";
		case CountyConstants.NV_Clark_STRING:
			//219-09-801-018
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 11) {
				return pid.replaceAll(
						"(?is)(\\d{3})(\\d{2})(\\d{3})(\\d{3})",
						"$1-$2-$3-$4");
			}
			break;
		default:
			break;
		}
		
		String stateCounty = getStateCounty();

		if (stateCounty.equals("TXAndrews")
				|| stateCounty.equals("TXBandera")) {
			// 3900-02364-0000
			pid = pid.replaceAll("-", "");
			if (pid.length() == 14) {
				return pid.replaceAll("(?is)(\\d{5})(\\d{5})(\\d{4})",
						"$1-$2-$3");
			}
		} else if (stateCounty.equals("TXAngelina")) {
			pid = pid.replaceAll("-", "");
			// 3944-318-001-007-00
			// 0173-022B-006-000-00
			if (pid.length() == 15) {
				return pid.replaceAll(
						"(?is)(\\d{4})(\\d{3})(\\d{3})(\\d{3})(\\d{2})",
						"$1-$2-$3-$4-$5");
			} else if (pid.length() == 16) {
				return pid
						.replaceAll(
								"(?is)(\\d{4})(\\d{3}[A-Z])(\\d{3})(\\d{3})(\\d{2})",
								"$1-$2-$3-$4-$5");
			}
		} else if (stateCounty.equals("TXArmstrong")
				|| stateCounty.equals("TXBee")
				|| stateCounty.equals("TXBorden")
				|| stateCounty.equals("TXCarson")
				|| stateCounty.equals("TXChambers")
				|| stateCounty.equals("TXChildress")
				|| stateCounty.equals("TXCottle")
				|| stateCounty.equals("TXDawson")
				|| stateCounty.equals("TXDeWitt")
				|| stateCounty.equals("TXFoard")
				|| stateCounty.equals("TXFranklin")
				|| stateCounty.equals("TXFreestone")
				|| stateCounty.equals("TXFrio")
				|| stateCounty.equals("TXHall")
				|| stateCounty.equals("TXHemphill")
				|| stateCounty.equals("TXHouston")
				|| stateCounty.equals("TXHutchinson")
				|| stateCounty.equals("TXIrion")
				|| stateCounty.equals("TXJim Hogg")
				|| stateCounty.equals("TXJones")
				|| stateCounty.equals("TXKent")
				|| stateCounty.equals("TXKing")
				|| stateCounty.equals("TXLeon")
				|| stateCounty.equals("TXLoving")
				|| stateCounty.equals("TXLynn")
				|| stateCounty.equals("TXMarion")
				|| stateCounty.equals("TXMcCulloch")
				|| stateCounty.equals("TXPanola")
				|| stateCounty.equals("TXPecos")
				|| stateCounty.equals("TXReagan")
				|| stateCounty.equals("TXReeves")
				|| stateCounty.equals("TXRefugio")
				|| stateCounty.equals("TXRusk")
				|| stateCounty.equals("TXSan Saba")
				|| stateCounty.equals("TXSherman")
				|| stateCounty.equals("TXStarr")) {
			// 01228-00400-08000-000000
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 21) {
				return pid.replaceAll(
						"(?is)(\\d{5})(\\d{5})(\\d{5})(\\d{6})",
						"$1-$2-$3-$4");
			}
		} else if (stateCounty.equals("TXAtascosa")) {
			// 02592-03-000-013300
			pid = pid.replaceAll("-", "");
			if (pid.length() == 16) {
				return pid.replaceAll(
						"(?is)(\\d{5})(\\d{2})(\\d{3})(\\d{6})",
						"$1-$2-$3-$4");
			} else if(pid.startsWith("R")){
				return pid.substring(1); //task 8345
			}
		} else if (stateCounty.equals("TXBell")
				|| stateCounty.equals("TXCallahan")
				|| stateCounty.equals("TXColorado")
				|| stateCounty.equals("TXComal")
				|| stateCounty.equals("TXCoryell")
				|| stateCounty.equals("TXDallam")
				|| stateCounty.equals("TXEl Paso")
				|| stateCounty.equals("TXHamilton")
				|| stateCounty.equals("TXHartley")
				|| stateCounty.equals("TXKleberg")
				|| stateCounty.equals("TXLavaca")
				|| stateCounty.equals("TXLipscomb")
				|| stateCounty.equals("TXMaverick")
				|| stateCounty.equals("TXMcLennan")
				|| stateCounty.equals("TXMcMullen")
				|| stateCounty.equals("TXMills")
				|| stateCounty.equals("TXMorris")
				|| stateCounty.equals("TXRunnels")
				|| stateCounty.equals("TXShackelford")
				|| stateCounty.equals("TXSomervell")
				|| stateCounty.equals("TXSterling")
				|| stateCounty.equals("TXTaylor")
				|| stateCounty.equals("TXTravis")
				|| stateCounty.equals("TXWichita")
				|| stateCounty.equals("TXYoakum")
				|| stateCounty.equals("TXZapata")) {
			// 0543990201
			pid = pid.replaceAll("[-\\s\\.]", "");
			return pid;
		} else if (stateCounty.equals("TXBexar")) {
			// 16885-085-0240
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 12) {
				return pid.replaceAll("(?is)(\\d{5})(\\d{3})(\\d{4})",
						"$1-$2-$3");
			}
		} else if (stateCounty.equals("TXBrazoria")) {
			// 8181-0105-000
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 11) {
				return pid.replaceAll("(?is)(\\d{4})(\\d{4})(\\d{3})",
						"$1-$2-$3");
			}
		} else if (stateCounty.equals("TXBrazos")) {
			// 150500-0103-0080
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 14) {
				return pid.replaceAll("(?is)(\\d{6})(\\d{4})(\\d{4})",
						"$1-$2-$3");
			}
		} else if (stateCounty.equals("TXBrooks")) {
			// 00097-0000-449-02
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 14) {
				return pid.replaceAll(
						"(?is)(\\d{5})(\\d{4})(\\d{3})(\\d{2})",
						"$1-$2-$3-$4");
			}
		} else if (stateCounty.equals("TXBrown")) {
			// A1497-0003-00
			// 81830-0054-00
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 11) {
				return pid.replaceAll(
						"(?is)(\\d{5}|[A-Z]\\d{4})(\\d{4})(\\d{2})",
						"$1-$2-$3");
			}
		} else if (stateCounty.equals("TXBurleson")) {
			// 3450-002-000-16200
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 15) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){4})((\\d|[A-Z]){3})((\\d|[A-Z]){3})((\\d|[A-Z]){5})",
								"$1-$2-$3-$4");
			}
		} else if (stateCounty.equals("TXBurnet")) {
			// 05220-K070-07022-000
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 17) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){5})((\\d|[A-Z]){4})((\\d|[A-Z]){5})((\\d|[A-Z]){3})",
								"$1-$3-$5-$7");
			}
		} else if (stateCounty.equals("TXCaldwell")) {
			// 0003550-001-003-00
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 15) {
				return pid.replaceAll(
						"(?is)(\\d{7})(\\d{3})(\\d{3})(\\d{2})",
						"$1-$2-$3-$4");
			}
		} else if (stateCounty.equals("TXCalhoun")) {
			// S0265-00070-0016-00
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 16) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){5})((\\d|[A-Z]){5})((\\d|[A-Z]){4})((\\d|[A-Z]){2})",
								"$1-$3-$5-$7");
			}
		} else if (stateCounty.equals("TXCameron")) {
			// 22-3820-0020-0140-00
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 16) {
				return pid.replaceAll(
						"(?is)(\\d{2})(\\d{4})(\\d{4})(\\d{4})(\\d{2})",
						"$1-$2-$3-$4-$5");
			}
		} else if (stateCounty.equals("TXCamp")) {
			// 10001-11300-00133-000000
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 21) {
				return pid.replaceAll(
						"(?is)(\\d{5})(\\d{5})(\\d{5})(\\d{6})",
						"$1-$2-$3-$4");
			}
		} else if (stateCounty.equals("TXCherokee")) {
			// 246355-00010-0010000
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 18) {
				return pid.replaceAll("(?is)(\\d{6})(\\d{5})(\\d{7})",
						"$1-$2-$3");
			}
		} else if (stateCounty.equals("TXCochran")) {
			// 0-101-005-075-015-00
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 15) {
				return pid
						.replaceAll(
								"(?is)(\\d{1})(\\d{3})(\\d{3})(\\d{3})(\\d{3})(\\d{2})",
								"$1-$2-$3-$4-$5-$6");
			}
		} else if (stateCounty.equals("TXCollin")) {
			// R-0478-003-0200-1
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 13) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){1})((\\d|[A-Z]){4})((\\d|[A-Z]){3})((\\d|[A-Z]){4})((\\d|[A-Z]){1})",
								"$1-$3-$5-$7-$9");
			}
		} else if (stateCounty.equals("TXComanche")) {
			pid = pid.replaceAll("[-\\s]", "");
			// SCO-05-079
			if (pid.length() == 8) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){3})((\\d|[A-Z]){2})((\\d|[A-Z]){3})",
								"$1-$3-$5");
			}
			// CCO-03-0467
			if (pid.length() == 9) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){3})((\\d|[A-Z]){2})((\\d|[A-Z]){4})",
								"$1-$3-$5");
			}
			// SDE-14-115-10
			if (pid.length() == 10) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){3})((\\d|[A-Z]){2})((\\d|[A-Z]){3})((\\d|[A-Z]){2})",
								"$1-$3-$5-$7");
			}
		} else if (stateCounty.equals("TXCooke")) {
			// 0414-004-00000-MHL
			// 0197-013-00000
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 12) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){4})((\\d|[A-Z]){3})((\\d|[A-Z]){5})",
								"$1-$3-$5");
			}
			if (pid.length() == 15) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){4})((\\d|[A-Z]){3})((\\d|[A-Z]){5})((\\d|[A-Z]){3})?",
								"$1-$3-$5-$7");
			}
		} else if (countyId.equals(CountyConstants.TX_Dallas_STRING) || 
				   countyId.equals(CountyConstants.TX_Harris_STRING)) {
			return pid.replaceAll("-", "");
		}  else if (stateCounty.equals("TXDelta")) {
			// 0198-0003-0000-03
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 14) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){4})((\\d|[A-Z]){4})((\\d|[A-Z]){4})((\\d|[A-Z]){2})",
								"$1-$3-$5-$7");
			}
		} else if (stateCounty.equals("TXDenton")) {
			pid = pid.replaceAll("[-\\s]", "");

			// SM0034A-000000-0000-0035-0000
			if (pid.length() == 25) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){7})((\\d|[A-Z]){6})((\\d|[A-Z]){4})((\\d|[A-Z]){4})((\\d|[A-Z]){4})",
								"$1-$3-$5-$7-$9");
			}
			// A1330A-000-0133-00D3
			if (pid.length() == 17) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){6})((\\d|[A-Z]){3})((\\d|[A-Z]){4})((\\d|[A-Z]){4})",
								"$1-$3-$5-$7");
			}
		} else if (stateCounty.equals("TXDickens")) {
			// 215-0002-0004-000000
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 17) {
				return pid.replaceAll(
						"(?is)(\\d{3})(\\d{4})(\\d{4})(\\d{6})",
						"$1-$2-$3-$4");
			}
		} else if (stateCounty.equals("TXDimmit")) {
			// 9100000-1040050000
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 17) {
				return pid
						.replaceAll("(?is)((\\d){7})((\\d){10})", "$1-$3");
			}
		} else if (stateCounty.equals("TXDonley")) {
			// 11-01-0120-0160-0001
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 16) {
				return pid.replaceAll(
						"(?is)(\\d{2})(\\d{2})(\\d{4})(\\d{4})(\\d{4})",
						"$1-$2-$3-$4-$5");
			}
		} else if (stateCounty.equals("TXDuval")) {
			// 9100000-1040050000
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 13) {
				return pid.replaceAll("(?is)(\\d{6})(\\d{3})(\\d{4})",
						"$1-$2-$3");
			}
		} else if (stateCounty.equals("TXEdwards")) {
			// 6881-00-00400
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 11) {
				return pid.replaceAll("(?is)(\\d{6})(\\d{3})(\\d{4})",
						"$1-$2-$3");
			}
			if (pid.length() == 12) {
				if (pid.matches("(?is).*[A-Z].*")) // 0718E-06-00000
					return pid.replaceAll(
							"(?is)((\\d|[A-Z]){5})(\\d{2})(\\d{5})",
							"$1-$3-$4");
				else
					// 6550-000-02600
					return pid.replaceAll("(?is)(\\d{4})(\\d{3})(\\d{5})",
							"$1-$2-$3");
			}
		} else if (stateCounty.equals("TXEllis")) {
			// 54.3090.904.006.00.108
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 17) {
				return pid
						.replaceAll(
								"(?is)(\\d{2})(\\d{4})(\\d{3})(\\d{3})(\\d{2})(\\d{3})",
								"$1.$2.$3.$4.$5.$6");
			}
		} else if (stateCounty.equals("TXErath")) {
			// 54.3090.904.006.00.108
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 13) {
				return pid.replaceAll(
						"(?is)([A-Z]{1})(\\d{4})(\\d{5})(\\d{2})(\\d{1})",
						"$1.$2.$3.$4.$5");
			}
		} else if (stateCounty.equals("TXFannin")) {
			// 9575-002-004A-05
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 13) {
				return pid.replaceAll(
						"(?is)(\\d{4})(\\d{3})((\\d|[A-Z]){4})(\\d{2})",
						"$1-$2-$3-$5");
			}
		} else if (stateCounty.equals("TXFayette")) {
			// 9575-002-004A-05
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 16) {
				return pid.replaceAll(
						"(?is)(\\d{2})(\\d{4})(\\d{7})(\\d{3})",
						"$1-$2-$3-$4");
			}
			if (pid.length() == 17) {
				return pid.replaceAll(
						"(?is)(\\d{2})(\\d{4})(\\d{8})(\\d{3})",
						"$1-$2-$3-$4");
			}
		} else if (stateCounty.equals("TXFloyd")) {
			// 20248.010.0000.0
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 13) {
				return pid.replaceAll(
						"(?is)(\\d{5})((\\d|[A-Z]){3})(\\d{4})(\\d{1})",
						"$1.$2.$4.$5");
			}
		} else if (stateCounty.equals("TXGarza")) {
			// 122-0002-0006-000-00-00
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 18) {
				return pid
						.replaceAll(
								"(?is)(\\d{3})(\\d{4})(\\d{4})(\\d{3})(\\d{2})(\\d{2})",
								"$1-$2-$3-$4-$5-$6");
			}
		} else if (stateCounty.equals("TXGlasscock")) {
			// 9000-02571-00000-00
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 16) {
				return pid.replaceAll(
						"(?is)(\\d{4})(\\d{5})(\\d{5})(\\d{2})",
						"$1-$2-$3-$4");
			}
		} else if (stateCounty.equals("TXGoliad")) {
			// 1001-053095-034038
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 16) {
				return pid.replaceAll("(?is)(\\d{4})(\\d{6})(\\d{6})",
						"$1-$2-$3");
			}
		} else if (stateCounty.equals("TXGregg")) {
			// 6340000002-010-00-01
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 17) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){10})((\\d|[A-Z]){3})((\\d|[A-Z]){2})((\\d|[A-Z]){2})",
								"$1-$3-$5-$7");
			}
		} else if (stateCounty.equals("TXGuadalupe")) {
			// 1G2030-1050-01502-0-00
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 18) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){6})((\\d|[A-Z]){4})((\\d|[A-Z]){5})((\\d|[A-Z]){1})((\\d|[A-Z]){2})",
								"$1-$3-$5-$7-$9");
			}
		} else if (stateCounty.equals("TXHardin")
				|| stateCounty.equals("TXRobertson")) {
			// 002055-008350
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 12) {
				return pid.replaceAll("(?is)(\\d{6})(\\d{6})", "$1-$2");
			}
		} else if (stateCounty.equals("TXHarrison")) {
			// 05120.00120.00000.000000
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 21) {
				return pid.replaceAll(
						"(?is)(\\d{5})(\\d{5})(\\d{5})(\\d{6})",
						"$1.$2.$3.$4");
			}
		} else if (stateCounty.equals("TXHaskell")) {
			// 0011-00316-00036-000900
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 20) {
				return pid.replaceAll(
						"(?is)(\\d{4})(\\d{5})(\\d{5})(\\d{6})",
						"$1-$2-$3-$4");
			}
		} else if (stateCounty.equals("TXHenderson")) {
			// 2390.0005.2620.30
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 14) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){4})((\\d|[A-Z]){4})((\\d|[A-Z]){4})((\\d|[A-Z]){2})",
								"$1.$3.$5.$7");
			}
		} else if (stateCounty.equals("TXHidalgo")) {
			// F0770-00-000-0061-00
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 16) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){5})((\\d|[A-Z]){2})((\\d|[A-Z]){3})((\\d|[A-Z]){4})((\\d|[A-Z]){2})",
								"$1-$3-$5-$7-$9");
			}
		} else if (stateCounty.equals("TXHill")) {
			// 11610-75000-00000-505000
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 21) {
				return pid.replaceAll(
						"(?is)(\\d{5})(\\d{5})(\\d{5})(\\d{6})",
						"$1-$2-$3-$4");
			}
		} else if (stateCounty.equals("TXHockley")) {
			// 11310-00040-00080-00000
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 20) {
				return pid.replaceAll(
						"(?is)(\\d{5})(\\d{5})(\\d{5})(\\d{5})",
						"$1-$2-$3-$4");
			}
		} else if (stateCounty.equals("TXHood")) {
			// 11367.000.0475.0
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 13) {
				return pid.replaceAll(
						"(?is)(\\d{5})(\\d{3})(\\d{4})(\\d{1})",
						"$1.$2.$3.$4");
			}
		} else if (stateCounty.equals("TXHopkins")) {
			// 40.0036.003.001.00
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 14) {
				return pid.replaceAll(
						"(?is)(\\d{2})(\\d{4})(\\d{3})(\\d{3})(\\d{2})",
						"$1.$2.$3.$4.$5");
			}
		} else if (stateCounty.equals("TXHudspeth")) {
			// A070-000-00A0-0320
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 15) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){4})((\\d|[A-Z]){3})((\\d|[A-Z]){4})((\\d|[A-Z]){4})",
								"$1-$3-$5-$7");
			}
		} else if (stateCounty.equals("TXHunt")) {
			// 4385-2090-0040-41
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 14) {
				return pid.replaceAll(
						"(?is)(\\d{4})(\\d{4})(\\d{4})(\\d{2})",
						"$1-$2-$3-$4");
			}
		} else if (stateCounty.equals("TXJeff Davis")) {
			// X020-002-10100580
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 15) {
				return pid.replaceAll("(?is)(\\w\\d{3})(\\d{3})(\\d{8})",
						"$1-$2-$3");
			}
		} else if (stateCounty.equals("TXJefferson")) {
			// 000560-000-001100-00000-0
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 21) {
				return pid.replaceAll(
						"(?is)(\\d{6})(\\d{3})(\\d{6})(\\d{5})(\\d{1})",
						"$1-$2-$3-$4-$5");
			}
		} else if (stateCounty.equals("TXJones")) {
			// 109319-000
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 9) {
				return pid.replaceAll("(?is)(\\d{6})(\\d{3})", "$1-$2");
			}
		} else if (stateCounty.equals("TXKaufman")) {
			// S0710022600
			// 99.0286.0000.0455.01.02.00
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 11)
				return pid;
			if (pid.length() == 20) {
				return pid
						.replaceAll(
								"(?is)(\\d{2})(\\d{4})(\\d{4})(\\d{4})(\\d{2})(\\d{2})(\\d{2})",
								"$1.$2.$3.$4.$5.$6.$7");
			}
		} else if (stateCounty.equals("TXKendall")) {
			// 1-5175-0002-0040
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 13) {
				return pid.replaceAll(
						"(?is)(\\d{1})(\\d{4})(\\d{4})(\\d{4})",
						"$1-$2-$3-$4");
			}
		} else if (stateCounty.equals("TXKenedy")) {
			// 131-1000-090-15-0-0-0-00
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 17) {
				return pid
						.replaceAll(
								"(?is)(\\d{3})(\\d{4})(\\d{3})(\\d{2})(\\d{1})(\\d{1})(\\d{1})(\\d{2})",
								"$1-$2-$3-$4-$5-$6-$7-$8");
			}
		} else if (stateCounty.equals("TXKerr")
				|| stateCounty.equals("TXKimble")) {
			// 5924-0130-028000
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 14) {
				return pid.replaceAll("(?is)(\\d{4})(\\d{4})(\\d{6})",
						"$1-$2-$3");
			}
		} else if (stateCounty.equals("TXKinney")) {
			// 000-0700-0000-0491-13
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 17) {
				return pid.replaceAll(
						"(?is)(\\d{3})(\\d{4})(\\d{4})(\\d{4})(\\d{2})",
						"$1-$2-$3-$4-$5");
			}
		} else if (stateCounty.equals("TXKnox")) {
			// 51500.00090.00000.340018
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 21) {
				return pid.replaceAll(
						"(?is)(\\d{5})(\\d{5})(\\d{5})(\\d{6})",
						"$1.$2.$3.$4");
			}
		} else if (stateCounty.equals("TXLamar")) {
			// A0416-0020-0000-25
			// 0565-0000-0660
			// 015500-08801-0060
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 12) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){4})((\\d|[A-Z]){4})((\\d|[A-Z]){4})",
								"$1-$3-$5");
			}
			if (pid.length() == 15) {
				return pid.replaceAll("(?is)(\\d{6})(\\d{5})(\\d{4})",
						"$1-$2-$3");
			}
			if (pid.length() == 15) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){5})((\\d|[A-Z]){4})((\\d|[A-Z]){4})((\\d|[A-Z]){2})",
								"$1-$3-$5-$7");
			}
		} else if (stateCounty.equals("TXLamb")) {
			// 00000-18620-001
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 13) {
				return pid.replaceAll("(?is)(\\d{5})(\\d{5})(\\d{3})",
						"$1-$2-$3");
			}
		} else if (stateCounty.equals("TXLampasas")) {
			// 10040-000-002-00
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 13) {
				return pid.replaceAll(
						"(?is)(\\d{5})(\\d{3})(\\d{3})(\\d{2})",
						"$1-$2-$3-$4");
			}
		} else if (stateCounty.equals("TXLiberty")) {
			// 006170-000064-108
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 15) {
				return pid.replaceAll("(?is)(\\d{6})(\\d{6})(\\d{3})",
						"$1-$2-$3");
			}
		} else if (stateCounty.equals("TXLive Oak")) {
			// 0060-0126-0010-00
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 14) {
				return pid.replaceAll(
						"(?is)(\\d{4})(\\d{4})(\\d{4})(\\d{2})",
						"$1-$2-$3-$4");
			}
		} else if (stateCounty.equals("TXLlano")) {
			// 13370-0F3-0004-6
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 13) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){5})((\\d|[A-Z]){3})((\\d|[A-Z]){4})((\\d|[A-Z]){1})",
								"$1-$3-$5-$7");
			}
		} else if (stateCounty.equals("TXMadison")) {
			// R-2100-000-0190-901
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 15) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){1})(\\d{4})(\\d{3})(\\d{4})(\\d{3})",
								"$1-$3-$4-$5-$6");
			}
		} else if (stateCounty.equals("TXMason")) {
			// 00691-50-20050
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 12) {
				return pid.replaceAll("(?is)(\\d{5})(\\d{2})(\\d{5})",
						"$1-$2-$3");
			}
		} else if (stateCounty.equals("TXMatagorda")) {
			// 4081-0060-000A00
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 14) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){4})((\\d|[A-Z]){4})((\\d|[A-Z]){6})",
								"$1-$3-$5");
			}
		} else if (stateCounty.equals("TXMenard")) {
			// 00225-0284-170-10
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 14) {
				return pid.replaceAll(
						"(?is)(\\d{5})(\\d{4})(\\d{3})(\\d{2})",
						"$1-$2-$3-$4");
			}
		} else if (stateCounty.equals("TXMidland")) {
			// 00077630.005.0040
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 15) {
				return pid.replaceAll("(?is)(\\d{8})(\\d{3})(\\d{4})",
						"$1.$2.$3");
			}
		} else if (stateCounty.equals("TXMilam")) {
			// S10800-A08-03-00
			// A326-203-047-00
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 12) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){4})((\\d|[A-Z]){3})((\\d|[A-Z]){3})((\\d|[A-Z]){2})",
								"$1-$3-$5-$7");
			}
			if (pid.length() == 13) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){6})((\\d|[A-Z]){3})((\\d|[A-Z]){2})((\\d|[A-Z]){2})",
								"$1-$3-$5-$7");
			}
		} else if (stateCounty.equals("TXMontague")) {
			// 10500.0016.0001.0000
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 17) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){5})((\\d|[A-Z]){4})((\\d|[A-Z]){4})((\\d|[A-Z]){4})",
								"$1.$3.$5.$7");
			}
		} else if (stateCounty.equals("TXMoore")) {
			// 30000-02921-06720-000000
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 21) {
				return pid.replaceAll(
						"(?is)(\\d{5})(\\d{5})(\\d{5})(\\d{6})",
						"$1-$2-$3-$4");
			}
		} else if (stateCounty.equals("TXNacogdoches")) {
			// 18-390-6704-001010
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 15) {
				return pid.replaceAll(
						"(?is)(\\d{2})(\\d{3})(\\d{4})(\\d{6})",
						"$1-$2-$3-$4");
			}
		} else if (stateCounty.equals("TXNavarro")) {
			// A0200.03.00020.023.00.0
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 18) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){5})((\\d|[A-Z]){2})((\\d|[A-Z]){5})((\\d|[A-Z]){3})((\\d|[A-Z]){2})((\\d|[A-Z]){1})",
								"$1.$3.$5.$7.$9.$11");
			}
		} else if (stateCounty.equals("TXNolan")) {
			// S1500-0002-07
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 15) {
				return pid.replaceAll("(?is)(\\w\\d{4})(\\d{4})(\\d{2})",
						"$1-$2-$3");
			}
		} else if (stateCounty.equals("TXParker")) {
			// 11020.003.014.10
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 13) {
				return pid.replaceAll(
						"(?is)(\\d{5})(\\d{3})(\\d{3})(\\d{2})",
						"$1.$2.$3.$4");
			}
		} else if (stateCounty.equals("TXParmer")) {
			// 6-OOA-048-000-007
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 13) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){1})((\\d|[A-Z]){3})((\\d|[A-Z]){3})((\\d|[A-Z]){3})((\\d|[A-Z]){3})",
								"$1-$3-$5-$7-$9");
			}
		} else if (stateCounty.equals("TXRains")) {
			// 1150-0000-009G-42
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 14) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){4})((\\d|[A-Z]){4})((\\d|[A-Z]){4})((\\d|[A-Z]){2})",
								"$1-$3-$5-$7");
			}
		} else if (stateCounty.equals("TXRed River")) {
			// 0-12330-26800-0010-00
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 17) {
				return pid.replaceAll(
						"(?is)(\\d{1})(\\d{5})(\\d{5})(\\d{4})(\\d{2})",
						"$1-$2-$3-$4-$5");
			}
		} else if (stateCounty.equals("TXRockwall")) {
			// 5021-0000-0019-00-0R
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 16) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){4})((\\d|[A-Z]){4})((\\d|[A-Z]){4})((\\d|[A-Z]){2})((\\d|[A-Z]){2})",
								"$1-$3-$5-$7-$9");
			}
		} else if (stateCounty.equals("TXSan Jacinto")) {
			// 1010-000-1400
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 11) {
				return pid.replaceAll("(?is)(\\d{4})(\\d{3})(\\d{4})",
						"$1-$2-$3");
			}
		} else if (stateCounty.equals("TXSan Patricio")) {
			// R10263
			if (pid.matches("\\d{5}")) 
				return "R" + pid;
			if (!pid.matches("(?is)R\\d{5}"))
				return "";
		} else if (stateCounty.equals("TXScurry")) {
			// 01-0208-0030-0021-0004
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 18) {
				return pid.replaceAll(
						"(?is)(\\d{2})(\\d{4})(\\d{4})(\\d{4})(\\d{4})",
						"$1-$2-$3-$4-$5");
			}
		} else if (stateCounty.equals("TXShelby")) {
			// 06-0354-0000-0009-00
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 16) {
				return pid.replaceAll(
						"(?is)(\\d{2})(\\d{4})(\\d{4})(\\d{4})(\\d{2})",
						"$1-$2-$3-$4-$5");
			}
		} else if (stateCounty.equals("TXStephens")) {
			// 12011.905.000.06
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 13) {
				return pid.replaceAll(
						"(?is)(\\d{5})(\\d{3})(\\d{3})(\\d{2})",
						"$1.$2.$3.$4");
			}
		} else if (stateCounty.equals("TXSwisher")) {
			// 31-0100-0121-0000
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 14) {
				return pid.replaceAll(
						"(?is)(\\d{2})(\\d{4})(\\d{4})(\\d{4})",
						"$1-$2-$3-$4");
			}
		} else if (stateCounty.equals("TXTitus")) {
			// 00409-00000-00430
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 15) {
				return pid.replaceAll("(?is)(\\d{5})(\\d{5})(\\d{5})",
						"$1-$2-$3");
			}
		} else if (stateCounty.equals("TXUpton")) {
			// 2942HT-035-059-090
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 15) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){6})((\\d|[A-Z]){3})((\\d|[A-Z]){3})((\\d|[A-Z]){3})",
								"$1-$3-$5-$7");
			}
		} else if (stateCounty.equals("TXUvalde")) {
			// A0379-0005-01
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 11) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){5})((\\d|[A-Z]){4})((\\d|[A-Z]){2})",
								"$1-$3-$5");
			}
		} else if (stateCounty.equals("TXVal Verde")) {
			// 7010-0040-0030
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 12) {
				return pid.replaceAll("(?is)(\\d{4})(\\d{4})(\\d{4})",
						"$1-$2-$3");
			}
		} else if (stateCounty.equals("TXVan Zandt")) {
			// 064.0891.6960.0000.0000
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 19) {
				return pid.replaceAll(
						"(?is)(\\d{3})(\\d{4})(\\d{4})(\\d{4})(\\d{4})",
						"$1.$2.$3.$4.$5");
			}
		} else if (stateCounty.equals("TXVictoria")) {
			// 03430-000-37900
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 13) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){5})((\\d|[A-Z]){3})((\\d|[A-Z]){5})",
								"$1-$3-$5");
			}
		} else if (stateCounty.equals("TXWalker")) {
			// 2150-111-0-00200
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 13) {
				return pid.replaceAll(
						"(?is)(\\d{4})(\\d{3})(\\d{1})(\\d{5})",
						"$1-$2-$3-$4");
			}
		} else if (stateCounty.equals("TXWebb")) {
			// 923-00018-339
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 11) {
				return pid.replaceAll("(?is)(\\d{3})(\\d{5})(\\d{3})",
						"$1-$2-$3");
			}
		} else if (stateCounty.equals("TXWheeler")) {
			// 1-20-005000146000000000
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 21) {
				return pid.replaceAll("(?is)(\\d{1})(\\d{2})(\\d{18})",
						"$1-$2-$3");
			}
		} else if (stateCounty.equals("TXWilbarger")) {
			// 5001-063-0000-011-3
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 15) {
				return pid.replaceAll(
						"(?is)(\\d{4})(\\d{3})(\\d{4})(\\d{3})(\\d{1})",
						"$1-$2-$3-$4-$5");
			}
		} else if (stateCounty.equals("TXWilson")) {
			// 0010-00000-08607
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 14) {
				return pid.replaceAll("(?is)(\\d{4})(\\d{5})(\\d{5})",
						"$1-$2-$3");
			}
		} else if (stateCounty.equals("TXWinkler")) {
			// 4140-0013-0009000
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 15) {
				return pid.replaceAll("(?is)(\\d{4})(\\d{4})(\\d{7})",
						"$1-$2-$3");
			}
		} else if (stateCounty.equals("TXWise")) {
			// S2650.0020.00
			pid = pid.replaceAll("[-\\s\\.]", "");
			if (pid.length() == 11) {
				return pid
						.replaceAll(
								"(?is)((\\d|[A-Z]){5})(\\d{4})(\\d{2})",
								"$1.$3.$4");
			}
		} else if (stateCounty.equals("TXWood")) {
			// 2023-0000-0003-65
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 14) {
				return pid.replaceAll(
						"(?is)(\\d{4})(\\d{4})(\\d{4})(\\d{2})",
						"$1-$2-$3-$4");
			}
		} else if (stateCounty.equals("TXYoung")) {
			// 01070-0105-0000
			pid = pid.replaceAll("[-\\s]", "");
			if (pid.length() == 13) {
				return pid.replaceAll("(?is)(\\d{5})(\\d{4})(\\d{4})",
						"$1-$2-$3");
			}
		}

		return pid;
	}

	private String getNameTokenFromOwnersList(String key) {
		
		Set<NameI> names = null;
		if(key.startsWith("OWNER_") || key.startsWith("O_")) {
			names = getOwners().getNames();
		} else if(key.startsWith("BUYER_") || key.startsWith("B_")) {
			names = getBuyers().getNames();
		} else {
			Log.sendExceptionViaEmail(
					MailConfig.getMailLoggerToEmailAddress(), 
					"get NameTokenFromOwnersList with key " + key, 
					null, 
					"SearchId used: " + searchId + ", Key used: " + key);
			return "";
		}
		
		
		for (NameI nameI : names) {
			switch (key) {
			case OWNER_LAST_NAME:
				if(nameI.isCompany()) {
					continue;
				}
				return nameI.getLastName();
			case OWNER_FIRST_NAME:
				if(nameI.isCompany()) {
					continue;
				}
				return nameI.getFirstName();
			case OWNER_MIDDLE_NAME:
				if(nameI.isCompany()) {
					continue;
				}
				return nameI.getMiddleInitial();
			case OWNER_COMPANY_NAME:
				if(!nameI.isCompany()) {
					continue;
				}
				return nameI.getLastName();
			case OWNER_LNAME:
			case BUYER_LNAME:
				return nameI.getLastName();
			case OWNER_FNAME:
			case BUYER_FNAME:
				return nameI.getFirstName();
			case OWNER_MNAME:
			case BUYER_MNAME:
				return nameI.getMiddleName();
			case OWNER_MINITIAL:
				return nameI.getMiddleInitial();
			case OWNER_LF_NAME:
			case OWNER_FULL_NAME:
			case BUYER_FULL_NAME:
				return nameI.getLastName() + " " + nameI.getFirstName().replaceAll("\\s{2,}", "").trim();
			case OWNER_LCFM_NAME:
			{
				String lastName = nameI.getLastName();
				String firstName = nameI.getFirstName();
				String middleName = nameI.getMiddleName();
				String retVal;
				if (org.apache.commons.lang.StringUtils.isBlank(middleName)) {
					retVal = (lastName + ", " + firstName).trim();
				} else {
					retVal = (lastName + ", " + firstName + " " + middleName).trim();
				}
				if (retVal.equals(",")) {
					break;		// go to next name if available
				} else {
					return retVal;
				}
			}
			case OWNER_LCF_NAME:
			{
				String retVal = (nameI.getLastName() + ", " + nameI.getFirstName()).trim();
				if (retVal.equals(",")) {
					break;		// go to next name if available
				} else {
					return retVal;
				}
			}
			case OWNER_LFM_NAME:
				return (nameI.getLastName() + " " + nameI.getFirstName() + " " + nameI.getMiddleName())
						.replaceAll("\\s{2,}", "").trim();
			case OWNER_FML_NAME:
				return (nameI.getFirstName() + " " + nameI.getMiddleName() + " " + nameI.getLastName())
						.replaceAll("\\s{2,}", "").trim();
			default:
				break;
			}
		}
		
		return "";
	}

	public Object getObjectAtribute(String key) {
		if (key.equals(OWNER_OBJECT)) {
			return getOwners();
		} else if (key.equals(BUYER_OBJECT)) {
			return getBuyers();
		} else if (key.equals(ABSTRACTOR_OBJECT)) {
			return getAbstractorObject();
		} else if (extraHashSearch.containsKey(key)) {
			return extraHashSearch.get(key);
		} else if (key.equals(RO_CROSS_REF_INSTR_LIST)
				&& !extraHashSearch.containsKey(key)) {
			extraHashSearch.put(key, new ArrayList<Instrument>());
			return extraHashSearch.get(key);
		} else if (key.equals(INSTR_LIST) && !extraHashSearch.containsKey(key)) {
			extraHashSearch.put(key, new ArrayList());
			return extraHashSearch.get(key);
		} else if (key.equals(GB_MANAGER_OBJECT)
				&& !extraHashSearch.containsKey(key)) {
			extraHashSearch.put(GB_MANAGER_OBJECT, new GBManager());
			return extraHashSearch.get(key);
		} else if (key
				.equals(SearchAttributes.INITIAL_SEARCH_MIDDLE_NAME_MISSING)
				&& !extraHashSearch.containsKey(key)) {
			extraHashSearch.put(key, new Boolean[] { null, null, null, null });
			return extraHashSearch.get(key);

		} else if (key.equals(SITES_WITH_NAME_SEARCH_SKIPPED)) {
			return extraHashSearch.get(key);
		} else if (key.equals(OWNERS_LIST) && !extraHashSearch.containsKey(key)) {
			extraHashSearch.put(key, new Party(PType.GRANTOR));
			return extraHashSearch.get(key);
		} else if (key.equals(SEARCH_PAGE_MANUAL_OWNERS_LIST)
				&& !extraHashSearch.containsKey(key)) {
			extraHashSearch.put(key, new Party(PType.GRANTOR));
			return extraHashSearch.get(key);
		} else if (key.equals(SEARCH_PAGE_MANUAL_BUYERS_LIST)
				&& !extraHashSearch.containsKey(key)) {
			extraHashSearch.put(key, new Party(PType.GRANTEE));
			return extraHashSearch.get(key);
		} else if (key.equals(SEARCH_PAGE_MANUAL_FIELDS)
				&& !extraHashSearch.containsKey(key)) {
			extraHashSearch.put(key, new Hashtable<String, String>());
			return extraHashSearch.get(key);
		} else if (key.equals(BUYERS_LIST) && !extraHashSearch.containsKey(key)) {
			extraHashSearch.put(key, new Party(PType.GRANTEE));
			return extraHashSearch.get(key);
		} else if (key.equals(DATA_SOURCES_ON_SEARCH) && !extraHashSearch.containsKey(key)){
			extraHashSearch.put(key, new HashSet<DataSources>());
			return extraHashSearch.get(key);
		}  else if (key.equals(CERTIFICATION_DATE_ON_SEARCH) && !extraHashSearch.containsKey(key)){
			extraHashSearch.put(CERTIFICATION_DATE_ON_SEARCH, new ArrayList<CertificationDateDS>());
			return extraHashSearch.get(key);
		}else if (key.equals(REVIEW_CHECKER_LIST) && !extraHashSearch.containsKey(key)){
			extraHashSearch.put(key, new LinkedHashMap<String, ReviewChecker>());
			return extraHashSearch.get(key);
		} else if (key.equals(ORDER_COUNT_CACHE) && !extraHashSearch.containsKey(key)){
			extraHashSearch.put(key, new HashMap<String, Set<Integer>>());
			return extraHashSearch.get(key);
		} else if (key.equals(INTERNAL_LOG_ORIGINAL_LOCATION) && !extraHashSearch.containsKey(key)){
			extraHashSearch.put(key, new Integer(0));
			return extraHashSearch.get(key);
		}else {
			return getAtribute(key);
		}
	}

	public void advanceToPreviousOwner(long searchId, int level) {
		DocumentsManagerI documentsManager = InstanceManager.getManager()
				.getCurrentInstance(searchId).getCrtSearchContext()
				.getDocManager();
		try {
			documentsManager.getAccess();
			List<TransferI> allTransfers = documentsManager
					.getTransferList(true);

			Collections.sort(allTransfers, new RecordedDateComparator());
			documentsManager.getDocumentsList();
			GBManager gbm = (GBManager) getObjectAtribute(GB_MANAGER_OBJECT);
			gbm.getGbTransfers().clear();
			int i = 0;
			for (Iterator<TransferI> iterator = allTransfers.iterator(); iterator
					.hasNext() && i < level;) {
				TransferI transfer = iterator.next();
				if (!gbm.containsId(transfer.getId())) {
					gbm.addGbTransfers(transfer.getId());
					gbm.addGbTransferHistory(transfer.getId());
					transfer.setTsrIndexColorClass("gwt-goback-transfer");
					i++;
				}
			}
			setObjectAtribute(GB_MANAGER_OBJECT, gbm);
		}
		/*
		 * if ("reset_level".equals(type)){ gbm.getGbTransferHistory().clear();
		 * }
		 */

		finally {
			documentsManager.releaseAccess();
		}
	}

	public void setObjectAtribute(String key, Object value) {
		if (extraHashSearch.containsKey(key)
				|| key.equals(SearchAttributes.INITIAL_SEARCH_MIDDLE_NAME_MISSING)
				|| key.equals(SITES_WITH_NAME_SEARCH_SKIPPED)
				|| key.equals(RO_CROSS_REF_INSTR_LIST)
				|| key.equals(CERTIFICATION_DATE_ON_SEARCH)) {
			extraHashSearch.put(key, value);
		} else {
			setAtribute(key, (String) value);
		}
	}

	// add a value to SITES_WITH_NAME_SEARCH_SKIPPED
	// s is the site abbreviation
	public void setSiteNameSearchSkipped(String s) {
		HashSet<String> tmpHash = (HashSet<String>) getObjectAtribute(SITES_WITH_NAME_SEARCH_SKIPPED);
		if (tmpHash == null) {
			tmpHash = new HashSet<String>();
		}
		tmpHash.add(s);
		setObjectAtribute(SITES_WITH_NAME_SEARCH_SKIPPED, tmpHash);
	}

	// pune in SA numai daca nu exista deja valoare in acel camp
	// ret true daca s-a schimbat valoarea, false daca a ramas ce era inainte
	public boolean conditionalSetAtribute(String key, String value) {
		if (value == null) {
			value = "";
		}
		value = value.trim();
		if ("".equals(((String) hashSearch.get(key)).trim())
				&& !"".equals(value)) {
			hashSearch.put(key, value);
			return true;
		}
		return false;
	}

	private static final transient Set<String> HASH_SEARCH_ATTRIBUTES_TO_ADD = new HashSet<String>() {
		private static final long serialVersionUID = 1L;
		{
			add(ASSESSED_VALUE);
			add(ADDITIONAL_EXCEPTIONS);
			add(P_MUNICIPALITY);
			add(LD_SUBDIV_TWN);
			add(LD_SUBDIV_RNG);
			add(LD_SECTION);
			add(LD_BOOKNO_1);
			add(LD_PAGENO_1);
			add(P_IDENTIFIER_TYPE);
			add(POOR_SEARCH_DATA);
			add(TSOPCODE);
			add(LD_ACRES);
			add(ATS_DEFAULT_CERTIFICATION_DATE);
			add(P_STREET_POST_DIRECTION);
			add(LD_NCB_NO);
			add(LD_ABS_NO);
			add(ADDITIONAL_SEARCH_TYPE);
			add(OTHER_RESULTS);
			add(LD_SUBLOT);
		}
	};
	private static final transient Set<String> HASH_SEARCH_ATTRIBUTES_TO_IGNORE = new HashSet<String>() {
		private static final long serialVersionUID = 1L;
		{
			add(LD_PARCELNO_MAP);
			add(LD_PARCELNO_CTRL_MAP);
			add(LD_PARCELNO_GROUP);
			add(LD_PARCELNO_PARCEL);
			add(LD_PARCELNO_CONDO);
			add(LD_PARCELNO_PREFIX);
			add(LD_PARCELNO2);
			add(LD_PARCELNONDB);
			add(LD_SUBDIV_PARCEL);
			add(ARB);
			add(LD_AREA);
			add(LD_PI_AREA);
			add(LD_PI_BLOCK);
			add(LD_PI_PARCEL);
			add(LD_PI_SEC);
			add(LD_PI_UNIT);
			add(LD_PI_MAP_CODE);
			add(LD_PI_MAJ_LEGAL_NAME);
			add(LD_IL_WILL_AO_BLOCK);
			add(LD_IL_WILL_AO_COMP_CODE);
			add(LD_IL_WILL_AO_LOT);
			add(LD_IL_WILL_AO_MISC);
			add(LD_IL_WILL_AO_SEC);
			add(LD_IL_WILL_AO_TWN);
			add(LD_ADDITION);
			add(WEEK);
			add(BUILDING);
			add(AGENT_USER);
			add(AGENT_PASSWORD);
			add(LD_PARCELNO2_ALTERNATE);
			add(LD_PARCELNO3);
			add(STEWARTORDERS_ORDER_ID);
			add(ATIDS_FILE_REFERENCE_ID);
			add(ATIDS_FILE_REFERENCE_CREATED);
			add(SURECLOSE_FILE_ID);
			add(STEWARTORDERS_ORDER_PRODUCT_ID);
			add(STEWARTORDERS_CUSTOMER_GUID);
			add(STEWARTORDERS_PARENT_ORDER_GUID);
			add(STEWARTORDERS_TO_UPDATE_ORDER_GUID);
			add(QUARTER_ORDER);

			add(LD_TS_SUBDIV_NAME);
			add(LD_TS_PLAT_BOOK);
			add(LD_TS_PLAT_PAGE);

			add(LD_TS_LOT);
			add(LD_TS_BLOCK);
			add(OWNER_GUID);
			add(BUYER_GUID);
			add(QUARTER_VALUE);
			add(LEGAL_DESCRIPTION);
			add(LEGAL_DESCRIPTION_STATUS);
			add(ADDITIONAL_REQUIREMENTS);
			add(P_STREET_FULL_NAME_EX);
			add(P_STREETNAME_SUFFIX_UNIT_NO);
			add(P_STREET_FULL_NAME_NO_SUFFIX);
			add(P_STREET_NO_STAR_NA_STAR);
			add(LD_LOT_SUBDIVISION);
			add(LD_PARCELNO_PARCEL_CONDO);
			add(P_STREETDIRECTION_ABBREV);
			add(P_STREETUNIT_CLEANED);
			add(SEARCH_WARNING);
			add(SEARCH_ORIGIN);
			add(SEARCH_STEWART_TYPE);
			add(TITLEDESK_ORDER_ID);
			add(ECORE_AGENT_ID);
			add(TITLE_UNIT);
			add(ADDITIONAL_LENDER_LANGUAGE);
			add(LD_GEO_NUMBER);
			add(ATS_MULTIPLE_LEGALS_FOUND);
			add(ATS_MULTIPLE_LEGAL_INSTRUMENTS);
			add(IS_CONDO);
			add(IS_UPDATED_ONCE);
			add(IS_PLATED);
			add(IS_PRODUCT_CHANGED_ONCE);
			add(IS_UPDATE_OF_A_REFINANCE);
			add(ORIGINAL_SEARCH_OLDER_THAN_ONE_YEAR);
			add(LD_PARCELNO_MAP_GENERIC_TR);
			add(LD_PARCELNO_CTRL_MAP_GENERIC_TR);
			add(LD_PARCELNO_GROUP_GENERIC_TR);
			add(LD_PARCELNO_PARCEL_GENERIC_TR);
			add(FVS_UPDATE);
			add(LAST_SCHEDULED_FVS_UPDATE);
			add(FVS_UPDATE_AUTO_LAUNCHED);
			add(LD_MO_PLATTE_TWN);
			add(LD_MO_PLATTE_AREA);
			add(LD_MO_PLATTE_BLOCK);
			add(LD_MO_PLATTE_PARCEL);
			add(LD_MO_PLATTE_QTRSECT);
			add(LD_MO_PLATTE_SECT);
			add(LD_DISTRICT);
		}
	};

	/**
	 * @see ro.cst.tsearch.servers.response.InfSet#setAtribute(java.lang.String,
	 *      java.lang.String)
	 */
	public void setAtribute(String key, String value) {
		// cleanup the value
		if (value == null) {
			value = "";
		} else {
			value = value.trim();
		}

		if (!hashSearch.containsKey(key)) {
			if (HASH_SEARCH_ATTRIBUTES_TO_ADD.contains(key)) {
				hashSearch.put(key, value);
			} else if (HASH_SEARCH_ATTRIBUTES_TO_IGNORE.contains(key)) {
				// do nothing
			} else if (key.equals(P_STREET_FULL_NAME)) {
				setFullStreetName(key, value);
			} else if (IGNORE_MNAME_BUYER.contains(key)) {
				hashSearch.put(key, value);
			} else {
//				throw new IllegalArgumentException(
//						"Unknown Search atribute! >>> key = " + key);
				
				Log.sendExceptionViaEmail(
						MailConfig.getMailLoggerToEmailAddress(), 
						"SetAtribute with key " + key, 
						null, 
						"SearchId used: " + searchId + ", Key used: " + key + ", Value used: " + value);
			}
		} else {
			hashSearch.put(key, value);
		}
	}

	/**
	 * function to set the STREET_FULL_NAME type of search attributes was
	 * separated in order to treat all the cases with the same code
	 * 
	 * @param key
	 *            the key - can only be P_STREET_FULL_NAME
	 * @param value
	 *            the new value
	 */
	private void setFullStreetName(String key, String value) {
		String dirKey, nameKey, suffKey, unitKey, noKey, postDirKey = "";

		// we only need to set the unit for property and owner - to preserve
		// original behaviour
		boolean handleUnit = false;

		// decide which keys to use
		if (key.equals(P_STREET_FULL_NAME)) {
			dirKey = P_STREETDIRECTION;
			nameKey = P_STREETNAME;
			suffKey = P_STREETSUFIX;
			postDirKey = P_STREET_POST_DIRECTION;
			unitKey = P_STREETUNIT;
			noKey = P_STREETNO;
			handleUnit = true;
		} else {
			return; // the function is meant to treat only the above cases
		}

		// parse the string composed of street number and street full name
		StandardAddress stdAddr = new StandardAddress(
				((String) hashSearch.get(noKey)) + " " + value);
		// set the keys
		if (("".equals(stdAddr
				.getAddressElement(StandardAddress.STREET_PREDIRECTIONAL)))
				&& (!"".equals(stdAddr
						.getAddressElement(StandardAddress.STREET_POSTDIRECTIONAL)))) {
			if (!postDirKey.isEmpty()) {
				hashSearch
						.put(postDirKey,
								stdAddr.getAddressElement(StandardAddress.STREET_POSTDIRECTIONAL));
			} else {
				hashSearch
						.put(dirKey,
								stdAddr.getAddressElement(StandardAddress.STREET_POSTDIRECTIONAL));
			}

		} else {
			hashSearch.put(dirKey, stdAddr
					.getAddressElement(StandardAddress.STREET_PREDIRECTIONAL));
			if (!postDirKey.isEmpty()) {
				hashSearch.put(postDirKey, stdAddr.getAddressElement(StandardAddress.STREET_POSTDIRECTIONAL));
			}
		}

		if (key.equals(P_STREET_FULL_NAME)) {
			if (NameNormalizer
					.normalize((String) hashSearch.get(nameKey))
					.toUpperCase()
					.equals(stdAddr
							.getAddressElement(StandardAddress.STREET_NAME))
					&& !((String) hashSearch.get(nameKey))
							.toUpperCase()
							.equals(stdAddr
									.getAddressElement(StandardAddress.STREET_NAME))) {
				// do nothing, leave the old street name, just go to Upper Case
				hashSearch.put(nameKey,
						((String) hashSearch.get(nameKey)).toUpperCase());
			} else {
				hashSearch.put(nameKey,
						stdAddr.getAddressElement(StandardAddress.STREET_NAME));
			}
		} else {
			hashSearch.put(nameKey,
					stdAddr.getAddressElement(StandardAddress.STREET_NAME));
		}

		hashSearch.put(suffKey,
				stdAddr.getAddressElement(StandardAddress.STREET_SUFFIX));
		// only set the unit key for property and owner addresses - to preserve
		// original behaviour
		if (handleUnit) {
			String parsedUnit = (stdAddr
					.getAddressElement(StandardAddress.STREET_SEC_ADDR_RANGE))
					.trim();

			if (CountyConstants.AK_Anchorage_Borough_STRING
					.equals(getCountyId())
					&& "true"
							.equalsIgnoreCase(getAtribute(SearchAttributes.IS_CONDO))) {
				if (StringUtils.isEmpty(getAtribute(P_STREETUNIT))) {
					if (!StringUtils.isEmpty(parsedUnit)) {
						hashSearch.put(unitKey, parsedUnit);
					}
				}
			} else {

				if (!StringUtils.isEmpty(parsedUnit)) {
					hashSearch.put(unitKey, parsedUnit);
				}
			}
		}
	}

	public String toString() {
		return "Search Attributes " + "[owner = " + getOwners().toString() + "; " + "address = "
				+ getAtribute(P_STREET_FULL_NAME) + " "
				+ getAtribute(P_STREETNO) + ", " + getAtribute(P_CITY) + " ["
				+ getAtribute(P_STATE) + "] " + getAtribute(P_ZIP) + "; ("
				+ getAtribute(P_COUNTY) + ")" 
				+ "lot#=" + getAtribute(LD_LOTNO) + "; "
				+ "sublot#=" + getAtribute(LD_SUBLOT) + "; "
				+ "subdiv=" + getAtribute(LD_SUBDIV_NAME) + "; "
				+ "sec=" + getAtribute(LD_SUBDIV_SEC) + "; " + "phase="
				+ getAtribute(LD_SUBDIV_PHASE) + "; " + "code="
				+ getAtribute(LD_SUBDIV_CODE) + "; " + " unit#="
				+ getAtribute(LD_SUBDIV_UNIT) + " fromdate = "
				+ getAtribute(FROMDATE) + ", todate= " + getAtribute(TODATE)
				+ "; " + "]";
	}

	/**
	 * Formats legal description for logging purposes
	 * 
	 * @return
	 */
	private String displayLegal() {

		// determine each item
		String lot = getAtribute(LD_LOTNO);
		String subLot = getAtribute(LD_SUBLOT);
		String block = getAtribute(LD_SUBDIV_BLOCK);
		String subdiv = getAtribute(LD_SUBDIV_NAME);
		String subdivSec = getAtribute(LD_SECTION);
		String secTownship = getAtribute(LD_SUBDIV_SEC);
		String twn = getAtribute(LD_SUBDIV_TWN);
		String rng = getAtribute(LD_SUBDIV_RNG);
		String phase = getAtribute(LD_SUBDIV_PHASE);
		String code = getAtribute(LD_SUBDIV_CODE);
		String unit = getAtribute(LD_SUBDIV_UNIT);
		String building = getAtribute(BUILDING);
		String tract = getAtribute(LD_SUBDIV_TRACT);
		String pb = getAtribute(LD_BOOKNO);
		String pp = getAtribute(LD_PAGENO);

		// concatenate items
		String retVal = "";
		if (!"".equals(lot)) {
			retVal += "lot: " + lot + " ";
		}
		if (!"".equals(subLot)) {
			retVal += "sublot: " + subLot + " ";
		}
		if (!"".equals(block)) {
			retVal += "block: " + block + " ";
		}
		if (!"".equals(subdiv)) {
			retVal += "subdiv: " + subdiv + " ";
		}
		if (!"".equals(subdivSec)) {
			retVal += "subdiv section: " + subdivSec + " ";
		}
		if (!"".equals(secTownship)) {
			retVal += "township section: " + secTownship + " ";
		}
		if (!"".equals(twn)) {
			retVal += "twn: " + twn + " ";
		}
		if (!"".equals(rng)) {
			retVal += "rng: " + rng + " ";
		}
		if (!"".equals(phase)) {
			retVal += "phase: " + phase + " ";
		}
		if (!"".equals(code)) {
			retVal += "code: " + code + " ";
		}
		if (!"".equals(unit)) {
			retVal += "unit: " + unit + " ";
		}
		if (StringUtils.isNotEmpty(building)) {
			retVal += "building: " + building + " ";
		}
		if (StringUtils.isNotEmpty(tract)) {
			retVal += "tract: " + tract + " ";
		}
		if (StringUtils.isNotEmpty(pb)) {
			retVal += "pb: " + pb + " ";
		}
		if (StringUtils.isNotEmpty(pp)) {
			retVal += "pp: " + pp + " ";
		}
		return retVal.trim();
	}

	/**
	 * formats owner, property address, legal description for logging
	 * purposes
	 * 
	 * @return
	 */
	public String display() {
		String owner = getOwners().getFullNames(NameFormaterI.PosType.LFM, "/");

		String address = getPropertyAddress().toString(Address.FORMAT_LOG);
		String legal = displayLegal();

		// concatenate items
		String retVal = "";
		if (!"".equals(owner)) {
			retVal += "Owner(s) = <b>" + owner + "</b> ";
		}
		if (!"".equals(address)) {
			retVal += "Address = <b>" + address + "</b> ";
		}
		if (!"".equals(legal)) {
			retVal += "Legal = <b>" + legal + "</b> ";
		}
		return retVal.trim();
	}

	public Address getPropertyAddress() {
		Address a1 = new Address(
				getAtribute(SearchAttributes.P_STREETNO),
				getAtribute(SearchAttributes.P_STREETDIRECTION),
				getAtribute(SearchAttributes.P_STREETNAME),
				getAtribute(SearchAttributes.P_STREETSUFIX),
				getAtribute(SearchAttributes.P_STREET_POST_DIRECTION),
				getAtribute(SearchAttributes.P_STREETUNIT),
				getAtribute(SearchAttributes.P_CITY),
				FormatSa.getStateNameAbbrev(getAtribute(SearchAttributes.P_STATE)),
				getAtribute(SearchAttributes.P_ZIP));
		return a1;
	}

	public void setOrderedBy(UserAttributes agentAttributes) {
		this.setAtribute(SearchAttributes.ORDERBY_ID, agentAttributes.getID()
				.toString());
		this.setAtribute(SearchAttributes.ORDERBY_FNAME,
				agentAttributes.getFIRSTNAME());
		this.setAtribute(SearchAttributes.ORDERBY_MNAME,
				agentAttributes.getMIDDLENAME());
		this.setAtribute(SearchAttributes.ORDERBY_LNAME,
				agentAttributes.getLASTNAME());
	}

	public void resetOrderedBy() {
		this.setAtribute(SearchAttributes.ORDERBY_ID, "");
		this.setAtribute(SearchAttributes.ORDERBY_FNAME, "");
		this.setAtribute(SearchAttributes.ORDERBY_MNAME, "");
		this.setAtribute(SearchAttributes.ORDERBY_LNAME, "");
	}

	public void setAbstractor(UserAttributes userAttributes) {
		this.hashSearch.put(SearchAttributes.ABSTRACTOR_OBJECT, userAttributes);
	}

	public UserAttributes getAbstractorObject() {
		return (UserAttributes) hashSearch.get(ABSTRACTOR_OBJECT);
	}

	public static String getHTMLSearchProducts(long commId, int searchProd,
			long searchId) {
		StringBuilder htmlCode = new StringBuilder();

		long newCommID = -1;
		try {
			newCommID = InstanceManager.getManager()
					.getCurrentInstance(searchId).getCurrentCommunity().getID()
					.longValue();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (newCommID > 0) {
			commId = newCommID;
		}

		Products currentCommunityProducts = CommunityProducts.getProduct(commId);
		List<ProductsMapper> products = DBManager.getProducts(true);

		for (int i = 0; i < products.size(); i++) {
			ProductsMapper product = products.get(i);
			int product_id = product.getProductId();

			htmlCode.append("<option value=\"").append(product_id).append("\"");
			
			if (product_id == searchProd) {
				htmlCode.append(" selected ");
			} else {
				htmlCode.append(" ");
			}
			
			htmlCode.append(">").append(currentCommunityProducts.getProductName(product_id))
				.append(" (").append(product_id).append(")").append("</option>");
		}

		return htmlCode.toString();
	}

	public static String getHTMLSearchProductOptionById(long commId, int product_id, long searchId) {
		String htmlCode = "";

		long newCommID = -1;
		try {
			newCommID = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity().getID().longValue();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (newCommID > 0) {
			commId = newCommID;
		}
		Products currentCommunityProducts = CommunityProducts.getProduct(commId);

		htmlCode += "<option value=\"" + product_id + "\" selected  >" 
						+ currentCommunityProducts.getProductName(product_id) + " (" + product_id + ")"
					+ "</option>";
		return htmlCode;
	}
	// ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// private functions

	public static String getSearchProduct(String productId) {
		String productName = "";
		DBConnection conn = null;

		try {
			conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData sqlResult = conn
					.executeSQL("SELECT * FROM " + DBConstants.TABLE_SEARCH_PRODUCTS + " WHERE PRODUCT_ID="
							+ productId);
			productName = sqlResult.getValue("NAME", 0).toString();
		} catch (BaseException e) {
			logger.error("Error to get a connection to the database!");
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				logger.error(e);
			}
		}
		return productName;
	}

	public int getInstrumentOrigin(String instr) {
		List l = (List) getObjectAtribute(SearchAttributes.INSTR_LIST);
		boolean isBP = instr.matches("\\w+_\\w+");
		if (l != null && l.size() > 0) {
			for (Iterator iter = l.iterator(); iter.hasNext();) {
				Instrument element = (Instrument) iter.next();
				String instrNo = (isBP ? element.getBookNo() + "_"
						+ element.getPageNo() : element.getInstrumentNo());
				if (instr.equals(instrNo)) {
					return element.getOrigin();
				}
			}
		}
		return Instrument.NO_ORIGIN_DEFINED;
	}

	/**
	 * @return Returns the abstrFileName.
	 */
	public String getAbstractorFileName() {
		return abstrFileName;
	}

	/**
	 * @param abstrFileName
	 *            The abstrFileName to set.
	 */
	public void setAbstractorFileName(Search global) {
		String shortName = Products.getProductShortNameStringLength3(searchId)
				.toUpperCase();
		SearchAttributes sa = global.getSa();
		boolean isUpdate = sa.getAtribute(SearchAttributes.SEARCHUPDATE).trim()
				.toLowerCase().equals("true");

		String defaultFileName = sa
				.getAtribute(SearchAttributes.ABSTRACTOR_FILENO);

		defaultFileName = defaultFileName.replaceAll("_", ""); // elimin _ din
																// fileID
																// introdus de
																// abstractor

		if (defaultFileName.trim().equals(""))
			defaultFileName = "UnknownFileNo";

		if (global.getSearchFlags().isBase()) {
			shortName = "BAS";
		} else {
			if (isUpdate) {
				shortName = "TSU";
			} else if (isFVSUpdate()){
				shortName = "FVS";
			}
		}

		defaultFileName = shortName + "-" + defaultFileName + "_"
				+ new SimpleDateFormat(FormatDate.PATTERN_MMddyyyy_HHmmss).format(new Date())
				+ ((isUpdate || isFVSUpdate()) ? "_" + global.getTsuNo() : "");
		this.abstrFileName = defaultFileName;
	}

	public static String getEntity(int entityId) {
		if (entityId == COMP_INDIVIDUAL)
			return "Individual";
		if (entityId == COMP_TRUST)
			return "Trust";
		if (entityId == COMP_CORPORATION)
			return "Corporation";
		if (entityId == COMP_PART)
			return "Part.";
		if (entityId == COMP_PARTNERSHIP)
			return "Partnership";
		if (entityId == COMP_FEDGOV)
			return "Fed. Gov.";
		if (entityId == COMP_STATEGOV)
			return "State Gov";
		if (entityId == COMP_FEDBANK)
			return "Fed. Bank";
		if (entityId == COMP_STATEBANK)
			return "State Bank";
		if (entityId == COMP_LLC)
			return "LLC";
		return "";
	}

	public synchronized Object clone() {

		try {

			SearchAttributes newSearchAttributes = (SearchAttributes) super
					.clone();

			newSearchAttributes.abstrFileName = abstrFileName;

			newSearchAttributes.hashSearch = new HashMap();
			if (hashSearch != null) {
				for (Iterator iter = hashSearch.keySet().iterator(); iter
						.hasNext();) {

					try {
						String key = (String) iter.next();
						Object value = hashSearch.get(key);
						if (value instanceof String)
							newSearchAttributes.hashSearch.put(key, new String(
									(String) value));
						else if (value instanceof UserAttributes)
							newSearchAttributes.hashSearch.put(key,
									((UserAttributes) value).clone());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			try {
				newSearchAttributes.extraHashSearch = (HashMap) extraHashSearch
						.clone();
			} catch (Exception e) {
				e.printStackTrace();
			}

			return newSearchAttributes;

		} catch (CloneNotSupportedException cnse) {
			throw new InternalError();
		}
	}

	/**
	 * @return Returns the isSet.
	 */
	public boolean isSet() {
		return isSet;
	}

	/**
	 * @param isSet
	 *            The isSet to set.
	 */
	public void setSet(boolean isSet) {
		this.isSet = isSet;
	}

	public void setSearchId(long searchId) {
		this.searchId = searchId;
	}

	public static SimpleDateFormat DATE_FORMAT_MMddyyyy = new SimpleDateFormat(
			"MM/dd/yyyy");

	public void setCertificationDate() {

		if ("0".equals(getCountyId())){
			return;
		}
		
		String certDate = "";		
		
		
		if (certification == null){
			certification = new CertificationDate();
		}
		
		CertificationDateManager certificationDateManager = getCertificationDateManager();
		if (!certificationDateManager.isEmpty()){
		    CertificationDateDS searchCertificationDate = certificationDateManager.getSearchCertificationDate();
		    if(searchCertificationDate != null) {
				Date maxDate = searchCertificationDate.getCertificationDateDS();
				//used below and is important if certDate is empty
				certDate = new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY).format(maxDate);
				if (!certification.isEdited()){
					if (certification.getDate() == null || certification.getDate().before(maxDate)){
						SearchLogger.info("Set certification date from " + searchCertificationDate.toHtml() + "<br>", searchId);
						certification.setDate(maxDate);
						certification.setDefaultDate(false);
						setAtribute(SearchAttributes.ATS_DEFAULT_CERTIFICATION_DATE, "false");
					} else if (certification.isDefaultDate()){
						SearchLogger.info("Set certification date from " + searchCertificationDate.toHtml() + "<br>", searchId);
						certification.setDate(maxDate);
						certification.setDefaultDate(false);
						setAtribute(SearchAttributes.ATS_DEFAULT_CERTIFICATION_DATE, "false");
					}
				}
		    }
		} else {
			//for old searches
			certDate = getAtribute(SearchAttributes.CERTICICATION_DATE);
			if (org.apache.commons.lang.StringUtils.isNotEmpty(certDate)){
				try {
					if (certification.getDate() == null){
						certification.setDate(DEFAULT_DATE_PARSER.parse(certDate));
						certification.setDefaultDate(false);
						setAtribute(SearchAttributes.ATS_DEFAULT_CERTIFICATION_DATE, "false");
					}
				} catch (ParseException e) {
					logger.error("Could not parse certification date from CERTICICATION_DATE for an old search " + searchId + " " + e);
				}
			}
		}
		
		if ("".equals(certDate.trim())){
			int offset = CertificationDateManager.CERTIFICATION_DATE_OFFSET_FOR_EMPTY_INPUT;
			try {
				offset = CountyCommunityManager.getInstance().getCountyCommunityMapper(Integer.parseInt(getCountyId()), getCommId())
										.getDefaultCertificationDateOffset();
			} catch (Exception e) {
				logger.error("Offset for Certification Date can't be obtained. " + searchId);
			}
			if (offset != CertificationDateManager.CERTIFICATION_DATE_OFFSET_FOR_EMPTY_INPUT){
				if (certification.getDate() == null){
					Calendar date = Calendar.getInstance();
					date.add(java.util.Calendar.DATE, -offset); // go back N days
					
					certification.setDate(date.getTime());
					
					certDate = DATE_FORMAT_MMddyyyy.format(date.getTime());
					SearchLogger.info("Set certification date to ATS default : " + certDate + "<br>", searchId);
						
					setAtribute(SearchAttributes.ATS_DEFAULT_CERTIFICATION_DATE, "true");
					certification.setDefaultDate(true);
				}
			}
		}
	}
	
	
	
	
	/**
	 * 
	 * @param dataSite
	 * @param candidateDate
	 * @return true if the candidate value was set
	 */
	public boolean updateCertificationDateObject(DataSite dataSite, Date candidateDate){
		
		if (candidateDate != null){
			CertificationDateDS newDate = new CertificationDateDS(candidateDate, dataSite.getSiteTypeInt());
			return updateCertificationDateObject(newDate, dataSite);
		}
		
		return false;
	}
	
	public boolean updateCertificationDateObject(CertificationDateDS newDate, DataSite dataSite) {
		
		if(newDate != null) {
			CertificationDateManager certificationDateManager = getCertificationDateManager();
			return certificationDateManager.updateCertificationDate(newDate, this);
		}
		return false;
		
	}
	
	public ArrayList<CertificationDateDS> getCertificationDateOnSearchObject(){
		
		return (ArrayList<CertificationDateDS>) getObjectAtribute(SearchAttributes.CERTIFICATION_DATE_ON_SEARCH);
	}
	
	public CertificationDateManager getCertificationDateManager() {
		return new CertificationDateManager(getCertificationDateOnSearchObject());
	}
	
	/**
	 * @return the originalSearhId for an update search
	 */
	public long getOriginalSearchId() {
		return originalSearchId;
	}

	public void setOriginalSearchId(long originalSearchId) {
		this.originalSearchId = originalSearchId;
	}

	public long getSearchId() {
		return searchId;
	}

	public Integer getProductId() {
		try {
			if (getAtribute(SearchAttributes.SEARCH_PRODUCT).isEmpty()) {
				return 1;
			}
			return new Integer(getAtribute(SearchAttributes.SEARCH_PRODUCT));
		} catch (NumberFormatException e) {
			logger.error("Could not parse search product id", e);
		}
		return null;
	}
	
	public void setProductId(int productId) {
		setAtribute(SearchAttributes.SEARCH_PRODUCT, String.valueOf(productId));
	}

	/**
	 * Obtain search state and county, ie ILCook
	 * 
	 * @return state county
	 */
	public String getStateCounty() {
		long stateId = Long.parseLong(getAtribute(SearchAttributes.P_STATE));
		long countyId = Long.parseLong(getAtribute(SearchAttributes.P_COUNTY));
		return DBManager.getStateForId(stateId).getStateAbv()
				+ DBManager.getCountyForId(countyId).getName();
	}
	
	/**
	 * Obtain search state abbreviation, e.g. IL
	 * @return state abbreviation
	 */
	public String getStateAbv() {
		long stateId = Long.parseLong(getAtribute(SearchAttributes.P_STATE));
		return DBManager.getStateForId(stateId).getStateAbv();
	}

	/**
	 * 
	 * @return countyName
	 */
	public String getCountyName() {
		long countyId = Long.parseLong(getAtribute(SearchAttributes.P_COUNTY));
		return DBManager.getCountyForId(countyId).getName();
	}

	public String getCountyId() {
		return getAtribute(SearchAttributes.P_COUNTY);
	}
	
	public String getStateId() {
		return getAtribute(SearchAttributes.P_STATE);
	}

	/**
	 * This function creates an xml order from the search
	 * 
	 * @return the order
	 */
	public static String getXmlOrder(Search search) {

		StringBuffer sb = new StringBuffer();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sb.append("<ats version=\"3.x\">");
		sb.append("<order>");

		long commId = -1;
		try {
			commId = InstanceManager.getManager()
					.getCurrentInstance(search.getID()).getCurrentCommunity()
					.getID().longValue();
		} catch (Exception e) {
			commId = DBManager.getCommunityForSearch(search.getID());
		}

		sb.append("<product_type>"
				+ escapeXml(MailTemplateUtils.getTransactionTypeString(search
						.getSa())) + "</product_type>");
		sb.append("<from_date>");
		Calendar cal = Calendar.getInstance();
		try { // reading the from date - I KNOW it will not through an exception
			cal.setTime(Util.dateParser3((search.getSa()
					.getAtribute(SearchAttributes.FROMDATE))));
		} catch (Exception e) {
			e.printStackTrace();
		}

		sb.append("<year>" + cal.get(Calendar.YEAR) + "</year>");
		sb.append("<month>" + (cal.get(Calendar.MONTH) + 1) + "</month>");
		sb.append("<day>" + cal.get(Calendar.DATE) + "</day>");
		sb.append("</from_date>");

		try {
			cal.setTime(Util.dateParser3(search.getSa().getAtribute(
					SearchAttributes.TODATE)));
		} catch (Exception e) {
			e.printStackTrace();
		}
		sb.append("<thru_date>");
		sb.append("<year>" + cal.get(Calendar.YEAR) + "</year>");
		sb.append("<month>" + (cal.get(Calendar.MONTH) + 1) + "</month>");
		sb.append("<day>" + cal.get(Calendar.DATE) + "</day>");
		sb.append("</thru_date>");

		sb.append("<property>");
		try {
			sb.append(search.getSa().getXmlAddress());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Something when wrong while formatting xml address for searchid " + search.getID(), e);
		}
		try {
			sb.append(search.getSa().getXmlLegalDescription());
		} catch (Exception e) {
			logger.error("Something when wrong while formatting xml description for searchid " + search.getID(), e);
			e.printStackTrace();
		}
		String pid = search.getSa().getAtribute(SearchAttributes.LD_PARCELNO);
		if (pid != null && !pid.equals("")) {
			sb.append("<property_id>" + escapeXml(pid) + "</property_id>");
		}
		sb.append("</property>");
		sb.append(search.getSa().getXmlOwners());
		sb.append(search.getSa().getXmlBuyers());
		sb.append(search.getSa().getXmlSaleValue());
		String lender = search.getSa().getAtribute(
				SearchAttributes.BM1_LENDERNAME);
		if (lender != null && !lender.equals(""))
			sb.append("<lender>" + escapeXml(lender) + "</lender>");
		// the next field is Amount not account, I don't know why the name
		// difference
		String loanAmount = search.getSa().getAtribute(
				SearchAttributes.BM1_LOADACCOUNTNO);
		if (loanAmount != null && !loanAmount.equals(""))
			sb.append("<loan_amount>" + escapeXml(loanAmount)
					+ "</loan_amount>");
		sb.append("<agent_file_id>"
				+ escapeXml(search.getSa().getAtribute(
						SearchAttributes.ORDERBY_FILENO)) + "</agent_file_id>");
		sb.append("<community_file_id>"
				+ escapeXml(search.getSa().getAtribute(
						SearchAttributes.ABSTRACTOR_FILENO)
						+ "_" + search.getSearchID()) + "</community_file_id>");
		String information = (String) search.getSa().getAtribute(
				SearchAttributes.ADDITIONAL_INFORMATION);
		if (information != null && !information.equals(""))
			sb.append("<information>" + escapeXml(information)
					+ "</information>");
		String requirements = (String) search.getSa().getAtribute(
				SearchAttributes.ADDITIONAL_REQUIREMENTS);
		if (requirements != null && !requirements.equals(""))
			sb.append("<requirements>" + escapeXml(requirements)
					+ "</requirements>");
		String exceptions = (String) search.getSa().getAtribute(
				SearchAttributes.ADDITIONAL_EXCEPTIONS);
		if (exceptions != null && !exceptions.equals(""))
			sb.append("<exceptions>" + escapeXml(exceptions) + "</exceptions>");
		String agentName = null;
		if (search.getAgent() != null)
			agentName = search.getAgent().getLOGIN();
		if (!isEmpty(agentName))
			sb.append("<agent_name>" + escapeXml(agentName) + "</agent_name>");
		sb.append("</order>");
		sb.append("</ats>");

		return sb.toString();
	}

	private String getXmlAddress() {
		StringBuffer sb = new StringBuffer();
		sb.append("<address>");
		String number = getAtribute(SearchAttributes.P_STREETNO);
		String pre_direction = getAtribute(SearchAttributes.P_STREETDIRECTION);
		String name = getAtribute(SearchAttributes.P_STREETNAME);
		String suffix = getAtribute(SearchAttributes.P_STREETSUFIX);
		String post_direction = getAtribute(SearchAttributes.P_STREET_POST_DIRECTION);
		String unit = getAtribute(SearchAttributes.P_STREETUNIT);
		String type = getAtribute(SearchAttributes.P_IDENTIFIER_TYPE);
		if (!isEmpty(number) || !isEmpty(pre_direction) || !isEmpty(name)
				|| !isEmpty(suffix) || !isEmpty(post_direction)
				|| !isEmpty(unit) || StringUtils.isNotEmpty(type)) {
			// this means we have some info for the street
			sb.append("<street>");
			if (!isEmpty(number)) {
				String[] numbers = number.split("[ ]*,[ ]*");
				if (numbers.length > 1) { // we have a number list
					sb.append("<number_list>");
					for (int i = 0; i < numbers.length; i++) {
						String[] limits = numbers[i].split("[ ]*-[ ]*");
						if (limits.length > 1) {// interval
							sb.append("<interval>");
							sb.append("<low>" + escapeXml(limits[0]) + "</low>");
							sb.append("<high>" + escapeXml(limits[1])
									+ "</high>");
							sb.append("</interval>");
						} else {
							sb.append("<value>" + escapeXml(limits[0])
									+ "</value>");
						}
					}
					sb.append("</number_list>");
				} else { // we have just a number that can also be an interval
					String[] limits = numbers[0].split("[ ]*-[ ]*");
					if (limits.length > 1) {// interval
						sb.append("<number_list>");
						sb.append("<interval>");
						sb.append("<low>" + escapeXml(limits[0]) + "</low>");
						sb.append("<high>" + escapeXml(limits[1]) + "</high>");
						sb.append("</interval>");
						sb.append("</number_list>");
					} else {
						sb.append("<number>" + escapeXml(limits[0])
								+ "</number>");
					}
				}
			}
			if (!isEmpty(pre_direction)) {
				sb.append("<pre_direction>" + escapeXml(pre_direction)
						+ "</pre_direction>");
			}
			if (!isEmpty(name)) {
				sb.append("<name>" + escapeXml(name) + "</name>");
			}
			if (!isEmpty(suffix)) {
				sb.append("<suffix>" + escapeXml(suffix) + "</suffix>");
			}
			if (!isEmpty(post_direction)) {
				sb.append("<post_direction>" + escapeXml(post_direction)
						+ "</post_direction>");
			}
			if (!isEmpty(type)) {
				sb.append("<type>" + escapeXml(type) + "</type>");
			}
			if (!isEmpty(unit)) {
				sb.append("<unit>");
				String[] numbers = unit.split("[ ]*,[ ]*");
				if (numbers.length > 1) { // we have a number list
					sb.append("<number_list>");
					for (int i = 0; i < numbers.length; i++) {
						String[] limits = numbers[i].split("[ ]*-[ ]*");
						if (limits.length > 1) {// interval
							sb.append("<interval>");
							sb.append("<low>" + escapeXml(limits[0]) + "</low>");
							sb.append("<high>" + escapeXml(limits[1])
									+ "</high>");
							sb.append("</interval>");
						} else {
							sb.append("<value>" + escapeXml(limits[0])
									+ "</value>");
						}
					}
					sb.append("</number_list>");
				} else { // we have just a number that can also be an interval
					String[] limits = numbers[0].split("[ ]*-[ ]*");
					if (limits.length > 1) {// interval
						sb.append("<number_list>");
						sb.append("<interval>");
						sb.append("<low>" + escapeXml(limits[0]) + "</low>");
						sb.append("<high>" + escapeXml(limits[1]) + "</high>");
						sb.append("</interval>");
						sb.append("</number_list>");
					} else {
						sb.append("<number>" + escapeXml(limits[0])
								+ "</number>");
					}
				}
				sb.append("</unit>");
			}
			sb.append("</street>");
		}
		String city = getAtribute(SearchAttributes.P_CITY);
		if (!isEmpty(city))
			sb.append("<city>" + escapeXml(city) + "</city>");
		String zip = getAtribute(SearchAttributes.P_ZIP);
		if (!isEmpty(zip))
			sb.append("<zip>" + escapeXml(zip) + "</zip>");
		GenericCounty county = DBManager.getCountyForId(Long
				.parseLong(getAtribute(SearchAttributes.P_COUNTY)));
		GenericState state = DBManager.getStateForId(county.getStateId());
		sb.append("<state>" + escapeXml(state.getStateAbv()) + "</state>");
		sb.append("<county>" + escapeXml(county.getName()) + "</county>");
		sb.append("</address>");
		return sb.toString();
	}

	private String getXmlLegalDescription() {

		boolean isSubdivided = false;
		boolean isSectional = false;
		boolean isFreeform = false;
		StringBuffer sb = new StringBuffer();
		String lot = getAtribute(SearchAttributes.LD_LOTNO);
		String subLot = getAtribute(SearchAttributes.LD_SUBLOT);
		String subdivision = getAtribute(SearchAttributes.LD_SUBDIV_NAME);
		String block = getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
		String sectionTownship = getAtribute(SearchAttributes.LD_SUBDIV_SEC);
		String township = getAtribute(SearchAttributes.LD_SUBDIV_TWN);
		String range = getAtribute(SearchAttributes.LD_SUBDIV_RNG);
		String book = getAtribute(SearchAttributes.LD_BOOKNO);
		String page = getAtribute(SearchAttributes.LD_PAGENO);
		String sectionSubdivision = getAtribute(SearchAttributes.LD_SECTION);
		String unit = getAtribute(SearchAttributes.LD_SUBDIV_UNIT);

		isSectional = !isEmpty(sectionTownship) && !isEmpty(township)
				&& !isEmpty(range);

		if ((lot != null && !lot.equals(""))
				|| (subdivision != null && !subdivision.equals(""))
				|| (block != null && !block.equals(""))
				|| (sectionSubdivision != null && !sectionSubdivision
						.equals(""))
				|| (sectionTownship != null && !sectionTownship.equals(""))
				|| (book != null && !book.equals(""))
				|| (page != null && !page.equals(""))
				|| StringUtils.isNotEmpty(unit))
			isSubdivided = true;
		// here you should add code to read sectional and freeform sections in
		// the xml
		if (isSubdivided || isSectional || isFreeform) {
			sb.append("<legal>");
			if (isSubdivided) {
				sb.append("<subdivided>");
				if (!isEmpty(lot)) {
					String[] lots = lot.replaceAll("\\s*-\\s*", "-").split("[ ,]+");
					if (lots.length > 1) { // we have a lot list
						sb.append("<lot_list>");
						for (int i = 0; i < lots.length; i++) {
							String[] limits = lots[i].split("[ ]*-[ ]*");
							if (limits.length > 1) {// interval
								sb.append("<interval>");
								sb.append("<low>" + escapeXml(limits[0])
										+ "</low>");
								sb.append("<high>" + escapeXml(limits[1])
										+ "</high>");
								sb.append("</interval>");
							} else if (limits.length == 1) {
								sb.append("<value>" + escapeXml(limits[0])
										+ "</value>");
							}
						}
						sb.append("</lot_list>");
					} else { // we have just a lot that can also be an interval
						String[] limits = lots[0].split("[ ]*-[ ]*");
						if (limits.length > 1) {// interval
							sb.append("<lot_list>");
							sb.append("<interval>");
							sb.append("<low>" + escapeXml(limits[0]) + "</low>");
							sb.append("<high>" + escapeXml(limits[1])
									+ "</high>");
							sb.append("</interval>");
							sb.append("</lot_list>");
						} else if (limits.length == 1) {
							sb.append("<lot>" + escapeXml(limits[0]) + "</lot>");
						}
					}
				}
				if (!isEmpty(subLot)) {
					String[] subLots = subLot.replaceAll("\\s*-\\s*", "-").split("[ ,]+");
					if (subLots.length > 1) { // we have a lot list
						sb.append("<lot_list>");
						for (int i = 0; i < subLots.length; i++) {
							String[] limits = subLots[i].split("[ ]*-[ ]*");
							if (limits.length > 1) {// interval
								sb.append("<interval>");
								sb.append("<low>" + escapeXml(limits[0])
										+ "</low>");
								sb.append("<high>" + escapeXml(limits[1])
										+ "</high>");
								sb.append("</interval>");
							} else if (limits.length == 1) {
								sb.append("<value>" + escapeXml(limits[0])
										+ "</value>");
							}
						}
						sb.append("</lot_list>");
					} else { // we have just a lot that can also be an interval
						String[] limits = subLots[0].split("[ ]*-[ ]*");
						if (limits.length > 1) {// interval
							sb.append("<lot_list>");
							sb.append("<interval>");
							sb.append("<low>" + escapeXml(limits[0]) + "</low>");
							sb.append("<high>" + escapeXml(limits[1])
									+ "</high>");
							sb.append("</interval>");
							sb.append("</lot_list>");
						} else if (limits.length == 1)  {
							sb.append("<lot>" + escapeXml(limits[0]) + "</lot>");
						}
					}
				}
				if (!isEmpty(block)) {
					String[] blocks = block.replaceAll("\\s*-\\s*", "-").split("[ ]*,[ ]*");
					if (blocks.length > 1) { // we have a block list
						sb.append("<block_list>");
						for (int i = 0; i < blocks.length; i++) {
							String[] limits = blocks[i].split("[ ]*-[ ]*");
							if (limits.length > 1) {// interval
								sb.append("<interval>");
								sb.append("<low>" + escapeXml(limits[0])
										+ "</low>");
								sb.append("<high>" + escapeXml(limits[1])
										+ "</high>");
								sb.append("</interval>");
							} else if (limits.length == 1) {
								sb.append("<value>" + escapeXml(limits[0])
										+ "</value>");
							}
						}
						sb.append("</block_list>");
					} else { // we have just a block that can also be an
								// interval
						String[] limits = blocks[0].split("[ ]*-[ ]*");
						if (limits.length > 1) {// interval
							sb.append("<block_list>");
							sb.append("<interval>");
							sb.append("<low>" + escapeXml(limits[0]) + "</low>");
							sb.append("<high>" + escapeXml(limits[1])
									+ "</high>");
							sb.append("</interval>");
							sb.append("</block_list>");
						} else if (limits.length == 1) {
							sb.append("<block>" + escapeXml(limits[0])
									+ "</block>");
						}
					}
				}
				if (!isEmpty(sectionSubdivision)) {
					sb.append("<sectionSubdivision>"
							+ escapeXml(sectionSubdivision)
							+ "</sectionSubdivision>");
				}
				if (!isEmpty(sectionTownship)) {
					sb.append("<section>" + escapeXml(sectionTownship)
							+ "</section>");
				}
				if (!isEmpty(unit)) {
					sb.append("<unit>" + escapeXml(unit) + "</unit>");
				}
				if (!isEmpty(subdivision))
					sb.append("<subdivision>" + escapeXml(subdivision)
							+ "</subdivision>");
				if (!isEmpty(book) && !isEmpty(page)) {
					sb.append("<plat>");
					if (book != null && !book.equals(""))
						sb.append("<book>" + escapeXml(book) + "</book>");
					if (page != null && !page.equals(""))
						sb.append("<page>" + escapeXml(page) + "</page>");
					sb.append("</plat>");
				}
				sb.append("</subdivided>");
			}
			if (isSectional) {
				sb.append("<sectional>");
				sb.append("<section>" + escapeXml(sectionTownship)
						+ "</section>");
				sb.append("<township>" + escapeXml(township) + "</township>");
				sb.append("<range>" + escapeXml(range) + "</range>");
				sb.append("</sectional>");
			}
			if (isFreeform) {
				// TODO: to be implemented when freeform data is available
			}
			sb.append("</legal>");
		}

		return sb.toString();
	}

	/**
	 * The fields that will be included in the ASK order
	 * 
	 * @return
	 */
	private String getXmlOwners() {
		StringBuffer sb = new StringBuffer();
		
		PartyI owners = getOwners();
		if(owners.size() > 0) {
			sb.append("<owners>");
			for (NameI name : owners.getNames()) {
				if(name.isCompany()) {
					sb.append("<company>");
					sb.append("<name>" + escapeXml(name.getLastName()) + "</name>");
					sb.append("</company>");
				} else {
					sb.append("<person>");
					sb.append("<last>" + escapeXml(name.getLastName()) + "</last>");
					sb.append("<first>" + escapeXml(name.getFirstName()) + "</first>");
					sb.append("<middle>" + escapeXml(name.getMiddleName()) + "</middle>");
					sb.append("</person>");
				}
			}
			sb.append("</owners>");
		}
		return sb.toString();
	}

	private String getXmlBuyers() {
		StringBuffer sb = new StringBuffer();
		
		PartyI owners = getBuyers();
		if(owners.size() > 0) {
			sb.append("<buyers>");
			for (NameI name : owners.getNames()) {
				if(name.isCompany()) {
					sb.append("<company>");
					sb.append("<name>" + escapeXml(name.getLastName()) + "</name>");
					sb.append("</company>");
				} else {
					sb.append("<person>");
					sb.append("<last>" + escapeXml(name.getLastName()) + "</last>");
					sb.append("<first>" + escapeXml(name.getFirstName()) + "</first>");
					sb.append("<middle>" + escapeXml(name.getMiddleName()) + "</middle>");
					sb.append("</person>");
				}
			}
			sb.append("</buyers>");
		}
		return sb.toString();
	}

	private String getXmlSaleValue() {
		String saleValue = getAtribute(BM2_LOADACCOUNTNO);
		if (!isEmpty(saleValue)) {
			return "<sale_value>" + escapeXml(saleValue) + "</sale_value>";
		} else {
			return "";
		}
	}

	/**
	 * test if the IgnoreMiddleName checkbox is checked
	 * 
	 * @return
	 */
	public boolean isIgnoreOwnerMiddleName() {
		boolean result = false;
		try {
			result = Boolean.parseBoolean(getAtribute(IGNORE_MNAME));
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
		}
		return result;
	}

	/**
	 * test if the IgnoreMiddleName for buyer checkbox is checked
	 * 
	 * @return
	 */
	public boolean isIgnoreBuyerMiddleName() {
		boolean result = false;
		try {
			result = Boolean.parseBoolean(getAtribute(IGNORE_MNAME_BUYER));
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
		}
		return result;
	}

	public Collection<String> getPins(int limit) {
		Set<String> list = new LinkedHashSet<String>();
		String pin = getAtribute(LD_PARCELNO);
		pin = pin.replaceAll("\\s+", "").trim();
		String[] pins = pin.split(",");
		
		for (String string : pins) {
			if(org.apache.commons.lang.StringUtils.isNotBlank(string)) {
				if(list.size() == limit) {
					//already hit my limit
					break;
				}
				//don't care about duplicates
				list.add(string);
			}
		}
		return list;
	}

	private boolean shouldBeDeleted(String name) {
		String crit = name.replaceAll("[\\s.]+", "").toUpperCase();
		// add the values in uppercase, no white spaces
		String[] v = new String[] { "SEARCHONLY", "TBD", "TOBEDETERMINED",
				"OWNEROFRECORD", "BUYERTOFOLLOW", "PROPOSEDBUYER",
				"BUYEROFRECORD", "OWNEROFRECORD", "UNKNOWN", "N/A",
				"RECORDOWNER", "BUYERTOFOLLOW", "BUYERTFOLLOW" };
		for (int i = 0; i < v.length; i++) {
			if (v[i].equals(crit)) {
				return true;
			}
		}
		return false;
	}

	private List<String> cleanUpParty(PartyI party) {
		List<String> warnings = new LinkedList<String>();
		List<NameI> forDelete = new ArrayList<NameI>(2);
		for (NameI p : party.getNames()) {
			String name = p.getFirstName() + p.getMiddleName()
					+ p.getLastName();
			if (shouldBeDeleted(name)) {
				forDelete.add(p);
				warnings.add("Name ignored: " + name);
			}
		}

		for (NameI n : forDelete) {
			party.remove(n);
		}
		return warnings;
	}

	public void restoreSearchCriteria() {
		setObjectAtribute(INSTR_LIST, new ArrayList());
		setObjectAtribute(RO_CROSS_REF_INSTR_LIST, new ArrayList<Instrument>());
		setObjectAtribute(OWNERS_LIST, getSearchPageManualOwners());
		
		for( String attribute : new String[] {
				SearchAttributes.P_STREETNAME, 
				SearchAttributes.P_STREETNO, 
				SearchAttributes.P_STREETDIRECTION,
				SearchAttributes.P_STREETSUFIX,
				SearchAttributes.P_STREET_POST_DIRECTION,
				SearchAttributes.P_IDENTIFIER_TYPE,
				SearchAttributes.P_STREETUNIT,
				SearchAttributes.P_CITY,
				SearchAttributes.P_ZIP,
				SearchAttributes.LD_SUBDIV_NAME,
				SearchAttributes.LD_SUBDIV_BLOCK,
				SearchAttributes.LD_LOTNO, 
				SearchAttributes.LD_SUBLOT,
				SearchAttributes.LD_SUBDIV_PHASE,
				SearchAttributes.LD_SECTION,
				SearchAttributes.LD_SUBDIV_UNIT,
				SearchAttributes.LD_BOOKNO,
				SearchAttributes.LD_PAGENO,
				SearchAttributes.LD_PARCELNO,
				SearchAttributes.LD_SUBDIV_SEC,
				SearchAttributes.LD_SUBDIV_TWN,
				SearchAttributes.LD_SUBDIV_RNG,
				SearchAttributes.QUARTER_ORDER,
				SearchAttributes.QUARTER_VALUE,
				SearchAttributes.ARB,
				SearchAttributes.LD_INSTRNO,
				SearchAttributes.LD_BOOKPAGE
				} )	{
			if (!getSearchPageManualFields().containsKey(attribute)){
				getSearchPageManualFields().put(attribute, getAtribute(attribute));
			}
	  }
		
	}
	
	/**
	 * Cleanup names
	 * 
	 * @return list of warnings
	 */
	public Collection<String> cleanupNames() {
		List<String> warnings = new LinkedList<String>();
		
		warnings.addAll(cleanUpParty(getOwners()));
		warnings.addAll(cleanUpParty(getBuyers()));

		return warnings;
	}

	/**
	 * Cleanup legal description
	 * 
	 * @return list of warnings
	 */
	public Collection<String> cleanupLegal() {
		List<String> warnings = new LinkedList<String>();
		String subdivision = getAtribute(SearchAttributes.LD_SUBDIV_NAME);
		String crit = subdivision.toUpperCase().replaceAll("[^A-Z]+", "");
		if ("METESBOUND".equals(crit) || "METESANDBOUND".equals(crit)
				|| "METESANDBOUNDS".equals(crit)
				|| "MEETSANDBOUNDS".equals(crit) || "TBD".equals(crit)
				|| "TOBEDETERMINED".equals(crit)) {
			setAtribute(SearchAttributes.LD_SUBDIV_NAME, "");
			warnings.add("Subdivision name ignored: " + subdivision);
		}
		return warnings;
	}

	/**
	 * Cleanup address
	 * 
	 * @return
	 */
	public Collection<String> detectTimeShare() {
		List<String> warnings = new LinkedList<String>();

		String additionInfo = getAtribute(ADDITIONAL_INFORMATION);
		// if(additionInfo.toLowerCase().replaceAll("[ \t\n\r]",
		// "").contains("timeshare")){
		String unit = getAtribute(P_STREETUNIT);
		String lot = getAtribute(LD_LOTNO);
		String week = getAtribute(WEEK);
		String block = getAtribute(LD_SUBDIV_BLOCK);
		String building = "";
		if (block == null) {
			block = "";
		}

		if (((unit != null && unit.matches("[U][0-9a-zA-Z]*"))) && lot != null
				&& lot.matches("W[0-9a-zA-Z]+")) {

			if (block.matches("[X][a-zA-Z0-9]+")) {
				building = block;
				block = "";
			}

			unit = unit.substring(1);
			week = lot;
			lot = "";

			setAtribute(LD_LOTNO, lot);
			setAtribute(P_STREETUNIT, unit);
			setAtribute(WEEK, week);
			setAtribute(BUILDING, building);
			setAtribute(LD_SUBDIV_BLOCK, block);

			warnings.add("Time share detected Week: " + week + "unit: U" + unit);
		}
		// }

		return warnings;
	}

	/**
	 * Cleanup address
	 * 
	 * @return
	 */
	public Collection<String> cleanupAddress() {

		List<String> warnings = new LinkedList<String>();

		String nr = getAtribute(P_STREETNO).replaceAll("\\s{2,}", " ").trim();
		String dir = getAtribute(P_STREETDIRECTION).replaceAll("\\s{2,}", " ")
				.trim();
		String name = getAtribute(P_STREETNAME).replaceAll("\\s{2,}", " ")
				.trim();
		String suffix = getAtribute(P_STREETSUFIX).replaceAll("\\s{2,}", " ")
				.trim();
		String unit = getAtribute(P_STREETUNIT).replaceAll("\\s{2,}", " ")
				.trim();

		// check for unit number in street name
		if (name.matches(".*\\s#\\d+") && StringUtils.isEmpty(unit)) {
			unit = StringUtils.extractParameter(name, "#(\\d+)$");
			name = name.replaceFirst("#\\d+$", "");
			warnings.add("Considered '#" + unit
					+ "' from street name as unit number");
		}

		// remove dot at the end of street name
		name = name.replaceFirst("\\.$", "");

		// remove dot at the end of suffix
		suffix = suffix.replaceFirst("\\.$", "");

		// check for suffix in the street name
		String[] tokens = name.split(" ");
		if (tokens.length > 1 && StringUtils.isEmpty(suffix)) {
			String last = tokens[tokens.length - 1];
			String first = tokens[0];
			if (Normalize.isSuffix(last)
					&& (tokens.length >= 3 || tokens[0].length() >= 4
							&& suffix.length() < 4)) {
				suffix = last;
				String newName = "";
				for (int i = 0; i < tokens.length - 1; i++) {
					newName = newName + tokens[i] + " ";
				}
				name = newName.trim();
				warnings.add("Considered '" + last
						+ "' from street name as suffix");
			}
		}

		setAtribute(P_STREETNO, nr);
		setAtribute(P_STREETDIRECTION, dir);
		setAtribute(P_STREETNAME, name);
		setAtribute(P_STREETSUFIX, suffix);
		setAtribute(P_STREETUNIT, unit);

		return warnings;
	}

	public PartyI getBuyers() {
		return (Party) getObjectAtribute(BUYERS_LIST);
	}

	public PartyI getOwners() {
		return (Party) getObjectAtribute(OWNERS_LIST);
	}

	public PartyI getSearchPageManualOwners() {
		return (Party) getObjectAtribute(SEARCH_PAGE_MANUAL_OWNERS_LIST);
	}

	public PartyI getSearchPageManualBuyers() {
		return (Party) getObjectAtribute(SEARCH_PAGE_MANUAL_BUYERS_LIST);
	}

	public Map<String, String> getSearchPageManualFields() {
		return (Map<String, String>) getObjectAtribute(SEARCH_PAGE_MANUAL_FIELDS);
	}

	public boolean isFVSUpdate() {
		return getAtribute(SearchAttributes.FVS_UPDATE).trim().toLowerCase().equals("true");
	}
	
	public boolean isLastScheduledFVSUpdate() {
		return getAtribute(SearchAttributes.LAST_SCHEDULED_FVS_UPDATE).trim().toLowerCase().equals("true");
	}
	
	public boolean isFVSAutoLaunched(){
		return getAtribute(FVS_UPDATE_AUTO_LAUNCHED).trim().toLowerCase().equals("true");
	}
	
	public String getFromDateString(String format) {
		String fromDateStr = getAtribute(SearchAttributes.FROMDATE);
		Date fromDate = Util.dateParser3(fromDateStr);

		SimpleDateFormat sdf = new SimpleDateFormat(format);
		fromDateStr = sdf.format(fromDate);
		return fromDateStr;
	}

	public boolean hasOwner() {
		return getOwners().size() > 0;
	}

	public boolean hasBuyer() {
		return getBuyers().size() > 0;
	}

	public void setReopenSearch(boolean isReopen) {
		this.reopenSearch = isReopen;
	}

	public boolean isReopenSearch() {
		return this.reopenSearch;
	}

	@Override
	public VestingInfoI getVestingInfoGrantee() {
		if (vestingInfoGrantee == null) { // deserialization
			vestingInfoGrantee = new VestingInfo();
		}
		return vestingInfoGrantee;
	}

	@Override
	public void setSearchVestingInfoGrantee(VestingInfoI vestingInfograntee) {
		this.vestingInfoGrantee = vestingInfograntee;
	}

	@Override
	public LegalDescriptionI getLegalDescription() {
		if (legalDescription == null) { // deserialization
			legalDescription = new LegalDescription();
		}
		return legalDescription;
	}

	@Override
	public void setLegalDescription(LegalDescriptionI legalI) {
		this.legalDescription = legalI;
	}

	@Override
	public CertificationDate getCertificationDate() {
		if (certification == null) {
			certification = new CertificationDate();
		}
		return certification;
	}
	
	@Override
	public LinkedHashMap<String, ReviewChecker> getReviewCheckList(){
		return getReviewCheckList(null);
	}
	
	public LinkedHashMap<String, ReviewChecker> getReviewCheckList(SearchAttributes parentSearchSA){
		LinkedHashMap<String, ReviewChecker> reviewCheckList = (LinkedHashMap<String, ReviewChecker>) getObjectAtribute(REVIEW_CHECKER_LIST);
		
		if (getObjectAtribute(REVIEW_CHECKER_LIST) == null) {
			reviewCheckList =  new LinkedHashMap<String, ReviewChecker>();
		}
		LinkedHashMap<String, ReviewChecker> rcListFromOriginalSearch = null;
		if (isFVSUpdate()){
			if (parentSearchSA != null){
				rcListFromOriginalSearch = (LinkedHashMap<String, ReviewChecker>) parentSearchSA.getObjectAtribute(REVIEW_CHECKER_LIST);
				for (ReviewFlag flagName : ReviewChecker.ReviewFlag.values()) {
					ReviewChecker rc = rcListFromOriginalSearch.get(flagName.toString());
					if (rc == null){
						rc = new ReviewChecker(flagName.toString(), false);
						rc.setReviewFlagTooltip(ReviewChecker.ReviewTooltip.valueOf(flagName.toString()).toString());
					}
					reviewCheckList.put(flagName.toString(), rc);
				}
			}
		}
		
		if (reviewCheckList.size() == 0){
			for (ReviewFlag flagName : ReviewChecker.ReviewFlag.values()) {
				ReviewChecker rc = new ReviewChecker(flagName.toString(), false);
				rc.setReviewFlagTooltip(ReviewChecker.ReviewTooltip.valueOf(flagName.toString()).toString());
				reviewCheckList.put(flagName.toString(), rc);
			}
		}
		
		return reviewCheckList;
	}

	@Override
	public void setFlagFromReviewCheckList(ReviewChecker reviewChecker, Boolean value, Boolean whenChecking) {

		reviewChecker.setReviewFlagValue(value);		
		if (whenChecking){
			Date now = Calendar.getInstance().getTime();
			reviewChecker.setReviewFlagChangedDate(now);
			reviewChecker.setReviewFlagChangedDateAsString(FormatDate.getDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY_SPACE_HH_COLON_mm_COLON_ss).format(now));
		}
		getReviewCheckList().put(reviewChecker.getReviewFlagName(), reviewChecker);
		if (whenChecking){
			String checked = "unchecked";
			
			if (reviewChecker.isReviewFlagValue()){
				checked = "checked";
			}
			
			SearchLogger.info("</div><div>The review item <b>" + reviewChecker.getReviewFlagTooltip() + "</b> was manually <b>" + checked + "</b> "
								+ SearchLogger.getTimeStamp(getSearchId()) + ".<BR><div>", getSearchId());
		}
	}
	
	public ReviewChecker setAutomaticallyFlagFromReviewCheckList(OcredEditableElementI ocredEditableElement) {
		ReviewChecker reviewChecker = null;
		
		String editableElement = "";
		if (ocredEditableElement instanceof VestingInfo){
			reviewChecker = getReviewCheckList().get(ReviewChecker.ReviewFlag.VESTING_INFO.toString());
			if (!reviewChecker.isReviewFlagValue()) {// 9652 - if it was already checked, don't check it and don't give the message again in SLog
				reviewChecker.setReviewFlagValue(true);
				editableElement = "Vesting";
			}
			
		} else if (ocredEditableElement instanceof LegalDescription){
			reviewChecker = getReviewCheckList().get(ReviewChecker.ReviewFlag.LD.toString());
			if (!reviewChecker.isReviewFlagValue()) {// 9652
				reviewChecker.setReviewFlagValue(true);
				editableElement = "Legal Description";
			}
		}
		
		if (reviewChecker != null && org.apache.commons.lang.StringUtils.isNotEmpty(editableElement)){
			Date now = Calendar.getInstance().getTime();
			reviewChecker.setReviewFlagChangedDate(now);
			reviewChecker.setReviewFlagChangedDateAsString(FormatDate.getDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY_SPACE_HH_COLON_mm_COLON_ss).format(now));
			
			getReviewCheckList().put(reviewChecker.getReviewFlagName(), reviewChecker);
			SearchLogger.info("</div><div>The review item <b>" + reviewChecker.getReviewFlagTooltip() + "</b> was automatically <b>checked</b> because "
					+ "<b>" + editableElement + "</b> was viewed and/or edited "
					+ SearchLogger.getTimeStamp(getSearchId()) + ".<BR><div>", getSearchId());
			
		}
		return reviewChecker;
	}
	
	public void addStatementForSSF(String key, String value) {
		HashSet<String> keyValue = statementsForSSF.get(key);
		if (keyValue != null) {
			keyValue.add(value);
		} else {
			LinkedHashSet<String> hashSet = new LinkedHashSet<String>();
			hashSet.add(value);
			statementsForSSF.put(key, hashSet);
		}
	}

	public Map<String, LinkedHashSet<String>> getStatementsFromSSF() {
		return statementsForSSF;
	}

	@Override
	public void setCertificationDate(CertificationDate certificationDate) {
		this.certification = certificationDate;
		if (certificationDate != null && certificationDate.getDate() != null) {
			SearchLogger.info(
					"</div><div><b>Effective End Date</b> was edited in TSRIndex and set to <b>"
							+ new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY)
									.format(certificationDate.getDate())
							+ "</b> "
							+ SearchLogger.getTimeStamp(getSearchId())
							+ ".<BR><div>", getSearchId());
		}
	}

	@Override
	public void clearOwners() {
		try {
			getOwners().clear();
			getBuyers().clear();
		} catch (Exception ignored) {
		}
	}

	public boolean isAddressBootstrapped() {
		for (String attribute : new String[] { SearchAttributes.P_STREETNAME,
				SearchAttributes.P_STREETNO,
				SearchAttributes.P_STREETDIRECTION,
				SearchAttributes.P_STREETSUFIX, SearchAttributes.P_STREETUNIT,
				SearchAttributes.P_CITY, SearchAttributes.P_ZIP }) {
			if (!StringUtils.isEmpty(getAtribute(attribute))
					&& !getSearchPageManualFields().containsKey(attribute)) {
				return true;
			}
		}
		return false;
	}

	public static void checkChangedAddress(SearchAttributes sa) {
		for (String attribute : new String[] { SearchAttributes.P_STREETNAME,
				SearchAttributes.P_STREETNO,
				SearchAttributes.P_STREETDIRECTION,
				SearchAttributes.P_STREETSUFIX,
				SearchAttributes.P_STREET_POST_DIRECTION,
				SearchAttributes.P_STREETUNIT, SearchAttributes.P_CITY }) {
			try {
				if (!sa.getAtribute(attribute)
						.trim()
						.equalsIgnoreCase(
								sa.getSearchPageManualFields().get(attribute))) {
					sa.getSearchPageManualFields().remove(attribute);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * return an int that encodes the parts of the addresses changed by
	 * bootstrapped
	 * 
	 * @return
	 */
	public int addressBootstrappedCode() {
		checkChangedAddress(this);
		long code = 0;
		long i = 2; // we start with bit 1, bit 0 means address bootstrapped for
					// searches done before this fix
		for (String attribute : new String[] { SearchAttributes.P_STREETNAME, // bit
																				// 1
				SearchAttributes.P_STREETNO, // bit 2
				SearchAttributes.P_STREETDIRECTION, // bit 3
				SearchAttributes.P_STREETSUFIX, // bit 4
				SearchAttributes.P_STREETUNIT, // bit 5
				SearchAttributes.P_CITY, // bit 6
				SearchAttributes.P_ZIP }) { // bit7
			if (!StringUtils.isEmpty(getAtribute(attribute))
					&& !getSearchPageManualFields().containsKey(attribute)) {
				code = code | i;
			}
			i <<= 1;
		}
		return (int) code;
	}

	public boolean isUpdate() {
		try {
			String atribute = getAtribute(SearchAttributes.SEARCH_PRODUCT);
			return Products.UPDATE_PRODUCT == Integer.parseInt(atribute.equals("") ? "-1" : atribute);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean isFVSUpdateProduct() {
		try {
			String atribute = getAtribute(SearchAttributes.SEARCH_PRODUCT);
			return Products.FVS_PRODUCT == Integer.parseInt(atribute.equals("") ? "-1" : atribute);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * bug 1506 on tn shelby the pid from ao needs to be converted to tr format
	 * 
	 * @return the converted pattern or an empty string if there is no ao pid
	 * @throws Exception
	 *             if the pid from ao doesn't match any known pattern
	 */
	public String TNShelbyTRAOtoTRPID() throws Exception {
		String trPid = "";
		String aoPid = getAtribute(LD_PARCELNO).replaceAll("\\s|-", "");
		if (StringUtils.isEmpty(aoPid)) {
			return "";
		}
		if (aoPid.length() == 14) {
			return aoPid;
		}

		HashMap<Pattern, String> equivs = new HashMap();
		equivs.put(Pattern.compile("(\\d{6})(\\d{5})"), "$100$20");
		equivs.put(Pattern.compile("(\\d{6})([A-Za-z]\\d{5})"), "$10$20");
		equivs.put(Pattern.compile("([A-Za-z]\\d{4}[A-Za-z])([A-Za-z]\\d{5})"),
				"$10$20");
		equivs.put(Pattern.compile("([A-Za-z]\\d{4})(\\d{5})"), "$1000$20");
		equivs.put(Pattern.compile("([A-Za-z]\\d{4})([A-Za-z]\\d{5})"),
				"$100$20");
		equivs.put(
				Pattern.compile("([A-Za-z]\\d{4})([A-Za-z]\\d{5})([A-Za-z])"),
				"$100$2$3");
		equivs.put(Pattern.compile("(\\d{6})(\\d{5}[A-Za-z])"), "$100$2");
		equivs.put(Pattern.compile("([A-Za-z]\\d{4})(\\d{5}[A-Za-z])"),
				"$1000$2");
		Iterator<Pattern> i = equivs.keySet().iterator();
		while (i.hasNext()) {
			Pattern p = i.next();
			Matcher m = p.matcher(aoPid);
			if (m.matches()) {
				return m.replaceAll(equivs.get(p));
			}
		}

		throw new Exception(
				"AO PID doesn't match the known conversion patterns.");
	}

	public AtomicInteger getFileUploadInProgressCount() {
		if (fileUploadInProgressCount == null) {
			fileUploadInProgressCount = new AtomicInteger(0);
		}
		return fileUploadInProgressCount;
	}

	public void setFileUploadInProgressCount(
			AtomicInteger fileUploadInProgressCount) {
		this.fileUploadInProgressCount = fileUploadInProgressCount;
	}

	/**
	 * @return the invalidatedInstruments
	 */
	protected HashSet<InstrumentI> getInvalidatedInstruments() {
		if (invalidatedInstruments == null) {
			invalidatedInstruments = new LinkedHashSet<InstrumentI>();
		}
		return invalidatedInstruments;
	}

	/**
	 * @param invalidatedInstruments
	 *            the invalidatedInstruments to set
	 */
	protected void setInvalidatedInstruments(
			HashSet<InstrumentI> invalidatedInstruments) {
		this.invalidatedInstruments = invalidatedInstruments;
	}

	public void clearInvalidatedInstruments() {
		getInvalidatedInstruments().clear();
	}

	public boolean isInvalidatedInstrument(InstrumentI instrument) {
		return getInvalidatedInstruments().contains(instrument);
	}

	public boolean isInvalidatedInstrument(String instrumentNo) {
		if (StringUtils.isEmpty(instrumentNo)) {
			return false;
		}
		for (InstrumentI instrument : getInvalidatedInstruments()) {
			if (instrumentNo.equalsIgnoreCase(instrument.getInstno())
					|| instrumentNo.equalsIgnoreCase(instrument.getDocno())) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isInvalidatedInstrument(String book, String page) {
		if (StringUtils.isEmpty(book) || StringUtils.isEmpty(page)) {
			return false;
		}
		for (InstrumentI instrument : getInvalidatedInstruments()) {
			if (book.equalsIgnoreCase(instrument.getBook())
					&& page.equalsIgnoreCase(instrument.getPage())) {
				return true;
			}
		}
		return false;
	}

	public boolean addInvalidatedInstrument(InstrumentI instrument) {
		return getInvalidatedInstruments().add(instrument);
	}

	public int getCommId() {
		//need a valid searchId, other wise we will set a wrong commId
		if ( ( commId == HashCountyToIndex.ANY_COMMUNITY || commId == 0) && searchId > 0) {
			commId = InstanceManager.getManager().getCommunityId(searchId);
		}
		return commId;
	}

	public void setCommId(int commId) {
		this.commId = commId;
	}

	public boolean isStarterStewartOrders() {
		return getAtribute(SearchAttributes.SEARCH_STEWART_TYPE)
				.equalsIgnoreCase("STARTER")
				&& getAtribute(SearchAttributes.SEARCH_ORIGIN).toUpperCase()
						.startsWith("STEWARTORDERS");
	}

	@Override
	public String getSearchIdSKLD() {
		return searchIdSKLD;
	}

	@Override
	public void setSearchIdSKLD(String skldStartedId) {
		this.searchIdSKLD = skldStartedId;

	}

	public boolean isDateDown() {
		try {
			return DATE_DOWN
					.equalsIgnoreCase(getAtribute(SearchAttributes.ADDITIONAL_SEARCH_TYPE));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean isDataSource() {
		try {
			return DATA_SOURCE
					.equalsIgnoreCase(getAtribute(SearchAttributes.ADDITIONAL_SEARCH_TYPE));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean isCondo() {

		return ("true".equalsIgnoreCase(getAtribute(SearchAttributes.IS_CONDO)));
	}

	public Date getStartDate() {
		SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
		Date date = null;
		try {
			date = sdf.parse(getAtribute(SearchAttributes.FROMDATE));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return date;
	}

	public Date getEndDate() {
		SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
		Date date = null;
		try {
			date = sdf.parse(getAtribute(SearchAttributes.TODATE));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return date;
	}

	public AddressI getAddress() {
		String streetNo = getAtribute(SearchAttributes.P_STREETNO);
		String streetDir = getAtribute(SearchAttributes.P_STREETDIRECTION);
		String streetName = getAtribute(SearchAttributes.P_STREETNAME);
		String streetSufix = getAtribute(SearchAttributes.P_STREETSUFIX);
		String streetPostDirection = getAtribute(SearchAttributes.P_STREET_POST_DIRECTION);
		String streetUnit = getAtribute(SearchAttributes.P_STREETUNIT);

		AddressI a = new com.stewart.ats.base.address.Address();
		a.setStreetName(streetName);
		a.setNumber(streetNo);
		a.setPreDiretion(streetDir);
		a.setSuffix(streetSufix);
		a.setPostDirection(streetPostDirection);
		a.setIdentifierNumber(streetUnit);

		return a;
	}

	public boolean addForUpdateSearchGrantorName(NameI name, long serverId) {
		if (name == null) {
			return false;
		}
		if (forUpdateSearchGrantorNames == null) {
			forUpdateSearchGrantorNames = new HashMap<Long, List<NameI>>();
		}
		List<NameI> list = forUpdateSearchGrantorNames.get(serverId);

		if (list == null) {
			list = new ArrayList<NameI>();
			forUpdateSearchGrantorNames.put(serverId, list);
		}
		if (!list.contains(name)) {
			return list.add(name);
		}
		return false;
	}

	public boolean addForUpdateSearchGrantorNames(List<NameI> names,
			long serverId) {
		if (names == null) {
			return false;
		}

		if (forUpdateSearchGrantorNames == null) {
			forUpdateSearchGrantorNames = new HashMap<Long, List<NameI>>();
		}
		List<NameI> list = forUpdateSearchGrantorNames.get(serverId);

		if (list == null) {
			list = new ArrayList<NameI>();
			forUpdateSearchGrantorNames.put(serverId, list);
		}
		int originalSize = list.size();
		for (NameI nameI : names) {
			if (!list.contains(nameI)) {
				list.add(nameI);
			}
		}
		return list.size() > originalSize;
	}

	/**
	 * Returns the list of Grantor names to be search on Update Product<br>
	 * Can return null if the list was not initialized
	 * 
	 * @return a list of names or null
	 */
	public Map<Long, List<NameI>> getForUpdateSearchGrantorNames() {
		return forUpdateSearchGrantorNames;
	}

	/**
	 * Returns the list of Grantor names to be search on Update Product<br>
	 * This will never return null. As a side effect this can initialize the
	 * list if it this was undefined until now.
	 * 
	 * @return a list of names
	 */
	public List<NameI> getForUpdateSearchGrantorNamesNotNull(long serverId) {
		if (forUpdateSearchGrantorNames == null) {
			forUpdateSearchGrantorNames = new HashMap<Long, List<NameI>>();
		}
		List<NameI> list = forUpdateSearchGrantorNames.get(serverId);

		if (list == null) {
			list = new ArrayList<NameI>();
			forUpdateSearchGrantorNames.put(serverId, list);
		}
		return list;
	}

	public boolean addForUpdateSearchGranteeName(NameI name, long serverId) {
		if (name == null) {
			return false;
		}
		if (forUpdateSearchGranteeNames == null) {
			forUpdateSearchGranteeNames = new HashMap<Long, List<NameI>>();
		}
		List<NameI> list = forUpdateSearchGranteeNames.get(serverId);

		if (list == null) {
			list = new ArrayList<NameI>();
			forUpdateSearchGranteeNames.put(serverId, list);
		}
		if (!list.contains(name)) {
			return list.add(name);
		}
		return false;
	}

	public boolean addForUpdateSearchGranteeNames(List<NameI> names,
			long serverId) {
		if (names == null) {
			return false;
		}

		if (forUpdateSearchGranteeNames == null) {
			forUpdateSearchGranteeNames = new HashMap<Long, List<NameI>>();
		}
		List<NameI> list = forUpdateSearchGranteeNames.get(serverId);

		if (list == null) {
			list = new ArrayList<NameI>();
			forUpdateSearchGranteeNames.put(serverId, list);
		}
		int originalSize = list.size();
		for (NameI nameI : names) {
			if (!list.contains(nameI)) {
				list.add(nameI);
			}
		}
		return list.size() > originalSize;
	}

	/**
	 * Returns the list of Grantee names to be search on Update Product<br>
	 * Can return null if the list was not initialized
	 * 
	 * @return a list of names or null
	 */
	public Map<Long, List<NameI>> getForUpdateSearchGranteeNames() {
		return forUpdateSearchGranteeNames;
	}

	/**
	 * Returns the list of Grantee names to be search on Update Product<br>
	 * This will never return null. As a side effect this can initialize the
	 * list if it this was undefined until now.
	 * 
	 * @return a list of names
	 */
	public List<NameI> getForUpdateSearchGranteeNamesNotNull(long serverId) {
		if (forUpdateSearchGranteeNames == null) {
			forUpdateSearchGranteeNames = new HashMap<Long, List<NameI>>();
		}
		List<NameI> list = forUpdateSearchGranteeNames.get(serverId);

		if (list == null) {
			list = new ArrayList<NameI>();
			forUpdateSearchGranteeNames.put(serverId, list);
		}
		return list;
	}

	public boolean addForUpdateSearchAddress(AddressI address, long serverId) {
		if (address == null) {
			return false;
		}

		if (forUpdateSearchAddresses == null) {
			forUpdateSearchAddresses = new HashMap<Long, List<AddressI>>();
		}
		List<AddressI> list = forUpdateSearchAddresses.get(serverId);
		if (list == null) {
			list = new ArrayList<AddressI>();
			forUpdateSearchAddresses.put(serverId, list);
		}
		if (!list.contains(address)) {
			return list.add(address);
		}
		return false;
	}

	public boolean addForUpdateSearchAddresses(List<AddressI> addresses,
			long serverId) {
		if (addresses == null) {
			return false;
		}
		if (forUpdateSearchAddresses == null) {
			forUpdateSearchAddresses = new HashMap<Long, List<AddressI>>();
		}
		List<AddressI> list = forUpdateSearchAddresses.get(serverId);
		if (list == null) {
			list = new ArrayList<AddressI>();
			forUpdateSearchAddresses.put(serverId, list);
		}
		int originalSize = list.size();
		for (AddressI addressI : addresses) {
			if (!list.contains(addressI)) {
				list.add(addressI);
			}
		}
		return list.size() > originalSize;
	}

	/**
	 * Returns the list of addresses to be search on Update Product<br>
	 * Can return null if the list was not initialized
	 * 
	 * @return a list of addresses or null
	 */
	public Map<Long, List<AddressI>> getForUpdateSearchAddresses() {
		return forUpdateSearchAddresses;
	}

	/**
	 * Returns the list of addresses to be search on Update Product<br>
	 * This will never return null. As a side effect this can initialize the
	 * list if it this was undefined until now.
	 * 
	 * @return a list of addresses
	 */
	public List<AddressI> getForUpdateSearchAddressesNotNull(long serverId) {
		if (forUpdateSearchAddresses == null) {
			forUpdateSearchAddresses = new HashMap<Long, List<AddressI>>();
		}
		List<AddressI> list = forUpdateSearchAddresses.get(serverId);
		if (list == null) {
			list = new ArrayList<AddressI>();
			forUpdateSearchAddresses.put(serverId, list);
		}
		return list;
	}

	public boolean addForUpdateSearchLegal(LegalI legal, long serverId) {
		if (legal == null) {
			return false;
		}
		if (forUpdateSearchLegals == null) {
			forUpdateSearchLegals = new HashMap<Long, List<LegalI>>();
		}
		List<LegalI> list = forUpdateSearchLegals.get(serverId);
		if (list == null) {
			list = new ArrayList<LegalI>();
			forUpdateSearchLegals.put(serverId, list);
		}
		if (!list.contains(legal)) {
			return list.add(legal);
		}
		return false;
	}

	public boolean addForUpdateSearchLegals(List<LegalI> legals, long serverId) {
		if (legals == null) {
			return false;
		}
		if (forUpdateSearchLegals == null) {
			forUpdateSearchLegals = new HashMap<Long, List<LegalI>>();
		}
		List<LegalI> list = forUpdateSearchLegals.get(serverId);
		if (list == null) {
			list = new ArrayList<LegalI>();
			forUpdateSearchLegals.put(serverId, list);
		}
		int originalSize = list.size();
		for (LegalI legalI : legals) {
			if (!list.contains(legalI)) {
				list.add(legalI);
			}
		}
		return list.size() > originalSize;
	}

	/**
	 * Returns the list of legals to be search on Update Product<br>
	 * Can return null if the list was not initialized
	 * 
	 * @return a list of legals or null
	 */
	public Map<Long, List<LegalI>> getForUpdateSearchLegals() {
		return forUpdateSearchLegals;
	}

	/**
	 * Returns the list of legals to be search on Update Product<br>
	 * This will never return null. As a side effect this can initialize the
	 * list if it this was undefined until now.
	 * 
	 * @return a list of legals
	 */
	public List<LegalI> getForUpdateSearchLegalsNotNull(long serverId) {
		if (forUpdateSearchLegals == null) {
			forUpdateSearchLegals = new HashMap<Long, List<LegalI>>();
		}
		List<LegalI> list = forUpdateSearchLegals.get(serverId);
		if (list == null) {
			list = new ArrayList<LegalI>();
			forUpdateSearchLegals.put(serverId, list);
		}
		return list;
	}

	public Date getEffectiveStartDate() {
		if (effectiveStartDate != null && effectiveStartDate.getDate() != null) {
			return effectiveStartDate.getDate();
		}
		try {
			List<DataSite> allSites = HashCountyToIndex.getAllSites(
					getCommId(), Integer.parseInt(getCountyId()));
			Date tempDate = null;
			
			for (DataSite dataSite : allSites) {
				if (dataSite.isRoLikeSite() && dataSite.isEnableSite(getCommId())) {
					if (Products.isOneOfUpdateProductType(getProductId())){
						if (getStartDate() != null) {
							tempDate = getStartDate();
						}
					}else {
						if (tempDate == null) {
							tempDate = dataSite.getEffectiveStartDate();
						} else {
							if (dataSite.getEffectiveStartDate() != null) {
								if (tempDate
										.after(dataSite.getEffectiveStartDate())) {
									tempDate = dataSite.getEffectiveStartDate();
								}
							}
						}
					}
				}
			}
			return tempDate;
		} catch (Exception e) {
			logger.error("Problem reading effectiveStartDate for searchId "
					+ searchId + " and commId " + getCommId() + " and county "
					+ getCountyId(), e);
		}
		return null;
	}

	public String getEffectiveStartDateAsString() {
		Date tempDate = getEffectiveStartDate();
		if (tempDate != null) {
			try {
				return new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY)
						.format(tempDate);
			} catch (Exception e) {
				logger.error("Problem reading effectiveStartDate for searchId "
						+ searchId + " and commId " + getCommId()
						+ " and county " + getCountyId(), e);
			}
		}
		return null;
	}

	public void setEffectiveStartDate(Date date) {
		if (date != null) {
			effectiveStartDate = new CertificationDate();
			effectiveStartDate.setDate(date);
			effectiveStartDate.setEdited(true);

			SearchLogger.info(
					"</div><div><b>Effective Start Date</b> was edited in TSRIndex and set to <b>"
							+ new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY)
									.format(date) + "</b> "
							+ SearchLogger.getTimeStamp(getSearchId())
							+ ".<BR><div>", getSearchId());

		}
	}
	
	/**
	 * 
	 * Should only be used when the user does something in Search Page that validated the data<br>
	 * The purpose it to have a clear view of the last search page seen and validated by user
	 * @return the updated property
	 */
	public PropertyI updateValidatedProperty() {
		if(validatedProperty == null) {
			validatedProperty = new Property();
		}
		
		PinI pin = new Pin(); 
		pin.addPin(PinI.PinType.PID, getAtribute(LD_PARCELNO));
		validatedProperty.setPin(pin);
		
		validatedProperty.setAddress(getAddress().clone());
		LegalI legal = validatedProperty.getLegal();
		if(legal == null) {
			legal = new Legal();
			validatedProperty.setLegal(legal);
		}
		SubdivisionI subdivision = legal.getSubdivision();
		if(subdivision == null) {
			subdivision = new Subdivision();
			legal.setSubdivision(subdivision);
		}
		subdivision.setName(getAtribute(LD_SUBDIV_NAME));
		subdivision.setLot(getAtribute(LD_LOTNO));
		subdivision.setSubLot(getAtribute(LD_SUBLOT));
		subdivision.setBlock(getAtribute(LD_SUBDIV_BLOCK));
		subdivision.setPhase(getAtribute(LD_SUBDIV_PHASE));
		subdivision.setSection(getAtribute(LD_SECTION));
		subdivision.setUnit(getAtribute(LD_SUBDIV_UNIT));
		subdivision.setPlatBook(getAtribute(LD_BOOKNO));
		subdivision.setPlatPage(getAtribute(LD_PAGENO));
		
		TownShipI townShip = legal.getTownShip();
		if(townShip == null) {
			townShip = new TownShip();
			legal.setTownShip(townShip);
		}
		townShip.setRange(getAtribute(LD_SUBDIV_RNG));
		townShip.setTownship(getAtribute(LD_SUBDIV_TWN));
		townShip.setSection(getAtribute(LD_SUBDIV_SEC));
		
		validatedProperty.setOwner(getOwners().clone());
		
		return validatedProperty;
	}

	public PropertyI getValidatedProperty() {
		return validatedProperty;
	}
	
	public PropertyI getOrderProperty() {
		if (orderProperty == null) {
			orderProperty = new Property();
		}
		return orderProperty;
	}

	public void setOrderProperty(PropertyI orderProperty) {
		this.orderProperty = orderProperty;
	}

	/**
	 * Returns last validated subdivision name, which means the last subdivision present in search page that was seen by the user.<br>
	 * If the user did not see the search page the subdivision name will be taken from the order, since that is basically the last subdivision validated by user (when sending order). <br>
	 * By default it returns an empty string if no value is found
	 * @return subdivision name
	 */
	public String getValidatedSubdivisionName() {
		String result = null;
		
		if(validatedProperty != null) {
			result = validatedProperty.getLegal().getSubdivision().getName();
		} else if (getOrderProperty().getLegal() != null
				&& getOrderProperty().getLegal().getSubdivision() != null) {
			result = getOrderProperty().getLegal().getSubdivision().getName();
		}
		return org.apache.commons.lang.StringUtils.defaultString(result);
	}

	/**
	 * Returns last validated plat book, which means the last plat book present in search page that was seen by the user.<br>
	 * If the user did not see the search page the plat book will be taken from the order, since that is basically the last plat book validated by user (when sending order). <br>
	 * By default it returns an empty string if no value is found
	 * @return plat book
	 */
	public String getValidatedPlatBook() {
		String result = null;
		if(validatedProperty != null) {
			result = validatedProperty.getLegal().getSubdivision().getPlatBook();
		} else if (getOrderProperty().getLegal() != null
				&& getOrderProperty().getLegal().getSubdivision() != null) {
			result = getOrderProperty().getLegal().getSubdivision()
					.getPlatBook();
		}
		return org.apache.commons.lang.StringUtils.defaultString(result);
	}

	/**
	 * Returns last validated plat page, which means the last plat page present in search page that was seen by the user.<br>
	 * If the user did not see the search page the plat page will be taken from the order, since that is basically the last plat page validated by user (when sending order). <br>
	 * By default it returns an empty string if no value is found
	 * @return plat page
	 */
	public String getValidatedPlatPage() {
		String result = null;
		if(validatedProperty != null) {
			result = validatedProperty.getLegal().getSubdivision().getPlatPage();
		} else if (getOrderProperty().getLegal() != null
				&& getOrderProperty().getLegal().getSubdivision() != null) {
			result = getOrderProperty().getLegal().getSubdivision()
					.getPlatPage();
		}
		return org.apache.commons.lang.StringUtils.defaultString(result);
	}
	
	/**
	 * Returns last validated lot, which means the last lot present in search page that was seen by the user.<br>
	 * If the user did not see the search page the lot will be taken from the order, since that is basically the last lot validated by user (when sending order). <br>
	 * By default it returns an empty string if no value is found
	 * @return lot
	 */
	public String getValidatedLot() {
		String result = null;
		
		if(validatedProperty != null) {
			result = validatedProperty.getLegal().getSubdivision().getLot();
		} else if (getOrderProperty().getLegal() != null
				&& getOrderProperty().getLegal().getSubdivision() != null) {
			result = getOrderProperty().getLegal().getSubdivision().getLot();
		}
		return org.apache.commons.lang.StringUtils.defaultString(result);
	}
	
	/**
	 * Returns last validated block, which means the last block present in search page that was seen by the user.<br>
	 * If the user did not see the search page the block will be taken from the order, since that is basically the last block validated by user (when sending order). <br>
	 * By default it returns an empty string if no value is found
	 * @return block
	 */
	public String getValidatedBlock() {
		String result = null;
		
		if(validatedProperty != null) {
			result = validatedProperty.getLegal().getSubdivision().getBlock();
		} else if (getOrderProperty().getLegal() != null
				&& getOrderProperty().getLegal().getSubdivision() != null) {
			result = getOrderProperty().getLegal().getSubdivision().getBlock();
		}
		return org.apache.commons.lang.StringUtils.defaultString(result);
	}
	
	/**
	 * Returns last validated unit, which means the last unit present in search page that was seen by the user.<br>
	 * If the user did not see the search page the unit will be taken from the order, since that is basically the last unit validated by user (when sending order). <br>
	 * By default it returns an empty string if no value is found
	 * @return unit
	 */
	public String getValidatedAddressUnit() {
		String result = null;
		if(validatedProperty != null) {
			result = validatedProperty.getAddress().getIdentifierNumber();
		} else if (getOrderProperty().getAddress() != null) {
			result = getOrderProperty().getAddress().getIdentifierNumber();
		}
		return org.apache.commons.lang.StringUtils.defaultString(result).trim();
	}

	public HashSet<DataSources> getDataSourcesOnSearch(){
		
		try {
			if (getObjectAtribute(DATA_SOURCES_ON_SEARCH) == null) {
				return new HashSet<DataSources>();
			} else {
				return (HashSet<DataSources>) getObjectAtribute(DATA_SOURCES_ON_SEARCH);
			}
		} catch (Exception e) {
			return new HashSet<DataSources>();
		}
	}
	
	public HashMap<String, Set<Integer>> getOrderCountCached(){
		try {
			Object objectAtribute = getObjectAtribute(ORDER_COUNT_CACHE);
			if (objectAtribute == null) {
				return new HashMap<String, Set<Integer>>();
			} else {
				return (HashMap<String, Set<Integer>>) objectAtribute;
			}
		} catch (Exception e) {
			logger.error("Cannot getOrderCountCached for searchid " + searchId, e);
			return new HashMap<String, Set<Integer>>();
		}
	}
	
	/**
	 * Before INTERNAL_LOG_ORIGINAL_LOCATION flag the log was written in a file locally and then stored in the database (<b>value 0</b>)<br>
	 * After this flag was added it is possible to write the log directly in the database statement by statement (<b>value ServerConfig.getLogInTableVersion()</b>)<br>
	 * @return true only if the log was started directly in the database
	 */
	public boolean isLogInDatabase() {
		return getLogOriginalLocation() == ServerConfig.getLogInTableVersion();
	}
	
	public int getLogOriginalLocation() {
		try {
			Object objectAtribute = getObjectAtribute(INTERNAL_LOG_ORIGINAL_LOCATION);
			if (objectAtribute == null) {
				return 0;
			} else {
				if(objectAtribute instanceof Integer) {
					return (Integer)objectAtribute;
				}
				return 0;
			}
		} catch (Exception e) {
			logger.error("Cannot find if isLogInDatabase for searchid " + searchId, e);
			return 0;
		}
	}
}