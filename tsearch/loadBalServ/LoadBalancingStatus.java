package ro.cst.tsearch.loadBalServ;

import org.springframework.dao.DataAccessException;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.utils.DBConstants;

public class LoadBalancingStatus {

	public static final float BAD_LOAD			=	-1;
	private boolean enabledLoadAlgorithm		=	false;
	private boolean enabledSourceAlgorithm		=	false;
	private String defaultDestination			=	"";
	private boolean enableOverrideDestination	=	false;
	
	public LoadBalancingStatus(){
		String sql;
		DBConnection conn = null;
		
		try {
			sql = "SELECT * FROM " + DBConstants.TABLE_SERVER + " LIMIT 1 ";
			conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData data = conn.executeSQL(sql);
			if(data.getRowNumber() > 0){
				defaultDestination = (String)data.getValue(DBConstants.FIELD_SERVER_DEFAULT, 0);
				if((Integer)data.getValue(DBConstants.FIELD_SERVER_OVERRIDE_DESTINATION, 0) == 1) {
					enableOverrideDestination = true;
				}
			}
			
			sql = "SELECT * FROM " + DBConstants.TABLE_CONFIGS;
			data = conn.executeSQL(sql);
			for (int i = 0; i < data.getRowNumber(); i++) {
				String name = (String)data.getValue(DBConstants.FIELD_CONFIGS_NAME, i);
				String value = (String)data.getValue(DBConstants.FIELD_CONFIGS_VALUE, i);
				if(name.equalsIgnoreCase("lbs.enable.load.alg")){
					try {
						if(Integer.parseInt(value)==0)
							setEnabledLoadAlgorithm(false);
						else
							setEnabledLoadAlgorithm(true);
					} catch (Exception e) {
						setEnabledLoadAlgorithm(false);
						e.printStackTrace();
					}
				} else if(name.equalsIgnoreCase("lbs.enable.source.alg")){
					try {
						if(Integer.parseInt(value)==0)
							setEnabledSourceAlgorithm(false);
						else
							setEnabledSourceAlgorithm(true);
					} catch (Exception e) {
						setEnabledSourceAlgorithm(false);
						e.printStackTrace();
					}
				}
			}
			
		}
		catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
	        	ConnectionPool.getInstance().releaseConnection(conn);
	        } catch (BaseException e) {
	        	e.printStackTrace();
	        }
		}
	}
	public boolean isEnabledLoadAlgorithm(){
		return enabledLoadAlgorithm;
	}
	public boolean isEnabledSourceAlgorithm(){
		return enabledSourceAlgorithm;
	}
	public String getDefaultDestination() {
		return defaultDestination;
	}
	public void setDefaultDestination(String defaultDestination) {
		this.defaultDestination = defaultDestination;
	}
	public void setEnabledLoadAlgorithm(boolean enabledLoadAlgorithm) {
		this.enabledLoadAlgorithm = enabledLoadAlgorithm;
	}
	public void setEnabledSourceAlgorithm(boolean enabledSourceAlgorithm) {
		this.enabledSourceAlgorithm = enabledSourceAlgorithm;
	}
	/**
	 * @return the enableOverrideDestination
	 */
	public boolean isEnableOverrideDestination() {
		return enableOverrideDestination;
	}
	/**
	 * @param enableOverrideDestination the enableOverrideDestination to set
	 */
	public void setEnableOverrideDestination(boolean enableOverrideDestination) {
		this.enableOverrideDestination = enableOverrideDestination;
	}
	
	/**
	 * Checks if the current server is enabled from the interface 
	 * @return
	 */
	public static boolean isCurrentServerEnabled() {
		try {
			return DBManager.getSimpleTemplate().queryForInt(
					"select ifnull(sum(enabled), 0) from ts_server where id = ?", 
					ServerConfig.getServerId()) > 0;
		} catch (DataAccessException e) {
			DBManager.getLogger().error("Cannot determine if I am enabled on not", e);
			return false;
		}
	}
	
}
