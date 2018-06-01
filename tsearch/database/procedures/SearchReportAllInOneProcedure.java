package ro.cst.tsearch.database.procedures;

import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.StoredProcedure;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DBReports;
import ro.cst.tsearch.database.rowmapper.NameMapper;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.PriorFileAtsDocument;
import com.stewart.ats.base.document.PriorFileAtsDocumentI;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.property.PinI;
import com.stewart.ats.base.property.Property;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManager;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;

public class SearchReportAllInOneProcedure extends StoredProcedure {
	public static final String SP_NAME = "getSearchReportAllInOne";
	
	public static final String PARAM_COUNTY_ID = "countyId";
	public static final String PARAM_ABSTRACTOR_ID = "abstractorId";
	public static final String PARAM_AGENT_ID = "agentId";
	public static final String PARAM_STATE_ID = "stateId";
	public static final String PARAM_COMPANY_NAME = "compName";
	public static final String PARAM_ORDER_BY = "orderBy";
	public static final String PARAM_ORDER_TYPE = "orderType";
	public static final String PARAM_COMM_ID = "commId";
	public static final String PARAM_STATUS = "status";
	public static final String PARAM_INVOICE = "invoice";
	public static final String PARAM_SEARCH_TERM = "searchTerm";
	public static final String PARAM_FROM_DAY = "fromDay";
	public static final String PARAM_FROM_MONTH = "fromMonth";
	public static final String PARAM_FROM_YEAR = "fromYear";
	public static final String PARAM_TO_DAY = "toDay";
	public static final String PARAM_TO_MONTH = "toMonth";
	public static final String PARAM_TO_YEAR = "toYear";
	public static final String PARAM_SEARCH_FIELD = "searchField";
	public static final String PARAM_PAYRATE_TYPE = "isTSAdmin";
	public static final String PARAM_EXTRA_SEARCH_TERM_1 = "extraSearchTerm1";
	public static final String PARAM_EXTRA_SEARCH_TERM_2 = "extraSearchTerm2";
	public static final String PARAM_EXTRA_SEARCH_TERM_3 = "extraSearchTerm3";
	public static final String PARAM_EXTRA_SEARCH_TERM_4 = "extraSearchTerm4";
	public static final String PARAM_SEARCH_REPORT_TYPE = "searchReportType";
	public static final String PARAM_DATE_TYPE = "dateType";
	
	public static final String COLUMN_SEARCH_ID = "id";
	public static final String COLUMN_SDATE = "sdate";
	public static final String COLUMN_TSR_DATE = "tsr_date";
	public static final String COLUMN_FILE_ID = "file_id";
	
	public static final String COLUMN_ADDRESS_NO = "address_no";
	public static final String COLUMN_ADDRESS_DIR = "address_direction";
	public static final String COLUMN_ADDRESS_NAME = "address_name";
	public static final String COLUMN_ADDRESS_SUF = "address_suffix";
	public static final String COLUMN_ADDRESS_UNIT = "address_unit";
	public static final String COLUMN_CITY = "city";
	public static final String COLUMN_ZIP = "zip";
	public static final String COLUMN_ADDRESS_APN = "parcel_id";
	public static final String COLUMN_ADDRESS_LOT = "lotno";
	
	public static final String COLUMN_PLAT_BOOK = "platbook";
	public static final String COLUMN_PLAT_PAGE = "page";
	
	public static final String COLUMN_LEGAL_APN = "apn";
	public static final String COLUMN_SUBDIVISION_LOT = "lot";
	public static final String COLUMN_SUBDIVISION_NAME = "name";
	public static final String COLUMN_SUBDIVISION_BLOCK = "block";
	public static final String COLUMN_SUBDIVISION_PHASE = "phase";
	public static final String COLUMN_SUBDIVISION_UNIT = "unit";
	public static final String COLUMN_SUBDIVISION_TRACT = "tract";
	public static final String COLUMN_TOWHSHIP_S = "section";
	public static final String COLUMN_TOWHSHIP_T = "township";
	public static final String COLUMN_TOWHSHIP_R = "range";
	
	public static final String COLUMN_AGENT_ID = "agentId";
	public static final String COLUMN_PRODUCT_ID = "prodId";
	public static final String COLUMN_STARTER = "starter";
	
	
	
