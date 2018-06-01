package ro.cst.tsearch.threads;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.database.ConnectionManager;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.OraConstants;
import ro.cst.tsearch.database.rowmapper.SlaveStatusMapper;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.utils.URLMaping;

/**
 * 
 * This class will wake from time to time and check if the replication is still up 
 * The test is done using the "show slave status" command.
 * If replication is down an email is sent to the designated addresses
 *  
 * @author aandrei
 *
 */
public class ReplicationMonitor extends TimerTask {
	
	private static final Category logger = Logger.getLogger(ReplicationMonitor.class);
	private boolean isReplicating = true;
	private int startRetries = 0;
	private static int MAX_START_RETRIES = 5;
	
	private Connection getConnection() {
		try {
	    	Class.forName("com.mysql.jdbc.Driver").newInstance();
	        return DriverManager.getConnection(
	        		"jdbc:mysql://"	+ ConnectionManager.host + ":" + ConnectionManager.port + "/", 
	        		OraConstants.REPLICATION_USER, 
	        		OraConstants.REPLICATION_PASSWORD);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	return null;
    }
	
	public static void init(){
		logger.info("Replication Monitoring: Starting service....");
		Timer timerNow = new Timer("ReplicationMonitor");
		timerNow.schedule(new ReplicationMonitor(), 2000, ServerConfig.getReplicationNotificationPeriod() * 1000);
		logger.info("Replication Monitoring: Service started.");
	}
	
	/**
	 * @return the isReplicating
	 */
	public boolean isReplicating() {
		return isReplicating;
	}

	/**
	 * @param isReplicating the isReplicating to set
	 */
	public void setReplicating(boolean isReplicating) {
		this.isReplicating = isReplicating;
	}

	@Override
	public void run() {
		String replicationStatus = "No Available Status";
		if(ServerConfig.isReplicationNotification()) {
			try {
				SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
				List<Map<String, Object>> result = sjt.queryForList("show slave status");
				SlaveStatusMapper status = new SlaveStatusMapper(result);
				
				replicationStatus = "ServerTime: " + Calendar.getInstance().getTime().toString() + "\n" + status.toString();
				if(!status.isIORunning() || !status.isSQLRunning()){
					//replication has failed
					System.err.println("Replication is DOWN. Do something!!!");
					System.err.println("Replication is DOWN. Do something!!!");
					System.err.println("Replication is DOWN. Do something!!!");
					logger.error("Replication is DOWN. Do something!!!");
					logger.error("Replication is DOWN. Do something!!!");
					logger.error("Replication is DOWN. Do something!!!");
					Connection conn = null;
					try {
						if(getStartRetries() < MAX_START_RETRIES) {
							conn = getConnection();
							Statement stmt = conn.createStatement();
							
							if(!status.isSQLRunning()) {
								if(status.getLastErrno() == 1062) {	//duplicate key
									try {
										stmt.executeQuery("set global sql_slave_skip_counter=1;");
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							}
							
							stmt.execute("start slave");
						}
					} catch (Exception e) {
						e.printStackTrace();
						logger.error("Error trying to restart slave", e);
					} finally {
						if(conn != null) {
							try {
								conn.close();
							} catch (Exception e) {}
						}
						setStartRetries(getStartRetries()+1);
					}
					
					if(isReplicating()){	
						//replication just failed so we need to send the email
						EmailClient email = new EmailClient();
						email.addTo(MailConfig.getReplicationNotificationEmail());
						email.setSubject("Replication failed on " + URLMaping.INSTANCE_DIR);
						email.addContent(replicationStatus);
						email.sendNow();
						setReplicating(false);
						
					}
					System.err.println(replicationStatus);
					logger.error(replicationStatus);
				} else {
					if(!isReplicating()){
						EmailClient email = new EmailClient();
						email.addTo(MailConfig.getReplicationNotificationEmail());
						email.setSubject("Replication started on " + URLMaping.INSTANCE_DIR);
						email.addContent(replicationStatus);
						email.sendNow();
						setReplicating(true);
						setStartRetries(0);
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Replication problem with status: ");
				System.err.println(replicationStatus);
				logger.error("Replication problem with status: " + replicationStatus, e);
				
			}			
		}
	}

	/**
	 * @return the startRetries
	 */
	public int getStartRetries() {
		return startRetries;
	}

	/**
	 * @param startRetries the startRetries to set
	 */
	public void setStartRetries(int startRetries) {
		this.startRetries = startRetries;
	}
}
