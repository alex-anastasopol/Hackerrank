package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.utils.DBConstants;

public class ModuleMapper implements ParameterizedRowMapper<ModuleMapper> {
	
	private long moduleId;
	private int serverId;
	private int searchModuleId;
	private String description;
	private long searchId;

	public long getModuleId() {
		return moduleId;
	}

	public void setModuleId(long moduleId) {
		this.moduleId = moduleId;
	}

	public int getServerId() {
		return serverId;
	}

	public void setServerId(int serverId) {
		this.serverId = serverId;
	}

	public int getSearchModuleId() {
		return searchModuleId;
	}

	public void setSearchModuleId(int searchModuleId) {
		this.searchModuleId = searchModuleId;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public long getSearchId() {
		return searchId;
	}

	public void setSearchId(long searchId) {
		this.searchId = searchId;
	}

	@Override
	public ModuleMapper mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		ModuleMapper moduleMapper = new ModuleMapper();
		moduleMapper.setModuleId(resultSet.getLong(DBConstants.FIELD_MODULE_ID));
		moduleMapper.setSearchId(resultSet.getLong(DBConstants.FIELD_MODULE_SEARCH_ID));
		moduleMapper.setServerId(resultSet.getInt(DBConstants.FIELD_MODULE_SERVER_ID));
		moduleMapper.setSearchModuleId(resultSet.getInt(DBConstants.FIELD_MODULE_SEARCH_MODULE_ID));
		moduleMapper.setDescription(resultSet.getString(DBConstants.FIELD_MODULE_DESCRIPTION));
		return moduleMapper;
	}
	
	private static final String SQL_INSERT_MODULE	= 
		"INSERT INTO " + DBConstants.TABLE_MODULES + " (" + 
			DBConstants.FIELD_MODULE_SEARCH_ID + ", " +
			DBConstants.FIELD_MODULE_SEARCH_MODULE_ID + ", " +
			DBConstants.FIELD_MODULE_SERVER_ID + ", " +
			DBConstants.FIELD_MODULE_DESCRIPTION + " ) VALUES ( ?,?,?,? ) ";
	public boolean saveInDatabase(SimpleJdbcTemplate sjt) {
		Object[] params = new Object[]{ 
	    		getSearchId(),
	    		getSearchModuleId(),
	    		getServerId(),
	    		getDescription()
	    };
	    
	    PreparedStatementCreatorFactory pstat = new PreparedStatementCreatorFactory(SQL_INSERT_MODULE);
	    pstat.setReturnGeneratedKeys(true);
	    pstat.addParameter(new SqlParameter(DBConstants.FIELD_MODULE_SEARCH_ID,Types.BIGINT));
	    pstat.addParameter(new SqlParameter(DBConstants.FIELD_MODULE_SEARCH_MODULE_ID,Types.INTEGER));
	    pstat.addParameter(new SqlParameter(DBConstants.FIELD_MODULE_SERVER_ID,Types.INTEGER));
	    pstat.addParameter(new SqlParameter(DBConstants.FIELD_MODULE_DESCRIPTION,Types.VARCHAR));
	    
	    KeyHolder generatedKeyHolder = new GeneratedKeyHolder();
	    
		if(sjt.getJdbcOperations().update(pstat.newPreparedStatementCreator(params),generatedKeyHolder)!=1){
			//if the insert did not affect just one row we have an error
			return false;
		}
		setModuleId(generatedKeyHolder.getKey().longValue());
		return true;
		
	}
	
	
	private static final String SQL_GET_MODULES_FOR_SEARCH = 
		"SELECT * FROM " + DBConstants.TABLE_MODULES 
			+ " WHERE " + DBConstants.FIELD_MODULE_SEARCH_ID + " = ? "
			+ " ORDER BY " + DBConstants.FIELD_MODULE_ID + " ASC ";
	/**
	 * Loads all the search modules (description) for invalidated documents
	 * @param searchId the id of the search for which the modules are loaded
	 * @return a list with all modules ordered by entry time
	 */
	public static List<ModuleMapper> getModulesForSearch(long searchId) {
		return DBManager.getSimpleTemplate().query(SQL_GET_MODULES_FOR_SEARCH, new ModuleMapper(), searchId);
		
	}

}