	public SearchReportAllInOneProcedure(JdbcTemplate jdbct) {
		super(jdbct, SP_NAME);

		declareParameter(new SqlParameter(PARAM_COUNTY_ID, Types.VARCHAR));
		declareParameter(new SqlParameter(PARAM_ABSTRACTOR_ID, Types.VARCHAR));
		declareParameter(new SqlParameter(PARAM_AGENT_ID, Types.VARCHAR));
		declareParameter(new SqlParameter(PARAM_STATE_ID, Types.VARCHAR));
		declareParameter(new SqlParameter(PARAM_COMPANY_NAME, Types.VARCHAR));
		declareParameter(new SqlParameter(PARAM_ORDER_BY, Types.VARCHAR));
		declareParameter(new SqlParameter(PARAM_ORDER_TYPE, Types.VARCHAR));
		declareParameter(new SqlParameter(PARAM_COMM_ID, Types.INTEGER));
		declareParameter(new SqlParameter(PARAM_STATUS, Types.VARCHAR));
		declareParameter(new SqlParameter(PARAM_INVOICE, Types.INTEGER));
		declareParameter(new SqlParameter(PARAM_SEARCH_TERM, Types.VARCHAR));
		declareParameter(new SqlParameter(PARAM_FROM_DAY, Types.INTEGER));
		declareParameter(new SqlParameter(PARAM_FROM_MONTH, Types.INTEGER));
		declareParameter(new SqlParameter(PARAM_FROM_YEAR, Types.INTEGER));
		declareParameter(new SqlParameter(PARAM_TO_DAY, Types.INTEGER));
		declareParameter(new SqlParameter(PARAM_TO_MONTH, Types.INTEGER));
		declareParameter(new SqlParameter(PARAM_TO_YEAR, Types.INTEGER));
		declareParameter(new SqlParameter(PARAM_SEARCH_FIELD, Types.VARCHAR));
		declareParameter(new SqlParameter(PARAM_PAYRATE_TYPE, Types.INTEGER));
		declareParameter(new SqlParameter(PARAM_EXTRA_SEARCH_TERM_1, Types.VARCHAR));
		declareParameter(new SqlParameter(PARAM_EXTRA_SEARCH_TERM_2, Types.VARCHAR));
		declareParameter(new SqlParameter(PARAM_EXTRA_SEARCH_TERM_3, Types.VARCHAR));
		declareParameter(new SqlParameter(PARAM_EXTRA_SEARCH_TERM_4, Types.VARCHAR));
		declareParameter(new SqlParameter(PARAM_SEARCH_REPORT_TYPE, Types.INTEGER));
		declareParameter(new SqlParameter(PARAM_DATE_TYPE, Types.INTEGER));
		
		compile();
		
	}
	
