package ro.cst.tsearch.connection.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DBReports;
import ro.cst.tsearch.database.procedures.SearchReportAllInOneProcedure;
import ro.cst.tsearch.database.rowmapper.NameMapper;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.tags.StatusSelect;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.PriorFileAtsDocument;
import com.stewart.ats.base.document.PriorFileAtsDocumentI;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.Subdivision;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.property.PinI;
import com.stewart.ats.base.property.Property;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManager;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;

public class GenericATSConn {
	
	private static final String START_SQL_GENERIC;
	
	public static class SqlParams {
		public String sql;
		public Object[] params;
		public SearchType searchType;
	}
	
	static {
		StringBuilder sb = new StringBuilder();
		
		sb.append("SELECT DISTINCT a.id, ")
			.append("b.address_no, b.address_direction, b.address_name, ")
			.append("b.address_suffix, b.address_unit, b.city, b.zip, b.parcel_id, b.platbook, b.page, b.lotno, ")
			.append("ts_legal.apn, ")
			.append("j.name, j.lot, j.block, j.phase, j.unit, j.tract, ")
			.append("ts_township.range, ts_township.township, ts_township.section, ")
			.append("a.sdate, a.tsr_date, ")
			.append("a.ABSTR_FILENO file_no, a.file_id, ")
			.append("af.TSR_CREATED, a.STATUS, h.STATUS_SHORT_NAME, af.CHECKED_BY, ")
			.append("af.WAS_OPENED , ")
			.append("IFNULL(a.due_date,NOW()) due_date, ")
			.append("a.SEARCH_TYPE prodId, ")
			.append("a.AGENT_ID agentId, ")
			.append("af.forReview, ")
			.append("af.starter, b.isBootstrapped, af.isClosed, af.sourceCreationType , ")
			.append("tus.tsr_name_format agent_tsr_name_format, ")
			.append("tus.tsr_upper_lower agent_tsr_upper_lower, ")
			.append("a.sec_abstract_id ")
		
			.append("FROM ts_search a JOIN ts_property b ON a.property_id = b.ID ")
			.append("JOIN ts_search_flags af ON a.ID = af.search_id ")
			.append("LEFT JOIN ts_user c ON a.ABSTRACT_ID = c.USER_ID ")
			.append("LEFT JOIN ts_user sc ON a.sec_ABSTRACT_ID = sc.USER_ID ")
			.append("LEFT JOIN ts_user e ON a.AGENT_ID = e.USER_ID ")
			.append("LEFT JOIN ts_user_settings tus ON a.AGENT_ID = tus.USER_ID ")
			.append("LEFT JOIN ts_county f ON b.COUNTY_ID = f.ID ")
			.append("LEFT JOIN ts_state g ON b.STATE_ID = g.ID ")
			.append("JOIN ts_search_status h ON h.STATUS_ID = a.STATUS ")
			.append("LEFT JOIN property_owner d ON a.id = d.searchId ")
			.append("JOIN ts_legal ON a.legal_id = ts_legal.legalId ")
 			.append("JOIN ts_subdivision j ON ts_legal.subdivisionId = j.subdivisionID ")
			.append("JOIN ts_township ON ts_legal.townshipId = ts_township.townshipId ")
			.append("JOIN ts_community cm ON a.comm_id = cm.comm_id ")
			.append("JOIN ts_category cat ON cm.categ_id = cat.categ_id ");
		
		START_SQL_GENERIC = sb.toString();
	}
	
	private Search sourceSearch;
	
	
	public GenericATSConn(Search sourceSearch) {
		this.sourceSearch = sourceSearch;
	}

