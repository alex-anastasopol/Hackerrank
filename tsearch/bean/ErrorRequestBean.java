package ro.cst.tsearch.bean;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.SharedDriveUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

public class ErrorRequestBean {
	
	
	public static final String TABLE_ERROR_REQUEST	=	"error_request";
	public static final String FIELD_ID				=	"id";
	public static final String FIELD_SEARCH_ID		=	"search_id";
	public static final String FIELD_REQUEST_DATE	=	"req_date";
	public static final String FIELD_REQUEST		=	"request";
	public static final String FIELD_RESPONSE		=	"response";
	public static final String FIELD_ERROR			=	"error";
	
	
	private long id;
	private long searchId = Search.SEARCH_NONE;
	private Date requestDate;
	private String request;
	private String response;
	private String errorMessage;
	private Throwable throwable;
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getSearchId() {
		return searchId;
	}
	public void setSearchId(long searchId) {
		this.searchId = searchId;
	}
	public Date getRequestDate() {
		return requestDate;
	}
	public void setRequestDate(Date requestDate) {
		this.requestDate = requestDate;
	}
	public String getRequest() {
		return request;
	}
	public void setRequest(String request) {
		this.request = request;
	}
	public String getResponse() {
		return response;
	}
	public void setResponse(String response) {
		this.response = response;
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	public Throwable getThrowable() {
		return throwable;
	}
	public void setThrowable(Throwable throwable) {
		this.throwable = throwable;
	}
	/**
	 * This function must NEVER throw any exceptions<br>
	 * It stores this data in database
	 */
	public boolean saveToDatabase() {
		try {
			prepareDate();
			SimpleJdbcInsert sji = DBManager.getSimpleJdbcInsert().withTableName(TABLE_ERROR_REQUEST).usingGeneratedKeyColumns("id");
			
			Map<String, Object> parameters = new HashMap<String, Object>();
			
		   	parameters.put(FIELD_SEARCH_ID, getSearchId());
		   	parameters.put(FIELD_REQUEST_DATE, getRequestDate());
//		   	parameters.put(FIELD_REQUEST, getRequest());
//		   	parameters.put(FIELD_RESPONSE, getResponse());
		   	parameters.put(FIELD_ERROR, getErrorMessage());
		   	
		   	setId(sji.executeAndReturnKey(parameters).longValue());
		   	
		   	Date logTime = getRequestDate();
		   	
		   	String threadLogFolder = SharedDriveUtils.getErrorLogFolder(logTime, false);
		   	String savedRequestFileName = null;
		   	String savedResponseFileName = null;
		   	if(threadLogFolder != null) {
		   		String fileName = new SimpleDateFormat(FormatDate.PATTERN_MMddyyyy_HHmmss).format(logTime) + "_request_" + URLMaping.INSTANCE_DIR + ".txt";
		   		String toSaveFileName = threadLogFolder + fileName;
		   		try {
					//let's do the new save
					org.apache.commons.io.FileUtils.writeStringToFile(new File(toSaveFileName), getRequest() );
					savedRequestFileName = toSaveFileName;
				} catch (Exception e) {
//					logger.error("Cannot addThreadsStackTrace to " + toSaveFileName, e);
					e.printStackTrace();
					String documentIndexBackupLocalFolder = SharedDriveUtils.getErrorLogFolder(logTime, true);
					if(org.apache.commons.lang.StringUtils.isNotBlank(documentIndexBackupLocalFolder)) {
						try {
							org.apache.commons.io.FileUtils.writeStringToFile(new File(documentIndexBackupLocalFolder + fileName), getRequest());
							savedRequestFileName = documentIndexBackupLocalFolder + fileName;
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
		   		
		   		fileName = new SimpleDateFormat(FormatDate.PATTERN_MMddyyyy_HHmmss).format(logTime) + "_response_" + URLMaping.INSTANCE_DIR + ".txt";
		   		toSaveFileName = threadLogFolder + fileName;
		   		try {
					//let's do the new save
					org.apache.commons.io.FileUtils.writeStringToFile(new File(toSaveFileName), getResponse() );
					savedResponseFileName = toSaveFileName;
				} catch (Exception e) {
//					logger.error("Cannot addThreadsStackTrace to " + toSaveFileName, e);
					e.printStackTrace();
					String documentIndexBackupLocalFolder = SharedDriveUtils.getErrorLogFolder(logTime, true);
					if(org.apache.commons.lang.StringUtils.isNotBlank(documentIndexBackupLocalFolder)) {
						try {
							org.apache.commons.io.FileUtils.writeStringToFile(new File(documentIndexBackupLocalFolder + fileName), getResponse());
							savedResponseFileName = documentIndexBackupLocalFolder + fileName;
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
		   		
		   	} else {
		   		System.err.println("Cannot addThreadsStackTrace because I can't find path");
		   	}
		   	
		   	DBManager.getSimpleTemplate().update(
		   				"update " + DBConstants.TABLE_ERROR_REQUEST + 
		   				" set " + FIELD_REQUEST + " = ?, " + FIELD_RESPONSE + " = ? where " + FIELD_ID + " = ?", 
		   				savedRequestFileName, 
		   				savedResponseFileName, 
		   				getId());
		   	
		   	
		   	return true;
		   	
		} catch (Exception e) {
			DBManager.getLogger().error("Error while saving ErrorRequestBean", e);
		}
		
		return false;
	}
	
	private void prepareDate() {
		if(getRequestDate() == null) {
			setRequestDate(Calendar.getInstance().getTime());
		}
		if(throwable != null) {
			if(StringUtils.isNotEmpty(errorMessage)) {
				errorMessage += "\nStack Trace: " + throwable.getMessage() + "\n " + ServerResponseException.getExceptionStackTrace( throwable, "\n" );
			} else {
				errorMessage = "Stack Trace: " + throwable.getMessage() + "\n " + ServerResponseException.getExceptionStackTrace( throwable, "\n" );
			}
		}
		if(StringUtils.isEmpty(errorMessage)) {
			errorMessage = "Unknown error has occurred";
		}
	}
	/**
	 * Cleans the database from entries older that <b>days</b>
	 * @param days
	 */
	public static void deleteEntriesOlderThan(int days) {
		SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
        String stm = null;
        try {
            stm = "delete from "+ TABLE_ERROR_REQUEST +" where date_add(" + FIELD_REQUEST_DATE + ", interval ? day) < now()";
            int deletedSearches = sjt.update(stm, days);
            DBManager.getLogger().debug("Deleted " + deletedSearches + " ErrorRequestBean entries");
        } catch (Exception e) {
        	DBManager.getLogger().error("DBManager deleteEntriesOlderThan# SQL: " + stm, e);
        }
		
	}
}
