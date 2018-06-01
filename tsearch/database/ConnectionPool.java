package ro.cst.tsearch.database;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.loadBalServ.ServerInfoSingleton;
import ro.cst.tsearch.utils.URLMaping;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class ConnectionPool {

	private static final Logger logger = Logger.getLogger(ConnectionPool.class);
    
	/** The single instance of the connection pool */
	private static ConnectionPool thePool = new ConnectionPool();

	/** the filed hollding the current Class object in order to be used within the Log messages */
	private long startTime = 0;

	private static ComboPooledDataSource ds;
	private static SimpleJdbcTemplate 		simpleJdbcTemplate;
	private static JdbcTemplate 			jdbcTemplate ;
	private static TransactionTemplate 		transactionTemplate;
	private static NamedParameterJdbcTemplate 		namedParameterJdbcTemplate;
	private static SimpleJdbcInsert simpleJdbcInsert;
	
	Monitor monitor;
	
	/**
	 */
	
	/** Constructor for instantiating the ConnectionPool.
	 * It's private to prevent other classes from instantiating
	 * of this class */
	private ConnectionPool() {

		try {

            ds = new ComboPooledDataSource();
            ds.setDriverClass(ConnectionManager.driver);
            ds.setJdbcUrl(ConnectionManager.url);
            ds.setUser(ConnectionManager.user);
            ds.setPassword(ConnectionManager.password);
            
            ds.setMaxPoolSize(OraConstants.CONN_MAXNUMBER);
            ds.setMinPoolSize(OraConstants.CONN_MINNUMBER);
            
            // Seconds a Connection can remain pooled but unused before being discarded. 
            // Zero means idle connections never expire.
            ds.setMaxIdleTime(OraConstants.CONN_TTL_TIMEOUT);
            //Slow JDBC operations are generally performed by helper threads that don't hold contended locks. 
            //Spreading these operations over multiple threads can significantly improve performance 
            // by allowing multiple operations to be performed simultaneously.
            ds.setNumHelperThreads(OraConstants.DATABASE_POOL_HELPER_THREADS);
            
            // create spring jdbc template
            simpleJdbcTemplate = new SimpleJdbcTemplate(ds);
            
            
            jdbcTemplate = new JdbcTemplate(ds);
            
            // create spring transaction manager
        	transactionTemplate = new TransactionTemplate();	
        	
        	//named parameter jdbc template
        	namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(ds);
        	
        	// simple jdbc insert
        	simpleJdbcInsert = new SimpleJdbcInsert(ds);
        	
        	DataSourceTransactionManager tm = new DataSourceTransactionManager();
        	tm.setDataSource(ds);
        	transactionTemplate.setTransactionManager(tm);

		} catch (Exception ex) {
			logger.error("COULD NOT INITIATE CONNECTION POOL!!!!!");
			ex.printStackTrace();
		}

		monitor = new Monitor();
		monitor.setDaemon(true);
		monitor.start();
		startTime = System.currentTimeMillis();

	}

	/** Get the single possible instance of the ConnectionPool. */
	public static ConnectionPool getInstance() {
		return thePool;
	}

	public SimpleJdbcTemplate getSimpleTemplate(){
		return simpleJdbcTemplate;
	}
	
	public JdbcTemplate getTemplate(){
		return jdbcTemplate;
	}
	
	public TransactionTemplate getTransactionTemplate(){
		return transactionTemplate;
	}
	
	public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate(){
		return namedParameterJdbcTemplate;
	}
	
	public SimpleJdbcInsert getSimpleJdbcInsert(){
		return new SimpleJdbcInsert(ds);
	}
	
	
	/** Get a non null database connection. Throw an exception if
	 * it's not possible to do so. */
	public DBConnection requestConnection() throws BaseException {

		DBConnection conn = null;
		try {
			conn = new DBConnection(ds.getConnection());
			conn.setAutoCommit(false);
		} catch (Exception ex) {
			logger.error("::::::::=====>ERROR creating a new connection !!! ", ex);
			ex.printStackTrace();
			conn = null;
			throw new  BaseException(ex);
		}
		return conn;
	}	
	
	public void releaseConnection(DBConnection conn) throws BaseException {
	    if (conn != null) {
	        try {
	            conn.close();
	        } catch (Exception e) {e.printStackTrace();}
	        conn = null;
	    }
	}

    public String getStatusStr()
    {
        return monitor.getStatusStr();
    }
    
    public int getConnectionsInUse(){
    	try {
			return ds.getNumBusyConnectionsDefaultUser();
		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
    }
    
	protected class Monitor extends Thread {
		private boolean isRun = true;
		private long logInterval = OraConstants.DATABASE_POOL_MONITOR_STATISTICS_INTERVAL * 60 * 1000;

		public void run() {

			logger.info("MONITOR UP AND RUNNING");
			setName("ConnectionPool Monitor");

			while (getIsRun()) {

				try {

					long threadStartTime = System.currentTimeMillis();
					long poolLife = (threadStartTime - startTime) / (60 * 1000);
					String strDuration = "" + (int) poolLife / 60 + " hours " + poolLife % 60 + " min.";

					String infoToLog = 
						"\n\n"
							+ " *************************************************************\n"
							+ " ****  Connection pool statistics upTime [ "
							+ strDuration
							+ "]\n"
							+ " ****  [ In use / Total Allocated ] connections ["
							+ ds.getNumBusyConnectionsDefaultUser()
							+ "/"
							+ ds.getNumConnectionsDefaultUser()
							+ "]\n"
							+ " *************************************************************\n"
							+ " ****  Current duration "
							+ (System.currentTimeMillis() - threadStartTime)
							+ " milis.  See you in "
							+ logInterval / (60 * 1000)
							+ " minutes .... :))\n"
							+ " *************************************************************\n";
					
					logger.info(infoToLog);
					if(ds.getNumBusyConnectionsDefaultUser() >= ds.getMaxPoolSize() / 2
							|| ds.getNumUnclosedOrphanedConnections() >= ds.getMaxPoolSize() / 2
							) {
						StringBuilder result = new StringBuilder(infoToLog);
						result.append(ds.toString()).append("\n")
							.append("ds.getNumBusyConnectionsDefaultUser() = ").append(ds.getNumBusyConnectionsDefaultUser()).append("\n")
							.append("ds.getNumUnclosedOrphanedConnections() = ").append(ds.getNumUnclosedOrphanedConnections()).append("\n")
						;
						ServerInfoSingleton.sendNotificationViaEmail(result.toString(), "Possible Database Slowdown on " + URLMaping.INSTANCE_DIR);
					}
					Thread.sleep(logInterval);
				} catch (Exception ignored) {}
			}

		}
        
        public String getStatusStr()
        {
            long threadStartTime = System.currentTimeMillis();
            long poolLife = (threadStartTime - startTime) / (60 * 1000);
            String strDuration = "" + (int) poolLife / 60 + " hours " + poolLife % 60 + " min.";

            try {
				return "Connection pool statistics: upTime [ " + strDuration + "] <BR>\n [ In use / Total Allocated ] connections ["
				        + ds.getNumBusyConnectionsDefaultUser()
				        + "/"
				        + ds.getNumConnectionsDefaultUser()
				        + "]";
			} catch (SQLException ignored) {
			}
			return "Connection pool statistics: upTime [ " + strDuration + "]";
        }

		public void shutdown() {
			setIsRun(false);
		}

		private synchronized void setIsRun(boolean value) {
			isRun = value;
		}

		private synchronized boolean getIsRun() {
			return isRun;
		}

	}

}