	private SqlParams getParcelSqlParams(TSServerInfoModule module) {
		
		String searchTerm = module.getFunction(0).getParamValue().replaceAll("[\\s-]+", "");
		if(StringUtils.isEmpty(searchTerm)) {
			return null;
		}
		
		List<Object> params = new ArrayList<Object>();
		StringBuilder sql = new StringBuilder(START_SQL_GENERIC);
		
		sql.append("WHERE UPPER(REPLACE(REPLACE(b.parcel_id, ' ', ''), '-','')) LIKE ? ");
		params.add(StringEscapeUtils.escapeSql(searchTerm.toUpperCase()));
		
		String status = module.getFunction(1).getParamValue().replaceAll("[\\s-]+", "");
		Set<String> products = getProducts(module);
		SqlParams completeSqlParams = completeSqlParams(status, params, products, sql);
		completeSqlParams.searchType = SearchType.PN;
		return completeSqlParams;
	}
	
	private SqlParams getAddressSqlParams(TSServerInfoModule module) {
		
		String streetNo = module.getFunction(0).getParamValue().trim().replaceAll("\\b0+\\b", "");
		String streetName = module.getFunction(1).getParamValue().replaceAll("[\\s-]+", "").toUpperCase();
		
		if(StringUtils.isEmpty(streetNo + streetName)) {
			return null;
		}
		
		List<Object> params = new ArrayList<Object>();
		StringBuilder sql = new StringBuilder(START_SQL_GENERIC);
		
		
		if(StringUtils.isNotEmpty(streetNo)) {
			sql.append("WHERE b.ADDRESS_NO  = ? ");
			params.add(StringEscapeUtils.escapeSql(streetNo));
			
			if(StringUtils.isNotEmpty(streetName)) {
				sql.append("AND UPPER(REPLACE(REPLACE(b.ADDRESS_NAME, ' ', ''), '-','')) LIKE ? ");
				params.add(StringEscapeUtils.escapeSql(streetName + "%"));
			}
		} else {
			sql.append("WHERE UPPER(REPLACE(REPLACE(b.ADDRESS_NAME, ' ', ''), '-','')) LIKE ? ");
			params.add(StringEscapeUtils.escapeSql(streetName + "%"));
		}
		
		String streetUnit = module.getFunction(2).getParamValue().trim();
		streetUnit = streetUnit.replaceAll("[\\s#'-]", "").replaceAll("LOT", "");
		if(StringUtils.isNotEmpty(streetUnit)) {
			sql.append("AND REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(b.ADDRESS_UNIT, ' ', ''), '-',''), '''',''), 'LOT',''), '#','') LIKE ? ");
			params.add(StringEscapeUtils.escapeSql(streetUnit));
		}
		
		String status = module.getFunction(3).getParamValue().replaceAll("[\\s-]+", "");
		Set<String> products = getProducts(module);
		SqlParams completeSqlParams = completeSqlParams(status, params, products, sql);
		completeSqlParams.searchType = SearchType.AD;
		return completeSqlParams;
		
	}
	