	/**
	 * Used to get a map of searches with similar address mapped as documents (with key SearchId)
	 * @param sourceSearch the source that will be used to load data from
	 * @param module if available will be used to load search data
	 * @return a list of searches mapped as PriorFileAtsDocumentI documents
	 */
	public Map<Long, PriorFileAtsDocumentI> getDocumentsByAddress(Search sourceSearch, TSServerInfoModule module) {
		
		String addressSearch = null;
		if(module != null) {
			addressSearch = module.getFunction(0).getParamValue().trim() 
					+ module.getFunction(1).getParamValue().replaceAll("[\\s-]+", "");
		} else {
			String isCondoStr = sourceSearch.getSa().getAtribute(SearchAttributes.IS_CONDO);
			boolean isCondo = StringUtils.isEmpty(isCondoStr)?false:Boolean.valueOf(isCondoStr);
			
			String unitStr = sourceSearch.getSa().getAtribute( SearchAttributes.P_STREETUNIT );
			isCondo = isCondo || (StringUtils.isEmpty(unitStr)?false:true);
			
			addressSearch = sourceSearch.getSa().getAtribute(SearchAttributes.P_STREET_NO_NAME_NO_SPACE);
			if(!isCondo){
				addressSearch = sourceSearch.getSa().getAtribute(SearchAttributes.P_STREETNAME).replaceAll("\\s+", "");
			}	
		}
		
		if(StringUtils.isEmpty(addressSearch)) {
			return new LinkedHashMap<Long, PriorFileAtsDocumentI>();
		}
		
		
		Map<String, Object> inParams = new HashMap<String, Object>();
		inParams.put(PARAM_COUNTY_ID, "," + sourceSearch.getCountyId() + ",");
		inParams.put(PARAM_ABSTRACTOR_ID, ",-1,");
		inParams.put(PARAM_AGENT_ID, ",-1,");
		inParams.put(PARAM_STATE_ID, "," + sourceSearch.getSa().getAtribute(SearchAttributes.P_STATE) + ",");
		inParams.put(PARAM_COMPANY_NAME, ",-1,");
		inParams.put(PARAM_ORDER_BY, "SDATE");
		inParams.put(PARAM_ORDER_TYPE, "DESC");
		inParams.put(PARAM_COMM_ID, sourceSearch.getCommId());
		inParams.put(PARAM_STATUS, ",-1,");
		inParams.put(PARAM_INVOICE, 0);
		inParams.put(PARAM_SEARCH_TERM, StringEscapeUtils.escapeSql(addressSearch.toUpperCase()));
		inParams.put(PARAM_FROM_DAY, 1);
		inParams.put(PARAM_FROM_MONTH, 1);
		inParams.put(PARAM_FROM_YEAR, 1950);
		inParams.put(PARAM_TO_DAY, 31);
		inParams.put(PARAM_TO_MONTH, 12);
		inParams.put(PARAM_TO_YEAR, 2050);
		inParams.put(PARAM_SEARCH_FIELD, "Property Address");
		inParams.put(PARAM_PAYRATE_TYPE, 0);
		inParams.put(PARAM_EXTRA_SEARCH_TERM_1, "");
		inParams.put(PARAM_EXTRA_SEARCH_TERM_2, "");
		inParams.put(PARAM_EXTRA_SEARCH_TERM_3, "");
		inParams.put(PARAM_EXTRA_SEARCH_TERM_4, "");
		inParams.put(PARAM_SEARCH_REPORT_TYPE, 5);
		inParams.put(PARAM_DATE_TYPE, 1);
		
		return getDocuments(sourceSearch, inParams);
	}
	
	/**
	 * Used to get a map of searches with similar legal mapped as documents (with key SearchId)
	 * @param sourceSearch the source that will be used to load data from
	 * @param module if available will be used to load search data
	 * @return a list of searches mapped as PriorFileAtsDocumentI documents
	 */
	public Map<Long, PriorFileAtsDocumentI> getDocumentsByLegal(Search sourceSearch, TSServerInfoModule module) {
		
		String searchTerm = module.getFunction(0).getParamValue().replaceAll("[\\s-]+", "");
		
		if(StringUtils.isEmpty(searchTerm)) {
			return new LinkedHashMap<Long, PriorFileAtsDocumentI>();
		}
		
		
		Map<String, Object> inParams = new HashMap<String, Object>();
		inParams.put(PARAM_COUNTY_ID, "," + sourceSearch.getCountyId() + ",");
		inParams.put(PARAM_ABSTRACTOR_ID, ",-1,");
		inParams.put(PARAM_AGENT_ID, ",-1,");
		inParams.put(PARAM_STATE_ID, "," + sourceSearch.getSa().getAtribute(SearchAttributes.P_STATE) + ",");
		inParams.put(PARAM_COMPANY_NAME, ",-1,");
		inParams.put(PARAM_ORDER_BY, "SDATE");
		inParams.put(PARAM_ORDER_TYPE, "DESC");
		inParams.put(PARAM_COMM_ID, sourceSearch.getCommId());
		inParams.put(PARAM_STATUS, ",-1,");
		inParams.put(PARAM_INVOICE, 0);
		inParams.put(PARAM_SEARCH_TERM, StringEscapeUtils.escapeSql(searchTerm.toUpperCase()));
		inParams.put(PARAM_FROM_DAY, 1);
		inParams.put(PARAM_FROM_MONTH, 1);
		inParams.put(PARAM_FROM_YEAR, 1950);
		inParams.put(PARAM_TO_DAY, 31);
		inParams.put(PARAM_TO_MONTH, 12);
		inParams.put(PARAM_TO_YEAR, 2050);
		inParams.put(PARAM_SEARCH_FIELD, "Legal Description");
		inParams.put(PARAM_PAYRATE_TYPE, 0);
		inParams.put(PARAM_EXTRA_SEARCH_TERM_1, module.getFunction(1).getParamValue().replaceAll("[\\s-]+", ""));
		inParams.put(PARAM_EXTRA_SEARCH_TERM_2, module.getFunction(2).getParamValue().replaceAll("[\\s-]+", ""));
		inParams.put(PARAM_EXTRA_SEARCH_TERM_3, module.getFunction(3).getParamValue().replaceAll("[\\s-]+", ""));
		inParams.put(PARAM_EXTRA_SEARCH_TERM_4, module.getFunction(4).getParamValue().replaceAll("[\\s-]+", ""));
		inParams.put(PARAM_SEARCH_REPORT_TYPE, 5);
		inParams.put(PARAM_DATE_TYPE, 1);
		
		return getDocuments(sourceSearch, inParams);
	}
	
