package ro.cst.tsearch.database.transactions;

import static ro.cst.tsearch.utils.DBConstants.TABLE_SEARCH;

import java.math.BigInteger;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchFlags;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DBSearch;
import ro.cst.tsearch.database.rowmapper.ImageCount;
import ro.cst.tsearch.database.rowmapper.LegalMapper;
import ro.cst.tsearch.database.rowmapper.SearchUserTimeMapper;
import ro.cst.tsearch.search.bean.SearchLegalLotBean;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.user.GroupAttributes;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;

import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.Subdivision;
import com.stewart.ats.base.legal.TownShip;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.user.UserFilters;

public class SaveSearchTransaction implements TransactionCallback {

	private static final Logger logger = Logger.getLogger(SaveSearchTransaction.class);
	private static FormatDate fd = new FormatDate(FormatDate.TIMESTAMP);
	
	public static final int STATUS_SUCCES = 0;
	public static final int STATUS_FAIL_TIMEOUT = 1;
	public static final int STATUS_FAIL_MUTUAL_EXCLUSION_VIOLATION = 2;
	public static final int STATUS_FAIL_OTHER_EXCEPTION = -1;
	
	private Search search = null;
	private Search oldSearch = null;
	
	private long searchId;
	private long agentId;
	private Long agentCommunityId;
	private long abstractorId;
	private long propertyId;
	private long commId;
	private long payrateId;
	private long userRatingId;
	private long agentRatingId;
	private long timestamp;

	private String agentFileNo;
	private String abstractorFileNo;
	
	private String sDate;
	private String tsrDate;
	private String tsrLink;
	private String tsrSentTo;
	private String tsrFolder;
	private String noteClob;
	private String legalDescription;
	private String dueDate;

	private int searchType;
	private int status;
	private int tsrCreated;
	private int noteStatus;
	private int legalDescriptionStatus;
	private int checkedBy;
	
	/**
	 * Please implement transition between code and color in {@link DBSearch} ro.cst.tsearch.database.DBSearch.getColorCodeFromDatabaseStatus(int)
	 */
	private int colorCodeStatus = 0;
	private int abstractorGroupId = -1;
	
	public SaveSearchTransaction(Search global) {
		search = global;
		searchId = global.getID();
	}

	public String getAbstractorFileNo() {
		return abstractorFileNo;
	}

	public void setAbstractorFileNo(String abstractorFileNo) {
		this.abstractorFileNo = abstractorFileNo;
	}

	public long getAbstractorId() {
		return abstractorId;
	}

	public void setAbstractorId(long abstractorId) {
		this.abstractorId = abstractorId;
	}

	public String getAgentFileNo() {
		return agentFileNo;
	}

	public void setAgentFileNo(String agentFileNo) {
		this.agentFileNo = agentFileNo;
	}

	public long getAgentId() {
		return agentId;
	}

	public void setAgentId(long agentId) {
		this.agentId = agentId;
	}

	public Long getAgentCommunityId() {
		return agentCommunityId;
	}

	public void setAgentCommunityId(Long agentCommunityId) {
		this.agentCommunityId = agentCommunityId;
	}

	public long getAgentRatingId() {
		return agentRatingId;
	}

	public void setAgentRatingId(long agentRatingId) {
		this.agentRatingId = agentRatingId;
	}

	public int getCheckedBy() {
		return checkedBy;
	}

	public void setCheckedBy(int checkedBy) {
		this.checkedBy = checkedBy;
	}

	public long getCommId() {
		return commId;
	}

	public void setCommId(long commId) {
		this.commId = commId;
	}

	public String getLegalDescription() {
		return legalDescription;
	}

	public void setLegalDescription(String legalDescription) {
		this.legalDescription = legalDescription;
	}

	public int getLegalDescriptionStatus() {
		return legalDescriptionStatus;
	}

	public void setLegalDescriptionStatus(int legalDescriptionStatus) {
		this.legalDescriptionStatus = legalDescriptionStatus;
	}

	public String getNoteClob() {
		return noteClob;
	}

	public void setNoteClob(String noteClob) {
		this.noteClob = noteClob;
	}

	public int getNoteStatus() {
		return noteStatus;
	}

	public void setNoteStatus(int noteStatus) {
		this.noteStatus = noteStatus;
	}

	public long getPayrateId() {
		return payrateId;
	}

	public void setPayrateId(long payrateId) {
		this.payrateId = payrateId;
	}

	public long getPropertyId() {
		return propertyId;
	}

	public void setPropertyId(long propertyId) {
		this.propertyId = propertyId;
	}

	public String getSDate() {
		return sDate;
	}

	public void setSDate(String date) {
		sDate = date;
	}

	public long getSearchId() {
		return searchId;
	}

	public void setSearchId(long searchId) {
		this.searchId = searchId;
	}

	public int getSearchType() {
		return searchType;
	}