	private SqlParams getLegalSqlParams(TSServerInfoModule module) {
		
		String subdivisionName = module.getFunction(0).getParamValue().replaceAll("[\\s-]+", "");
		String platBook = module.getFunction(5).getParamValue().replaceAll("[\\s-]+", "");
		String platPage = module.getFunction(6).getParamValue().replaceAll("[\\s-]+", "");
		
		if(StringUtils.isEmpty(subdivisionName) && StringUtils.isEmpty(platPage) && StringUtils.isEmpty(platBook)) {
			return null;
		}
		
		List<Object> params = new ArrayList<Object>();
		StringBuilder sql = new StringBuilder(START_SQL_GENERIC);
		
		String lot = module.getFunction(1).getParamValue().trim();
		if(StringUtils.isNotEmpty(lot)) {
			sql.append(" JOIN ts_legal_lot l ON a.id = l.searchId ");
		}
		
		boolean printWhere = true;
		
		if(StringUtils.isNotEmpty(subdivisionName)) {
			printWhere = false;
			sql.append("WHERE UPPER(REPLACE(REPLACE(j.name, ' ', ''), '-','')) LIKE ? ");
			params.add(StringEscapeUtils.escapeSql(subdivisionName) + "%");
		}
		
		if(StringUtils.isNotEmpty(platBook)) {
			if(printWhere) {
				printWhere = false;
				sql.append("WHERE UPPER(REPLACE(REPLACE(REPLACE(b.platbook, ' ', ''), '-',''), '''','')) LIKE ? ");
			} else {
				sql.append("AND UPPER(REPLACE(REPLACE(REPLACE(b.platbook, ' ', ''), '-',''), '''','')) LIKE ? ");
			}
			params.add(StringEscapeUtils.escapeSql(platBook));
		}
		
		if(StringUtils.isNotEmpty(platPage)) {
			if(printWhere) {
				printWhere = false;
				sql.append("WHERE UPPER(REPLACE(REPLACE(REPLACE(b.page, ' ', ''), '-',''), '''','')) LIKE ? ");
			} else {
				sql.append("AND UPPER(REPLACE(REPLACE(REPLACE(b.page, ' ', ''), '-',''), '''','')) LIKE ? ");
			}
			params.add(StringEscapeUtils.escapeSql(platPage));
		}
		
		
		if(StringUtils.isNotEmpty(lot)) {
			Subdivision subdivision = new Subdivision();
			subdivision.setLot(lot);
			sql.append(" AND l.lotValue in (").append(StringEscapeUtils.escapeSql(subdivision.getLotExpandedAsString(","))).append(") ");
		}
		
		String block = module.getFunction(2).getParamValue().trim();
		if(StringUtils.isNotEmpty(block)) {
			sql.append(" AND j.block = ? ");
			params.add(StringEscapeUtils.escapeSql(block));
		}
		
		String phase = module.getFunction(3).getParamValue().trim();
		if(StringUtils.isNotEmpty(phase)) {
			sql.append(" AND j.phase = ? ");
			params.add(StringEscapeUtils.escapeSql(phase));
		}
		
		String section = module.getFunction(4).getParamValue().trim();
		if(StringUtils.isNotEmpty(section)) {
			sql.append(" AND ts_township.section = ? ");
			params.add(StringEscapeUtils.escapeSql(section));
		}
		
		String status = module.getFunction(7).getParamValue().replaceAll("[\\s-]+", "");
		Set<String> products = getProducts(module);
		SqlParams completeSqlParams = completeSqlParams(status, params, products, sql);
		
		completeSqlParams.searchType = SearchType.LD;
		return completeSqlParams;
	}
	
	private SqlParams getNameSqlParams(TSServerInfoModule module) {
		String firstName = module.getFunction(0).getParamValue().replaceAll("[\\s-]+", "").toUpperCase();
		String lastName = module.getFunction(1).getParamValue().replaceAll("[\\s-]+", "").toUpperCase();
		if(StringUtils.isEmpty(lastName)) {
			return null;
		}
		
		List<Object> params = new ArrayList<Object>();
		StringBuilder sql = new StringBuilder(START_SQL_GENERIC);
		
		sql.append("WHERE UPPER(REPLACE(REPLACE(d.lastName, ' ', ''), '-','')) LIKE ? ");
		params.add(StringEscapeUtils.escapeSql(lastName));
		
		if(StringUtils.isNotEmpty(firstName)) {
			sql.append("AND UPPER(REPLACE(REPLACE(d.firstName, ' ', ''), '-','')) LIKE ? ");
			params.add(StringEscapeUtils.escapeSql(firstName));
		}
		
		String status = module.getFunction(2).getParamValue().replaceAll("[\\s-]+", "");
		Set<String> products = getProducts(module);
		SqlParams completeSqlParams = completeSqlParams(status, params, products, sql);
		completeSqlParams.searchType = SearchType.GT;
		return completeSqlParams;
	}
	
