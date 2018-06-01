package ro.cst.tsearch.bean;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;

import com.stewart.ats.base.name.NameI;

/**
 * @author Oprina George
 */

public class ParentSiteAttributes {

	private SearchAttributes	sa;
	private Search				search;

	public ParentSiteAttributes(SearchAttributes sa) {
		this.sa = sa;
	}

	public ParentSiteAttributes(Search search) {
		this.search = search;
	}

	private static String[]		NAME_KEYS		= { 
												SearchAttributes.OWNER_FML_NAME,
												SearchAttributes.OWNER_FNAME,
												SearchAttributes.OWNER_FULL_NAME,
												SearchAttributes.OWNER_LCF_NAME,
												SearchAttributes.OWNER_LCFM_NAME,
												SearchAttributes.OWNER_LNAME,
												SearchAttributes.OWNER_LF_NAME,
												SearchAttributes.OWNER_LFM_NAME,
												SearchAttributes.OWNER_LNAME,
												SearchAttributes.OWNER_MINITIAL,
												SearchAttributes.OWNER_MNAME };

	private static List<String>	NAME_KEYS_LIST	= Arrays.asList(NAME_KEYS);

	private String getKeyValue(TSServerInfoFunction f, String saKey, NameI name) {
		if (NAME_KEYS_LIST.contains(saKey))
			return getNameValue(f, saKey, name);
		return f.getDefaultValue();// sa.getAtribute(saKey);
	}

	public String getNameValue(TSServerInfoFunction f, String saKey, NameI name) {
		if (StringUtils.isEmpty(saKey) || sa == null)
			return "";

		String value = "";

		if (saKey.equals(SearchAttributes.OWNER_FML_NAME)) {
			value = name.getFirstName() + " " + name.getMiddleName() + " " + name.getLastName();
		} else if (saKey.equals(SearchAttributes.OWNER_FNAME)) {
			value = name.getFirstName();
		} else if (saKey.equals(SearchAttributes.OWNER_FULL_NAME)) {
			value = name.getLastName() + " " + name.getFirstName();
		} else if (saKey.equals(SearchAttributes.OWNER_LCF_NAME)) {
			value = (name.getLastName() + ", " + name.getFirstName()).trim();
		} else if (saKey.equals(SearchAttributes.OWNER_LCFM_NAME)) {
			value = name.getLastName() + ", " + name.getFirstName() + " " + name.getMiddleName();
		} else if (saKey.equals(SearchAttributes.OWNER_LNAME)) {
			value = name.getLastName();
		} else if (saKey.equals(SearchAttributes.OWNER_LF_NAME)) {
			value = name.getLastName() + " " + name.getFirstName();
		} else if (saKey.equals(SearchAttributes.OWNER_LFM_NAME)) {
			value = name.getLastName() + " " + name.getFirstName() + " " + name.getMiddleName();
		} else if (saKey.equals(SearchAttributes.OWNER_MINITIAL)) {
			value = name.getMiddleInitial();
		} else if (saKey.equals(SearchAttributes.OWNER_MNAME)) {
			value = name.getMiddleName();
		}

		value = value.trim();

		if (value.equals(","))
			value = "";

		value = value.replaceAll("\\s+", " ");

		return value;

	}

	public String getKeyValuePairsForModule(TSServerInfoModule module, NameI name) {

		HashMap<String, String> map = new HashMap<String, String>();

		// get all keys

		List<TSServerInfoFunction> functions = module.getFunctionList();

		for (TSServerInfoFunction f : functions) {
			String saKey = f.getSaKey();
			map.put(saKey, getKeyValue(f, saKey, name));
		}

		return makeStringFromMap(map);
	}

	private String makeStringFromMap(HashMap<String, String> map) {
		StringBuffer buf = new StringBuffer();

		for (Entry<String, String> e : map.entrySet()) {
			buf.append("[" + e.getKey() + "=" + e.getValue() + "]");
		}
		return buf.toString();
	}

	// ---START ATI

	// keys used to save search parameters for ATI
	public static final String	ATI_BP_LEVELS					= "ATI_BP_LEVELS";					// List<List<String>>
	public static final String	ATI_IN_LEVELS					= "ATI_IN_LEVELS";					// List<List<String>>

	public static final String	ATI_STR_CODES					= "ATI_STR_CODES";					// List<String>
	public static final String	ATI_GOVLOT_CODES				= "ATI_GOVLOT_CODES";				// List<String>

	public static final String	ATI_INSTRUMENT_NUMBER			= "ATI_INSTRUMENT_NUMBER";
	public static final String	ATI_INSTRUMENT_YEAR				= "ATI_INSTRUMENT_YEAR";
	public static final String	ATI_INSTRUMENT_SERIES_CODE		= "ATI_INSTRUMENT_SERIES_CODE";
	public static final String	ATI_INSTRUMENT_NUMBER_SUFFIX	= "ATI_INSTRUMENT_NUMBER_SUFFIX";
	public static final String	ATI_INSTRUMENT_NUMBER_FROMDATE	= "ATI_INSTRUMENT_NUMBER_FROMDATE";
	public static final String	ATI_INSTRUMENT_NUMBER_TODATE	= "ATI_INSTRUMENT_NUMBER_TODATE";
	public static final String	ATI_INSTRUMENT_NUMBER_SOURCE	= "ATI_INSTRUMENT_NUMBER_SOURCE";

