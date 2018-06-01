package ro.cst.tsearch.parentsitedescribe;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.stewart.ats.base.document.DocumentI.SearchType;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.servers.types.XXGenericPublicDataParentSiteConfiguration;
import ro.cst.tsearch.utils.StringUtils;

public class ParentSiteEditorUtils {

	/**
	 * Used for the HTML Control zone in Parent Site to list available keys that
	 * can be set o a function
	 */
	public static final TreeMap<String, String> htmlFunctionSaKeys = new TreeMap<String, String>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		{
			put(SearchAttributes.OWNER_LAST_NAME, "");
			put(SearchAttributes.OWNER_FIRST_NAME, "");
			put(SearchAttributes.OWNER_MIDDLE_NAME, "");
			put(SearchAttributes.OWNER_COMPANY_NAME, "");
			put(SearchAttributes.OWNER_FNAME, "");
			put(SearchAttributes.OWNER_MNAME, "");
			put(SearchAttributes.OWNER_MINITIAL, "");
			put(SearchAttributes.OWNER_LNAME, "");
			put(SearchAttributes.OWNER_LF_NAME, "");
			put(SearchAttributes.OWNER_LFM_NAME, "");
			put(SearchAttributes.OWNER_FML_NAME, "");
			put(SearchAttributes.OWNER_ZIP, "");
			put(SearchAttributes.OWNER_FULL_NAME, "OWNER_FULL_NAME");
			put(SearchAttributes.OWNER_LCF_NAME, "");
			put(SearchAttributes.OWNER_LCFM_NAME, "");
			put(SearchAttributes.BUYER_FULL_NAME, "BUYER_FULL_NAME");
			put(SearchAttributes.FROMDATE, "");
			put(SearchAttributes.FROMDATE_YEAR, "");
			put(SearchAttributes.FROMDATE_MMDD, "");
			put(SearchAttributes.FROMDATE_MM, "");
			put(SearchAttributes.FROMDATE_DD, "");
			put(SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY, "");
			put(SearchAttributes.FROMDATE_MM_DD_YYYY, "");
			put(SearchAttributes.FROMDATE_MMM_DD_YYYY, "");
			put(SearchAttributes.FROMDATE_MM_DD_YY, "");
			put(SearchAttributes.FROMDATE_YYYY_MM_DD, "");
			put(SearchAttributes.TODATE, "");
			put(SearchAttributes.TODATE_YEAR, "");
			put(SearchAttributes.TODATE_MMDD, "");
			put(SearchAttributes.TODATE_MM, "");
			put(SearchAttributes.TODATE_DD, "");
			put(SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY, "");
			put(SearchAttributes.TODATE_MM_DD_YYYY, "");
			put(SearchAttributes.TODATE_MMM_DD_YYYY, "");
			put(SearchAttributes.TODATE_MM_DD_YY, "");
			put(SearchAttributes.TODATE_YYYY_MM_DD, "");
			put(SearchAttributes.P_FULL_ADDRESS_N_D_N_S, "");
			put(SearchAttributes.P_STREETNO, "");
			put(SearchAttributes.P_STREET_NO_NA_SU, "");
			put(SearchAttributes.P_STREET_NA_SU_NO, "");
			put(SearchAttributes.P_STREETDIRECTION, "");
			put(SearchAttributes.P_STREET_POST_DIRECTION, "");
			put(SearchAttributes.P_STREETDIRECTION_ABBREV, "");
			put(SearchAttributes.P_STREETNAME, "");
			put(SearchAttributes.P_STREET_NO_NAME, "");
			put(SearchAttributes.P_STREET_NO_DIR_NAME_POSTDIR, "");
			put(SearchAttributes.P_STREETSUFIX, "");
			put(SearchAttributes.P_STREETSUFIX_ABBREV, "");
			put(SearchAttributes.P_STREETUNIT, "");
			put(SearchAttributes.P_CITY, "");
			put(SearchAttributes.P_MUNICIPALITY, "");
			put(SearchAttributes.P_STATE, "");
			put(SearchAttributes.P_STATE_ABREV, "");
			put(SearchAttributes.P_COUNTY, "");
			put(SearchAttributes.P_COUNTY_FIPS, "");
			put(SearchAttributes.P_COUNTY_NAME, "");
			put(SearchAttributes.P_ZIP, "");
			put(SearchAttributes.P_STREET_FULL_NAME, "");
			put(SearchAttributes.P_STREET_FULL_NAME_EX, "");
			put(SearchAttributes.P_STREETNAME_SUFFIX_UNIT_NO, "");
			put(SearchAttributes.P_STREET_FULL_NAME_NO_SUFFIX, "");
			put(SearchAttributes.LD_INSTRNO, "");
			put(SearchAttributes.LD_NCB_NO, "");
			put(SearchAttributes.LD_ABS_NO, "");
			put(SearchAttributes.LD_BOOKNO, "");
			put(SearchAttributes.LD_PAGENO, "");
			put(SearchAttributes.LD_BOOKNO_1, "");
			put(SearchAttributes.LD_PAGENO_1, "");
			put(SearchAttributes.LD_BOOKPAGE, "");
			put(SearchAttributes.LD_SUBDIVISION, "");
			put(SearchAttributes.LD_LOTNO, "");
			put(SearchAttributes.LD_SUBLOT, "");
			put(SearchAttributes.LD_SUBDIV_NAME, "");
			put(SearchAttributes.LD_SUBDIV_NAME_MOJACKSONRO, "");
			put(SearchAttributes.LD_SUBDIV_BLOCK, "");
			put(SearchAttributes.LD_SUBDIV_PHASE, "");
			put(SearchAttributes.LD_SUBDIV_TRACT, "");
			put(SearchAttributes.LD_SUBDIV_SEC, "");
			put(SearchAttributes.LD_SUBDIV_TWN, "");
			put(SearchAttributes.LD_SECTION, "");
			put(SearchAttributes.LD_FL_HERNANDO_SECTION, "");
			put(SearchAttributes.LD_FL_HERNANDO_BOOK, "");
			put(SearchAttributes.LD_FL_HERNANDO_TOWNSHIP, "");
			put(SearchAttributes.LD_FL_HERNANDO_RANGE, "");
			put(SearchAttributes.LD_FL_HERNANDO_SUBDIVIZION, "");
			put(SearchAttributes.LD_FL_HERNANDO_BLOCK, "");
			put(SearchAttributes.LD_FL_HERNANDO_LOT, "");
			put(SearchAttributes.LD_FL_PALM_BEACH_TOWNSHIP, "");
			put(SearchAttributes.LD_FL_PALM_BEACH_RANGE, "");
			put(SearchAttributes.LD_FL_PALM_BEACH_SUBDIVISION, "");
			put(SearchAttributes.LD_FL_PALM_BEACH_BLOCK, "");
			put(SearchAttributes.LD_FL_PALM_BEACH_LOT, "");
			put(SearchAttributes.LD_FL_PALM_BEACH_CITY, "");
			put(SearchAttributes.LD_FL_PALM_BEACH_SECTION, "");
			put(SearchAttributes.LD_FL_PINELLAS_TOWNSHIP, "");
			put(SearchAttributes.LD_FL_PINELLAS_RANGE, "");
			put(SearchAttributes.LD_FL_PINELLAS_SUBDIVISION, "");
			put(SearchAttributes.LD_FL_PINELLAS_BLOCK, "");
			put(SearchAttributes.LD_FL_PINELLAS_LOT, "");
			put(SearchAttributes.LD_FL_PINELLAS_CITY, "");
			put(SearchAttributes.LD_FL_PINELLAS_SECTION, "");
			put(SearchAttributes.LD_FL_ORANGE_SECTION, "");
			put(SearchAttributes.LD_FL_ORANGE_TOWNSHIP, "");
			put(SearchAttributes.LD_FL_ORANGE_RANGE, "");
			put(SearchAttributes.LD_FL_ORANGE_SUBDIVISION, "");
			put(SearchAttributes.LD_FL_ORANGE_BLOCK, "");
			put(SearchAttributes.LD_FL_ORANGE_LOT, "");
			put(SearchAttributes.LD_MD_DISTRICT, "");
			put(SearchAttributes.LD_MD_ACCOUNT, "");
			put(SearchAttributes.LD_SUBDIV_RNG, "");
			put(SearchAttributes.LD_SUBDIV_CODE, "");
			put(SearchAttributes.LD_LOT_SUBDIVISION, "");
			put(SearchAttributes.LD_PARCELNO, "");
			put(SearchAttributes.LD_PARCELNO_PREFIX, "");
			put(SearchAttributes.LD_PARCELNO_FULL, "");
			put(SearchAttributes.LD_SUBDIV_UNIT, "");
			put(SearchAttributes.LD_PARCELNO_MAP, "");
			put(SearchAttributes.LD_PARCELNO_GROUP, "");
			put(SearchAttributes.LD_PARCELNO_PARCEL, "");
			put(SearchAttributes.LD_TN_WILLIAMSON_Ctl1, "");
			put(SearchAttributes.LD_TN_WILLIAMSON_Ctl2, "");
			put(SearchAttributes.LD_TN_WILLIAMSON_GROUP, "");
			put(SearchAttributes.LD_TN_WILLIAMSON_PARCEL, "");
			put(SearchAttributes.LD_TN_WILLIAMSON_ID, "");
			put(SearchAttributes.LD_TN_WILLIAMSON_SI, "");
			put(SearchAttributes.LD_TN_WILLIAMSON_YB_Ctl1, "");
			put(SearchAttributes.LD_TN_WILLIAMSON_YB_Ctl2, "");
			put(SearchAttributes.LD_TN_WILLIAMSON_YB_Group, "");
			put(SearchAttributes.LD_TN_WILLIAMSON_YB_Parcel, "");
			put(SearchAttributes.LD_TN_WILLIAMSON_YB_Id, "");
			put(SearchAttributes.LD_TN_WILLIAMSON_YB_Si, "");
			put(SearchAttributes.LD_TN_WILLIAMSON_YC_Ctl1, "");
			put(SearchAttributes.LD_TN_WILLIAMSON_YC_Ctl2, "");
			put(SearchAttributes.LD_TN_WILLIAMSON_YC_Group, "");
			put(SearchAttributes.LD_TN_WILLIAMSON_YC_Parcel, "");
			put(SearchAttributes.LD_TN_WILLIAMSON_YC_Id, "");
			put(SearchAttributes.LD_TN_WILLIAMSON_YC_Si, "");
			put(SearchAttributes.LD_PARCELNO_CONDO, "");
			put(SearchAttributes.LD_PARCELNO_PARCEL_CONDO, "");
			put(SearchAttributes.LD_PARCELNO_GENERIC_AO, "");
			put(SearchAttributes.LD_PARCELNO_GENERIC_TR, "");
			put(SearchAttributes.LD_PARCELNO_GENERIC_NDB, "");
			put(SearchAttributes.LD_PARCELNO_GENERIC_RO, "");
			put(SearchAttributes.LD_PARCELNO_GENERIC_PRI, "");
			put(SearchAttributes.BUYER_FNAME, "");
			put(SearchAttributes.BUYER_MNAME, "");
			put(SearchAttributes.BUYER_LNAME, "");
			put(SearchAttributes.CURRENT_TAX_YEAR, "");
			put(SearchAttributes.CURRENTDATE_YYYY, "");
			put(SearchAttributes.CURRENTDATE_MM, "");
			put(SearchAttributes.CURRENTDATE_DD, "");
			put(SearchAttributes.P_FULL_ADDRESS_N_D_N_S, "");
			put(SearchAttributes.P_STREET_FULL_NAME, "");
			put(SearchAttributes.P_STREET_FULL_NAME_EX, "");
			put(SearchAttributes.P_STREET_FULL_NAME_NO_SUFFIX, "");
			put(SearchAttributes.P_STREET_NO_STAR_NA_STAR, "");
			put(SearchAttributes.BM1_LENDERNAME, "");
			put(SearchAttributes.BM1_LOADACCOUNTNO, "");
			put(SearchAttributes.BM2_LENDERNAME, "");
			put(SearchAttributes.BM2_LOADACCOUNTNO, "");
			put(SearchAttributes.ADDITIONAL_INFORMATION, "");
			put(SearchAttributes.ADDITIONAL_REQUIREMENTS, "");
			put(SearchAttributes.ADDITIONAL_EXCEPTIONS, "");
			put(SearchAttributes.LEGAL_DESCRIPTION, "");
			put(SearchAttributes.LEGAL_DESCRIPTION_STATUS, "");
			put(SearchAttributes.CERTICICATION_DATE, "");
			put(SearchAttributes.SEARCH_PRODUCT, "");
			put(SearchAttributes.PAYRATE_NEW_VALUE, "");
			put(SearchAttributes.ASSESSED_VALUE, "");
			put(SearchAttributes.LD_PARCELNO2, "");
			put(SearchAttributes.LD_PARCELNO2_ALTERNATE, "");
			put(SearchAttributes.LD_GEO_NUMBER, "");
			put(SearchAttributes.TN_MONTGOMERY_EP_PID, "");
			put(SearchAttributes.LD_PARCELNO3, "");
			put(SearchAttributes.LD_PARCELNO_RANGE, "");
			put(SearchAttributes.LD_TN_SHELBY_WARD, "");
			put(SearchAttributes.LD_TN_SHELBY_BLOCK, "");
			put(SearchAttributes.LD_TN_SHELBY_SUB, "");
			put(SearchAttributes.LD_TN_SHELBY_PARCEL, "");
			put(SearchAttributes.LD_TN_SHELBY_TAG, "");
			put(SearchAttributes.LD_PARCELNO_TOWNSHIP, "");
			put(SearchAttributes.LD_AO_FRANKLIN_TAX_DISTRICT, "");
			put(SearchAttributes.LD_AO_FRANKLIN_PARCEL_NO, "");
			put(SearchAttributes.LD_CO_PARCEL_NO, "");
			put(SearchAttributes.LD_CO_ACCOUNT_NO, "");
			put(SearchAttributes.LD_SK_SUBDIVISION_NAME, "");
			put(SearchAttributes.LD_SK_MAPID_BOOK, "");
			put(SearchAttributes.LD_SK_MAPID_PAGE, "");
			put(SearchAttributes.LD_SK_LOT_LOW, "");
			put(SearchAttributes.LD_SK_LOT_HIGH, "");
			put(SearchAttributes.LD_SK_BLOCK_LOW, "");
			put(SearchAttributes.LD_SK_BLOCK_HIGH, "");

			put(SearchAttributes.LD_TS_SUBDIV_NAME, "");
			put(SearchAttributes.LD_TS_PLAT_BOOK, "");
			put(SearchAttributes.LD_TS_PLAT_PAGE, "");
			put(SearchAttributes.LD_TS_PLAT_BOOK, "");
			put(SearchAttributes.LD_TS_BLOCK, "");
			put(SearchAttributes.LD_TS_LOT, "");
			put(SearchAttributes.QUARTER_ORDER, "");
			put(SearchAttributes.QUARTER_VALUE, "");
			put(SearchAttributes.ARB, "");
			put(SearchAttributes.LD_AREA, "");
			put(SearchAttributes.LD_PI_AREA, "");
			put(SearchAttributes.LD_PI_BLOCK, "");
			put(SearchAttributes.LD_PI_PARCEL, "");
			put(SearchAttributes.LD_PI_SEC, "");
			put(SearchAttributes.LD_PI_UNIT, "");
			put(SearchAttributes.LD_PI_MAP_BOOK, "");
			put(SearchAttributes.LD_PI_MAP_PAGE, "");
			put(SearchAttributes.LD_PI_LOT, "");
			put(SearchAttributes.LD_PI_TRACT, "");
			put(SearchAttributes.LD_PI_MAP_CODE, "");
			put(SearchAttributes.LD_PI_MAJ_LEGAL_NAME, "");
			
			put(SearchAttributes.LD_IL_WILL_AO_BLOCK, "");
			put(SearchAttributes.LD_IL_WILL_AO_COMP_CODE, "");
			put(SearchAttributes.LD_IL_WILL_AO_LOT, "");
			put(SearchAttributes.LD_IL_WILL_AO_MISC, "");
			put(SearchAttributes.LD_IL_WILL_AO_SEC, "");
			put(SearchAttributes.LD_IL_WILL_AO_TWN, "");
			
			put(SearchAttributes.LD_PARCELNO_MAP_GENERIC_TR, "");
			put(SearchAttributes.LD_PARCELNO_GROUP_GENERIC_TR, "");
			put(SearchAttributes.LD_PARCELNO_PARCEL_GENERIC_TR, "");
			put(SearchAttributes.LD_PARCELNO_CTRL_MAP_GENERIC_TR, "");
			
			put(SearchAttributes.LD_MO_PLATTE_TWN, "");
			put(SearchAttributes.LD_MO_PLATTE_AREA, "");
			put(SearchAttributes.LD_MO_PLATTE_SECT, "");
			put(SearchAttributes.LD_MO_PLATTE_QTRSECT, "");
			put(SearchAttributes.LD_MO_PLATTE_BLOCK, "");
			put(SearchAttributes.LD_MO_PLATTE_PARCEL, "");
			
			put(SearchAttributes.LD_TAD_SUBDIVISION_OR_ACREAGE, "");
			put(SearchAttributes.LD_TAD_PLAT_BOOK_PAGE, "");
		}
	};

	// construeste un string pt Iterator din tabelul Parametri
	private static final String iteratorBuffer = "<option value=\"-1\">ITERATOR_TYPE_DEFAULT</OPTION>"
			+ "<option value=\"1\">ITERATOR_TYPE_DEFAULT_NOT_EMPTY</OPTION>"
			+ "<option value=\"2\">ITERATOR_TYPE_DEFAULT_TWO_STATES</OPTION>"
			+ "<option value=\"3\">ITERATOR_TYPE_DEFAULT_EMPTY</OPTION>"
			+ "<option value=\"65\">ITERATOR_TYPE_FROM_DATE</OPTION>"
			+ "<option value=\"10\">ITERATOR_TYPE_LAST_WORD_INITIAL</OPTION>"
			+ "<option value=\"11\">ITERATOR_TYPE_LOT</OPTION>"
			+ "<option value=\"20\">ITERATOR_TYPE_LAST_NAME_FAKE</OPTION>"
			+ "<option value=\"21\">ITERATOR_TYPE_FIRST_NAME_FAKE</OPTION>"
			+ "<option value=\"49\">ITERATOR_TYPE_MIDDLE_NAME_FAKE</OPTION>"
			+ "<option value=\"43\">ITERATOR_TYPE_LF_NAME_FAKE</OPTION>"
			+ "<option value=\"48\">ITERATOR_TYPE_LFM_NAME_FAKE</OPTION>"
			+ "<option value=\"47\">ITERATOR_TYPE_LCF_NAME_FAKE</OPTION>"
			+ "<option value=\"79\">ITERATOR_TYPE_FML_NAME_FAKE</OPTION>"
			+ "<option value=\"22\">ITERATOR_TYPE_ST_NAME_FAKE</OPTION>"
			+ "<option value=\"23\">ITERATOR_TYPE_ST_N0_FAKE</OPTION>"
			+ "<option value=\"24\">ITERATOR_TYPE_BOOK_FAKE</OPTION>"
			+ "<option value=\"25\">ITERATOR_TYPE_PAGE_FAKE</OPTION>"
			+ "<option value=\"26\">ITERATOR_TYPE_INSTRUMENT_LIST_FAKE</OPTION>"
			+ "<option value=\"27\">ITERATOR_TYPE_PARCELID_FAKE</OPTION>"
			+ "<option value=\"28\">ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH</OPTION>"
			+ "<option value=\"29\">ITERATOR_TYPE_BOOK_SEARCH</OPTION>"
			+ "<option value=\"30\">ITERATOR_TYPE_PAGE_SEARCH</OPTION>"
			+ "<option value=\"31\">ITERATOR_TYPE_BP_TYPE</OPTION>"
			+ "<option value=\"32\">ITERATOR_TYPE_PARCEL_MAP</OPTION>"
			+ "<option value=\"33\">ITERATOR_TYPE_PARCEL_SMAP</OPTION>"
			+ "<option value=\"34\">ITERATOR_TYPE_PARCEL_CMAP</OPTION>"
			+ "<option value=\"35\">ITERATOR_TYPE_PARCEL_PN</OPTION>"
			+ "<option value=\"36\">ITERATOR_TYPE_PARCEL_GR</OPTION>"
			+ "<option value=\"37\">ITERATOR_TYPE_PARCEL_DS</OPTION>"
			+ "<option value=\"38\">ITERATOR_TYPE_PARCEL_SI</OPTION>"
			+ "<option value=\"39\">ITERATOR_TYPE_PARCEL_PTYPE</OPTION>"
			+ "<option value=\"40\">ITERATOR_TYPE_RUTHERFORD_CLASS</OPTION>"
			+ "<option value=\"41\">ITERATOR_TYPE_MISSOURI_SBDIV_NAME</OPTION>"
			+ "<option value=\"42\">ITERATOR_TYPE_COMPANY_NAME</OPTION>"
			+ "<option value=\"43\">ITERATOR_TYPE_LF_NAME_FAKE</OPTION>"
			+ "<option value=\"44\">ITERATOR_TYPE_YEAR</OPTION>"
			+ "<option value=\"45\">ITERATOR_TYPE_SEQNO</OPTION>"
			+ "<option value=\"46\">ITERATOR_TYPE_DOCTYPE_SEARCH</OPTION>"
			+ "<option value=\"47\">ITERATOR_TYPE_LCF_NAME_FAKE</OPTION>"
			+ "<option value=\"55\">ITERATOR_TYPE_BLOCK</OPTION>"
			+ "<option value=\"58\">ITERATOR_TYPE_ARB</OPTION>"
			+ "<option value=\"59\">ITERATOR_TYPE_TOWNSHIP</OPTION>"
			+ "<option value=\"60\">ITERATOR_TYPE_RANGE</OPTION>"
			+ "<option value=\"61\">ITERATOR_TYPE_SECTION</OPTION>"
			+ "<option value=\"62\">ITERATOR_TYPE_SUFFIX</OPTION>"
			+ "</select>";

	private static final String moduleBuffer = "<option value=\"0\"> NAME_MODULE_IDX</OPTION>"
			+ "<option value=\"1\"> ADDRESS_MODULE_IDX</OPTION>"
			+ "<option value=\"2\"> PARCEL_ID_MODULE_IDX</OPTION>"
			+ "<option value=\"3\"> BOOK_AND_PAGE_MODULE_IDX</OPTION>"
			+ "<option value=\"4\"> INSTR_NO_MODULE_IDX</OPTION>"
			+ "<option value=\"5\">BGN_END_DATE_MODULE_IDX</OPTION>"
			+ "<option value=\"6\"> TYPE_NAME_MODULE_IDX</OPTION>"
			+ "<option value=\"7\">SUBDIVISION_MODULE_IDX</OPTION>"
			+ "<option value=\"8\">BOOK_AND_PAGE_LOCAL_MODULE_IDX</OPTION>"
			+ "<option value=\"9\">SUBDIVISION_PLAT_MODULE_IDX</OPTION>"
			+ "<option value=\"10\">TAX_BILL_MODULE_IDX</OPTION>"
			+ "<option value=\"11\">SORT_TYPE_MODULE_IDX</OPTION>"
			+ "<option value=\"12\">ADV_SEARCH_MODULE_IDX</OPTION>"
			+ "<option value=\"13\">ARCHIVE_DOCS_MODULE_IDX</OPTION>"
			+ "<option value=\"14\">SECOND_ARCHIVE_DOCS_MODULE_IDX</OPTION>"
			+ "<option value=\"15\">PROP_NO_IDX</OPTION>"
			+ "<option value=\"16\">TAX_BILL_NO_IDX</OPTION>"
			+ "<option value=\"17\">ADDRESS_MODULE_IDX2</OPTION>"
			+ "<option value=\"18\">GENERIC_MODULE_IDX</OPTION>"
			+ "<option value=\"19\">CONDOMIN_MODULE_IDX</OPTION>"
			+ "<option value=\"20\">SECTION_LAND_MODULE_IDX</OPTION>"
			+ "<option value=\"21\">SURVEYS_MODULE_IDX</OPTION>"
			+ "<option value=\"19\">MODULE_IDX19</OPTION>"
			+ "<option value=\"20\">MODULE_IDX20</OPTION>"
			+ "<option value=\"21\">MODULE_IDX21</OPTION>"
			+ "<option value=\"22\">MO_JACK_AO_PARCEL_STATUS_FILTER</OPTION>"
			+ "<option value=\"23\">MO_JACK_AO_PARCEL_ORIGIN_FILTER</OPTION>"
			+ "<option value=\"24\">MO_JACK_AO_PARCEL_TYPE_FILTER</OPTION>"
			+ "<option value=\"25\">BUSINESS_NAME_MODULE_IDX</OPTION>"
			+ "<option value=\"26\">SERIAL_ID_MODULE_IDX</OPTION>"
			+ "<option value=\"27\">SALES_MODULE_IDX</OPTION>"
			+ "<option value=\"28\">CASE_NAME_MODULE_IDX</OPTION>"
			+ "<option value=\"29\">MALE_NAME_MODULE_IDX</OPTION>"
			+ "<option value=\"30\">FEMALE_NAME_MODULE_IDX</OPTION>"
			+ "<option value=\"31\">LICENSE_DATE_MODULE_IDX</OPTION>"
			+ "<option value=\"32\">MODULE_IDX32</OPTION>"
			+ "<option value=\"33\">MODULE_IDX33</OPTION>"
			+ "<option value=\"34\">MOTHER_NAME_MODULE_IDX</OPTION>"
			+ "<option value=\"35\">FATHER_NAME_MODULE_IDX</OPTION>"
			+ "<option value=\"36\">RELATED_MODULE_IDX</OPTION>"
			+ "<option value=\"37\">FAKE_MODULE_IDX</OPTION>"
			+ "<option value=\"38\">MODULE_IDX38</OPTION>"
			+ "<option value=\"39\">MODULE_IDX39</OPTION>"
			+ "<option value=\"40\">MODULE_IDX40</OPTION>"
			+ "<option value=\"41\">MODULE_IDX41</OPTION>"
			+ "<option value=\"42\">MODULE_IDX42</OPTION>"
			+ "<option value=\"43\">MODULE_IDX43</OPTION>"
			+ "<option value=\"44\">MODULE_IDX44</OPTION>"
			+ "<option value=\"45\">MODULE_IDX45</OPTION>"
			+ "<option value=\"46\">MODULE_IDX46</OPTION>"
			+ "<option value=\"47\">MODULE_IDX47</OPTION>"
			+ "<option value=\"48\">MODULE_IDX48</OPTION>"
			+ "<option value=\"49\">MODULE_IDX49</OPTION>"
			+ "<option value=\"50\">MODULE_IDX50</OPTION>"
			+ "<option value=\"51\">MODULE_IDX51</OPTION>"
			+ "<option value=\"52\">MODULE_IDX52</OPTION>"
			+ "<option value=\"53\">MODULE_IDX53</OPTION>"
			+ "<option value=\"55\">ARB_MODULE_IDX</OPTION>"
			+ "<option value=\"56\">NEXT_LINK_MODULE_IDX</OPTION>"
			+ "<option value=\"57\">DATABASE_SEARCH_MODULE_IDX</OPTION>"
			+ "<option value=\"103\">OLD_ACCOUNT_MODULE_IDX</OPTION>"
			+ "<option value=\"144\">DASL_GENERAL_SEARCH_MODULE_IDX</OPTION>"
			+ "<option value=\"155\">IMG_MODULE_IDX</OPTION>";

	private static final String parcelIDBuffer = "<option value=\"1\">ID_SEARCH_BY_NAME"
			+ "<option value=\"2\">ID_SEARCH_BY_ADDRESS"
			+ "<option value=\"3\">ID_SEARCH_BY_PARCEL"
			+ "<option value=\"4\">ID_SEARCH_BY_TAX_BIL_NO"
			+ "<option value=\"5\">ID_SEARCH_BY_INSTRUMENT_NO"
			+ "<option value=\"6\">ID_SEARCH_BY_SUBDIVISION_NAME"
			+ "<option value=\"7\">ID_SAVE_TO_TSD"
			+ "<option value=\"8\">ID_SEARCH_BY_BOOK_AND_PAGE"
			+ "<option value=\"9\">ID_SEARCH_BY_SUBDIVISION_PLAT"
			+ "<option value=\"10\">ID_BROWSE_SCANNED_INDEX_PAGES"
			+ "<option value=\"11\">ID_BROWSE_BACKSCANNED_PLATS"
			+ "<option value=\"12\">ID_SEARCH_BY_PROP_NO"
			+ "<option value=\"13\">ID_GET_IMAGE"
			+ "<option value=\"14\">ID_SEARCH_BY_CONDO_NAME"
			+ "<option value=\"15\">ID_SEARCH_BY_SECTION_LAND"
			+ "<option value=\"16\">ID_SEARCH_BY_SURVEYS"
			+ "<option value=\"17\">ID_SEARCH_BY_SERIAL_ID"
			+ "<option value=\"18\">ID_SEARCH_BY_SALES"
			+ "<option value=\"19\">ID_BROWSE_BACKSCANNED_DEEDS"
			+ "<option value=\"19\">ID_SEARCH_BY_MODULE19"
			+ "<option value=\"20\">ID_SEARCH_BY_MODULE20"
			+ "<option value=\"21\">ID_SEARCH_BY_MODULE21"
			+ "<option value=\"22\">ID_SEARCH_BY_MODULE22"
			+ "<option value=\"23\">ID_SEARCH_BY_MODULE23"
			+ "<option value=\"24\">ID_SEARCH_BY_MODULE24"
			+ "<option value=\"25\">ID_SEARCH_BY_MODULE25"
			+ "<option value=\"26\">ID_SEARCH_BY_MODULE26"
			+ "<option value=\"27\">ID_SEARCH_BY_MODULE27"
			+ "<option value=\"28\">ID_SEARCH_BY_MODULE28"
			+ "<option value=\"29\">ID_SEARCH_BY_MODULE29"
			+ "<option value=\"30\">ID_SEARCH_BY_MODULE30"
			+ "<option value=\"31\">ID_SEARCH_BY_MODULE31"
			+ "<option value=\"32\">ID_SEARCH_BY_MODULE32"
			+ "<option value=\"33\">ID_SEARCH_BY_MODULE33"
			+ "<option value=\"34\">ID_SEARCH_BY_MODULE34"
			+ "<option value=\"35\">ID_SEARCH_BY_MODULE35"
			+ "<option value=\"36\">ID_SEARCH_BY_MODULE36"
			+ "<option value=\"37\">ID_SEARCH_BY_MODULE37"
			+ "<option value=\"38\">ID_SEARCH_BY_MODULE38"
			+ "<option value=\"39\">ID_SEARCH_BY_MODULE39"
			+ "<option value=\"40\">ID_SEARCH_BY_MODULE40"
			+ "<option value=\"41\">ID_SEARCH_BY_MODULE41"
			+ "<option value=\"42\">ID_SEARCH_BY_MODULE42"
			+ "<option value=\"43\">ID_SEARCH_BY_MODULE43"
			+ "<option value=\"44\">ID_SEARCH_BY_MODULE44"
			+ "<option value=\"45\">ID_SEARCH_BY_MODULE45"
			+ "<option value=\"46\">ID_SEARCH_BY_MODULE46"
			+ "<option value=\"47\">ID_SEARCH_BY_MODULE47"
			+ "<option value=\"48\">ID_SEARCH_BY_MODULE48"
			+ "<option value=\"49\">ID_SEARCH_BY_MODULE49"
			+ "<option value=\"50\">ID_SEARCH_BY_MODULE50"
			+ "<option value=\"51\">ID_SEARCH_BY_MODULE51"
			+ "<option value=\"52\">ID_SEARCH_BY_MODULE52"
			+ "<option value=\"53\">ID_SEARCH_BY_MODULE53"
			+ "<option value=\"55\">ID_SEARCH_BY_ARB"
			+ "<option value=\"101\">ID_DETAILS"
			+ "<option value=\"102\">ID_SEARCH_BY_MALE"
			+ "<option value=\"103\">ID_SEARCH_BY_FEMALE"
			+ "<option value=\"104\">ID_SEARCH_BY_LICENSE_DATE"
			+ "<option value=\"101\">COMBINED_MODULE_PID"
			+ "<option value=\"102\">SUBDIVIDED_MODULE_PID"
			+ "<option value=\"103\">ARB_MODULE_PID"
			+ "<option value=\"104\">SECTIONAL_MODULE_PID"
			+ "<option value=\"105\">REFERENCE_MODULE_PID"
			+ "<option value=\"106\">GENERAL_NAME_MODULE_PID"
			+ "<option value=\"107\">GRANTOR_GRANTEE_MODULE_PID"
			+ "<option value=\"108\">INSTRUMENT_MODULE_PID"
			+ "<option value=\"109\">CASE_MODULE_PID"
			+ "<option value=\"102\">ID_SEARCH_BY_BOOK_PAGE_ADV"
			+ "<option value=\"102\">ID_PID_INDUSTRIAL_SEARCH"
			+ "<option value=\"144\">DASL_GENERAL_SEARCH_MODULE"
			+ "<option value=\"155\">IMG_MODULE";

	private static final String setKeyBuffer = "<option value=\"NO_KEY\">NO_KEY"
			+ "<option value=\"BLANK_ATTR\">BLANK_ATTR"
			+ "<option value=\"OWNER_OBJECT\">OWNER_OBJECT"
			+ "<option value=\"BUYER_OBJECT\">BUYER_OBJECT"
			+ "<option value=\"INSTR_LIST\">INSTR_LIST"
			+ "<option value=\"SEARCHFINISH\">SEARCHFINISH"
			+ "<option value=\"Jan 1, 1810\">INITDATE"
			+ "<option value=\"FROMDATE\">FROMDATE"
			+ "<option value=\"TODATE\">TODATE"
			+ "<option value=\"SEARCHUPDATE\">SEARCHUPDATE"
			+ "<option value=\"P_STREETNO\">P_STREETNO"
			+ "<option value=\"P_STREETDIRECTION\">P_STREETDIRECTION"
			+ "<option value=\"P_STREET_POST_DIRECTION\">P_STREET_POST_DIRECTION"
			+ "<option value=\"P_STREETDIRECTION_ABBREV\">P_STREETDIRECTION_ABBREV"
			+ "<option value=\"P_STREETNAME\">P_STREETNAME"
			+ "<option value=\"P_STREET_NO_NAME\">P_STREET_NO_NAME"
			+ "<option value=\"P_STREET_NO_DIR_NAME_POSTDIR\">P_STREET_NO_DIR_NAME_POSTDIR"
			+ "<option value=\"P_STREETSUFIX\">P_STREETSUFIX"
			+ "<option value=\"P_STREETSUFIX_ABBREV\">P_STREETSUFIX_ABBREV"
			+ "<option value=\"P_STREETUNIT\">P_STREETUNIT"
			+ "<oprion value=\"P_CITY\">P_CITY"
			+ "<option value=\"P_MUNICIPALITY\">P_MUNICIPALITY"
			+ "<option value=\"P_STATE\">P_STATE"
			+ "<option value=\"P_COUNTY\">P_COUNTY"
			+ "<option value=\"P_ZIP\">P_ZIP"
			+ "<option value=\"P_STREET_FULL_NAME\">P_STREET_FULL_NAME"
			+ "<option value=\"P_STREET_FULL_NAME_EX\">P_STREET_FULL_NAME_EX"
			+ "<option value=\"P_STREETNAME_SUFFIX_UNIT_NO\">P_STREETNAME_SUFFIX_UNIT_NO"
			+ "<option value=\"P_STREET_FULL_NAME_NO_SUFFIX\">P_STREET_FULL_NAME_NO_SUFFIX"
			+ "<option value=\"P_STREET_NO_STAR_NA_STAR\">P_STREET_NO_STAR_NA_STAR"
			+ "<option value=\"P_STREET_NO_STAR_NA_STAR\">P_STREET_NO_STAR_NA_STAR"
			+ "<option value=\"P_STREET_NA_SU_NO\">P_STREET_NA_SU_NO"
			+ "<option value=\"LD_INSTRNO\">LD_INSTRNO"
			+ "<option value=\"LD_BOOKNO\">LD_BOOKNO"
			+ "<option value=\"LD_PAGENO\">LD_PAGENO"
			+ "<option value=\"LD_BOOKPAGE\">LD_BOOKPAGE"
			+ "<option value=\"LD_SUBDIVISION\">LD_SUBDIVISION"
			+ "<option value=\"LD_LOTNO\">LD_LOTNO"
			+ "<option value=\"LD_SUBDIV_NAME\">LD_SUBDIV_NAME"
			+ "<option value=\"LD_SUBDIV_NAME_MOJACKSONRO\">LD_SUBDIV_NAME_MOJACKSONRO"
			+ "<option value=\"LD_SUBDIV_BLOCK\">LD_SUBDIV_BLOCK"
			+ "<option value=\"LD_SUBDIV_PHASE\">LD_SUBDIV_PHASE"
			+ "<option value=\"LD_SUBDIV_TRACT\">LD_SUBDIV_TRACT"
			+ "<option value=\"LD_SUBDIV_SEC\">LD_SUBDIV_SEC"
			+ "<option value=\"LD_SUBDIV_TWN\">LD_SUBDIV_TWN"
			+ "<option value=\"LD_SECTION\">LD_SECTION"
			+ "<option value=\"LD_FL_HERNANDO_SECTION\">LD_FL_HERNANDO_SECTION"
			+ "<option value=\"LD_FL_HERNANDO_BOOK\">LD_FL_HERNANDO_BOOK"
			+ "<option value=\"LD_FL_HERNANDO_TOWNSHIP\">LD_FL_HERNANDO_TOWNSHIP"
			+ "<option value=\"LD_FL_HERNANDO_RANGE\">LD_FL_HERNANDO_RANGE"
			+ "<option value=\"LD_FL_HERNANDO_SUBDIVIZION\">LD_FL_HERNANDO_SUBDIVIZION"
			+ "<option value=\"LD_FL_HERNANDO_BLOCK\">LD_FL_HERNANDO_BLOCK"
			+ "<option value=\"LD_FL_HERNANDO_LOT\">LD_FL_HERNANDO_LOT"
			+ "<option value=\"LD_FL_PALM_BEACH_TOWNSHIP\">LD_FL_PALM_BEACH_TOWNSHIP"
			+ "<option value=\"LD_FL_PALM_BEACH_RANGE\">LD_FL_PALM_BEACH_RANGE"
			+ "<option value=\"LD_FL_PALM_BEACH_SUBDIVISION\">LD_FL_PALM_BEACH_SUBDIVISION"
			+ "<option value=\"LD_FL_PALM_BEACH_BLOCK\">LD_FL_PALM_BEACH_BLOCK"
			+ "<option value=\"LD_FL_PALM_BEACH_LOT\">LD_FL_PALM_BEACH_LOT"
			+ "<option value=\"LD_FL_PALM_BEACH_CITY\">LD_FL_PALM_BEACH_CITY"
			+ "<option value=\"LD_FL_PALM_BEACH_SECTION\">LD_FL_PALM_BEACH_SECTION"
			+ "<option value=\"LD_FL_PINELLAS_TOWNSHIP\">LD_FL_PINELLAS_TOWNSHIP"
			+ "<option value=\"LD_FL_PINELLAS_RANGE\">LD_FL_PINELLAS_RANGE"
			+ "<option value=\"LD_FL_PINELLAS_SUBDIVISION\">LD_FL_PINELLAS_SUBDIVISION"
			+ "<option value=\"LD_FL_PINELLAS_BLOCK\">LD_FL_PINELLAS_BLOCK"
			+ "<option value=\"LD_FL_PINELLAS_LOT\">LD_FL_PINELLAS_LOT"
			+ "<option value=\"LD_FL_PINELLAS_CITY\">LD_FL_PINELLAS_CITY"
			+ "<option value=\"LD_FL_PINELLAS_SECTION\">LD_FL_PINELLAS_SECTION"
			+ "<option value=\"LD_FL_ORANGE_SECTION\">LD_FL_ORANGE_SECTION"
			+ "<option value=\"LD_FL_ORANGE_TOWNSHIP\">LD_FL_ORANGE_TOWNSHIP"
			+ "<option value=\"LD_FL_ORANGE_RANGE\">LD_FL_ORANGE_RANGE"
			+ "<option value=\"LD_FL_ORANGE_SUBDIVISION\">LD_FL_ORANGE_SUBDIVISION"
			+ "<option value=\"LD_FL_ORANGE_BLOCK\">LD_FL_ORANGE_BLOCK"
			+ "<option value=\"LD_FL_ORANGE_LOT\">LD_FL_ORANGE_LOT"
			+ "<option value=\"LD_MD_DISTRICT\">LD_MD_DISTRICT"
			+ "<option value=\"LD_MD_ACCOUNT\">LD_MD_ACCOUNT"
			+ "<option value=\"LD_FL_WAKULLA_TR_PID\">LD_FL_WAKULLA_TR_PID"
			+ "<option value=\"LD_SUBDIV_RNG\">LD_SUBDIV_RNG"
			+ "<option value=\"LD_SUBDIV_CODE\">LD_SUBDIV_CODE"
			+ "<option value=\"LD_LOT_SUBDIVISION\">LD_LOT_SUBDIVISION"
			+ "<option value=\"LD_PARCELNO\">LD_PARCELNO"
			+ "<option value=\"LD_PARCELNO_PREFIX\">LD_PARCELNO_PREFIX"
			+ "<option value=\"LD_PARCELNO_FULL\">LD_PARCELNO_FULL"
			+ "<option value=\"LD_SUBDIV_UNIT\">LD_SUBDIV_UNIT"
			+ "<option value=\"LD_PARCELNO_MAP\">LD_PARCELNO_MAP"
			+ "<option value=\"LD_PARCELNO_GROUP\">LD_PARCELNO_GROUP"
			+ "<option value=\"LD_PARCELNO_PARCEL\">LD_PARCELNO_PARCEL"
			+ "<option value=\"LD_TN_WILLIAMSON_Ctl1\">LD_TN_WILLIAMSON_Ctl1"
			+ "<option value=\"LD_TN_WILLIAMSON_Ctl2\">LD_TN_WILLIAMSON_Ctl2"
			+ "<option value=\"LD_TN_WILLIAMSON_GROUP\">LD_TN_WILLIAMSON_GROUP"
			+ "<option value=\"LD_TN_WILLIAMSON_PARCEL\">LD_TN_WILLIAMSON_PARCEL"
			+ "<option value=\"LD_TN_WILLIAMSON_ID\">LD_TN_WILLIAMSON_ID"
			+ "<option value=\"LD_TN_WILLIAMSON_SI\">LD_TN_WILLIAMSON_SI"
			+ "<option value=\"LD_TN_WILLIAMSON_YB_Ctl1\">LD_TN_WILLIAMSON_YB_Ctl1"
			+ "<option value=\"LD_TN_WILLIAMSON_YB_Ctl2\">LD_TN_WILLIAMSON_YB_Ctl2"
			+ "<option value=\"LD_TN_WILLIAMSON_YB_Group\">LD_TN_WILLIAMSON_YB_Group"
			+ "<option value=\"LD_TN_WILLIAMSON_YB_Parcel\">LD_TN_WILLIAMSON_YB_Parcel"
			+ "<option value=\"LD_TN_WILLIAMSON_YB_Id\">LD_TN_WILLIAMSON_YB_Id"
			+ "<option value=\"LD_TN_WILLIAMSON_YB_SI\">LD_TN_WILLIAMSON_YB_SI"
			+ "<option value=\"LD_TN_WILLIAMSON_YC_Ctl1\">LD_TN_WILLIAMSON_YC_Ctl1"
			+ "<option value=\"LD_TN_WILLIAMSON_YC_Ctl2\">LD_TN_WILLIAMSON_YC_Ctl2"
			+ "<option value=\"LD_TN_WILLIAMSON_YC_Group\">LD_TN_WILLIAMSON_YC_Group"
			+ "<option value=\"LD_TN_WILLIAMSON_YC_Parcel\">LD_TN_WILLIAMSON_YC_Parcel"
			+ "<option value=\"LD_TN_WILLIAMSON_YC_Id\">LD_TN_WILLIAMSON_YC_Id"
			+ "<option value=\"LD_TN_WILLIAMSON_YC_SI\">LD_TN_WILLIAMSON_YC_SI"
			+ "<option value=\"LD_PARCELNO_CONDO\">LD_PARCELNO_CONDO"
			+ "<option value=\"LD_PARCELNO_PARCEL_CONDO\">LD_PARCELNO_PARCEL_CONDO"
			+ "<option value=\"LD_PARCELNO_CNT_DAVIDSON\">LD_PARCELNO_CNT_DAVIDSON"
			+ "<option value=\"LD_PARCELNO_GENERIC_AO\">LD_PARCELNO_GENERIC_AO</option>"
			+ "<option value=\"LD_PARCELNO_GENERIC_TR\">LD_PARCELNO_GENERIC_TR</option>"
			+ "<option value=\"LD_PARCELNO_GENERIC_NDB\">LD_PARCELNO_GENERIC_NDB</option>"
			+ "<option value=\"LD_PARCELNO_GENERIC_RO\">LD_PARCELNO_GENERIC_RO</option>"
			+ "<option value=\"OWNER_LAST_NAME\">OWNER_LAST_NAME</option>"
			+ "<option value=\"OWNER_FIRST_NAME\">OWNER_FIRST_NAME</option>"
			+ "<option value=\"OWNER_MIDDLE_NAME\">OWNER_MIDDLE_NAME</option>"
			+ "<option value=\"OWNER_COMPANY_NAME\">OWNER_COMPANY_NAME</option>"
			+ "<option value=\"OWNER_FNAME\">OWNER_FNAME</option>"
			+ "<option value=\"OWNER_MNAME\">OWNER_MNAME</option>"
			+ "<option value=\"OWNER_MINITIAL\">OWNER_MINITIAL"
			+ "<option value=\"OWNER_LNAME\">OWNER_LNAME"
			+ "<option value=\"OWNER_LF_NAME\">OWNER_LF_NAME"
			+ "<option value=\"OWNER_ZIP\">OWNER_ZIP"
			+ "<option value=\"O_FULL_NAME\">OWNER_FULL_NAME"
			+ "<option value=\"OWNER_LCF_NAME\">OWNER_LCF_NAME"
			+ "<option value=\"OWNER_LCFM_NAME\">OWNER_LCFM_NAME"
			+ "<option name=\"B_FULL_NAME\" >BUYER_FULL_NAME"
			+ "<option value=\"BUYER_FNAME\">BUYER_FNAME"
			+ "<option value=\"BUYER_MNAME\">BUYER_MNAME"
			+ "<option value=\"BUYER_LNAME\">BUYER_LNAME"
			+ "<option value=\"ABSTRACTOR_EMAIL\">ABSTRACTOR_EMAIL"
			+ "<option value=\"ABSTRACTOR_FILENO\">ABSTRACTOR_FILENO"
			+ "<option value=\"QUARTER_ORDER\">QUARTER_ORDER"
			+ "<option value=\"QUARTER_VALUE\">QUARTER_VALUE"
			+ "<option value=\"ARB\">ARB"
			+ "<option value=\"LD_AREA\">LD_AREA"
			+ "<option value=\"LD_PI_AREA\">LD_PI_AREA"
			+ "<option value=\"LD_PI_BLOCK\">LD_PI_BLOCK"
			+ "<option value=\"LD_PI_PARCEL\">LD_PI_PARCEL"
			+ "<option value=\"LD_PI_SEC\">LD_PI_SEC"
			+ "<option value=\"LD_PI_UNIT\">LD_PI_UNIT"
			+ "<option value=\"LD_PI_MAP_BOOK\">LD_PI_MAP_BOOK"
			+ "<option value=\"LD_PI_MAP_PAGE\">LD_PI_MAP_PAGE"
			+ "<option value=\"LD_PI_LOT\">LD_PI_LOT"
			+ "<option value=\"LD_PI_TRACT\">LD_PI_TRACT"
			+ "<option value=\"LD_PI_MAP_CODE\">LD_PI_MAP_CODE"
			+ "<option value=\"LD_PI_MAJ_LEGAL_NAME\">LD_PI_MAJ_LEGAL_NAME"
			+ "<option value=\"LD_IL_WILL_AO_BLOCK\">LD_IL_WILL_AO_BLOCK"
			+ "<option value=\"LD_IL_WILL_AO_COMP_CODE\">LD_IL_WILL_AO_COMP_CODE"
			+ "<option value=\"LD_IL_WILL_AO_LOT\">LD_IL_WILL_AO_LOT"
			+ "<option value=\"LD_IL_WILL_AO_MISC\">LD_IL_WILL_AO_MISC"
			+ "<option value=\"LD_IL_WILL_AO_SEC\">LD_IL_WILL_AO_SEC"
			+ "<option value=\"LD_IL_WILL_AO_TWN\">LD_IL_WILL_AO_TWN"
			+ "<option value=\"LD_PARCELNO_MAP_GENERIC_TR\">LD_PARCELNO_MAP_GENERIC_TR"
			+ "<option value=\"LD_PARCELNO_CTRL_MAP_GENERIC_TR\">LD_PARCELNO_CTRL_MAP_GENERIC_TR"
			+ "<option value=\"LD_PARCELNO_GROUP_GENERIC_TR\">LD_PARCELNO_GROUP_GENERIC_TR"
			+ "<option value=\"LD_PARCELNO_PARCEL_GENERIC_TR\">LD_PARCELNO_PARCEL_GENERIC_TR"
			+ "<option value=\"LD_MO_PLATTE_TWN\">LD_MO_PLATTE_TWN"
			+ "<option value=\"LD_MO_PLATTE_AREA\">LD_MO_PLATTE_AREA"
			+ "<option value=\"LD_MO_PLATTE_SECT\">LD_MO_PLATTE_SECT"
			+ "<option value=\"LD_MO_PLATTE_QTRSECT\">LD_MO_PLATTE_QTRSECT"
			+ "<option value=\"LD_MO_PLATTE_BLOCK\">LD_MO_PLATTE_BLOCK"
			+ "<option value=\"LD_MO_PLATTE_PARCEL\">LD_MO_PLATTE_PARCEL";

	private static final String oneParameterIterator = "<option value=\"-1\">ITERATOR_TYPE_DEFAULT'+\n"
			+ "'<option value=\"1\">ITERATOR_TYPE_DEFAULT_NOT_EMPTY'+\n"
			+ "'<option value=\"2\">ITERATOR_TYPE_DEFAULT_TWO_STATES'+\n"
			+ "'<option value=\"3\">ITERATOR_TYPE_DEFAULT_EMPTY'+\n"
			+ "'<option value=\"10\">ITERATOR_TYPE_LAST_WORD_INITIAL'+\n"
			+ "'<option value=\"11\">ITERATOR_TYPE_LOT'+\n"
			+ "'<option value=\"20\">ITERATOR_TYPE_LAST_NAME_FAKE'+\n"
			+ "'<option value=\"21\">ITERATOR_TYPE_FIRST_NAME_FAKE'+\n"
			+ "'<option value=\"49\">ITERATOR_TYPE_MIDDLE_NAME_FAKE'+\n"
			+ "'<option value=\"43\">ITERATOR_TYPE_LF_NAME_FAKE'+\n"
			+ "'<option value=\"48\">ITERATOR_TYPE_LFM_NAME_FAKE'+\n"
			+ "'<option value=\"47\">ITERATOR_TYPE_LCF_NAME_FAKE'+\n"
			+ "'<option value=\"79\">ITERATOR_TYPE_FML_NAME_FAKE'+\n"
			+ "'<option value=\"22\">ITERATOR_TYPE_ST_NAME_FAKE'+\n"
			+ "'<option value=\"23\">ITERATOR_TYPE_ST_N0_FAKE'+\n"
			+ "'<option value=\"24\">ITERATOR_TYPE_BOOK_FAKE'+\n"
			+ "'<option value=\"25\">ITERATOR_TYPE_PAGE_FAKE'+\n"
			+ "'<option value=\"26\">ITERATOR_TYPE_INSTRUMENT_LIST_FAKE'+\n"
			+ "'<option value=\"27\">ITERATOR_TYPE_PARCELID_FAKE'+\n"
			+ "'<option value=\"28\">ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH'+\n"
			+ "'<option value=\"29\">ITERATOR_TYPE_BOOK_SEARCH'+\n"
			+ "'<option value=\"30\">ITERATOR_TYPE_PAGE_SEARCH'+\n"
			+ "'<option value=\"31\">ITERATOR_TYPE_BP_TYPE'+\n"
			+ "'<option value=\"32\">ITERATOR_TYPE_PARCEL_MAP'+\n"
			+ "'<option value=\"33\">ITERATOR_TYPE_PARCEL_SMAP'+\n"
			+ "'<option value=\"34\">ITERATOR_TYPE_PARCEL_CMAP'+\n"
			+ "'<option value=\"35\">ITERATOR_TYPE_PARCEL_PN'+\n"
			+ "'<option value=\"36\">ITERATOR_TYPE_PARCEL_GR'+\n"
			+ "'<option value=\"37\">ITERATOR_TYPE_PARCEL_DS'+\n"
			+ "'<option value=\"38\">ITERATOR_TYPE_PARCEL_SI'+\n"
			+ "'<option value=\"39\">ITERATOR_TYPE_PARCEL_PTYPE'+\n"
			+ "'<option value=\"40\">ITERATOR_TYPE_RUTHERFORD_CLASS'+\n"
			+ "'<option value=\"41\">ITERATOR_TYPE_MISSOURI_SBDIV_NAME'+\n"
			+ "'<option value=\"42\">ITERATOR_TYPE_COMPANY_NAME'+\n"
			+ "'<option value=\"43\">ITERATOR_TYPE_LF_NAME_FAKE'+\n"
			+ "'<option value=\"44\">ITERATOR_TYPE_YEAR'+\n"
			+ "'<option value=\"45\">ITERATOR_TYPE_SEQNO'+\n"
			+ "'<option value=\"46\">ITERATOR_TYPE_DOCTYPE_SEARCH'+\n"
			+ "'<option value=\"47\">ITERATOR_TYPE_LCF_NAME_FAKE</OPTION>"
			+ "<option value=\"55\">ITERATOR_TYPE_BLOCK</OPTION>"
			+ "<option value=\"62\">ITERATOR_TYPE_SUFFIX</OPTION>"
			
			+ "<option value=\"67\">ITERATOR_TYPE_GENERIC_67</OPTION>"
			+ "<option value=\"68\">ITERATOR_TYPE_GENERIC_68</OPTION>"
			+ "<option value=\"69\">ITERATOR_TYPE_GENERIC_69</OPTION>"
			+ "<option value=\"70\">ITERATOR_TYPE_GENERIC_70</OPTION>"
			+ "<option value=\"71\">ITERATOR_TYPE_GENERIC_71</OPTION>"
			+ "<option value=\"72\">ITERATOR_TYPE_GENERIC_72</OPTION>"
			+ "<option value=\"73\">ITERATOR_TYPE_GENERIC_73</OPTION>"
			+ "<option value=\"74\">ITERATOR_TYPE_GENERIC_74</OPTION>"
			+ "<option value=\"75\">ITERATOR_TYPE_GENERIC_75</OPTION>"
			+ "<option value=\"76\">ITERATOR_TYPE_GENERIC_76</OPTION>"
			
			+ "</select>";

	private static final String oneParameterIterator2 = "<option value=\"\" selected>SELECT -OWNER-'+\n"
			+ "'<option value=\"OWNER_LAST_NAME\">OWNER_LAST_NAME'+\n"
			+ "'<option value=\"OWNER_FIRST_NAME\">OWNER_FIRST_NAME'+\n"
			+ "'<option value=\"OWNER_MIDDLE_NAME\">OWNER_MIDDLE_NAME'+\n"
			+ "'<option value=\"OWNER_COMPANY_NAME\">OWNER_COMPANY_NAME'+\n"
			+ "'<option value=\"OWNER_FNAME\">OWNER_FNAME'+\n"
			+ "'<option value=\"OWNER_MNAME\">OWNER_MNAME'+\n"
			+ "'<option value=\"OWNER_MINITIAL\">OWNER_MINITIAL'+\n"
			+ "'<option value=\"OWNER_LNAME\">OWNER_LNAME'+\n"
			+ "'<option value=\"OWNER_LF_NAME\">OWNER_LF_NAME'+\n"
			+ "'<option value=\"OWNER_ZIP\">OWNER_ZIP'+\n"
			+ "'<option value=\"O_FULL_NAME\">OWNER_FULL_NAME'+\n"
			+ "'<option value=\"OWNER_LCF_NAME\">OWNER_LCF_NAME'+\n"
			+ "'<option value=\"OWNER_LCFM_NAME\">OWNER_LCFM_NAME'+\n"
			+ "'<option value=\"B_FULL_NAME\">BUYER_FULL_NAME'+\n"
			+ "'<option value=\"FROMDATE\">FROMDATE'+\n"
			+ "'<option value=\"TODATE\">TODATE'+\n"
			+ "'<option value=\"P_STREETNO\">P_STREETNO'+\n"
			+ "'<option value=\"P_STREETDIRECTION\">P_STREETDIRECTION'+\n"
			+ "'<option value=\"P_STREETDIRECTION_ABBREV\">P_STREETDIRECTION_ABBREV'+\n"
			+ "'<option value=\"P_STREETNAME\">P_STREETNAME'+\n"
			+ "'<option value=\"P_STREET_NO_NAME\">P_STREET_NO_NAME'+\n"
			+ "'<option value=\"P_STREET_NO_DIR_NAME_POSTDIR\">P_STREET_NO_DIR_NAME_POSTDIR'+\n"
			+ "'<option value=\"P_STREETSUFIX\">P_STREETSUFIX'+\n"
			+ "'<option value=\"P_STREETSUFIX_ABBREV\">P_STREETSUFIX_ABBREV'+\n"
			+ "'<option value=\"P_STREETUNIT\">P_STREETUNIT'+\n"
			+ "'<option value=\"P_CITY\">P_CITY'+\n"
			+ "'<option value=\"P_MUNICIPALITY\">P_MUNICIPALITY'+\n"
			+ "'<option value=\"P_STATE\">P_STATE'+\n"
			+ "'<option value=\"P_COUNTY\">P_COUNTY'+\n"
			+ "'<option value=\"P_ZIP\">P_ZIP'+\n"
			+ "'<option value=\"P_STREET_FULL_NAME\">P_STREET_FULL_NAME'+\n"
			+ "'<option value=\"P_STREET_FULL_NAME_EX\">P_STREET_FULL_NAME_EX'+\n"
			+ "'<option value=\"P_STREETNAME_SUFFIX_UNIT_NO\">P_STREETNAME_SUFFIX_UNIT_NO'+\n"
			+ "'<option value=\"P_STREET_FULL_NAME_NO_SUFFIX\">P_STREET_FULL_NAME_NO_SUFFIX'+\n"
			+ "'<option value=\"P_STREET_NO_STAR_NA_STAR\">P_STREET_NO_STAR_NA_STAR'+\n"
			+ "'<option value=\"P_STREET_NA_SU_NO\">P_STREET_NA_SU_NO'+\n"
			+ "'<option value=\"LD_INSTRNO\">LD_INSTRNO'+\n"
			+ "'<option value=\"LD_BOOKNO\">LD_BOOKNO'+\n"
			+ "'<option value=\"LD_PAGENO\">LD_PAGENO'+\n"
			+ "'<option value=\"LD_BOOKPAGE\">LD_BOOKPAGE'+\n"
			+ "'<option value=\"LD_SUBDIVISION\">LD_SUBDIVISION'+\n"
			+ "'<option value=\"LD_LOTNO\">LD_LOTNO'+\n"
			+ "'<option value=\"LD_SUBDIV_NAME\">LD_SUBDIV_NAME'+\n"
			+ "'<option value=\"LD_SUBDIV_NAME_MOJACKSONRO\">LD_SUBDIV_NAME_MOJACKSONRO'+\n"
			+ "'<option value=\"LD_SUBDIV_BLOCK\">LD_SUBDIV_BLOCK'+\n"
			+ "'<option value=\"LD_SUBDIV_PHASE\">LD_SUBDIV_PHASE'+\n"
			+ "'<option value=\"LD_SUBDIV_TRACT\">LD_SUBDIV_TRACT'+\n"
			+ "'<option value=\"LD_SUBDIV_SEC\">LD_SUBDIV_SEC'+\n"
			+ "'<option value=\"LD_SUBDIV_TWN\">LD_SUBDIV_TWN'+\n"
			+ "'<option value=\"LD_SECTION\">LD_SECTION'+\n"
			+ "'<option value=\"LD_FL_HERNANDO_SECTION\">LD_FL_HERNANDO_SECTION'+\n"
			+ "'<option value=\"LD_FL_HERNANDO_BOOK\">LD_FL_HERNANDO_BOOK'+\n"
			+ "'<option value=\"LD_FL_HERNANDO_TOWNSHIP\">LD_FL_HERNANDO_TOWNSHIP'+\n"
			+ "'<option value=\"LD_FL_HERNANDO_RANGE\">LD_FL_HERNANDO_RANGE'+\n"
			+ "'<option value=\"LD_FL_HERNANDO_SUBDIVIZION\">LD_FL_HERNANDO_SUBDIVIZION'+\n"
			+ "'<option value=\"LD_FL_HERNANDO_BLOCK\">LD_FL_HERNANDO_BLOCK'+\n"
			+ "'<option value=\"LD_FL_HERNANDO_LOT\">LD_FL_HERNANDO_LOT'+\n"
			+ "'<option value=\"LD_FL_PALM_BEACH_SECTION\">LD_FL_PALM_BEACH_SECTION'+\n"
			+ "'<option value=\"LD_FL_PALM_BEACH_TOWNSHIP\">LD_FL_PALM_BEACH_TOWNSHIP'+\n"
			+ "'<option value=\"LD_FL_PALM_BEACH_RANGE\">LD_FL_PALM_BEACH_RANGE'+\n"
			+ "'<option value=\"LD_FL_PALM_BEACH_SUBDIVISION\">LD_FL_PALM_BEACH_SUBDIVISION'+\n"
			+ "'<option value=\"LD_FL_PALM_BEACH_BLOCK\">LD_FL_PALM_BEACH_BLOCK'+\n"
			+ "'<option value=\"LD_FL_PALM_BEACH_LOT\">LD_FL_PALM_BEACH_LOT'+\n"
			+ "'<option value=\"LD_FL_PALM_BEACH_CITY\">LD_FL_PALM_BEACH_CITY'+\n"
			+ "'<option value=\"LD_FL_PINELLAS_SECTION\">LD_FL_PINELLAS_SECTION'+\n"
			+ "'<option value=\"LD_FL_PINELLAS_TOWNSHIP\">LD_FL_PINELLAS_TOWNSHIP'+\n"
			+ "'<option value=\"LD_FL_PINELLAS_RANGE\">LD_FL_PINELLAS_RANGE'+\n"
			+ "'<option value=\"LD_FL_PINELLAS_SUBDIVISION\">LD_FL_PINELLAS_SUBDIVISION'+\n"
			+ "'<option value=\"LD_FL_PINELLAS_BLOCK\">LD_FL_PINELLAS_BLOCK'+\n"
			+ "'<option value=\"LD_FL_PINELLAS_LOT\">LD_FL_PINELLAS_LOT'+\n"
			+ "'<option value=\"LD_FL_PINELLAS_CITY\">LD_FL_PINELLAS_CITY'+\n"
			+ "'<option value=\"LD_FL_ORANGE_SECTION\">LD_FL_ORANGE_SECTION'+\n"
			+ "'<option value=\"LD_FL_ORANGE_TOWNSHIP\">LD_FL_ORANGE_TOWNSHIP'+\n"
			+ "'<option value=\"LD_FL_ORANGE_RANGE\">LD_FL_ORANGE_RANGE'+\n"
			+ "'<option value=\"LD_FL_ORANGE_SUBDIVISION\">LD_FL_ORANGE_SUBDIVISION'+\n"
			+ "'<option value=\"LD_FL_ORANGE_BLOCK\">LD_FL_ORANGE_BLOCK'+\n"
			+ "'<option value=\"LD_FL_ORANGE_LOT\">LD_FL_ORANGE_LOT'+\n"
			+ "'<option value=\"LD_FL_WAKULLA_TR_PID\">LD_FL_WAKULLA_TR_PID'+\n"
			+ "'<option value=\"LD_SUBDIV_RNG\">LD_SUBDIV_RNG'+\n"
			+ "'<option value=\"LD_SUBDIV_CODE\">LD_SUBDIV_CODE'+\n"
			+ "'<option value=\"LD_LOT_SUBDIVISION\">LD_LOT_SUBDIVISION'+\n"
			+ "'<option value=\"LD_PARCELNO\">LD_PARCELNO'+\n"
			+ "'<option value=\"LD_PARCELNO_PREFIX\">LD_PARCELNO_PREFIX'+\n"
			+ "'<option value=\"LD_PARCELNO_FULL\">LD_PARCELNO_FULL'+\n"
			+ "'<option value=\"LD_SUBDIV_UNIT\">LD_SUBDIV_UNIT'+\n"
			+ "'<option value=\"LD_PARCELNO_MAP\">LD_PARCELNO_MAP'+\n"
			+ "'<option value=\"LD_PARCELNO_GROUP\">LD_PARCELNO_GROUP'+\n"
			+ "'<option value=\"LD_PARCELNO_PARCEL\">LD_PARCELNO_PARCEL'+\n"
			+ "'<option value=\"LD_TN_WILLIAMSON_Ctl1\">LD_TN_WILLIAMSON_Ctl1'+\n"
			+ "'<option value=\"LD_TN_WILLIAMSON_Ctl2\">LD_TN_WILLIAMSON_Ctl2'+\n"
			+ "'<option value=\"LD_TN_WILLIAMSON_GROUP\">LD_TN_WILLIAMSON_GROUP'+\n"
			+ "'<option value=\"LD_TN_WILLIAMSON_PARCEL\">LD_TN_WILLIAMSON_PARCEL'+\n"
			+ "'<option value=\"LD_TN_WILLIAMSON_ID\">LD_TN_WILLIAMSON_ID'+\n"
			+ "'<option value=\"LD_TN_WILLIAMSON_SI\">LD_TN_WILLIAMSON_SI'+\n"
			+ "'<option value=\"LD_PARCELNO_CONDO\">LD_PARCELNO_CONDO'+\n"
			+ "'<option value=\"LD_PARCELNO_PARCEL_CONDO\">LD_PARCELNO_PARCEL_CONDO'+\n"
			+ "'<option value=\"LD_PARCELNO_CNT_DAVIDSON\">LD_PARCELNO_CNT_DAVIDSON'+\n"
			+ "'<option value=\"LD_PARCELNO_GENERIC_AO\">LD_PARCELNO_GENERIC_AO</option>'+\n"
			+ "'<option value=\"LD_PARCELNO_GENERIC_TR\">LD_PARCELNO_GENERIC_TR</option>'+\n"
			+ "'<option value=\"LD_PARCELNO_GENERIC_NDB\">LD_PARCELNO_GENERIC_NDB</option>'+\n"
			+ "'<option name=\"B_FULL_NAME\" >BUYER_FULL_NAME</option>'+\n"
			+ "'<option value=\"BUYER_FNAME\">BUYER_FNAME'+\n"
			+ "'<option value=\"BUYER_MNAME\">BUYER_MNAME'+\n"
			+ "'<option value=\"BUYER_LNAME\">BUYER_LNAME'+\n"
			+ "'<option value=\"P_STREET_FULL_NAME\">P_STREET_FULL_NAME'+\n"
			+ "'<option value=\"P_STREET_FULL_NAME_EX\">P_STREET_FULL_NAME_EX'+\n"
			+ "'<option value=\"P_STREETNAME_SUFFIX_UNIT_NO\">P_STREETNAME_SUFFIX_UNIT_NO'+\n"
			+ "'<option value=\"P_STREET_FULL_NAME_NO_SUFFIX\">P_STREET_FULL_NAME_NO_SUFFIX'+\n"
			+ "'<option value=\"P_STREET_NO_STAR_NA_STAR\">P_STREET_NO_STAR_NA_STAR'+\n"
			+ "'<option value=\"BM1_LENDERNAME\">BM1_LENDERNAME'+\n"
			+ "'<option value=\"BM1_LOADACCOUNTNO\">BM1_LOADACCOUNTNO'+\n"
			+ "'<option value=\"BM2_LENDERNAME\">BM2_LENDERNAME'+\n"
			+ "'<option value=\"BM2_LOADACCOUNTNO\">BM2_LOADACCOUNTNO'+\n"
			+ "'<option value=\"ADDITIONAL_INFORMATION\">ADDITIONAL_INFORMATION'+\n"
			+ "'<option value=\"ADDITIONAL_REQUIREMENTS\">ADDITIONAL_REQUIREMENTS'+\n"
			+ "'<option value=\"ADDITIONAL_EXCEPTIONS\">ADDITIONAL_EXCEPTIONS'+\n"
			+ "'<option value=\"LEGAL_DESCRIPTION\">LEGAL_DESCRIPTION'+\n"
			+ "'<option value=\"LEGAL_DESCRIPTION_STATUS\">LEGAL_DESCRIPTION_STATUS'+\n"
			+ "'<option value=\"CERTICICATION_DATE\">CERTICICATION_DATE'+\n"
			+ "'<option value=\"SEARCH_PRODUCT\">SEARCH_PRODUCT'+\n"
			+ "'<option value=\"PAYRATE_NEW_VALUE\">PAYRATE_NEW_VALUE'+\n"
			+ "'<option value=\"ASSESSED_VALUE\">ASSESSED_VALUE'+\n"
			+ "'<option value=\"QUARTER_ORDER\">QUARTER_ORDER'+\n"
			+ "'<option value=\"QUARTER_VALUE\">QUARTER_VALUE'+\n"
			+ "'<option value=\"ARB\">ARB'+\n"
			+ "'<option value=\"LD_AREA\">LD_AREA'+\n"
			+ "'<option value=\"LD_PI_AREA\">LD_PI_AREA'+\n"
			+ "'<option value=\"LD_PI_BLOCK\">LD_PI_BLOCK'+\n"
			+ "'<option value=\"LD_PI_PARCEL\">LD_PI_PARCEL'+\n"
			+ "'<option value=\"LD_PI_SEC\">LD_PI_SEC'+\n"
			+ "'<option value=\"LD_PI_UNIT\">LD_PI_UNIT'+\n"
			+ "'<option value=\"LD_PI_MAP_BOOK\">LD_PI_MAP_BOOK'+\n"
			+ "'<option value=\"LD_PI_MAP_PAGE\">LD_PI_MAP_PAGE'+\n"
			+ "'<option value=\"LD_PI_LOT\">LD_PI_LOT'+\n"
			+ "'<option value=\"LD_PI_TRACT\">LD_PI_TRACT'+\n"
			+ "'<option value=\"LD_PI_MAP_CODE\">LD_PI_MAP_CODE'+\n"
			+ "'<option value=\"LD_PI_MAJ_LEGAL_NAME\">LD_PI_MAJ_LEGAL_NAME'+\n"
			+ "'<option value=\"LD_IL_WILL_AO_BLOCK\">LD_IL_WILL_AO_BLOCK'+\n"
			+ "'<option value=\"LD_IL_WILL_AO_COMP_CODE\">LD_IL_WILL_AO_COMP_CODE'+\n"
			+ "'<option value=\"LD_IL_WILL_AO_LOT\">LD_IL_WILL_AO_LOT'+\n"
			+ "'<option value=\"LD_IL_WILL_AO_MISC\">LD_IL_WILL_AO_MISC'+\n"
			+ "'<option value=\"LD_IL_WILL_AO_SEC\">LD_IL_WILL_AO_SEC'+\n"
			+ "'<option value=\"LD_IL_WILL_AO_TWN\">LD_IL_WILL_AO_TWN'+\n"
			+ "'<option value=\"LD_PARCELNO_MAP_GENERIC_TR\">LD_PARCELNO_MAP_GENERIC_TR'+\n"
			+ "'<option value=\"LD_PARCELNO_CTRL_MAP_GENERIC_TR\">LD_PARCELNO_CTRL_MAP_GENERIC_TR'+\n"
			+ "'<option value=\"LD_PARCELNO_GROUP_GENERIC_TR\">LD_PARCELNO_GROUP_GENERIC_TR'+\n"
			+ "'<option value=\"LD_PARCELNO_PARCEL_GENERIC_TR\">LD_PARCELNO_PARCEL_GENERIC_TR'+\n"
			+ "'<option value=\"LD_MO_PLATTE_TWN\">LD_MO_PLATTE_TWN'+\n"
			+ "'<option value=\"LD_MO_PLATTE_AREA\">LD_MO_PLATTE_AREA'+\n"
			+ "'<option value=\"LD_MO_PLATTE_SECT\">LD_MO_PLATTE_SECT'+\n"
			+ "'<option value=\"LD_MO_PLATTE_QTRSECT\">LD_MO_PLATTE_QTRSECT'+\n"
			+ "'<option value=\"LD_MO_PLATTE_BLOCK\">LD_MO_PLATTE_BLOCK'+\n"
			+ "'<option value=\"LD_MO_PLATTE_PARCEL\">LD_MO_PLATTE_PARCEL"
			+ "</select>";
	
	private static String searchTypeBuffer = "<option value=\"\"></option>";
	static {
		for(SearchType searchType : SearchType.values()) {
			searchTypeBuffer += "<option value=\"" + searchType + "\">" + searchType + "</option>";
		}
	}

	public static String getOneParameterIterator() {
		return oneParameterIterator;
	}

	public static String getKeyBuffer() {
		StringBuilder result = new StringBuilder();

		for (String key : htmlFunctionSaKeys.keySet()) {
			String value = htmlFunctionSaKeys.get(key);
			if (StringUtils.isEmpty(value)) {
				result.append("<option value=\"" + key + "\">" + key
						+ "</option>");
			} else {
				result.append("<option value=\"" + key + "\">" + value
						+ "</option>");
			}
		}

		return result.toString();
	}

	public static String getIteratorBuffer() {
		return iteratorBuffer;
	}

	public static String getModuleBuffer() {
		return moduleBuffer;
	}

	public static String getParcelIDBuffer() {
		return parcelIDBuffer;
	}
	
	public static String getSearchTypeBuffer() {
		return searchTypeBuffer;
	}

	public static String getSetKeyBuffer() {
		return setKeyBuffer;
	}

	public static String getOneParameterIterator2() {
		return oneParameterIterator2;
	}

	public static String generateYearSelectOptions(int yearDepth,
			String selectName, int startYear, int defaultYear) {
		String optionPattern = "<option value=\"{0}\">{1}</option>";
		String optionDefaultPattern = "<option value=\"{0}\" selected=\"selected\">{1}</option>";
		String selectHtml = "<select name=\"" + selectName + "\">";
		StringBuilder html = new StringBuilder();
		html.append(selectHtml);
		for (int j = startYear; j >= startYear - yearDepth; j--) {
			html.append((j == defaultYear) ? MessageFormat.format(
					optionDefaultPattern, "" + j, "" + j) : MessageFormat
					.format(optionPattern, "" + j, "" + j));
		}
		html.append("</select>");
		return html.toString();
	}

	public static String generateYearSelectOptions(String selectName,
			int firstYear, int lastYear, int defaultYear) {
		String optionPattern = "<option value=\"{0}\">{1}</option>";
		String optionDefaultPattern = "<option value=\"{0}\" selected=\"selected\">{1}</option>";
		String selectHtml = "<select name=\"" + selectName + "\">";
		StringBuilder html = new StringBuilder();
		for (int j = firstYear; j <= lastYear; j++) {
			html.insert(
					0,
					(j == defaultYear) ? MessageFormat.format(
							optionDefaultPattern, "" + j, "" + j)
							: MessageFormat.format(optionPattern, "" + j, ""
									+ j));
		}
		html.insert(0, selectHtml);
		html.append("</select>");
		return html.toString();
	}

	public static String generateYearSelectOptions(String selectName,
			int firstYear, int lastYear, String defaultOption) {
		String generateYearSelectOptions = generateYearSelectOptions(
				selectName, firstYear, lastYear, -1);
				
		int indexOf = generateYearSelectOptions.indexOf(">");
		if(indexOf > 0) {
			generateYearSelectOptions = generateYearSelectOptions.substring(0, indexOf + 1) + defaultOption + generateYearSelectOptions.substring(indexOf + 1);
		}
//		String selectEndTag = "</select>";
//		generateYearSelectOptions = generateYearSelectOptions.replace(
//				"(<select[^>]+>)", defaultOption + selectEndTag);
		return generateYearSelectOptions;
	}

	public static String generateJSFunctionForClientHtmlSelect(
			String parentSiteParamName, String selectName, String selectName1,
			Map<String, List<Map<String, String>>> options,
			String functionName, boolean disableFirstChoice) {
		//select type of search list: name=o_value=  ,name=_value=Please select a search type,name=GRP_CIV_NAME_value=Civil Court Name Search,name=GRP_SXO_NAME_value=Sex Offender Name Search,name=GRP_CRI_NAME_value=Criminal Name Search,name=GRP_VOTER_NAME_value=Voter Name Search,name=GRP_DL_NAME_value=Driver's License Name Search,name=grp_pl_name_value=Professional License Name Search,name=grp_cad_name_value=Property Tax Name Search, name=GRP_SOS_NAME_value=Secretary of State Corporation Name Search
		
		Set<Entry<String, List<Map<String, String>>>> entrySet = options
				.entrySet();
		StringBuilder jsFunction = new StringBuilder("");
		String formName = "NameSearch";

		String optionFormat = "document." + formName + "." + selectName
				+ ".options[%s]=new Option(\"%s\",\"%s\");";

		String optionFormat1 = "document." + formName + "." + selectName1
				+ ".options[%s]=new Option(\"%s\",\"%s\");";

		if (StringUtils.isEmpty(functionName)) {
			functionName = "dropdownlist";
		}

		jsFunction = jsFunction.append("function " + functionName
				+ "(listindex){\n");
		// jsFunction = jsFunction.append("alert( listindex ); \n");

		if (functionName.equals("dropdownlist")) {
			jsFunction = jsFunction.append("seconddropdownlist(\"\");\n");
		}

		jsFunction = jsFunction.append("\ndocument." + formName + "."
				+ selectName + ".options.length = 0;");

		if (functionName.equals("seconddropdownlist")) {
			jsFunction.append("\nif(!listindex) {"
					+ String.format(optionFormat, "0", "", "")
					+ "\n return true;\n}\n");
		}

		jsFunction = jsFunction.append("\nswitch (listindex.toUpperCase())\n{");

		for (Entry<String, List<Map<String, String>>> upperSelectValues : entrySet) {
			jsFunction = jsFunction.append("\n");
			jsFunction = jsFunction.append("case "
					+ "\""
					+ org.apache.commons.lang.StringUtils.defaultIfEmpty(
							upperSelectValues.getKey().toUpperCase(), "")
					+ "\"  :\n");
			jsFunction = jsFunction.append("\n");

			List<Map<String, String>> optionList = upperSelectValues.getValue();
			int optionCounter = 0;
			for (Map<String, String> map : optionList) {
				String label = map.get("label");
				String value = map.get("value");
				String jsOption = "";
				// exceptii
				if (functionName.equals("dropdownlist")
						&& (value.equals("GRP_PL_NAME") || value
								.equalsIgnoreCase("GRP_CAD_NAME"))) {
					jsOption = String.format(optionFormat,
							"" + optionCounter++, "", "");
					jsFunction = jsFunction.append(jsOption);
					jsFunction = jsFunction.append("\n");
					jsOption = String.format(optionFormat1, "0", "", "");
					jsFunction = jsFunction.append(jsOption);
					jsFunction = jsFunction.append("\n");
				} else {
					jsOption = String.format(optionFormat,
							"" + optionCounter++, label, value);
					jsFunction = jsFunction.append(jsOption);
					jsFunction = jsFunction.append("\n");
				}
			}

			if (upperSelectValues.getKey().toUpperCase().equals("GRP_DL_NAME")
					&& functionName.equals("dropdownlist")) {
				optionCounter = 0;
				for (String[] s : XXGenericPublicDataParentSiteConfiguration.DPPA) {
					if (s.length == 2) {
						String jsOption = String.format(optionFormat1, ""
								+ optionCounter++, s[1], s[0]);
						jsFunction = jsFunction.append(jsOption);
						jsFunction = jsFunction.append("\n");
					}
				}
			}
			jsFunction = jsFunction.append("break;");
		}

		if (functionName.equals("seconddropdownlist")) {
			for (String[] op : XXGenericPublicDataParentSiteConfiguration.DRIVERS_LICENCE_NAME_SEARCH_OPTIONS) {
				int optionCounter = 0;
				jsFunction = jsFunction.append("\n");
				jsFunction = jsFunction.append("case "
						+ "\""
						+ org.apache.commons.lang.StringUtils.defaultIfEmpty(
								op[0].toUpperCase(), "") + "\"  :\n");
				jsFunction = jsFunction.append("\n");

				for (String[] s : XXGenericPublicDataParentSiteConfiguration.DPPA) {
					if (s.length == 2) {
						String jsOption = String.format(optionFormat, ""
								+ optionCounter++, s[1], s[0]);
						jsFunction = jsFunction.append(jsOption);
						jsFunction = jsFunction.append("\n");
					}
				}

				jsFunction = jsFunction.append("break;");
			}
		}

		if (functionName.equals("DMV_DPPA_list")) {
			for (String[] op : XXGenericPublicDataParentSiteConfiguration.DMV_PLATE_OPTIONS) {
				int optionCounter = 0;
				jsFunction = jsFunction.append("\n");
				jsFunction = jsFunction.append("case "
						+ "\""
						+ org.apache.commons.lang.StringUtils.defaultIfEmpty(
								op[0].toUpperCase(), "") + "\"  :\n");
				jsFunction = jsFunction.append("\n");

				for (String[] s : XXGenericPublicDataParentSiteConfiguration.DPPA) {
					if (s.length == 2) {
						String jsOption = String.format(optionFormat, ""
								+ optionCounter++, s[1], s[0]);
						jsFunction = jsFunction.append(jsOption);
						jsFunction = jsFunction.append("\n");
					}
				}

				jsFunction = jsFunction.append("break;");
			}

			for (String[] op : XXGenericPublicDataParentSiteConfiguration.DMV_VIN_OPTIONS) {
				int optionCounter = 0;
				jsFunction = jsFunction.append("\n");
				jsFunction = jsFunction.append("case "
						+ "\""
						+ org.apache.commons.lang.StringUtils.defaultIfEmpty(
								op[0].toUpperCase(), "") + "\"  :\n");
				jsFunction = jsFunction.append("\n");

				for (String[] s : XXGenericPublicDataParentSiteConfiguration.DPPA) {
					if (s.length == 2) {
						String jsOption = String.format(optionFormat, ""
								+ optionCounter++, s[1], s[0]);
						jsFunction = jsFunction.append(jsOption);
						jsFunction = jsFunction.append("\n");
					}
				}

				jsFunction = jsFunction.append("break;");
			}
		}

		jsFunction = jsFunction.append("} \n return true;\n}");

		return jsFunction.toString();
	}

	public static String generateJSFunctionForClientHtmlSelect(
			String parentSiteParamName, String selectName,
			Map<String, List<Map<String, String>>> options, String functionName) {
		return generateJSFunctionForClientHtmlSelect(parentSiteParamName,
				selectName, "", options, functionName, false);
	}
}