	public void setSearchType(int searchType) {
		this.searchType = searchType;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getTsrCreated() {
		return tsrCreated;
	}

	public void setTsrCreated(int tsrCreated) {
		this.tsrCreated = tsrCreated;
	}

	public String getTsrDate() {
		return tsrDate;
	}

	public void setTsrDate(String tsrDate) {
		this.tsrDate = tsrDate;
	}

	public String getTsrFolder() {
		return tsrFolder;
	}

	public void setTsrFolder(String tsrFolder) {
		this.tsrFolder = tsrFolder;
	}

	public String getTsrLink() {
		return tsrLink;
	}

	public void setTsrLink(String tsrLink) {
		this.tsrLink = tsrLink;
	}

	public String getTsrSentTo() {
		return tsrSentTo;
	}

	public void setTsrSentTo(String tsrSentTo) {
		this.tsrSentTo = tsrSentTo;
	}

	public long getUserRatingId() {
		return userRatingId;
	}

	public void setUserRatingId(long userRatingId) {
		this.userRatingId = userRatingId;
	}
	
	public Search getOldSearch() {
		return oldSearch;
	}

	public void setOldSearch(Search oldSearch) {
		this.oldSearch = oldSearch;
	}

	public Search getSearch() {
		return search;
	}

	public void setSearch(Search search) {
		this.search = search;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getDueDate() {
		return dueDate;
	}

	public void setDueDate(String dueDate) {
		this.dueDate = dueDate;
	}

	public String getFileId() {
		return getSearch().getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO);
	}

	public Object doInTransaction(TransactionStatus status) {
		Date newStartIntervalWork = null;
		
		try{
			timestamp = System.currentTimeMillis();
			search.loadLegalFromSearchAttributes();
			
			if(getPayrateId() == 0) {
				Log.sendEmail("SearchID:\n " + searchId + "\n has payrate_id 0\n\n" +
		    			"This is not good, and must be investigated and added to bugzilla\n\nFull Dump:\n" +
		    			toString());
			}
			
			
			
			SimpleJdbcTemplate sjt = ConnectionPool.getInstance().getSimpleTemplate();
			if(mutualExclusionViolation(sjt, "", true))
				return STATUS_FAIL_MUTUAL_EXCLUSION_VIOLATION;
			if(tsrCreated == Search.SEARCH_TSR_CREATED)
				saveTSR( sjt );
			else {
				
				if(search.isSearchProductTypeOfUpdate()) {
					DocumentsManagerI docManager = search.getDocManager();
					try {
						docManager.getAccess();
						
						DocumentsManagerI manager = search.getDocManager();
			            boolean foundRoDocs = ro.cst.tsearch.templates.TemplateUtils.foundRoDocs(manager, false);
						
						if(foundRoDocs) {
							colorCodeStatus = 1;
						}
						
					} finally {
						docManager.releaseAccess();
					}
				}

				
				
				if(search.isUpdate()){
					boolean updateResult = (Boolean)updateSearch(sjt);
					//false is returned if the save could not be made
					if(!updateResult)
						return STATUS_FAIL_OTHER_EXCEPTION;
				} else {
					saveSearch(sjt);
				}
				search.setUpdate(true);
			}
			Search.saveSearch(search);
			try {
				updateTableWith(sjt, DBConstants.TABLE_PROPERTY_OWNER, search.getSa().getOwners().getNames(),search.getSa().getSearchPageManualOwners().getNames());
				updateTableWith(sjt, DBConstants.TABLE_PROPERTY_BUYER, search.getSa().getBuyers().getNames(),search.getSa().getSearchPageManualBuyers().getNames());
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			try 
			{
				//if by any chance we have an exception we just leave the old saving mechanism
				updateLegal(sjt);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			//saveImageCount(sjt,searchId);
			
			if(tsrCreated != Search.SEARCH_TSR_CREATED) {
				newStartIntervalWork = updateWorkedTime(sjt);
			}
			
			try {
				//no need to update since we will not filter with these again
				//updateSearchFilters(sjt);
			} catch (Exception e) {
				logger.error("Error while saving search_filters", e);
			}
			
		} catch (CannotAcquireLockException cale) {
			cale.printStackTrace();
			logger.error("CannotAcquireLockException", cale);
			status.setRollbackOnly();
			return STATUS_FAIL_TIMEOUT;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Other Exception", e);
			status.setRollbackOnly();
			return STATUS_FAIL_OTHER_EXCEPTION;
		}
		
		// only on success update work date to be sure next save will include worked interval
		if(newStartIntervalWork != null) {
			search.setStartIntervalWorkDate(newStartIntervalWork);
		}
		
		return STATUS_SUCCES;
	}
	
	
	private Date updateWorkedTime(SimpleJdbcTemplate sjt) {
		
		if(search.getStartIntervalWorkDate() == null) {
			return null;
		}
		
		Date now = new Date();
        try {
			long workedTime = sjt.queryForLong("SELECT " + SearchUserTimeMapper.FIELD_WORKED_TIME + 
						" from " + SearchUserTimeMapper.TABLE_SEARCH_USER_TIME + 
						" where " + SearchUserTimeMapper.FIELD_SEARCH_ID + " = ? " + 
						" and " + SearchUserTimeMapper.FIELD_USER_ID + " = ?", 
						searchId, 
						abstractorId);
			
			workedTime += (now.getTime() - search.getStartIntervalWorkDate().getTime()) / 1000;
			
			sjt.update("update " + SearchUserTimeMapper.TABLE_SEARCH_USER_TIME + 
					" SET " + SearchUserTimeMapper.FIELD_WORKED_TIME + " = ? " + 
					" where " + SearchUserTimeMapper.FIELD_SEARCH_ID + " = ? " + 
					" and " + SearchUserTimeMapper.FIELD_USER_ID + " = ?",
					workedTime,
					searchId, 
					abstractorId);
			
			
		} catch (EmptyResultDataAccessException e) {
			long workedTime = (now.getTime() - search.getStartIntervalWorkDate().getTime()) / 1000;
			sjt.update(SearchUserTimeMapper.INSERT_NAMED_PARAMS, 
					new BeanPropertySqlParameterSource(new SearchUserTimeMapper(searchId, abstractorId, workedTime)));
			
		}
        
        return now;
	}

	@SuppressWarnings("unused")
	private static void saveImageCount(SimpleJdbcTemplate sjt, long searchId) {
		try  {
			sjt.update(DELTE_IMAGE_COUNT,searchId);
			Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			
			/* Ugly for now. Only first image source counts. */
			
			int type = -1;
			
			Vector<DataSite> sites = HashCountyToIndex.getAllDataSites(search.getCommId(),
					InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv(), 
					InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName(), 
					true, Products.FULL_SEARCH_PRODUCT);
			for(DataSite site : sites) {
				if(TSServer.isRoLike(site.getCityCheckedInt())) {
					type = site.getCityCheckedInt();
					break;
				}
			}
			
			synchronized (search.getImagesCount()) {
				if(type!=-1 && search.getImagesCount().containsKey(type)) {
					sjt.update(ImageCount.INSERT_IMAGE_COUNT,searchId,type,search.getImagesCount().get(type));
				}else {
					for(Entry<Integer,Integer> e : search.getImagesCount().entrySet()) {
						sjt.update(ImageCount.INSERT_IMAGE_COUNT,searchId,e.getKey(),e.getValue());
						break;
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static final String DELTE_IMAGE_COUNT = "DELETE FROM " + 
		DBConstants.TABLE_IMAGE_COUNT + " WHERE searchId = ? ";
	
	private static final String INSERT_WARNING_LONG = "INSERT INTO " + 
		DBConstants.TABLE_SEARCH_FILTERS + "(searchId, longValue,type) VALUES (?,?,?)"; 
	private static final String INSERT_WARNING_STRING = "INSERT INTO " + 
	DBConstants.TABLE_SEARCH_FILTERS + "(searchId, stringValue,type) VALUES (?,?,?)"; 
	@SuppressWarnings("unused")
	private void updateSearchFilters(SimpleJdbcTemplate sjt) {
		sjt.update(DELETE_FROM_TABLE.replace("@@tableName@@", DBConstants.TABLE_SEARCH_FILTERS), searchId);
		
		int type = UserFilters.TYPE_ASSIGN_ORDERS_WARNING;
		for (Integer warningId : getSearch().getSearchFlags().getWarnings()) {
			sjt.update(INSERT_WARNING_LONG, searchId, warningId, type);
		}
		
		type = UserFilters.TYPE_ASSIGN_ORDERS_CATEGANDSUBCATEG;
		for (String categInfo : getSearch().getDistinctCategorySubcategoryPairs()) {
			sjt.update(INSERT_WARNING_STRING, searchId, categInfo, type);
		}
		
	}


	private static final String SELECT_PRIMARY_KEY_FROM_TABLE = 
		"SELECT @@primaryKey@@ FROM @@tableName@@ WHERE searchId = ? ORDER BY @@primaryKey@@ DESC";
	private static final String DELETE_FROM_TABLE = 
		"DELETE FROM @@tableName@@ WHERE searchId = ?";
	private static final String DELETE_FROM_TABLE_BY_KEY = 
		"DELETE FROM @@tableName@@ WHERE @@primaryKey@@ = ? ";
	private static final String INSERT_PARTY_NAME = 
		"INSERT INTO @@tableName@@ (" + 
		DBConstants.FIELD_PROPERTY_OWNER_LAST_NAME + ", " +
		DBConstants.FIELD_PROPERTY_OWNER_FIRST_NAME + ", " + 
		DBConstants.FIELD_PROPERTY_OWNER_MIDDLE_NAME + ", " + 
		DBConstants.FIELD_PROPERTY_OWNER_SUFFIX + ", " +
		DBConstants.FIELD_PROPERTY_OWNER_PREFIX + ", " + 
		DBConstants.FIELD_PROPERTY_OWNER_IS_COMPANY + ", " + 
		DBConstants.FIELD_PROPERTY_OWNER_SSN4 + ", " +
		DBConstants.FIELD_PROPERTY_OWNER_COLOR + ", " +
		DBConstants.FIELD_PROPERTY_OWNER_SEARCH_ID + ") VALUES ( ?, ?, ? , ?, ?, ?, ?, ?, ? )"; 
	private static final String UPDATE_PARTY_NAME = 
		"UPDATE @@tableName@@ SET " + 
		DBConstants.FIELD_PROPERTY_OWNER_LAST_NAME + "= ?, " +
		DBConstants.FIELD_PROPERTY_OWNER_FIRST_NAME + "= ?, " + 
		DBConstants.FIELD_PROPERTY_OWNER_MIDDLE_NAME + "= ?, " + 
		DBConstants.FIELD_PROPERTY_OWNER_SUFFIX + "= ?, " +
		DBConstants.FIELD_PROPERTY_OWNER_PREFIX + "= ?, " + 
		DBConstants.FIELD_PROPERTY_OWNER_IS_COMPANY + "= ?, " + 
		DBConstants.FIELD_PROPERTY_OWNER_SSN4 + "= ?, " +
		DBConstants.FIELD_PROPERTY_OWNER_COLOR + "= ?, " +
		DBConstants.FIELD_PROPERTY_OWNER_SEARCH_ID + "= ? " +
		" WHERE " + DBConstants.FIELD_PROPERTY_OWNER_ID + "= ? ";
		
	
	private void updateTableWith(SimpleJdbcTemplate sjt, String tablePropertyName, Set<NameI> names, Set<NameI> searchPageNames) {
		
		List<Map<String,Object>> oldRecords = sjt.queryForList(SELECT_PRIMARY_KEY_FROM_TABLE.replace("@@tableName@@", tablePropertyName).replace("@@primaryKey@@", "id"),searchId);
		int newRecordCount  = names.size();
		int current = 0;
		
		String updt = UPDATE_PARTY_NAME.replace("@@tableName@@", tablePropertyName);
		NameI[] namesA = (NameI[])names.toArray(new NameI[0]);
		
		for(Map<String,Object> r : oldRecords) {
			long id = (Long)r.get("id");
			
			if(current < newRecordCount) { 
				sjt.update(updt, 
						namesA[current].getLastName(), 
						namesA[current].getFirstName(),
						namesA[current].getMiddleName(),
						namesA[current].getSufix(),
						namesA[current].getPrefix(),
						namesA[current].isCompany(),
						namesA[current].getSsn4Encoded(),
						searchPageNames.contains(namesA[current])?"#000000":"#FF0000",
						search.getID(),
						id
						);
				
				current ++;
			}else {
				sjt.update(DELETE_FROM_TABLE_BY_KEY.replace("@@tableName@@", tablePropertyName).replace("@@primaryKey@@", "id"),id);
			}
		}
		for(int i = current; i < newRecordCount ; i++ ) {
			String insert  = INSERT_PARTY_NAME.replace("@@tableName@@", tablePropertyName);
			sjt.update(insert, 
					namesA[i].getLastName(), 
					namesA[i].getFirstName(),
					namesA[i].getMiddleName(),
					namesA[i].getSufix(),
					namesA[i].getPrefix(),
					namesA[i].isCompany(),
					namesA[i].getSsn4Encoded(),
					searchPageNames.contains(namesA[i])?"#000000":"#FF0000",
					search.getID()
					);
		}
		
	}

	/*
	private static final String SQL_SAVE_TSR_UPDATE_FLAGS = "UPDATE " + DBConstants.TABLE_SEARCH_FLAGS + " SET " + 
			DBConstants.FIELD_SEARCH_FLAGS_LEGAL_DESCR_STATUS + " = ?, " +
			DBConstants.FIELD_SEARCH_FLAGS_CHECKED_BY + " = ?, " +
			DBConstants.FIELD_SEARCH_FLAGS_TSR_CREATED + " = ?, " +
			DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED + " = ?, " + 
			DBConstants.FIELD_SEARCH_FLAGS_STARTER + " = ?  WHERE " +
			DBConstants.FIELD_SEARCH_FLAGS_ID + " = ?";
	*/
	
	private static final String SQL_SAVE_TSR_UPDATE_FLAGS_SOURCE = "UPDATE " + DBConstants.TABLE_SEARCH_FLAGS + " SET " + 
			DBConstants.FIELD_SEARCH_FLAGS_LEGAL_DESCR_STATUS + " = ?, " +
			DBConstants.FIELD_SEARCH_FLAGS_CHECKED_BY + " = ?, " +
			DBConstants.FIELD_SEARCH_FLAGS_TSR_CREATED + " = ?, " +
			DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED + " = ?, " + 
			DBConstants.FIELD_SEARCH_FLAGS_STARTER + " = ?, " + 
			DBConstants.FIELD_SEARCH_FLAGS_SOURCE_CREATION_TYPE + " = ?, " + 
			DBConstants.FIELD_SEARCH_FLAGS_COLOR_FLAG + " = ? WHERE " +
			DBConstants.FIELD_SEARCH_FLAGS_ID + " = ?";
	
	
	private static final String SQL_SAVE_TSR_UPDATE_BLOGS = "UPDATE " + 
			DBConstants.TABLE_SEARCH_DATA_BLOB + " SET " + 
        	DBConstants.FIELD_SEARCH_DATA_BLOB_LEGAL_DESCR + " = ? WHERE " + 
        	DBConstants.FIELD_SEARCH_DATA_BLOB_ID + " = ? ";
	
	/**
	 * saves a TSR in the database
	 * @param sjt the jdbc template to be used when updating the database;
	 * @return
	 */
	private Object saveTSR(SimpleJdbcTemplate sjt) {
		SearchFlags searchFlags = search.getSearchFlags();
		String sql = "SELECT SDATE FROM "+ TABLE_SEARCH +" WHERE ID = " + searchId;
		
		String searchDate = fd.getLocalDate(sjt.queryForObject(sql, Date.class));
		String tsrDate = fd.getDate(timestamp);
        
		String sqlTsrDate = "str_to_date( '" + tsrDate + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
        String sqlSearchDate = "str_to_date( '" + searchDate + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
        
        String tsrInitialDate = "";
        ro.cst.tsearch.SearchFlags.CREATION_SOURCE_TYPES type = DBSearch.getCreationSourceType(search.getSearchID());
        
        if(type!=null && type!=ro.cst.tsearch.SearchFlags.CREATION_SOURCE_TYPES.REOPENED && tsrCreated == 1){
        	String dbTSRIDate = DBManager.getSearchTSRInitialDate(searchId, null);
        	if (ro.cst.tsearch.utils.StringUtils.isEmpty(dbTSRIDate)) {
        		tsrInitialDate = sqlTsrDate;
        	}
        }
        
        sql = "UPDATE "+ TABLE_SEARCH +" SET " + 
        	"AGENT_ID= ?, " +
        	"ABSTRACT_ID= ?, " +
        	"PROPERTY_ID= ?, " +
        	"AGENT_FILENO= ?, " +
        	DBConstants.FIELD_SEARCH_ABSTRACT_FILENO + " = ?, " +
        	"SEARCH_TYPE= ?, " +
        	"PAYRATE_ID= ?, " +
        	"SDATE=" + sqlSearchDate + ", " +
        	"TSR_FILE_LINK= ?, " +
        	"TSR_SENT_TO= ?, " +
        	"USER_RATING_ID=?, " +
        	"TSR_FOLDER=	 ?, " +
        	"STATUS = -1 , " +								//set default status (which is -1)
        	"COMM_ID= ?, " +
        	DBConstants.FIELD_SEARCH_TSR_DATE + " = " + sqlTsrDate + ", " +
        	DBConstants.FIELD_SEARCH_REPORTS_DATE + " = " + sqlTsrDate + ", " +
        	(ro.cst.tsearch.utils.StringUtils.isEmpty(tsrInitialDate) ? "" : "TSR_INITIAL_DATE=" + tsrInitialDate + ", " )+
        	"NOTE_CLOB= ?, " +
        	"NOTE_STATUS = 0, " +
        	"DUE_DATE = " + dueDate + ", " +
        	"AGENT_RATING_ID= ?, " + 
        	DBConstants.FIELD_SEARCH_FILE_ID + " = ?, " +
        	DBConstants.FIELD_SEARCH_LAST_SAVE_DATE + " = now(), " +
        	DBConstants.FIELD_SEARCH_TSRI_LINK + " = ?, " +
        	DBConstants.FIELD_SEARCH_SEC_ABSTRACT_ID + " = ?, " +
        	DBConstants.FIELD_SEARCH_TIME_ELAPSED + " = getBussinesElapsedTime(unix_timestamp( " + sqlSearchDate + "), unix_timestamp( " +
        	sqlTsrDate + " ), 1) " +
        	" WHERE ID = 		?";
        
        //updating the ts_search table
        sjt.update(sql,agentId,abstractorId,propertyId,agentFileNo,abstractorFileNo,searchType,payrateId,tsrLink,
        					 tsrSentTo,userRatingId,tsrFolder,commId,noteClob,agentRatingId,
        					 getFileId(), getSearch().getTsriLink(), getSearch().getSecondaryAbstractorId(),
        					 searchId);
        
        //updating the ts_search_flags table
        //if(!searchFlags.getCreationSourceType().equals(CREATION_SOURCE_TYPES.NORMAL)) {
        	sjt.update(SQL_SAVE_TSR_UPDATE_FLAGS_SOURCE,
        			legalDescriptionStatus,
        			getCheckedBy(),
        			tsrCreated, 
        			false, 
        			searchFlags.isBase(),
        			searchFlags.getCreationSourceTypeForDatabase(),
        			colorCodeStatus,
        			searchId);
        /*} else {
        	sjt.update(SQL_SAVE_TSR_UPDATE_FLAGS,
        			legalDescriptionStatus,
        			getCheckedBy(),
        			tsrCreated, 
        			false, 
        			searchFlags.isStarter(), 
        			searchId);
        }*/
        
                
        //updating the ts_search_data_blob table
        sjt.update(SQL_SAVE_TSR_UPDATE_BLOGS, legalDescription, searchId);
        
        search.setUpdate(false);
        
        return true;
	}

	/*
	private static final String SQL_SAVE_SEARCH_UPDATE_FLAGS = "UPDATE " + 
			DBConstants.TABLE_SEARCH_FLAGS + " SET " + 
			DBConstants.FIELD_SEARCH_FLAGS_LEGAL_DESCR_STATUS + " = ?, " +
			DBConstants.FIELD_SEARCH_FLAGS_CHECKED_BY + " = ?, " +
			DBConstants.FIELD_SEARCH_FLAGS_TSR_CREATED + " = ?, " +
			DBConstants.FIELD_SEARCH_FLAGS_STARTER + " = ?, " +
			DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED + " = ? " +
			" WHERE " +
			DBConstants.FIELD_SEARCH_FLAGS_ID + " = ? " ;
	*/
	private static final String SQL_SAVE_SEARCH_UPDATE_FLAGS_SOURCE = "UPDATE " + 
			DBConstants.TABLE_SEARCH_FLAGS + " SET " + 
			DBConstants.FIELD_SEARCH_FLAGS_LEGAL_DESCR_STATUS + " = ?, " +
			DBConstants.FIELD_SEARCH_FLAGS_CHECKED_BY + " = ?, " +
			DBConstants.FIELD_SEARCH_FLAGS_TSR_CREATED + " = ?, " +
			DBConstants.FIELD_SEARCH_FLAGS_STARTER + " = ?, " +
			DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED + " = ?, " +
			DBConstants.FIELD_SEARCH_FLAGS_SOURCE_CREATION_TYPE + " = ?, " +
			DBConstants.FIELD_SEARCH_FLAGS_COLOR_FLAG + " = ? " +
			" WHERE " +
			DBConstants.FIELD_SEARCH_FLAGS_ID + " = ? " ;
	/*
	private static final String SQL_SAVE_SEARCH_INSERT_FLAGS = "INSERT INTO " + 
			DBConstants.TABLE_SEARCH_FLAGS + " (" + 
			DBConstants.FIELD_SEARCH_FLAGS_ID + ", " + 
			DBConstants.FIELD_SEARCH_FLAGS_TSR_CREATED + ", " + 
			DBConstants.FIELD_SEARCH_FLAGS_LEGAL_DESCR_STATUS + ", " + 
			DBConstants.FIELD_SEARCH_FLAGS_CHECKED_BY + ", " +
			DBConstants.FIELD_SEARCH_FLAGS_STARTER + ") VALUES (" + 
			 " ?, ?, ?, ?, ? )";
	*/
	private static final String SQL_SAVE_SEARCH_INSERT_FLAGS_SOURCE = "INSERT INTO " + 
			DBConstants.TABLE_SEARCH_FLAGS + " (" + 
			DBConstants.FIELD_SEARCH_FLAGS_ID + ", " + 
			DBConstants.FIELD_SEARCH_FLAGS_TSR_CREATED + ", " + 
			DBConstants.FIELD_SEARCH_FLAGS_LEGAL_DESCR_STATUS + ", " + 
			DBConstants.FIELD_SEARCH_FLAGS_CHECKED_BY + ", " +
			DBConstants.FIELD_SEARCH_FLAGS_STARTER + ", " +
			DBConstants.FIELD_SEARCH_FLAGS_LOG_ORIGINAL_LOCATION + ", " +
			DBConstants.FIELD_SEARCH_FLAGS_SOURCE_CREATION_TYPE + ") VALUES (" + 
			 " ?, ?, ?, ?, ?, ?, ? )";
	
	/**
	 * saves a search in the database
	 * @param sjt the jdbc template to be used when updating the database;
	 * @return
	 */
	private Object saveSearch(SimpleJdbcTemplate sjt) {
		
		if (search.getTSROrderDate() == null)
            search.setTSROrderDate(new Date(timestamp));
        
        String mysqlDate = fd.getDate(search.getTSROrderDate().getTime());
        String searchDate = "str_to_date( '" + mysqlDate + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
        SearchFlags searchFlags = search.getSearchFlags();
        try {
        	if(oldSearch!=null){
	            String sqlGetSdate = "SELECT SDATE FROM "+ TABLE_SEARCH +" WHERE ID = " + oldSearch.getSearchID();
	            searchDate = fd.getLocalDate(sjt.queryForObject(sqlGetSdate, Date.class));
	            searchDate = "str_to_date( '" + searchDate + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
        	}
        } catch (Exception e) {
        	logger.error("Error Caught in saveSearch", e);
        }
        
        String sql = "UPDATE "+ TABLE_SEARCH +" SET " + 
	    	"AGENT_ID = ?, " +
	    	"ABSTRACT_ID =?, " +
	    	"PROPERTY_ID =?, " +
	    	"AGENT_FILENO =?, " +
	    	"ABSTR_FILENO =?, " +
	    	"SEARCH_TYPE =?, " +
	    	"PAYRATE_ID =?, " +
	    	"SDATE=" + searchDate + ", " +
	    	DBConstants.FIELD_SEARCH_REPORTS_DATE + " = " + searchDate + ", " +
	    	"TSR_FILE_LINK=?, " +
	    	"TSR_SENT_TO =?, " +
	    	"USER_RATING_ID =?, " +
	    	"TSR_FOLDER= ?, " +
	    	"STATUS = ?, " +
	    	"COMM_ID=?, " +
	    	"NOTE_CLOB=?, " +	
	    	"NOTE_STATUS=" + -1 + ", " +
	    	"DUE_DATE = " + dueDate + ", " +
	    	"AGENT_RATING_ID= ?, "+
	    	DBConstants.FIELD_SEARCH_FILE_ID + " = ?, "+
	    	DBConstants.FIELD_SEARCH_SEC_ABSTRACT_ID + " = ?, "+
	    	DBConstants.FIELD_SEARCH_LAST_SAVE_DATE + " = now() " +
	    	" WHERE ID = ? ";
    
        logger.info("Logging current search " + sql + ".................." + " Params: " +agentId+ " " +abstractorId+ " " +propertyId+ " " +agentFileNo+ " " +abstractorFileNo+ " " +searchType+ " " +payrateId+ " " +tsrLink+ " " +tsrSentTo+ " " +userRatingId+ " " +
        		tsrFolder+ " " +status+ " " +commId+ " " +noteClob+ " " +agentRatingId+ " " +searchId );
        
        
     // sa evitam cazul in care ii schimbam ownerul direct, fara sa foloseasca operatia "Unlock"
        
        if(sjt.update(sql,agentId,abstractorId,propertyId,agentFileNo,abstractorFileNo,searchType,payrateId,tsrLink,tsrSentTo,userRatingId,
        		tsrFolder,status,commId,noteClob,agentRatingId,
        		getFileId(), getSearch().getSecondaryAbstractorId(),
        		searchId )==1){

        	//updating the ts_search_flags table
        	//if(!searchFlags.getCreationSourceType().equals(CREATION_SOURCE_TYPES.NORMAL)) {
    	        sjt.update(SQL_SAVE_SEARCH_UPDATE_FLAGS_SOURCE,
    	        		legalDescriptionStatus,getCheckedBy(),tsrCreated, 
    	        		searchFlags.isBase(), 
    	        		searchFlags.isClosed(), 
    	        		searchFlags.getCreationSourceTypeForDatabase(),
    	        		colorCodeStatus,
    	        		searchId);	
        	/*} else {
    	        sjt.update(SQL_SAVE_SEARCH_UPDATE_FLAGS,
    	        		legalDescriptionStatus,getCheckedBy(),tsrCreated, 
    	        		searchFlags.isStarter(), searchFlags.isClosed(), searchId);
        	}*/
	        
	        sql = "UPDATE " + DBConstants.TABLE_SEARCH_DATA_BLOB + " SET " + 
	        	DBConstants.FIELD_SEARCH_DATA_BLOB_LEGAL_DESCR + " = ? " +
	        			" WHERE " + 
	        	DBConstants.FIELD_SEARCH_DATA_BLOB_ID + " = ? ";
	        
	        //updating the ts_search_data_blob table
	        sjt.update(sql,legalDescription,searchId);
	        
        } else {
        	//the "Save" button was pressed
        	sql = "insert into "+ TABLE_SEARCH +" (ID, AGENT_ID, ABSTRACT_ID, PROPERTY_ID, " +
	            "AGENT_FILENO, ABSTR_FILENO, SEARCH_TYPE, PAYRATE_ID, SDATE, " + DBConstants.FIELD_SEARCH_REPORTS_DATE + ", " +
	            "TSR_FILE_LINK, TSR_SENT_TO, USER_RATING_ID, TSR_FOLDER, STATUS, " +
	            "COMM_ID, NOTE_CLOB, NOTE_STATUS, AGENT_RATING_ID, DUE_DATE, " + 
	            DBConstants.FIELD_SEARCH_FILE_ID + "," + DBConstants.FIELD_SEARCH_SEC_ABSTRACT_ID  + " ) " +
	            "VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, " + searchDate + ", " + searchDate + ", ?, ?, ?, ?, ?, ?, ?, 0, ?, DUE_DATE = " + dueDate  + ", ?, ? ) ";
			sjt.update(sql, 
					searchId, agentId, abstractorId, propertyId,
					agentFileNo, abstractorFileNo, searchType, payrateId,
					tsrLink, tsrSentTo, userRatingId, tsrFolder, status,
					commId, noteClob, agentRatingId, getFileId(), 
					getSearch().getSecondaryAbstractorId());
        	
        	//if(!searchFlags.getCreationSourceType().equals(CREATION_SOURCE_TYPES.NORMAL)) {
        		sjt.update(SQL_SAVE_SEARCH_INSERT_FLAGS_SOURCE,
        				searchId,tsrCreated,legalDescriptionStatus,getCheckedBy(), 
        				searchFlags.isBase(), 
        				searchFlags.getCreationSourceTypeForDatabase(), 
        				search.getSa().getLogOriginalLocation(),
        				colorCodeStatus);
        	/*} else {
        		sjt.update(SQL_SAVE_SEARCH_INSERT_FLAGS,
        				searchId,tsrCreated,legalDescriptionStatus,getCheckedBy(), 
        				searchFlags.isStarter());
        	}
        	*/
        	sql = "INSERT INTO " + DBConstants.TABLE_SEARCH_DATA_BLOB + " (" + 
        		DBConstants.FIELD_SEARCH_DATA_BLOB_ID + ", " + 
        		DBConstants.FIELD_SEARCH_DATA_BLOB_LEGAL_DESCR + ") VALUES (" + 
        		" ?, ? )";
        	sjt.update(sql,searchId,legalDescription);
        }
                
        return true;
	}

	/*
	private static final String SQL_UPDATE_SEARCH_UPDATE_FLAGS = "UPDATE " + 
			DBConstants.TABLE_SEARCH_FLAGS + " SET " + 
			DBConstants.FIELD_SEARCH_FLAGS_TSR_CREATED + " = ?, " +
			DBConstants.FIELD_SEARCH_FLAGS_STARTER + " = ?, " +
			DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED + " = ? " + 
			" WHERE " + 
			DBConstants.FIELD_SEARCH_FLAGS_ID + " = ?";
	*/
	private static final String SQL_UPDATE_SEARCH_UPDATE_FLAGS_SOURCE = "UPDATE " + 
			DBConstants.TABLE_SEARCH_FLAGS + " SET " + 
			DBConstants.FIELD_SEARCH_FLAGS_TSR_CREATED + " = ?, " +
			DBConstants.FIELD_SEARCH_FLAGS_STARTER + " = ?, " +
			DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED + " = ?, " +
			DBConstants.FIELD_SEARCH_FLAGS_SOURCE_CREATION_TYPE + " = ?, " +
			DBConstants.FIELD_SEARCH_FLAGS_COLOR_FLAG + " = ? " +
			" WHERE " + 
			DBConstants.FIELD_SEARCH_FLAGS_ID + " = ?";
	private Object updateSearch(SimpleJdbcTemplate sjt) {
		
		SearchFlags searchFlags = search.getSearchFlags();
		String sql = "update "+ TABLE_SEARCH +" set " +
                "AGENT_ID = ?, " +
                "ABSTRACT_ID = ?, " +
                "PROPERTY_ID = ?, " +
                "AGENT_FILENO = ?, " +
                "ABSTR_FILENO = ?, " +
                "SEARCH_TYPE = ?, " +
                "PAYRATE_ID = ?, " +
                "TSR_FILE_LINK = ?, " +
                "TSR_SENT_TO = ?, " +
                "USER_RATING_ID = ?, " +
                "AGENT_RATING_ID = ?, " +
                "TSR_FOLDER = ?, " +
                "DUE_DATE = " + dueDate + ", " +
                "STATUS = ?, " +
                DBConstants.FIELD_SEARCH_FILE_ID + " = ?, " +
                DBConstants.FIELD_SEARCH_TSRI_LINK + " = ?, " +
                DBConstants.FIELD_SEARCH_SEC_ABSTRACT_ID + " = ?, " +
                DBConstants.FIELD_SEARCH_LAST_SAVE_DATE + " = now() " +
                " where ID = ? ";
             
        logger.info("Logging current search " + sql + ".................." + " Params: " +agentId+ " " +abstractorId+ " " +
        		propertyId+ " " +agentFileNo+ " " +abstractorFileNo+ " " +
        		searchType+ " " +payrateId+ " " +tsrLink+ " " +tsrSentTo+ " " +userRatingId+ " " +
        		agentRatingId+ " " +tsrFolder+ " " +status+ " " +searchId + ".................., currentUser : " + 
                InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser().getID().longValue());
        
        if(sjt.update(sql, 
        		agentId,abstractorId,propertyId,agentFileNo,abstractorFileNo,
        		searchType,payrateId,tsrLink,tsrSentTo,userRatingId,
        		agentRatingId,tsrFolder,status,
        		getFileId(), 
        		getSearch().getTsriLink(),
        		getSearch().getSecondaryAbstractorId(),
        		searchId)==0)
        	logger.error("UNUSEFULL UPDATE_______________UNUSEFULL UPDATE nothing to update");
        //if(!searchFlags.getCreationSourceType().equals(CREATION_SOURCE_TYPES.NORMAL)) {
        	sjt.update(SQL_UPDATE_SEARCH_UPDATE_FLAGS_SOURCE, 
        			tsrCreated, 
        			searchFlags.isBase(), 
        			searchFlags.isClosed(), 
        			searchFlags.getCreationSourceTypeForDatabase(),
        			colorCodeStatus,
        			searchId);	
        /*} else {
        	sjt.update(SQL_UPDATE_SEARCH_UPDATE_FLAGS, tsrCreated, searchFlags.isStarter(), 
        			searchFlags.isClosed(), searchId);
        }
        */
        return true;
	}
	
	
	private static final String SQL_MUTUAL_EXCLUSION_VIOLATION_CHECK_USER = "SELECT c." +
			DBConstants.FIELD_SEARCH_FLAGS_CHECKED_BY + ", a." + 
			DBConstants.FIELD_SEARCH_ABSTRACT_ID + ", a." + 
			DBConstants.FIELD_SEARCH_SEC_ABSTRACT_ID + ", b." + 
			DBConstants.FIELD_USER_COMM_ID + " commIDUser, a." +
			DBConstants.FIELD_SEARCH_COMM_ID + " commIDSearch FROM "+ 
			
			DBConstants.TABLE_SEARCH +" a, " + 
			DBConstants.TABLE_USER + " b , " + 
			DBConstants.TABLE_SEARCH_FLAGS + " c WHERE a." +
			
			DBConstants.FIELD_SEARCH_ID  + "= ? AND b." + 
			DBConstants.FIELD_USER_ID + " = a." + 
			DBConstants.FIELD_SEARCH_AGENT_ID + " AND a.ID = c." + 
			DBConstants.FIELD_SEARCH_FLAGS_ID;
	
	private static final String SQL_MUTUAL_EXCLUSION_VIOLATION_CHECK_COMMUNITY = "SELECT b." +
			DBConstants.FIELD_USER_COMM_ID + " abstr_comm , a." +
			DBConstants.FIELD_SEARCH_COMM_ID + " currComm_id, a." +
			DBConstants.FIELD_SEARCH_STATUS + ", b." +
			DBConstants.FIELD_USER_ID + " dbAbstractorId FROM "+ 
			DBConstants.TABLE_SEARCH +" a, " +
			DBConstants.TABLE_USER + " b WHERE a.ID = ? AND b." +
			DBConstants.FIELD_USER_ID + " = a." + 
			DBConstants.FIELD_SEARCH_ABSTRACT_ID;
	
	/*
	private static final String SQL_MUTUAL_EXCLUSION_VIOLATION_CHECK_COMMUNITY_REOPEN = "SELECT b." +
			DBConstants.FIELD_USER_COMM_ID + " abstr_comm , a." +
			DBConstants.FIELD_SEARCH_COMM_ID + " currComm_id, a." +
			DBConstants.FIELD_SEARCH_STATUS + ", b." +
			DBConstants.FIELD_USER_ID + " dbAbstractorId FROM "+ 
			DBConstants.TABLE_SEARCH +" a, " +
			DBConstants.TABLE_USER + " b WHERE a.ID = ? AND b." +
			DBConstants.FIELD_USER_ID + " = a." + 
			DBConstants.FIELD_SEARCH_SEC_ABSTRACT_ID;
	*/
	
	/**
	 * Test if the search can be saved in the given condition
	 * If a mutual exclusion violation is found and <b>sendEmail</b> parameter is set, 
	 * 		an email will be sent to log the error
	 * @param sjt SimpleJdbcTemplate that allows running extra sql queries
	 * @param extraInformation that can be used when sending the email
	 * @param sendEmail flag that determines whether we send the email notification or not
	 * @return true if an error is detected
	 */
	private boolean mutualExclusionViolation(SimpleJdbcTemplate sjt, String extraInformation, boolean sendEmail) {
		
		List<Map<String, Object>> allData = sjt.queryForList(SQL_MUTUAL_EXCLUSION_VIOLATION_CHECK_USER, searchId);
				
		long currentWorkingAbstractor = abstractorId;
		
		long checkedById = -1;
        long dbOwnerId = -1;
        long agentCommunityId = -1;
        long currentSearchCommunityId = -1;
        boolean isSuperDuperAdmin = getAbstractorGroupId() == GroupAttributes.CCA_ID 
        		|| getAbstractorGroupId() == GroupAttributes.TA_ID;
        
        
        Map<String, Object> data = new HashMap<String, Object>();
        if(allData.size()>=1)
        	data = allData.get(0);
        
        if (data.get(DBConstants.FIELD_SEARCH_FLAGS_CHECKED_BY) != null) {
            checkedById = (Integer)data.get(DBConstants.FIELD_SEARCH_FLAGS_CHECKED_BY);
        }
        

        if (data.get(DBConstants.FIELD_SEARCH_ABSTRACT_ID) != null) {
            dbOwnerId = ((BigInteger)data.get(DBConstants.FIELD_SEARCH_ABSTRACT_ID)).longValue();
        }

       
        if (data.get("commIDUser") != null) {
            agentCommunityId = ((BigInteger)data.get("commIDUser")).longValue();
        }
        
        if (data.get("commIDSearch") != null) {
            currentSearchCommunityId = ((BigInteger)data.get("commIDSearch")).longValue();
        }


        allData = sjt.queryForList(SQL_MUTUAL_EXCLUSION_VIOLATION_CHECK_COMMUNITY, searchId);
        
     
        data = new HashMap<String, Object>();
        if(allData.size()>=1)
        	data = allData.get(0);
        
        long abstrComm_Id=-1;
        long currComm_Id=-1;         //currentSearchCommunityId may not be set here, which means -1
        long currStatus = DBConstants.SEARCH_NOT_SAVED;	//
        long dbAbstractorId = -1;
        
        if (data.get("abstr_comm") != null) {
        	abstrComm_Id = ((BigInteger)data.get("abstr_comm")).longValue();
        }
        
        if (data.get("currComm_id") != null) {
        	currComm_Id = ((BigInteger)data.get("currComm_id")).longValue();
        }
        if (data.get("status") != null){
        	currStatus = ((Integer)data.get("status")).longValue();
        }
        if (data.get("dbAbstractorId") != null) {
        	try {
        		dbAbstractorId = Long.parseLong(data.get("dbAbstractorId").toString());
        	} catch (Exception e) {
        		dbAbstractorId = -2;
			}
        }
        
        boolean agentCommunitySimilar = true;
        
        if( getAgentCommunityId() != null) {
        	agentCommunitySimilar = getAgentCommunityId().longValue() == commId;
        }
        
		
		if((( dbOwnerId == currentWorkingAbstractor && checkedById == 1 ) || checkedById < 1 ) && 
        		((abstrComm_Id == currComm_Id) || 
        			(currStatus == DBConstants.SEARCH_NOT_SAVED) || 
        			isSuperDuperAdmin ) && 
        		agentCommunitySimilar) {
			
			return false;
		} else {
			if(sendEmail){
				String errorMessage = extraInformation + "\n  checkedById = " + checkedById + "\n" + 
	            		" dbOwnerId = " + dbOwnerId +  "\n" + 
	            		" abstractorId = " + currentWorkingAbstractor + 
	            		" isSuperDuperAmind = " + isSuperDuperAdmin +  "\n" +  
	            		" agentCommID = " + agentCommunityId +  "\n" + 
	            		" currentSearchCommId = " + currentSearchCommunityId + "\n" + 
	            		" abstrComm_Id = "+abstrComm_Id + " currComm_Id = " + currComm_Id + "\n" + 
	            		" dbAbstractorId = " + dbAbstractorId + " currStatus = " + currStatus + "\n" + 
	            		" agentAbstractorCommunitySimilar = " + agentCommunitySimilar + "\n" +
	            		(agentCommunitySimilar?"":
	            		" agentAttributes.getId() = " + agentId + " ___" +
	            		" agentAttributes.getCOMMID() = " + getAgentCommunityId() + " ___ commId = " + commId);
				
				Log.sendEmail(MailConfig.getExceptionEmail(), "Mutual exclusion violation for search " + searchId + "!", 
	            		errorMessage );	
				System.err.println( errorMessage);
			}
			return true;
		}
	}

	private static final String SQL_GET_LEGAL_FOR_SEARCH = " SELECT " +
		DBConstants.FIELD_LEGAL_ID + ", " +
		DBConstants.FIELD_LEGAL_SEARCH_ID + ", " +
		DBConstants.FIELD_LEGAL_SUBDIVISION_ID + ", " +
		DBConstants.FIELD_LEGAL_FREEFORM + ", " +
		DBConstants.FIELD_LEGAL_TOWNSHIP_ID + ", " + 
		DBConstants.FIELD_LEGAL_APN + " FROM " + 
		DBConstants.TABLE_LEGAL + " WHERE " + 
		DBConstants.FIELD_LEGAL_SEARCH_ID + " = ?";

	private static final String SQL_INSERT_LEGAL = "INSERT INTO " + 
		DBConstants.TABLE_LEGAL + " ( " +
		DBConstants.FIELD_LEGAL_SEARCH_ID + ", " + 
		DBConstants.FIELD_LEGAL_SUBDIVISION_ID + ", " + 
		DBConstants.FIELD_LEGAL_TOWNSHIP_ID + ", " +
		DBConstants.FIELD_LEGAL_FREEFORM + ", " +
		DBConstants.FIELD_LEGAL_APN + " ) VALUES (?,?,?,?,?)";
	
	private static final String SQL_UPDATE_LEGAL_ID_IN_TS_SEARCH = "UPDATE " + 
		DBConstants.TABLE_SEARCH + " SET " + 
		DBConstants.FIELD_SEARCH_LEGAL_ID + " = ? WHERE " +
		DBConstants.FIELD_SEARCH_ID + " = ? ";
		
	private boolean updateLegal(SimpleJdbcTemplate sjt){
		
		LegalMapper legalMapper = null;
		try {
			legalMapper = sjt.queryForObject(SQL_GET_LEGAL_FOR_SEARCH, new LegalMapper(), searchId);
		} catch (IncorrectResultSizeDataAccessException e) {
			//no legal in the database so we need to insert it
			//first create the subdivision
			long subdivisionId = insertSubdivision(sjt);
			//then create the township
			long townshipId = insertTownship(sjt);
			//now let's insert the legal in the database
			KeyHolder generatedKeyHolder = new GeneratedKeyHolder();
			PreparedStatementCreatorFactory pstat = new PreparedStatementCreatorFactory(SQL_INSERT_LEGAL);
			pstat.setReturnGeneratedKeys(true);
			pstat.addParameter(new SqlParameter(DBConstants.FIELD_LEGAL_SEARCH_ID,Types.INTEGER));
			pstat.addParameter(new SqlParameter(DBConstants.FIELD_LEGAL_SUBDIVISION_ID,Types.INTEGER));
			pstat.addParameter(new SqlParameter(DBConstants.FIELD_LEGAL_TOWNSHIP_ID,Types.INTEGER));
			pstat.addParameter(new SqlParameter(DBConstants.FIELD_LEGAL_FREEFORM,Types.VARCHAR));
			pstat.addParameter(new SqlParameter(DBConstants.FIELD_LEGAL_APN, Types.VARCHAR));
			
			Object[] params = new Object[]{ 
					search.getID(),
					subdivisionId,
					townshipId,
					search.getLegal().getFreeForm(),
					search.getSa().getAtribute(SearchAttributes.LD_PARCELNO)
			};
			
			sjt.getJdbcOperations().update(
					pstat.newPreparedStatementCreator(params), 
					generatedKeyHolder);
			
			legalMapper = new LegalMapper();
			legalMapper.setLegalId( generatedKeyHolder.getKey().longValue());
			legalMapper.setSubdivisionId(subdivisionId);
			legalMapper.setTownshipId(townshipId);
			legalMapper.setSearchId(searchId);
			legalMapper.setFreeForm(search.getLegal().getFreeForm());
			legalMapper.setApn(search.getSa().getAtribute(SearchAttributes.LD_PARCELNO));
			
			//finally let's update the ts_search table with the lotId
			sjt.update(SQL_UPDATE_LEGAL_ID_IN_TS_SEARCH, legalMapper.getLegalId(), searchId);
			
		}
		
		if(legalMapper == null) {
			return false;
		}
			
		updateSubdivision(sjt, legalMapper);
		updateLot(sjt);
		updateTownship(sjt, legalMapper);
		
		return true;
	}
	
	private static final String SQL_UPDATE_SUBDIVISION = " UPDATE " + 
		DBConstants.TABLE_SUBDIVISION + " SET " + 
		DBConstants.FIELD_SUBDIVISION_NAME + " = ?, " + 
		DBConstants.FIELD_SUBDIVISION_LOT + " = ?, " + 
		DBConstants.FIELD_SUBDIVISION_BLOCK + " = ?, " +
		DBConstants.FIELD_SUBDIVISION_PHASE + " = ?, " +
		DBConstants.FIELD_SUBDIVISION_TRACT + " = ?, " + 
		DBConstants.FIELD_SUBDIVISION_UNIT + " = ? WHERE " +
		DBConstants.FIELD_SUBDIVISION_ID + " = ? ";
	private void updateSubdivision(SimpleJdbcTemplate sjt, LegalMapper legalMapper) {
		Subdivision subdivision = (Subdivision)search.getLegal().getSubdivision(); 
				
		sjt.update(SQL_UPDATE_SUBDIVISION, subdivision.getName(),
				subdivision.getLot(),
				subdivision.getBlock(),
				subdivision.getPhase(),
				subdivision.getTract(),
				subdivision.getUnit(),
				legalMapper.getSubdivisionId());
		
	}


	private static final String SQL_INSERT_SUBDIVISION = " INSERT INTO " + 
		DBConstants.TABLE_SUBDIVISION + " (" + 
		DBConstants.FIELD_SUBDIVISION_NAME + ", " + 
		DBConstants.FIELD_SUBDIVISION_LOT + ", " + 
		DBConstants.FIELD_SUBDIVISION_BLOCK + ", " +
		DBConstants.FIELD_SUBDIVISION_PHASE + ", " +
		DBConstants.FIELD_SUBDIVISION_TRACT + ", " + 
		DBConstants.FIELD_SUBDIVISION_UNIT + ") VALUES (?,?,?,?,?,?)";
	/**
	 * Inserts a new subdivision in the subdivision table
	 * @param sjt
	 * @return
	 */
	private long insertSubdivision(SimpleJdbcTemplate sjt) {
		KeyHolder subdivisionGeneratedKeyHolder = new GeneratedKeyHolder();
		
		PreparedStatementCreatorFactory subdivisionPstat = new PreparedStatementCreatorFactory(SQL_INSERT_SUBDIVISION);
		subdivisionPstat.setReturnGeneratedKeys(true);
		subdivisionPstat.addParameter(new SqlParameter(DBConstants.FIELD_SUBDIVISION_NAME,Types.VARCHAR));
		subdivisionPstat.addParameter(new SqlParameter(DBConstants.FIELD_SUBDIVISION_LOT,Types.VARCHAR));
		subdivisionPstat.addParameter(new SqlParameter(DBConstants.FIELD_SUBDIVISION_BLOCK,Types.VARCHAR));
		subdivisionPstat.addParameter(new SqlParameter(DBConstants.FIELD_SUBDIVISION_PHASE,Types.VARCHAR));
		subdivisionPstat.addParameter(new SqlParameter(DBConstants.FIELD_SUBDIVISION_TRACT,Types.VARCHAR));
		subdivisionPstat.addParameter(new SqlParameter(DBConstants.FIELD_SUBDIVISION_UNIT,Types.VARCHAR));
		
		Subdivision subdivision = (Subdivision)search.getLegal().getSubdivision(); 
		
		Object[] params = new Object[]{ 
				subdivision.getName(),
				subdivision.getLot(),
				subdivision.getBlock(),
				subdivision.getPhase(),
				subdivision.getTract(),
				subdivision.getUnit()
		};
		
		sjt.getJdbcOperations().update(
				subdivisionPstat.newPreparedStatementCreator(params), 
				subdivisionGeneratedKeyHolder);
		
		return subdivisionGeneratedKeyHolder.getKey().longValue();
	}
	
	private static final String SQL_INSERT_TOWNSHIP = "INSERT INTO " +
		DBConstants.TABLE_TOWNSHIP + " ( " + 
		DBConstants.FIELD_TOWNSHIP_SECTION + ", " + 
		DBConstants.FIELD_TOWNSHIP_RANGE + ", " + 
		DBConstants.FIELD_TOWNSHIP_TOWNSHIP + ") VALUES (?,?,?) ";
	/**
	 * Inserts a new subdivision in the subdivision table
	 * @param sjt
	 * @return
	 */
	private long insertTownship(SimpleJdbcTemplate sjt) {
		KeyHolder generatedKeyHolder = new GeneratedKeyHolder();
		PreparedStatementCreatorFactory subdivisionPstat = new PreparedStatementCreatorFactory(SQL_INSERT_TOWNSHIP);
		subdivisionPstat.setReturnGeneratedKeys(true);
		subdivisionPstat.addParameter(new SqlParameter(DBConstants.FIELD_TOWNSHIP_SECTION, Types.VARCHAR));
		subdivisionPstat.addParameter(new SqlParameter(DBConstants.FIELD_TOWNSHIP_RANGE, Types.VARCHAR));
		subdivisionPstat.addParameter(new SqlParameter(DBConstants.FIELD_TOWNSHIP_TOWNSHIP, Types.VARCHAR));
		
		TownShip township = (TownShip)search.getLegal().getTownShip(); 
		
		Object[] params = new Object[]{ 
				township.getSection(),
				township.getRange(),
				township.getTownship()
		};
		
		sjt.getJdbcOperations().update(
				subdivisionPstat.newPreparedStatementCreator(params), 
				generatedKeyHolder);
		
		return generatedKeyHolder.getKey().longValue();
	}

	private static String SQL_GET_LOTS = "SELECT * FROM " + DBConstants.TABLE_LEGAL_LOT + 
		" WHERE " + DBConstants.FIELD_LEGAL_LOT_SEARCH_ID + " = ?";
	private static String SQL_INSERT_LOT = "INSERT INTO " + DBConstants.TABLE_LEGAL_LOT + 
		" ( " + DBConstants.FIELD_LEGAL_LOT_SEARCH_ID + ", " + DBConstants.FIELD_LEGAL_LOT_VALUE + 
		" ) VALUES (?, ?)"; 
	private static String SQL_UPDATE_LOT = "UPDATE " + DBConstants.TABLE_LEGAL_LOT + 
		" SET " + DBConstants.FIELD_LEGAL_LOT_VALUE + " = ? WHERE " + 
		DBConstants.FIELD_LEGAL_LOT_ID + " = ?";
	private static String SQL_DELETE_LOTS = "DELETE FROM " + DBConstants.TABLE_LEGAL_LOT + 
		" WHERE " + DBConstants.FIELD_LEGAL_LOT_ID + " IN ( ? )";
	/**
	 * The strategy is the following:
	 * - get all lots that must be entered in the database
	 * - real all lots currently in the database
	 * - determine which elements should be deleted
	 * - determine which elements should be updated
	 * - determine which elements should be inserted
	 * The strategy wants to minimize sql queries so the old values 
	 * will be updated if possible not just deleted and only after 
	 * all are reused we will insert more if needed 
	 * @param sjt the template used for the queries
	 * @return
	 */
	private boolean updateLot(SimpleJdbcTemplate sjt){
		Legal legal = search.getLegal();
		HashSet<String> expandedLot = legal.getSubdivision().getLotExpanded();
		List<SearchLegalLotBean> dbLots = sjt.query(
				SQL_GET_LOTS, new SearchLegalLotBean(), search.getID());
		List<SearchLegalLotBean> toBeDeleted = new ArrayList<SearchLegalLotBean>();
		int rowsAffected = 0;
		for (SearchLegalLotBean dbLot : dbLots) {
			if(!expandedLot.remove(dbLot.getLotValue())){
				toBeDeleted.add(dbLot);
			}
		}
		for (String newLot : expandedLot) {
			if(toBeDeleted.size() > 0){
				SearchLegalLotBean updateMe = toBeDeleted.remove(0);
				rowsAffected = sjt.update(SQL_UPDATE_LOT, newLot, updateMe.getLegalLotId());
				if(rowsAffected == 0)	//backup, if nothing to update, then insert
					sjt.update(SQL_INSERT_LOT, searchId, newLot);
			} else 
				sjt.update(SQL_INSERT_LOT, searchId, newLot);
		}
		String deleteThem = "";
		for (SearchLegalLotBean lotBean : toBeDeleted) {
			deleteThem += lotBean.getLegalLotId() + ",";
		}
		if(!deleteThem.isEmpty()){
			sjt.update(SQL_DELETE_LOTS, deleteThem.substring(0,deleteThem.length()-1));
		}
		
		return true;
	}
	
	private static final String SQL_UPDATE_TOWHSHIP = "UPDATE " + DBConstants.TABLE_TOWNSHIP + " SET " + 
			DBConstants.FIELD_TOWNSHIP_SECTION + " = ?, " + 
			DBConstants.FIELD_TOWNSHIP_TOWNSHIP + " = ?, " +
			DBConstants.FIELD_TOWNSHIP_RANGE + " = ? WHERE " +
			DBConstants.FIELD_TOWNSHIP_ID + " = ? ";
	private boolean updateTownship(SimpleJdbcTemplate sjt, LegalMapper legalMapper){
		TownShip township = (TownShip)search.getLegal().getTownShip(); 
		sjt.update(SQL_UPDATE_TOWHSHIP, township.getSection(), township.getTownship(), township.getRange(), legalMapper.getTownshipId());
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SaveSearchTransaction [searchId=");
		builder.append(searchId);
		builder.append(", agentId=");
		builder.append(agentId);
		builder.append(", abstractorId=");
		builder.append(abstractorId);
		builder.append(", propertyId=");
		builder.append(propertyId);
		builder.append(", commId=");
		builder.append(commId);
		builder.append(", payrateId=");
		builder.append(payrateId);
		builder.append(", userRatingId=");
		builder.append(userRatingId);
		builder.append(", agentRatingId=");
		builder.append(agentRatingId);
		builder.append(", timestamp=");
		builder.append(timestamp);
		builder.append(", agentFileNo=");
		builder.append(agentFileNo);
		builder.append(", abstractorFileNo=");
		builder.append(abstractorFileNo);
		builder.append(", sDate=");
		builder.append(sDate);
		builder.append(", tsrDate=");
		builder.append(tsrDate);
		builder.append(", tsrLink=");
		builder.append(tsrLink);
		builder.append(", tsrSentTo=");
		builder.append(tsrSentTo);
		builder.append(", tsrFolder=");
		builder.append(tsrFolder);
		builder.append(", dueDate=");
		builder.append(dueDate);
		builder.append(", searchType=");
		builder.append(searchType);
		builder.append(", status=");
		builder.append(status);
		builder.append(", tsrCreated=");
		builder.append(tsrCreated);
		builder.append(", noteStatus=");
		builder.append(noteStatus);
		builder.append(", legalDescriptionStatus=");
		builder.append(legalDescriptionStatus);
		builder.append(", checkedBy=");
		builder.append(checkedBy);
		if(oldSearch != null) {
			builder.append(", oldSearchId=");
			builder.append(oldSearch.getID());
		}
		
		builder.append("]");
		return builder.toString();
	}

	public void setAbstractorGroupId(int abstractorGroupId) {
		this.abstractorGroupId  = abstractorGroupId;
	}

	public int getAbstractorGroupId() {
		return abstractorGroupId;
	}
	
}