	/**
	 * Used to get a map of searches with similar parcel mapped as documents (with key SearchId)
	 * @param sourceSearch the source that will be used to load data from
	 * @param module if available will be used to load search data
	 * @return a list of searches mapped as PriorFileAtsDocumentI documents
	 */
	public Map<Long, PriorFileAtsDocumentI> getDocumentsByParcel(Search sourceSearch, TSServerInfoModule module) {
		
		String searchTerm = module.getFunction(0).getParamValue().replaceAll("[\\s-]+", "");
		
		if(StringUtils.isEmpty(searchTerm)) {
			return new LinkedHashMap<Long, PriorFileAtsDocumentI>();
		}
		
		
		Map<String, Object> inParams = new HashMap<String, Object>();
		inParams.put(PARAM_COUNTY_ID, "," + sourceSearch.getCountyId() + ",");
		inParams.put(PARAM_ABSTRACTOR_ID, ",-1,");
		inParams.put(PARAM_AGENT_ID, ",-1,");
		inParams.put(PARAM_STATE_ID, "," + sourceSearch.getSa().getAtribute(SearchAttributes.P_STATE) + ",");
		inParams.put(PARAM_COMPANY_NAME, ",-1,");
		inParams.put(PARAM_ORDER_BY, "SDATE");
		inParams.put(PARAM_ORDER_TYPE, "DESC");
		inParams.put(PARAM_COMM_ID, sourceSearch.getCommId());
		inParams.put(PARAM_STATUS, ",-1,");
		inParams.put(PARAM_INVOICE, 0);
		inParams.put(PARAM_SEARCH_TERM, StringEscapeUtils.escapeSql(searchTerm.toUpperCase()));
		inParams.put(PARAM_FROM_DAY, 1);
		inParams.put(PARAM_FROM_MONTH, 1);
		inParams.put(PARAM_FROM_YEAR, 1950);
		inParams.put(PARAM_TO_DAY, 31);
		inParams.put(PARAM_TO_MONTH, 12);
		inParams.put(PARAM_TO_YEAR, 2050);
		inParams.put(PARAM_SEARCH_FIELD, "APN");
		inParams.put(PARAM_PAYRATE_TYPE, 0);
		inParams.put(PARAM_EXTRA_SEARCH_TERM_1, "");
		inParams.put(PARAM_EXTRA_SEARCH_TERM_2, "");
		inParams.put(PARAM_EXTRA_SEARCH_TERM_3, "");
		inParams.put(PARAM_EXTRA_SEARCH_TERM_4, "");
		inParams.put(PARAM_SEARCH_REPORT_TYPE, 5);
		inParams.put(PARAM_DATE_TYPE, 1);
		
		return getDocuments(sourceSearch, inParams);
	}
	
