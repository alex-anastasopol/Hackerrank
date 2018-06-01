package ro.cst.tsearch.database.transactions;

import java.util.Calendar;
import java.util.List;

import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.recoverdocument.ModuleShortDescription;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.rowmapper.ModuleMapper;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FormatDate;

import com.stewart.ats.base.document.RestoreDocumentData;
import com.stewart.ats.base.document.RestoreDocumentDataI;

public class SaveRestoreDocumentTransaction implements TransactionCallback {
	
	private RestoreDocumentDataI restoreDocumentDataI = null;
	private Search search = null;
	
	public SaveRestoreDocumentTransaction(RestoreDocumentDataI rsRestoreDocumentDataI, Search search) {
		this.restoreDocumentDataI = rsRestoreDocumentDataI;
		this.search = search;
		if(search == null) {
			throw new NullPointerException("Field search cannot be null");
		}
		if(restoreDocumentDataI == null) {
			throw new NullPointerException("Field restoreDocumentDataI cannot be null");
		}
	}
	
	private final static String SQL_CKECK_AVAILABLE_MODULES = 
		"SELECT * FROM " + DBConstants.TABLE_MODULES + " WHERE " + 
			DBConstants.FIELD_MODULE_SERVER_ID + " = ? AND " + 
			DBConstants.FIELD_MODULE_SEARCH_MODULE_ID + " = ? AND " + 
			DBConstants.FIELD_MODULE_DESCRIPTION + " = ? AND " + 
			DBConstants.FIELD_MODULE_SEARCH_ID + " = ?" ;
	private final static String SQL_INSERT_MODULE_TO_DOCUMENT =
		"INSERT INTO " + DBConstants.TABLE_MODULE_TO_DOCUMENT + " VALUES (?,?)";
	private final static String SQL_CHECK_MODULE_TO_DOCUMENT =
		"SELECT count(*) FROM " + DBConstants.TABLE_MODULE_TO_DOCUMENT + " where " + 
			DBConstants.FIELD_MODULE_TO_DOCUMENT_MODULE_ID + " = ? AND " +
			DBConstants.FIELD_MODULE_TO_DOCUMENT_DOCUMENT_ID + " = ?";
	@Override
	public Object doInTransaction(TransactionStatus transactionStatus) {
		
		try {
			//StringBuilder sqlCheckDocumentPresent = getSqlCkeckDocumentPresent();
			
			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			//List<RestoreDocumentData> availableDocuments = 
			//	sjt.query(sqlCheckDocumentPresent.toString(), new RestoreDocumentData());
			boolean stored = restoreDocumentDataI.saveInDatabase(sjt);
			/*
			if(availableDocuments.size() > 0) {
				restoreDocumentDataI.setId(availableDocuments.get(0).getId());
				if(restoreDocumentDataI.updateInDatabase(sjt) > 0) {
					stored = true;
				}
			} else {
				stored = restoreDocumentDataI.saveInDatabase(sjt);
			}
			*/
			if(!stored) {
				transactionStatus.setRollbackOnly();
				return false;
			}
			
			ModuleShortDescription moduleShortDescription = (ModuleShortDescription) getSearch()
				.getAdditionalInfo(TSServerInfoConstants.TS_SERVER_INFO_MODULE_DESCRIPTION);
			if(moduleShortDescription == null) {
				moduleShortDescription = new ModuleShortDescription();
				moduleShortDescription.setDescription("Unknown search module");
				moduleShortDescription.setSearchModuleId(0);
			}
			
			List<ModuleMapper> availableModules =
				sjt.query(SQL_CKECK_AVAILABLE_MODULES, new ModuleMapper(), 
						restoreDocumentDataI.getServerId(),
						moduleShortDescription.getSearchModuleId(),
						moduleShortDescription.getDescription(),
						search.getID());
			ModuleMapper currentModule = null;
			if(availableModules.size() > 0) {
				currentModule = availableModules.get(0);
			} else {
				currentModule = new ModuleMapper();
				currentModule.setServerId(restoreDocumentDataI.getServerId());
				currentModule.setSearchModuleId(moduleShortDescription.getSearchModuleId());
				currentModule.setDescription(moduleShortDescription.getDescription());
				currentModule.setSearchId(search.getID());
				stored = currentModule.saveInDatabase(sjt);
				if(!stored) {
					transactionStatus.setRollbackOnly();
					return false;
				}
			}
			if(sjt.queryForInt(SQL_CHECK_MODULE_TO_DOCUMENT, currentModule.getModuleId(), restoreDocumentDataI.getId()) == 0) {
				sjt.update(SQL_INSERT_MODULE_TO_DOCUMENT, currentModule.getModuleId(), restoreDocumentDataI.getId());
			}
			
		
		} catch (Exception e) {
			e.printStackTrace();
			transactionStatus.setRollbackOnly(); 
			return false;
		}
		
		return true;
	}
	
	