	private SqlParams getFileNoSqlParams(TSServerInfoModule module) {
		String fileNo = module.getFunction(0).getParamValue().replaceAll("[\\s-]+", "").toUpperCase();
		if(StringUtils.isEmpty(fileNo)) {
			return null;
		}
		
		List<Object> params = new ArrayList<Object>();
		StringBuilder sql = new StringBuilder(START_SQL_GENERIC);
		
		sql.append("WHERE UPPER(REPLACE(REPLACE(a.file_id, ' ', ''), '-',''))  LIKE ? ");
		params.add(StringEscapeUtils.escapeSql(fileNo));
		
		String status = module.getFunction(1).getParamValue().replaceAll("[\\s-]+", "");
		Set<String> products = getProducts(module);
		SqlParams completeSqlParams = completeSqlParams(status, params, products, sql);
		completeSqlParams.searchType = SearchType.GT;
		return completeSqlParams;
	}
	
	private SqlParams getSearchIdSqlParams(TSServerInfoModule module) {
		String searchId = module.getFunction(0).getParamValue().replaceAll("[\\s-]+", "").toUpperCase();
		if(StringUtils.isEmpty(searchId)) {
			return null;
		}
		
		List<Object> params = new ArrayList<Object>();
		StringBuilder sql = new StringBuilder(START_SQL_GENERIC);
		
		sql.append("WHERE CONCAT(a.ID, '') like ? ");
		params.add(StringEscapeUtils.escapeSql(searchId));
		
		String status = module.getFunction(1).getParamValue().replaceAll("[\\s-]+", "");
		
		Set<String> products = getProducts(module);
		
		SqlParams completeSqlParams = completeSqlParams(status, params, products, sql);
		completeSqlParams.searchType = SearchType.GT;
		return completeSqlParams;
	}

	public Set<String> getProducts(TSServerInfoModule module) {
		Set<String> products = new HashSet<String>();
		for (TSServerInfoFunction function : module.getFunctionList()) {
			if("product".equals(function.getParamName())) {
				if(!function.getParamValue().isEmpty()) {
					if("0".equals(function.getParamValue())) {
						products.clear();
						break;
					} else {
						products.add(function.getParamValue());
					}
				}
			}
		}
		return products;
	}
	
	private SqlParams completeSqlParams(String status, List<Object> params, Set<String> products, StringBuilder sql) {
		sql.append(" AND cat.categ_id in (select categ_id from ts_community where comm_id = ?) ");
		params.add(sourceSearch.getCommId());
		
		sql.append(" AND b.COUNTY_ID = ? ");
		params.add(sourceSearch.getCountyId());
		
		
		
		if(StringUtils.isNotEmpty(status)) {
			String[] split = status.split("[\\s,]+");
			
			StringBuilder statusSB = new StringBuilder();
			
			for (String string : split) {
				if(Integer.toString(StatusSelect.STATUS_STARTER).endsWith(string)) {
					if(statusSB.length() != 0) {
						statusSB.append(" OR af.starter ");
					} else {
						statusSB.append(" af.starter ");
					}
				} else if(Integer.toString(StatusSelect.STATUS_NOT_STARTER).endsWith(string)) {
					if(statusSB.length() != 0) {
						statusSB.append(" OR !af.starter ");
					} else {
						statusSB.append(" !af.starter ");
					}
				} else if(Integer.toString(StatusSelect.STATUS_D_AND_NOT_T).endsWith(string)) {
					if(statusSB.length() != 0) {
						statusSB.append(" OR ( !af.starter AND a.status <> 1 AND (( a.STATUS = -1 OR a.STATUS = 1 OR a.status=2 ) AND NOT (a.STATUS = 1 AND NOT (af.WAS_OPENED ))) ) ");
					} else {
						statusSB.append(" ( !af.starter AND a.status <> 1 AND (( a.STATUS = -1 OR a.STATUS = 1 OR a.status=2 ) AND NOT (a.STATUS = 1 AND NOT (af.WAS_OPENED ))) ) ");
					}
				}
			}
			
			if(statusSB.length() > 0) {
				sql.append(" AND ( ").append(statusSB).append(" ) ");
			}
			
		}
		
		if(!products.isEmpty()) {
			sql.append(" AND a.search_type in (" );
			for (String product : products) {
				sql.append(product).append(",");
			}
			sql.replace(sql.length()-1, sql.length(), ")");
		}
		
		sql.append(" ORDER BY SDATE DESC");
		
		SqlParams sqlParams = new SqlParams();
		sqlParams.sql = sql.toString();
		sqlParams.params = params.toArray();
		return sqlParams;
	}
	