	public static final String	ATI_BOOK						= "ATI_BOOK";
	public static final String	ATI_PAGE						= "ATI_PAGE";
	public static final String	ATI_BOOK_SUFFIX					= "ATI_BOOK_SUFFIX";
	public static final String	ATI_PAGE_SUFFIX					= "ATI_PAGE_SUFFIX";
	public static final String	ATI_BP_FROMDATE					= "ATI_BP_FROMDATE";
	public static final String	ATI_BP_TODATE					= "ATI_BP_TODATE";
	public static final String	ATI_BP_SOURCE					= "ATI_BP_SOURCE";

	public static final String	ATI_SECTION						= "ATI_SECTION";
	public static final String	ATI_TOWNSHIP					= "ATI_TOWNSHIP";
	public static final String	ATI_RANGE						= "ATI_RANGE";
	public static final String	ATI_TOWNSHIP_DIRECTION			= "ATI_TOWNSHIP_DIRECTION";
	public static final String	ATI_RANGE_DIRECTION				= "ATI_RANGE_DIRECTION";
	public static final String	ATI_STR_FROMDATE				= "ATI_STR_FROMDATE";
	public static final String	ATI_STR_TODATE					= "ATI_STR_TODATE";

	public static final String	ATI_GOVLOT_SECTION				= "ATI_GOVLOT_SECTION";
	public static final String	ATI_GOVLOT_TOWNSHIP				= "ATI_GOVLOT_TOWNSHIP";
	public static final String	ATI_GOVLOT_RANGE				= "ATI_GOVLOT_RANGE";
	public static final String	ATI_GOVLOT_TOWNSHIP_DIRECTION	= "ATI_GOVLOT_TOWNSHIP_DIRECTION";
	public static final String	ATI_GOVLOT_RANGE_DIRECTION		= "ATI_GOVLOT_RANGE_DIRECTION";
	public static final String	ATI_GOVLOT_FROMDATE				= "ATI_GOVLOT_FROMDATE";
	public static final String	ATI_GOVLOT_TODATE				= "ATI_GOVLOT_TODATE";

	public List<List<String>> getATIPlattedBPLevels() {
		try {
			@SuppressWarnings("unchecked")
			List<List<String>> levels = (List<List<String>>) search.getAdditionalInfo(ATI_BP_LEVELS);

			return levels;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// getters for parent site search parameters task 7773/7685

	public List<List<String>> getATIPlattedINLevels() {
		try {
			@SuppressWarnings("unchecked")
			List<List<String>> levels = (List<List<String>>) search.getAdditionalInfo(ATI_IN_LEVELS);

			return levels;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<String> getATIUnplattedCodes() {
		try {
			@SuppressWarnings("unchecked")
			List<String> codes = (List<String>) search.getAdditionalInfo(ATI_STR_CODES);

			return codes;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<String> getATIUnplattedGovLots() {
		try {
			@SuppressWarnings("unchecked")
			List<String> codes = (List<String>) search.getAdditionalInfo(ATI_GOVLOT_CODES);

			return codes;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getATIInstrumentNumber() {
		try {
			return StringUtils.defaultString("" + search.getAdditionalInfo(ATI_INSTRUMENT_NUMBER));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATIInstrumentNumberSuffix() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_INSTRUMENT_NUMBER_SUFFIX));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATIInstrumentYear() {
		try {
			return StringUtils.defaultString("" + search.getAdditionalInfo(ATI_INSTRUMENT_YEAR));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATIInstrumentSeriesCode() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_INSTRUMENT_SERIES_CODE));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATIInstrumentFromDate() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_INSTRUMENT_NUMBER_FROMDATE));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATIInstrumentToDate() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_INSTRUMENT_NUMBER_TODATE));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATIInstrumentSource() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_INSTRUMENT_NUMBER_SOURCE));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATIBook() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_BOOK));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATIPage() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_PAGE));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATIBookSuffix() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_BOOK_SUFFIX));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATIPageSuffix() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_PAGE_SUFFIX));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATIBPFromDate() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_BP_FROMDATE));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATIBPToDate() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_BP_TODATE));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATIBPSource() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_BP_SOURCE));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATISection() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_SECTION));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATITownship() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_TOWNSHIP));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATIRange() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_RANGE));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATITownshipDirection() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_TOWNSHIP_DIRECTION));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATIRangeDirection() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_RANGE_DIRECTION));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATISTRFromDate() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_STR_FROMDATE));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATISTRToDate() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_STR_TODATE));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATIGovLotSection() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_GOVLOT_SECTION));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATIGovLotTownship() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_GOVLOT_TOWNSHIP));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATIGovLotRange() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_GOVLOT_RANGE));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATIGovLotTownshipDirection() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_GOVLOT_TOWNSHIP_DIRECTION));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATIGovLotRangeDirection() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_GOVLOT_RANGE_DIRECTION));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATIGovLotFromDate() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_GOVLOT_FROMDATE));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getATIGovLotToDate() {
		try {
			return StringUtils.defaultString((String) search.getAdditionalInfo(ATI_GOVLOT_TODATE));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	// ----- END ATI

}
