package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.utils.DBConstants;

public class LoadInfoMapper implements ParameterizedRowMapper<LoadInfoMapper> {
	
	private Long id;
    private Float cpu;
    private Float memory;
    private Float network;
    private Date timestamp;
    private Long serverConn;
    private Long dbConn;
    private String serverName;
    private Float memoryFree;
    private Float loadFactor;
    private Integer type;
	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * @return the cpu
	 */
	public Float getCpu() {
		return cpu;
	}
	/**
	 * @param cpu the cpu to set
	 */
	public void setCpu(Float cpu) {
		this.cpu = cpu;
	}
	/**
	 * @return the memory
	 */
	public Float getMemory() {
		return memory;
	}
	/**
	 * @param memory the memory to set
	 */
	public void setMemory(Float memory) {
		this.memory = memory;
	}
	/**
	 * @return the network
	 */
	public Float getNetwork() {
		return network;
	}
	/**
	 * @param network the network to set
	 */
	public void setNetwork(Float network) {
		this.network = network;
	}
	/**
	 * @return the timestamp
	 */
	public Date getTimestamp() {
		return timestamp;
	}
	/**
	 * @param timestamp the timestamp to set
	 */
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	/**
	 * @return the serverConn
	 */
	public Long getServerConn() {
		return serverConn;
	}
	/**
	 * @param serverConn the serverConn to set
	 */
	public void setServerConn(Long serverConn) {
		this.serverConn = serverConn;
	}
	/**
	 * @return the dbConn
	 */
	public Long getDbConn() {
		return dbConn;
	}
	/**
	 * @param dbConn the dbConn to set
	 */
	public void setDbConn(Long dbConn) {
		this.dbConn = dbConn;
	}
	/**
	 * @return the serverName
	 */
	public String getServerName() {
		return serverName;
	}
	/**
	 * @param serverName the serverName to set
	 */
	public void setServerName(String serverName) {
		this.serverName = serverName;
	}
	/**
	 * @return the memoryFree
	 */
	public Float getMemoryFree() {
		return memoryFree;
	}
	/**
	 * @param memoryFree the memoryFree to set
	 */
	public void setMemoryFree(Float memoryFree) {
		this.memoryFree = memoryFree;
	}
	/**
	 * @return the loadFactor
	 */
	public Float getLoadFactor() {
		return loadFactor;
	}
	/**
	 * @param loadFactor the loadFactor to set
	 */
	public void setLoadFactor(Float loadFactor) {
		this.loadFactor = loadFactor;
	}
	/**
	 * @return the type
	 */
	public Integer getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(Integer type) {
		this.type = type;
	}
	@Override
	public LoadInfoMapper mapRow(ResultSet rs, int rownum) throws SQLException {
		LoadInfoMapper lim = new LoadInfoMapper();
		try {
			lim.setId((Long)rs.getObject(DBConstants.FIELD_LOAD_INFO_ID));
		} catch (Exception e) {}
		try {
			lim.setCpu(rs.getFloat(DBConstants.FIELD_LOAD_INFO_CPU));
		} catch (Exception e) {}
		try {
			lim.setMemory(rs.getFloat(DBConstants.FIELD_LOAD_INFO_MEMORY));
		} catch (Exception e) {}
		try {
			lim.setNetwork(rs.getFloat(DBConstants.FIELD_LOAD_INFO_NETWORK));
		} catch (Exception e) {}
		try {
			lim.setTimestamp((Date)rs.getObject(DBConstants.FIELD_LOAD_INFO_TIMESTAMP));
		} catch (Exception e) {}
		try {
			lim.setServerConn((Long)rs.getObject(DBConstants.FIELD_LOAD_INFO_SERVERCONN));
		} catch (Exception e) {}
		try {
			lim.setDbConn((Long)rs.getObject(DBConstants.FIELD_LOAD_INFO_DBCONN));
		} catch (Exception e) {}
		try {
			lim.setServerName((String)rs.getObject(DBConstants.FIELD_LOAD_INFO_SERVER_NAME));
		} catch (Exception e) {}
		try {
			lim.setMemoryFree(rs.getFloat(DBConstants.FIELD_LOAD_INFO_MEMORY_FREE));
		} catch (Exception e) {}
		try {
			lim.setLoadFactor(rs.getFloat(DBConstants.FIELD_LOAD_INFO_LOAD_FACTOR));
		} catch (Exception e) {}
		try {
			lim.setType((Integer)rs.getObject(DBConstants.FIELD_LOAD_INFO_TYPE));
		} catch (Exception e) {}

		return lim;
	}
	/**
	 * 
	 * @return the string representation of the object 
	 */
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("LoadInfoMapper[");
		buffer.append("cpu = ").append(cpu);
		buffer.append(" dbConn = ").append(dbConn);
		buffer.append(" id = ").append(id);
		buffer.append(" loadFactor = ").append(loadFactor);
		buffer.append(" memory = ").append(memory);
		buffer.append(" memoryFree = ").append(memoryFree);
		buffer.append(" network = ").append(network);
		buffer.append(" serverConn = ").append(serverConn);
		buffer.append(" serverName = ").append(serverName);
		buffer.append(" timestamp = ").append(timestamp);
		buffer.append(" type = ").append(type);
		buffer.append("]");
		return buffer.toString();
	}

}
