package ro.cst.tsearch.database.rowmapper;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

public class SlaveStatusMapper implements ParameterizedRowMapper<SlaveStatusMapper> {

	private String slaveIOState;
	private String masterHost;
	private String masterUser;
	private Long masterPort;
	private Long connectRetry;
	private String masterLogFile;
	private BigInteger readMasterLogPos;
	private String relayLogFile;
	private BigInteger relayLogPos;
	private String relayMasterLogFile;
	private String slaveIORunning;
	private String slaveSQLRunning;
	private String replicateDoDB;
	private String replicateIgnoreDB;
	private String replicateDoTable;
	private String replicateIgnoreTable;
	private String replicateWildDoTable;
	private String replicateWildIgnoreTable;
	private Long lastErrno;
	private String lastError;
	private Long skipCounter;
	private BigInteger execMasterLogPos;
	private String relayLogSpace;
	private String untilCondition;
	private String untilLogFile;
	private BigInteger untilLogPos;
	private String masterSSLAllowed;
	private String masterSSLCAFile;
	private String masterSSLCAPath;
	private String masterSSLCert;
	private String masterSSLCipher;
	private String masterSSLKey;
	private BigInteger secondsBehindMaster;
	
	public SlaveStatusMapper(){
		
	}
	
	public SlaveStatusMapper(List<Map<String, Object>> result) {
		try {
			setSlaveIOState((String)result.get(0).get("Slave_IO_State"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setMasterHost((String)result.get(0).get("Master_Host"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setMasterUser((String)result.get(0).get("Master_User"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setMasterPort((Long)result.get(0).get("Master_Port"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setConnectRetry((Long)result.get(0).get("Connect_Retry"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setMasterLogFile((String)result.get(0).get("Master_Log_File"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setReadMasterLogPos((BigInteger)result.get(0).get("Read_Master_Log_Pos"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setRelayLogFile((String)result.get(0).get("Relay_Log_File"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setRelayLogPos((BigInteger)result.get(0).get("Relay_Log_Pos"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setRelayMasterLogFile((String)result.get(0).get("Relay_Master_Log_File"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setSlaveIORunning((String)result.get(0).get("Slave_IO_Running"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setSlaveSQLRunning((String)result.get(0).get("Slave_SQL_Running"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setReplicateDoDB((String)result.get(0).get("Replicate_Do_DB"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setReplicateIgnoreDB((String)result.get(0).get("Replicate_Ignore_DB"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setReplicateDoTable((String)result.get(0).get("Replicate_Do_Table"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setReplicateIgnoreTable((String)result.get(0).get("Replicate_Ignore_Table"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setReplicateWildDoTable((String)result.get(0).get("Replicate_Wild_Do_Table"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setReplicateWildIgnoreTable((String)result.get(0).get("Replicate_Wild_Ignore_Table"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setLastErrno((Long)result.get(0).get("Last_Errno"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setLastError((String)result.get(0).get("Last_Error"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setSkipCounter((Long)result.get(0).get("Skip_Counter"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setExecMasterLogPos((BigInteger)result.get(0).get("Exec_Master_Log_Pos"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setRelayLogPos((BigInteger)result.get(0).get("Relay_Log_Space"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setUntilCondition((String)result.get(0).get("Until_Condition"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setUntilLogFile((String)result.get(0).get("Until_Log_File"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setUntilLogPos((BigInteger)result.get(0).get("Until_Log_Pos"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setMasterSSLAllowed((String)result.get(0).get("Master_SSL_Allowed"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setMasterSSLCAFile((String)result.get(0).get("Master_SSL_CA_File"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setMasterSSLCAPath((String)result.get(0).get("Master_SSL_CA_Path"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setMasterSSLCert((String)result.get(0).get("Master_SSL_Cert"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setMasterSSLCipher((String)result.get(0).get("Master_SSL_Cipher"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			setMasterSSLKey((String)result.get(0).get("Master_SSL_Key"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {	
			setSecondsBehindMaster((BigInteger)result.get(0).get("Seconds_Behind_Master"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}

	/**
	 * @return the slaveIOState
	 */
	public String getSlaveIOState() {
		return slaveIOState;
	}

	/**
	 * @param slaveIOState the slaveIOState to set
	 */
	public void setSlaveIOState(String slaveIOState) {
		this.slaveIOState = slaveIOState;
	}

	/**
	 * @return the masterHost
	 */
	public String getMasterHost() {
		return masterHost;
	}

	/**
	 * @param masterHost the masterHost to set
	 */
	public void setMasterHost(String masterHost) {
		this.masterHost = masterHost;
	}

	/**
	 * @return the masterUser
	 */
	public String getMasterUser() {
		return masterUser;
	}

	/**
	 * @param masterUser the masterUser to set
	 */
	public void setMasterUser(String masterUser) {
		this.masterUser = masterUser;
	}

	/**
	 * @return the masterPort
	 */
	public long getMasterPort() {
		return masterPort;
	}

	/**
	 * @param masterPort the masterPort to set
	 */
	public void setMasterPort(long masterPort) {
		this.masterPort = masterPort;
	}

	/**
	 * @return the connectRetry
	 */
	public Long getConnectRetry() {
		return connectRetry;
	}

	/**
	 * @param connectRetry the connectRetry to set
	 */
	public void setConnectRetry(Long connectRetry) {
		this.connectRetry = connectRetry;
	}

	/**
	 * @return the masterLogFile
	 */
	public String getMasterLogFile() {
		return masterLogFile;
	}

	/**
	 * @param masterLogFile the masterLogFile to set
	 */
	public void setMasterLogFile(String masterLogFile) {
		this.masterLogFile = masterLogFile;
	}

	/**
	 * @return the readMasterLogPos
	 */
	public BigInteger getReadMasterLogPos() {
		return readMasterLogPos;
	}

	/**
	 * @param readMasterLogPos the readMasterLogPos to set
	 */
	public void setReadMasterLogPos(BigInteger readMasterLogPos) {
		this.readMasterLogPos = readMasterLogPos;
	}

	/**
	 * @return the relayLogFile
	 */
	public String getRelayLogFile() {
		return relayLogFile;
	}

	/**
	 * @param relayLogFile the relayLogFile to set
	 */
	public void setRelayLogFile(String relayLogFile) {
		this.relayLogFile = relayLogFile;
	}

	/**
	 * @return the relayLogPos
	 */
	public BigInteger getRelayLogPos() {
		return relayLogPos;
	}

	/**
	 * @param relayLogPos the relayLogPos to set
	 */
	public void setRelayLogPos(BigInteger relayLogPos) {
		this.relayLogPos = relayLogPos;
	}

	/**
	 * @return the relayMasterLogFile
	 */
	public String getRelayMasterLogFile() {
		return relayMasterLogFile;
	}

	/**
	 * @param relayMasterLogFile the relayMasterLogFile to set
	 */
	public void setRelayMasterLogFile(String relayMasterLogFile) {
		this.relayMasterLogFile = relayMasterLogFile;
	}

	/**
	 * @return the slaveIORunning
	 */
	public String getSlaveIORunning() {
		return slaveIORunning;
	}

	/**
	 * @param slaveIORunning the slaveIORunning to set
	 */
	public void setSlaveIORunning(String slaveIORunning) {
		this.slaveIORunning = slaveIORunning;
	}

	/**
	 * @return the slaveSQLRunning
	 */
	public String getSlaveSQLRunning() {
		return slaveSQLRunning;
	}

	/**
	 * @param slaveSQLRunning the slaveSQLRunning to set
	 */
	public void setSlaveSQLRunning(String slaveSQLRunning) {
		this.slaveSQLRunning = slaveSQLRunning;
	}

	/**
	 * @return the replicateDoDB
	 */
	public String getReplicateDoDB() {
		return replicateDoDB;
	}

	/**
	 * @param replicateDoDB the replicateDoDB to set
	 */
	public void setReplicateDoDB(String replicateDoDB) {
		this.replicateDoDB = replicateDoDB;
	}

	/**
	 * @return the replicateIgnoreDB
	 */
	public String getReplicateIgnoreDB() {
		return replicateIgnoreDB;
	}

	/**
	 * @param replicateIgnoreDB the replicateIgnoreDB to set
	 */
	public void setReplicateIgnoreDB(String replicateIgnoreDB) {
		this.replicateIgnoreDB = replicateIgnoreDB;
	}

	/**
	 * @return the replicateDoTable
	 */
	public String getReplicateDoTable() {
		return replicateDoTable;
	}

	/**
	 * @param replicateDoTable the replicateDoTable to set
	 */
	public void setReplicateDoTable(String replicateDoTable) {
		this.replicateDoTable = replicateDoTable;
	}

	/**
	 * @return the replicateIgnoreTable
	 */
	public String getReplicateIgnoreTable() {
		return replicateIgnoreTable;
	}

	/**
	 * @param replicateIgnoreTable the replicateIgnoreTable to set
	 */
	public void setReplicateIgnoreTable(String replicateIgnoreTable) {
		this.replicateIgnoreTable = replicateIgnoreTable;
	}

	/**
	 * @return the replicateWildDoTable
	 */
	public String getReplicateWildDoTable() {
		return replicateWildDoTable;
	}

	/**
	 * @param replicateWildDoTable the replicateWildDoTable to set
	 */
	public void setReplicateWildDoTable(String replicateWildDoTable) {
		this.replicateWildDoTable = replicateWildDoTable;
	}

	/**
	 * @return the replicateWildIgnoreTable
	 */
	public String getReplicateWildIgnoreTable() {
		return replicateWildIgnoreTable;
	}

	/**
	 * @param replicateWildIgnoreTable the replicateWildIgnoreTable to set
	 */
	public void setReplicateWildIgnoreTable(String replicateWildIgnoreTable) {
		this.replicateWildIgnoreTable = replicateWildIgnoreTable;
	}

	/**
	 * @return the lastErrno
	 */
	public Long getLastErrno() {
		return lastErrno;
	}

	/**
	 * @param lastErrno the lastErrno to set
	 */
	public void setLastErrno(Long lastErrno) {
		this.lastErrno = lastErrno;
	}

	/**
	 * @return the lastError
	 */
	public String getLastError() {
		return lastError;
	}

	/**
	 * @param lastError the lastError to set
	 */
	public void setLastError(String lastError) {
		this.lastError = lastError;
	}

	/**
	 * @return the skipCounter
	 */
	public Long getSkipCounter() {
		return skipCounter;
	}

	/**
	 * @param skipCounter the skipCounter to set
	 */
	public void setSkipCounter(Long skipCounter) {
		this.skipCounter = skipCounter;
	}

	/**
	 * @return the execMasterLogPos
	 */
	public BigInteger getExecMasterLogPos() {
		return execMasterLogPos;
	}

	/**
	 * @param execMasterLogPos the execMasterLogPos to set
	 */
	public void setExecMasterLogPos(BigInteger execMasterLogPos) {
		this.execMasterLogPos = execMasterLogPos;
	}

	/**
	 * @return the relayLogSpace
	 */
	public String getRelayLogSpace() {
		return relayLogSpace;
	}

	/**
	 * @param relayLogSpace the relayLogSpace to set
	 */
	public void setRelayLogSpace(String relayLogSpace) {
		this.relayLogSpace = relayLogSpace;
	}

	/**
	 * @return the untilCondition
	 */
	public String getUntilCondition() {
		return untilCondition;
	}

	/**
	 * @param untilCondition the untilCondition to set
	 */
	public void setUntilCondition(String untilCondition) {
		this.untilCondition = untilCondition;
	}

	/**
	 * @return the untilLogFile
	 */
	public String getUntilLogFile() {
		return untilLogFile;
	}

	/**
	 * @param untilLogFile the untilLogFile to set
	 */
	public void setUntilLogFile(String untilLogFile) {
		this.untilLogFile = untilLogFile;
	}

	/**
	 * @return the untilLogPos
	 */
	public BigInteger getUntilLogPos() {
		return untilLogPos;
	}

	/**
	 * @param untilLogPos the untilLogPos to set
	 */
	public void setUntilLogPos(BigInteger untilLogPos) {
		this.untilLogPos = untilLogPos;
	}

	/**
	 * @return the masterSSLAllowed
	 */
	public String getMasterSSLAllowed() {
		return masterSSLAllowed;
	}

	/**
	 * @param masterSSLAllowed the masterSSLAllowed to set
	 */
	public void setMasterSSLAllowed(String masterSSLAllowed) {
		this.masterSSLAllowed = masterSSLAllowed;
	}

	/**
	 * @return the masterSSLCAFile
	 */
	public String getMasterSSLCAFile() {
		return masterSSLCAFile;
	}

	/**
	 * @param masterSSLCAFile the masterSSLCAFile to set
	 */
	public void setMasterSSLCAFile(String masterSSLCAFile) {
		this.masterSSLCAFile = masterSSLCAFile;
	}

	/**
	 * @return the masterSSLCAPath
	 */
	public String getMasterSSLCAPath() {
		return masterSSLCAPath;
	}

	/**
	 * @param masterSSLCAPath the masterSSLCAPath to set
	 */
	public void setMasterSSLCAPath(String masterSSLCAPath) {
		this.masterSSLCAPath = masterSSLCAPath;
	}

	/**
	 * @return the masterSSLCert
	 */
	public String getMasterSSLCert() {
		return masterSSLCert;
	}

	/**
	 * @param masterSSLCert the masterSSLCert to set
	 */
	public void setMasterSSLCert(String masterSSLCert) {
		this.masterSSLCert = masterSSLCert;
	}

	/**
	 * @return the masterSSLCipher
	 */
	public String getMasterSSLCipher() {
		return masterSSLCipher;
	}

	/**
	 * @param masterSSLCipher the masterSSLCipher to set
	 */
	public void setMasterSSLCipher(String masterSSLCipher) {
		this.masterSSLCipher = masterSSLCipher;
	}

	/**
	 * @return the masterSSLKey
	 */
	public String getMasterSSLKey() {
		return masterSSLKey;
	}

	/**
	 * @param masterSSLKey the masterSSLKey to set
	 */
	public void setMasterSSLKey(String masterSSLKey) {
		this.masterSSLKey = masterSSLKey;
	}

	/**
	 * @return the secondsBehindMaster
	 */
	public BigInteger getSecondsBehindMaster() {
		return secondsBehindMaster;
	}

	/**
	 * @param secondsBehindMaster the secondsBehindMaster to set
	 */
	public void setSecondsBehindMaster(BigInteger secondsBehindMaster) {
		this.secondsBehindMaster = secondsBehindMaster;
	}

	@Override
	public SlaveStatusMapper mapRow(ResultSet rs, int rowNum) 
	throws SQLException {
		SlaveStatusMapper ssm = new SlaveStatusMapper();
		ssm.setSlaveIOState(rs.getString("Slave_IO_State"));
		ssm.setMasterHost(rs.getString("Master_Host"));
		ssm.setMasterUser(rs.getString("Master_User"));
		ssm.setMasterPort(rs.getLong("Master_Port"));
		ssm.setConnectRetry(rs.getLong("Connect_Retry"));
		ssm.setMasterLogFile(rs.getString("Master_Log_File"));
		ssm.setReadMasterLogPos(new BigInteger(rs.getString("Read_Master_Log_Pos")));
		ssm.setRelayLogFile(rs.getString("Relay_Log_File"));
		ssm.setRelayLogPos(new BigInteger(rs.getString("Relay_Log_Pos")));
		ssm.setRelayMasterLogFile(rs.getString("Relay_Master_Log_File"));
		ssm.setSlaveIORunning(rs.getString("Slave_IO_Running"));
		ssm.setSlaveSQLRunning(rs.getString("Slave_SQL_Running"));
		ssm.setReplicateDoDB(rs.getString("Replicate_Do_DB"));
		ssm.setReplicateIgnoreDB(rs.getString("Replicate_Ignore_DB"));
		ssm.setReplicateDoTable(rs.getString("Replicate_Do_Table"));
		ssm.setReplicateIgnoreTable(rs.getString("Replicate_Ignore_Table"));
		ssm.setReplicateWildDoTable(rs.getString("Replicate_Wild_Do_Table"));
		ssm.setReplicateWildIgnoreTable(rs.getString("Replicate_Wild_Ignore_Table"));
		ssm.setLastErrno(rs.getLong("Last_Errno"));
		ssm.setLastError(rs.getString("Last_Error"));
		ssm.setSkipCounter(rs.getLong("Skip_Counter"));
		ssm.setExecMasterLogPos(new BigInteger(rs.getString("Exec_Master_Log_Pos")));
		ssm.setRelayLogPos(new BigInteger(rs.getString("Relay_Log_Space")));
		ssm.setUntilCondition(rs.getString("Until_Condition"));
		ssm.setUntilLogFile(rs.getString("Until_Log_File"));
		ssm.setUntilLogPos(new BigInteger(rs.getString("Until_Log_Pos")));
		ssm.setMasterSSLAllowed(rs.getString("Master_SSL_Allowed"));
		ssm.setMasterSSLCAFile(rs.getString("Master_SSL_CA_File"));
		ssm.setMasterSSLCAPath(rs.getString("Master_SSL_CA_Path"));
		ssm.setMasterSSLCert(rs.getString("Master_SSL_Cert"));
		ssm.setMasterSSLCipher(rs.getString("Master_SSL_Cipher"));
		ssm.setMasterSSLKey(rs.getString("Master_SSL_Key"));
		ssm.setSecondsBehindMaster(new BigInteger(rs.getString("Seconds_Behind_Master")));
		return ssm;
	}
	
	public boolean isIORunning(){
		return getSlaveIORunning().equalsIgnoreCase("Yes");
	}
	
	public boolean isSQLRunning(){
		return getSlaveSQLRunning().equalsIgnoreCase("Yes");
	}

	/**
	 * 
	 * @return the string image of the object
	 * @author 
	 */
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("SlaveStatusMapper[");
		buffer.append("\n connectRetry = ").append(connectRetry);
		buffer.append("\n execMasterLogPos = ").append(execMasterLogPos);
		buffer.append("\n lastErrno = ").append(lastErrno);
		buffer.append("\n lastError = ").append(lastError);
		buffer.append("\n masterHost = ").append(masterHost);
		buffer.append("\n masterLogFile = ").append(masterLogFile);
		buffer.append("\n masterPort = ").append(masterPort);
		buffer.append("\n masterSSLAllowed = ").append(masterSSLAllowed);
		buffer.append("\n masterSSLCAFile = ").append(masterSSLCAFile);
		buffer.append("\n masterSSLCAPath = ").append(masterSSLCAPath);
		buffer.append("\n masterSSLCert = ").append(masterSSLCert);
		buffer.append("\n masterSSLCipher = ").append(masterSSLCipher);
		buffer.append("\n masterSSLKey = ").append(masterSSLKey);
		buffer.append("\n masterUser = ").append(masterUser);
		buffer.append("\n readMasterLogPos = ").append(readMasterLogPos);
		buffer.append("\n relayLogFile = ").append(relayLogFile);
		buffer.append("\n relayLogPos = ").append(relayLogPos);
		buffer.append("\n relayLogSpace = ").append(relayLogSpace);
		buffer.append("\n relayMasterLogFile = ").append(relayMasterLogFile);
		buffer.append("\n replicateDoDB = ").append(replicateDoDB);
		buffer.append("\n replicateDoTable = ").append(replicateDoTable);
		buffer.append("\n replicateIgnoreDB = ").append(replicateIgnoreDB);
		buffer.append("\n replicateIgnoreTable = ").append(replicateIgnoreTable);
		buffer.append("\n replicateWildDoTable = ").append(replicateWildDoTable);
		buffer.append("\n replicateWildIgnoreTable = ").append(replicateWildIgnoreTable);
		buffer.append("\n secondsBehindMaster = ").append(secondsBehindMaster);
		buffer.append("\n skipCounter = ").append(skipCounter);
		buffer.append("\n slaveIORunning = ").append(slaveIORunning);
		buffer.append("\n slaveIOState = ").append(slaveIOState);
		buffer.append("\n slaveSQLRunning = ").append(slaveSQLRunning);
		buffer.append("\n untilCondition = ").append(untilCondition);
		buffer.append("\n untilLogFile = ").append(untilLogFile);
		buffer.append("\n untilLogPos = ").append(untilLogPos);
		buffer.append("]");
		return buffer.toString();
	}

}