	public Map<Long, PriorFileAtsDocumentI> getDocumentsByParcel(TSServerInfoModule module) {
		SqlParams sql = getParcelSqlParams( module);
		return getDocuments(sql);
	}
	
	public Map<Long, PriorFileAtsDocumentI> getDocumentsByAddress(TSServerInfoModule module) {
		SqlParams sql = getAddressSqlParams( module);
		return getDocuments(sql);
	}
	
	public Map<Long, PriorFileAtsDocumentI> getDocumentsByLegal(TSServerInfoModule module) {
		SqlParams sql = getLegalSqlParams( module);
		return getDocuments(sql);
	}
	
	public Map<Long, PriorFileAtsDocumentI> getDocumentsByName(TSServerInfoModule module) {
		SqlParams sql = getNameSqlParams(module);
		return getDocuments(sql);
	}
	
	public Map<Long, PriorFileAtsDocumentI> getDocumentsByFileNo(TSServerInfoModule module) {
		SqlParams sql = getFileNoSqlParams(module);
		return getDocuments(sql);
	}
	
	public Map<Long, PriorFileAtsDocumentI> getDocumentsBySearchId(TSServerInfoModule module) {
		SqlParams sql = getSearchIdSqlParams(module);
		return getDocuments(sql);
	}

	@SuppressWarnings("unchecked")
	private Map<Long, PriorFileAtsDocumentI> getDocuments(SqlParams sqlParams) {
		
		if(sqlParams == null) {
			return null;
		}
		
		Map<Long, PriorFileAtsDocumentI> results = new LinkedHashMap<Long, PriorFileAtsDocumentI>();
		
		List<PriorFileAtsDocumentI> documents;
		try {
			documents = DBManager.getJdbcTemplate().query(
					sqlParams.sql, sqlParams.params, new PriorFileAtsMapper(sourceSearch));
		} catch (DataAccessException e) {
			e.printStackTrace();
			return null;
		}
		
		StringBuilder sb = new StringBuilder();
		for (PriorFileAtsDocumentI doc : documents) {
			if(doc != null && Long.parseLong(doc.getDocno()) != sourceSearch.getID()) {
				doc.setSearchType(sqlParams.searchType);
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
		
		
		return results;
	}
	
	private final class PriorFileAtsMapper implements ParameterizedRowMapper<PriorFileAtsDocumentI> {
		
		private Search sourceSearch;
		
		public PriorFileAtsMapper(Search sourceSearch) {
			this.sourceSearch = sourceSearch;
		}

		@Override
		public PriorFileAtsDocumentI mapRow(ResultSet row, int rowNum) throws SQLException {
			InstrumentI instr = new Instrument();
			instr.setInstno(row.getString(SearchReportAllInOneProcedure.COLUMN_FILE_ID));
			instr.setDocno(row.getString(SearchReportAllInOneProcedure.COLUMN_SEARCH_ID));
			PriorFileAtsDocumentI doc = new PriorFileAtsDocument(
					new RegisterDocument( DocumentsManager.generateDocumentUniqueId(sourceSearch.getID(), instr) ));
			doc.setInstrument(instr);
			
			boolean isStarter = row.getBoolean(SearchReportAllInOneProcedure.COLUMN_STARTER);
			
			
			String serverDocType = isStarter?DocumentTypes.PRIORFILE_BASE_SEARCH:DocumentTypes.PRIORFILE_PRIOR_SEARCH;
			String docCateg = DocumentTypes.getDocumentCategory(serverDocType, sourceSearch.getID()); 
	    	instr.setDocType(docCateg);
	    	String stype = DocumentTypes.getDocumentSubcategory(serverDocType, sourceSearch.getID());
	    	if("MISCELLANEOUS".equals(stype)&&!"MISCELLANEOUS".equals(docCateg)){
	    		stype = docCateg;
	    	}
	    	instr.setDocSubType(stype);
			
	    	doc.setServerDocType(serverDocType);
	    	doc.setType(SimpleChapterUtils.DType.ROLIKE);
	    	
			doc.setInstrumentDate(new Date(((Date)row.getDate(SearchReportAllInOneProcedure.COLUMN_SDATE)).getTime()));
			try {
				java.sql.Date tsrDate = row.getDate(SearchReportAllInOneProcedure.COLUMN_TSR_DATE);
				if(tsrDate != null) {
					doc.setTsrDate(new Date(tsrDate.getTime()));
				}
			} catch (Exception e) {}
			
			PropertyI property = Property.createEmptyProperty();
			doc.addProperty(property);
			
			property.getAddress().setNumber(row.getString(SearchReportAllInOneProcedure.COLUMN_ADDRESS_NO));
			property.getAddress().setPreDiretion(row.getString(SearchReportAllInOneProcedure.COLUMN_ADDRESS_DIR));
			property.getAddress().setStreetName(row.getString(SearchReportAllInOneProcedure.COLUMN_ADDRESS_NAME));
			property.getAddress().setSuffix(row.getString(SearchReportAllInOneProcedure.COLUMN_ADDRESS_SUF));
			property.getAddress().setIdentifierNumber(row.getString(SearchReportAllInOneProcedure.COLUMN_ADDRESS_UNIT));
			property.getAddress().setCity(row.getString(SearchReportAllInOneProcedure.COLUMN_CITY));
			property.getAddress().setZip(row.getString(SearchReportAllInOneProcedure.COLUMN_ZIP));
			
			
			PinI pin = property.getPin();
			String addressTablePin = row.getString(SearchReportAllInOneProcedure.COLUMN_ADDRESS_APN);
			String legalTablePin = row.getString(SearchReportAllInOneProcedure.COLUMN_LEGAL_APN);
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
			
			String lot = row.getString(SearchReportAllInOneProcedure.COLUMN_SUBDIVISION_LOT);
			if(StringUtils.isEmpty(lot)) {
				lot = row.getString(SearchReportAllInOneProcedure.COLUMN_ADDRESS_LOT);
			}
			if(StringUtils.isNotEmpty(lot)) {
				subdivision.setLot(lot);
			}
			
			subdivision.setPlatBook(row.getString(SearchReportAllInOneProcedure.COLUMN_PLAT_BOOK));
			subdivision.setPlatPage(row.getString(SearchReportAllInOneProcedure.COLUMN_PLAT_PAGE));
			
			subdivision.setName(row.getString(SearchReportAllInOneProcedure.COLUMN_SUBDIVISION_NAME));
			subdivision.setBlock(row.getString(SearchReportAllInOneProcedure.COLUMN_SUBDIVISION_BLOCK));
			subdivision.setPhase(row.getString(SearchReportAllInOneProcedure.COLUMN_SUBDIVISION_PHASE));
			subdivision.setUnit(row.getString(SearchReportAllInOneProcedure.COLUMN_SUBDIVISION_UNIT));
			subdivision.setTract(row.getString(SearchReportAllInOneProcedure.COLUMN_SUBDIVISION_TRACT));
			
			TownShipI township = legal.getTownShip();
			township.setSection(row.getString(SearchReportAllInOneProcedure.COLUMN_TOWHSHIP_S));
			township.setTownship(row.getString(SearchReportAllInOneProcedure.COLUMN_TOWHSHIP_T));
			township.setRange(row.getString(SearchReportAllInOneProcedure.COLUMN_TOWHSHIP_R));
			
			
			doc.setFileId(row.getString(SearchReportAllInOneProcedure.COLUMN_FILE_ID));
			doc.setAgentId(row.getLong(SearchReportAllInOneProcedure.COLUMN_AGENT_ID));
			doc.setProductId(row.getInt(SearchReportAllInOneProcedure.COLUMN_PRODUCT_ID));
			
			doc.setBase(isStarter);
			
			return doc;
		}
	}

}
