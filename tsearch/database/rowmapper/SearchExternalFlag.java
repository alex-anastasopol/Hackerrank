package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ro.cst.tsearch.database.DBManager;

/**
 * Class that maps to search_external_flags table
 * @author Andrei
 *
 */
public class SearchExternalFlag implements ParameterizedRowMapper<SearchExternalFlag> {

	private static final Logger logger = Logger.getLogger(SearchExternalFlag.class);
	
	public static final String TABLE_SEARCH_EXTERNAL_FLAGS = "search_external_flags";
	
	public static final String FIELD_SEARCH_ID = "search_id";
	public static final String FIELD_SO_ORDER_ID = "so_order_id";
	public static final String FIELD_SO_CUSTOMER_GUID = "so_customer_guid";
	public static final String FIELD_SO_ORDER_PRODUCT_ID = "so_order_product_id";
	public static final String FIELD_SC_FILE_ID = "sc_file_id";
	public static final String FIELD_TD_ORDER_ID = "td_order_id";
	public static final String FIELD_ATI_FILE_REFERENCE = "ati_file_reference";
	public static final String FIELD_SO_TO_UPDATE_GUID = "so_to_update_guid";
	public static final String FIELD_SO_PARENT_GUID = "so_parent_guid";
	
	private static final String INSERT_ENTRY = "INSERT INTO " + 
			TABLE_SEARCH_EXTERNAL_FLAGS + "(" + 
			FIELD_SEARCH_ID + " , " + 
			FIELD_SO_ORDER_ID + "," +
			FIELD_SO_CUSTOMER_GUID + "," +
			FIELD_SO_ORDER_PRODUCT_ID + "," + 
			FIELD_SC_FILE_ID + "," + 
			FIELD_TD_ORDER_ID + "," + 
			FIELD_SO_TO_UPDATE_GUID + "," + 
			FIELD_SO_PARENT_GUID +
			") VALUES (?,?,?,?,?,?,?,?) on duplicate key update " +
			FIELD_SO_ORDER_ID + " = ?, " +
			FIELD_SO_CUSTOMER_GUID + " = ?, " + 
			FIELD_SO_ORDER_PRODUCT_ID + " = ?, " + 
			FIELD_SC_FILE_ID + " = ?, " + 
			FIELD_TD_ORDER_ID + " = ?, " +
			FIELD_SO_TO_UPDATE_GUID + " = ?, " + 
			FIELD_SO_PARENT_GUID + " = ? ";
	
	private static final String INSERT_ATI_FILE_REFERENCE = "INSERT INTO " + 
			TABLE_SEARCH_EXTERNAL_FLAGS + "(" + 
			FIELD_SEARCH_ID + " , " + 
			FIELD_ATI_FILE_REFERENCE + ") VALUES (?,?) on duplicate key update " +
			FIELD_ATI_FILE_REFERENCE + " = ? ";
	
	private long searchId;
	private String soOrderId;
	private String soCustomerGuid;
	private String soToUpdateGuid;
	private String soParentGuid;
	private String soOrderProductId;
	private String scFileId;
	private String tdOrderId;
	
	public SearchExternalFlag(long searchId) {
		super();
		this.searchId = searchId;
	}

	public long getSearchId() {
		return searchId;
	}

	public void setSearchId(long searchId) {
		this.searchId = searchId;
	}

	public String getSoOrderId() {
		return soOrderId;
	}

	public void setSoOrderId(String soOrderId) {
		this.soOrderId = soOrderId;
	}

	public String getSoCustomerGuid() {
		return soCustomerGuid;
	}

	public void setSoCustomerGuid(String soCustomerGuid) {
		this.soCustomerGuid = soCustomerGuid;
	}

	public String getSoToUpdateGuid() {
		return soToUpdateGuid;
	}

	public void setSoToUpdateGuid(String soToUpdateGuid) {
		this.soToUpdateGuid = soToUpdateGuid;
	}

	public String getSoParentGuid() {
		return soParentGuid;
	}

	public void setSoParentGuid(String soParentGuid) {
		this.soParentGuid = soParentGuid;
	}

	public String getSoOrderProductId() {
		return soOrderProductId;
	}

	public void setSoOrderProductId(String soOrderProductId) {
		this.soOrderProductId = soOrderProductId;
	}

	public String getScFileId() {
		return scFileId;
	}

	public void setScFileId(String scFileId) {
		this.scFileId = scFileId;
	}

	public String getTdOrderId() {
		return tdOrderId;
	}

	public void setTdOrderId(String tdOrderId) {
		this.tdOrderId = tdOrderId;
	}

	/**
	 * Only saves values received from the order
	 * @return
	 */
	public boolean saveToDatabase() {
		return saveToDatabase(2);
	}
	
	public boolean saveToDatabase(int retry) {
		
		if(retry <= 0) {
			retry = 1;
		}
		SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
		for (int i = 0; i < retry; i++) {
			try {
				sjt.update(INSERT_ENTRY, getSearchId(), 
						getSoOrderId(), 
						getSoCustomerGuid(), 
						getSoOrderProductId(), 
						getScFileId(), 
						getTdOrderId(), 
						getSoOrderId(), 
						getSoToUpdateGuid(),
						getSoParentGuid(),
						getSoCustomerGuid(), 
						getSoOrderProductId(), 
						getScFileId(), 
						getTdOrderId(),
						getSoToUpdateGuid(),
						getSoParentGuid());
				return true;
			} catch (CannotAcquireLockException cale) {
				logger.error("Could not Acquire lock, number of retry " + i, cale);
			} catch (DataAccessException e) {
				logger.error("Error while saving entry in database", e);
			}
		}
		return false;
	}
	
	public static boolean saveAtiFileReference(long searchId, String atiFileReference) {
		SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
		
		try {
			sjt.update(INSERT_ATI_FILE_REFERENCE, searchId, atiFileReference, atiFileReference);
			return true;
		} catch (Exception e) {
			logger.error("Error while saving ati file reference in database", e);
		}
		
		return false;
	}
	
	@Override
	public SearchExternalFlag mapRow(ResultSet resultMap, int rowNum) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

}