	public Search getSearch() {
		return search;
	}

	public void setSearch(Search search) {
		this.search = search;
	}

	/**
	 * Created the sql needed to check if the document to save already exists.
	 * @return
	 */
	private StringBuilder getSqlCkeckDocumentPresent() {
		StringBuilder sqlCheckDocumentPresent = new StringBuilder();
		sqlCheckDocumentPresent
			.append("SELECT * FROM ")
			.append(DBConstants.TABLE_RECOVER_DOCUMENTS)
			.append(" WHERE ");
		sqlCheckDocumentPresent
			.append(DBConstants.FIELD_RECOVER_DOCUMENTS_BOOK);
		if(restoreDocumentDataI.getBook() != null) {
			sqlCheckDocumentPresent
				.append(" = '").append(restoreDocumentDataI.getBook()).append("' ");
		} else {
			sqlCheckDocumentPresent.append(" is null ");
		}
		sqlCheckDocumentPresent.append(" AND ");
		
		sqlCheckDocumentPresent
			.append(DBConstants.FIELD_RECOVER_DOCUMENTS_PAGE);
		if(restoreDocumentDataI.getPage() != null) {
			sqlCheckDocumentPresent
				.append(" = '").append(restoreDocumentDataI.getPage()).append("' ");
		} else {
			sqlCheckDocumentPresent.append(" is null ");
		}
		sqlCheckDocumentPresent.append(" AND ");
		
		
		sqlCheckDocumentPresent.append(DBConstants.FIELD_RECOVER_DOCUMENTS_INSTRUMENT_NUMBER);
		if(restoreDocumentDataI.getInstrumentNumber() != null) {
			sqlCheckDocumentPresent.append(" = '").append(restoreDocumentDataI.getInstrumentNumber()).append("' ");
		} else {
			sqlCheckDocumentPresent.append(" is null ");
		}
		sqlCheckDocumentPresent.append(" AND ");
		
		sqlCheckDocumentPresent.append(DBConstants.FIELD_RECOVER_DOCUMENTS_DOCUMENT_NUMBER);
		if(restoreDocumentDataI.getDocumentNumber() != null) {
			sqlCheckDocumentPresent.append(" = '").append(restoreDocumentDataI.getDocumentNumber()).append("' ");
		} else {
			sqlCheckDocumentPresent.append(" is null ");
		}
		sqlCheckDocumentPresent.append(" AND ");
		
		sqlCheckDocumentPresent.append(DBConstants.FIELD_RECOVER_DOCUMENTS_YEAR)
			.append(" = ").append(restoreDocumentDataI.getYear())
			.append(" AND ");
		
		sqlCheckDocumentPresent.append(DBConstants.FIELD_RECOVER_DOCUMENTS_DOCTYPE_FOR_SEARCH);
		if(restoreDocumentDataI.getDoctypeForSearch() != null) {
			sqlCheckDocumentPresent.append(" = '").append(restoreDocumentDataI.getDoctypeForSearch()).append("' ");
		} else {
			sqlCheckDocumentPresent.append(" is null ");
		}
		sqlCheckDocumentPresent.append(" AND ");
		
		sqlCheckDocumentPresent.append(DBConstants.FIELD_RECOVER_DOCUMENTS_RECORDED_DATE);
		if(restoreDocumentDataI.getRecordedDate() != null) {
			Calendar cald = Calendar.getInstance();
			cald.setTime(restoreDocumentDataI.getRecordedDate());
			sqlCheckDocumentPresent.append(" = str_to_date( '" + cald.get(Calendar.DAY_OF_MONTH) + "-" + (cald.get(Calendar.MONTH) + 1) + "-" + cald.get(Calendar.YEAR) + " 00:00:00" 
					+ "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' ) ");
		} else {
			sqlCheckDocumentPresent.append(" is null ");
		}
		sqlCheckDocumentPresent.append(" AND ");
		
		sqlCheckDocumentPresent.append(DBConstants.FIELD_RECOVER_DOCUMENTS_SERVER_ID)
			.append(" = ").append(restoreDocumentDataI.getServerId());
		
	
		return sqlCheckDocumentPresent;
	}

}