	/**
	 * Used to get a map of searches with similar grantor names mapped as documents (with key SearchId)
	 * @param sourceSearch the source that will be used to load data from
	 * @param module if available will be used to load search data
	 * @return a list of searches mapped as PriorFileAtsDocumentI documents
	 */
	public Map<Long, PriorFileAtsDocumentI> getDocumentsByName(Search sourceSearch, TSServerInfoModule module) {
		
		String searchTerm = module.getFunction(0).getParamValue().replaceAll("[\\s-]+", "") + 
				module.getFunction(1).getParamValue().replaceAll("[\\s-]+", "");;
		
		if(StringUtils.isEmpty(searchTerm)) {
			return new LinkedHashMap<Long, PriorFileAtsDocumentI>();
		}
		
		
		Map<String, Object> inParams = new HashMap<String, Object>();
		inParams.put(PARAM_COUNTY_ID, "," + sourceSearch.getCountyId() + ",");
		inParams.put(PARAM_ABSTRACTOR_ID, ",-1,");
		inParams.put(PARAM_AGENT_ID, ",-1,");
		inParams.put(PARAM_STATE_ID, "," + sourceSearch.getSa().getAtribute(SearchAttributes.P_STATE) + ",");
		inParams.put(PARAM_COMPANY_NAME, ",-1,");
		inParams.put(PARAM_ORDER_BY, "SDATE");
		inParams.put(PARAM_ORDER_TYPE, "DESC");
		inParams.put(PARAM_COMM_ID, sourceSearch.getCommId());
		inParams.put(PARAM_STATUS, ",-1,");
		inParams.put(PARAM_INVOICE, 0);
		inParams.put(PARAM_SEARCH_TERM, StringEscapeUtils.escapeSql(searchTerm.toUpperCase()));
		inParams.put(PARAM_FROM_DAY, 1);
		inParams.put(PARAM_FROM_MONTH, 1);
		inParams.put(PARAM_FROM_YEAR, 1950);
		inParams.put(PARAM_TO_DAY, 31);
		inParams.put(PARAM_TO_MONTH, 12);
		inParams.put(PARAM_TO_YEAR, 2050);
		inParams.put(PARAM_SEARCH_FIELD, "Property Owners");
		inParams.put(PARAM_PAYRATE_TYPE, 0);
		inParams.put(PARAM_EXTRA_SEARCH_TERM_1, "");
		inParams.put(PARAM_EXTRA_SEARCH_TERM_2, "");
		inParams.put(PARAM_EXTRA_SEARCH_TERM_3, "");
		inParams.put(PARAM_EXTRA_SEARCH_TERM_4, "");
		inParams.put(PARAM_SEARCH_REPORT_TYPE, 5);
		inParams.put(PARAM_DATE_TYPE, 1);
		
		return getDocuments(sourceSearch, inParams);
	}

	public Map<Long, PriorFileAtsDocumentI> getDocuments(Search sourceSearch,
			Map<String, Object> inParams) {
		Map<Long, PriorFileAtsDocumentI> results = new LinkedHashMap<Long, PriorFileAtsDocumentI>();
		Map<?, ?> mapResult;
		try {
			mapResult = super.execute(inParams);
		} catch (DataAccessException e) {
			e.printStackTrace();
			DBManager.getLogger().error("Error while executing SearchReportAllInOneProcedure", e);
			return null;
		}
		if(mapResult.containsKey("#result-set-1")) {
			
			StringBuilder sb = new StringBuilder();
			
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> rowList = (List<Map<String, Object>>)mapResult.get("#result-set-1");
			for (Map<String, Object> row : rowList) {
				PriorFileAtsDocumentI doc = loadDocument(sourceSearch, row);
				if(doc != null && Long.parseLong(doc.getDocno()) != sourceSearch.getID()) {
					results.put(Long.parseLong(doc.getDocno()), doc);
					if(sb.length() == 0) {
						sb.append(doc.getDocno());
					} else {
						sb.append(",").append(doc.getDocno());
					}
				}
			}
			
			if(sb.length() > 0) {
				List<NameMapper> names = DBReports.getNamesForSearchIdsFromTable(
						sb.toString(), DBConstants.TABLE_PROPERTY_OWNER);
				for (NameMapper name : names) {
					PriorFileAtsDocumentI doc = results.get(name.getSearchId());
					if (doc != null) {
						doc.getGrantor().add(name.getName());
					}
				}
				names = DBReports.getNamesForSearchIdsFromTable(
						sb.toString(), DBConstants.TABLE_PROPERTY_BUYER);
				for (NameMapper name : names) {
					PriorFileAtsDocumentI doc = results.get(name.getSearchId());
					if (doc != null) {
						doc.getGrantee().add(name.getName());
					}
				}
			}
		}
		
		return results;
	}
	
	

	private PriorFileAtsDocumentI loadDocument(Search sourceSearch, Map<String, Object> row) {
		InstrumentI instr = new Instrument();
		instr.setInstno(row.get(COLUMN_FILE_ID).toString());
		instr.setDocno(row.get(COLUMN_SEARCH_ID).toString());
		PriorFileAtsDocumentI doc = new PriorFileAtsDocument(
				new RegisterDocument( DocumentsManager.generateDocumentUniqueId(sourceSearch.getID(), instr) ));
		doc.setInstrument(instr);
		
		String serverDocType = "Starter";
		String docCateg = DocumentTypes.getDocumentCategory(serverDocType, sourceSearch.getID()); 
    	instr.setDocType(docCateg);
    	String stype = DocumentTypes.getDocumentSubcategory(serverDocType, sourceSearch.getID());
    	if("MISCELLANEOUS".equals(stype)&&!"MISCELLANEOUS".equals(docCateg)){
    		stype = docCateg;
    	}
    	instr.setDocSubType(stype);
		
    	doc.setServerDocType(serverDocType);
    	doc.setType(SimpleChapterUtils.DType.ROLIKE);
    	
		doc.setRecordedDate(new Date(((Date)row.get(COLUMN_SDATE)).getTime()));
		if(row.get(COLUMN_TSR_DATE) != null) {
			doc.setInstrumentDate(new Date(((Date)row.get(COLUMN_TSR_DATE)).getTime()));
		}
		
		PropertyI property = Property.createEmptyProperty();
		doc.addProperty(property);
		
		property.getAddress().setNumber((String)row.get(COLUMN_ADDRESS_NO));
		property.getAddress().setPreDiretion((String)row.get(COLUMN_ADDRESS_DIR));
		property.getAddress().setStreetName((String)row.get(COLUMN_ADDRESS_NAME));
		property.getAddress().setSuffix((String)row.get(COLUMN_ADDRESS_SUF));
		property.getAddress().setIdentifierNumber((String)row.get(COLUMN_ADDRESS_UNIT));
		property.getAddress().setCity((String)row.get(COLUMN_CITY));
		property.getAddress().setZip((String)row.get(COLUMN_ZIP));
		
		
		PinI pin = property.getPin();
		String addressTablePin = (String)row.get(COLUMN_ADDRESS_APN);
		String legalTablePin = (String)row.get(COLUMN_LEGAL_APN);
		if(StringUtils.isNotEmpty(legalTablePin)) {
			pin.addPin(PinI.PinType.PID, legalTablePin);
			if(StringUtils.isNotEmpty(addressTablePin) && !addressTablePin.equals(legalTablePin)) {
				pin.addPin(PinI.PinType.PID_ALT1, addressTablePin);
			}
		} else if(StringUtils.isNotEmpty(addressTablePin)) {
			pin.addPin(PinI.PinType.PID, addressTablePin);
		}
		
		LegalI legal = property.getLegal();
		SubdivisionI subdivision = legal.getSubdivision();
		
		String lot = (String)row.get(COLUMN_SUBDIVISION_LOT);
		if(StringUtils.isEmpty(lot)) {
			lot = (String)row.get(COLUMN_ADDRESS_LOT);
		}
		if(StringUtils.isNotEmpty(lot)) {
			subdivision.setLot(lot);
		}
		
		subdivision.setPlatBook((String)row.get(COLUMN_PLAT_BOOK));
		subdivision.setPlatPage((String)row.get(COLUMN_PLAT_PAGE));
		
		subdivision.setName((String)row.get(COLUMN_SUBDIVISION_NAME));
		subdivision.setBlock((String)row.get(COLUMN_SUBDIVISION_BLOCK));
		subdivision.setPhase((String)row.get(COLUMN_SUBDIVISION_PHASE));
		subdivision.setUnit((String)row.get(COLUMN_SUBDIVISION_UNIT));
		subdivision.setTract((String)row.get(COLUMN_SUBDIVISION_TRACT));
		
		TownShipI township = legal.getTownShip();
		township.setSection((String)row.get(COLUMN_TOWHSHIP_S));
		township.setTownship((String)row.get(COLUMN_TOWHSHIP_T));
		township.setRange((String)row.get(COLUMN_TOWHSHIP_R));
		
		
		doc.setFileId(row.get(COLUMN_FILE_ID).toString());
		doc.setAgentId((Long)row.get(COLUMN_AGENT_ID));
		doc.setProductId(((Long)row.get(COLUMN_PRODUCT_ID)).intValue());
		return doc;
	}

}
