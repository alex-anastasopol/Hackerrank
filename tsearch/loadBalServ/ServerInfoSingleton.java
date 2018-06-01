package ro.cst.tsearch.loadBalServ;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.LoadConfig;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.DeadlockBean;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.threads.deadlock.ReentrantInfoLock;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.URLMaping;

public class ServerInfoSingleton  {
	private static final Category logger = Logger.getLogger(ServerInfoSingleton.class);
	
	private static int lockAllSearchTimeout			=	ServerConfig.getInteger("lockAllSearchTimeout", 60);
	private static int saveThreadStackTraceInterval	=	ServerConfig.getInteger("saveThreadStackTraceInterval", 15);
	private static int saveThreadStackTraceCount	=	ServerConfig.getInteger("saveThreadStackTraceCount", 12);
	private static int checkDatabaseIntervalSeconds	=	ServerConfig.getInteger("checkDatabaseIntervalSeconds", 30);
	
	private static float weighting 			=	getFloatField("weighting"); 
	private static float bogomips			=	getFloatField("lbs.bogomips"); 
	private static float cpul				=	getFloatField("lbs.cpul");
	private static float meml				=	getFloatField("lbs.meml");
	private static float netl				=	getFloatField("lbs.netl");
//	private static float networkBandwidth	= 	getFloatField("network.bandwidth")/8/1024;
	private static float maxAvailability	=	1; 
//			cpul * bogomips * weighting/100 + 
//			meml * LoadInformation.maxMemory * weighting/100 + 
//			netl * networkBandwidth * weighting/100;
	
	private static boolean databaseWorking	= 	false;
	
	private final ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
	
	private DeadlockBean deadlockBean = null;
	
	static {
//		float maxMemory = LoadInformation.maxMemory;
//		System.err.println("maxMemory: " + maxMemory);
//		if(maxMemory <= 0 ) {
//			if(URLMaping.INSTANCE_DIR.equals("ats03"))
//				maxMemory = 9727;
//			else
//				maxMemory = 12223;
//		}
//		maxAvailability = 
//			cpul * bogomips * weighting/100 + 
//			meml * maxMemory * weighting/100 + 
//			netl * networkBandwidth * weighting/100;
		System.err.println("maxAvailability: " + maxAvailability);
	}
	
	private String defaultDestination = "";
	private boolean enabled = true;
	private boolean enabledLoadAlgorithm = false;
	private boolean enabledSourceAlgorithm = false;
	
	private ServerInfoSingleton() {
		deadlockBean = new DeadlockBean();
		
		TimerTask updater = new ServerInfoSingletonTask();
		updater.run();
		
		Timer timerNow = new Timer("ServerInfoSingletonTask");
   	 	timerNow.schedule(updater, checkDatabaseIntervalSeconds * 1000, checkDatabaseIntervalSeconds * 1000);
   	 	
	}
	
	public final class SaveStackThread extends Thread {
		private final int	saveThreadStackTraceCount;
		private final int	saveThreadStackTraceInterval;
		private StringBuilder stackTraceContent;

		private SaveStackThread(int saveThreadStackTraceCount, int saveThreadStackTraceInterval) {
			this.saveThreadStackTraceCount = saveThreadStackTraceCount;
			this.saveThreadStackTraceInterval = saveThreadStackTraceInterval;
		}

		@Override
		public void run() {
			
			
			
			for (int i = 0; i < saveThreadStackTraceCount; i++) {
				
				long startTime = System.currentTimeMillis();
				
				stackTraceContent = new StringBuilder();
				ThreadInfo[] threadInfos = mbean.dumpAllThreads(true, true);
				
				stackTraceContent.append("Dumping threads took: " + (System.currentTimeMillis() - startTime) + " millis\n\n");
				for (ThreadInfo threadInfo : threadInfos) {
					stackTraceContent.append(ServerInfoSingleton.getThreadInfoRepresentation(threadInfo)).append("\n");
				}
				stackTraceContent.append("Getting all threads info took: " + (System.currentTimeMillis() - startTime) + " millis\n\n");
				DBManager.addThreadsStackTrace(stackTraceContent.toString());

				logger.info("SaveStackThread ALL took " + (System.currentTimeMillis() - startTime));
				try {
					Thread.sleep(saveThreadStackTraceInterval * 1000);
				} catch (InterruptedException e) {
					logger.error("Error while sleeping between stacks");
				}
			}
		}
		
		public StringBuilder getStackTraceContent() {
			return stackTraceContent;
		}
	}

	private static class SingletonHolder {
		private static ServerInfoSingleton instance = new ServerInfoSingleton();
	} 
	
	/**
	 * Not really a singleton.
	 * It also updates the info with the current database information
	 * @return
	 */
	public static ServerInfoSingleton getInstance() {
		return SingletonHolder.instance;
		
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getDefaultDestination() {
		return defaultDestination;
	}

	public void setDefaultDestination(String defaultDestination) {
		this.defaultDestination = defaultDestination;
	}

	public boolean isEnabledLoadAlgorithm(){
		return enabledLoadAlgorithm;
	}

	public boolean isEnabledSourceAlgorithm() {
		return enabledSourceAlgorithm;
	}

	public void setEnabledSourceAlgorithm(boolean enabledSourceAlgorithm) {
		this.enabledSourceAlgorithm = enabledSourceAlgorithm;
	}

	public void setEnabledLoadAlgorithm(boolean enabledLoadAlgorithm) {
		this.enabledLoadAlgorithm = enabledLoadAlgorithm;
	}
	
	public static float getFloatField(String string) {
		float field = -1;
		try {
			field = Float.parseFloat(ServerConfig.getString(string));
        } catch (Exception e){
        	e.printStackTrace();
        }
		logger.debug("Using " + string + ": " + field + " at Load Balancing Service");
		return field;
	}

	/**
	 * Returns the current load from the latest system load information
	 * @return
	 */
	public static float getLatestLocalLoad() {
		try {
			return DBManager.getSimpleTemplate().queryForObject(
					"SELECT " + DBConstants.FIELD_SERVER_CPU + 
						", " + DBConstants.FIELD_SERVER_MEM + 
						", " + DBConstants.FIELD_SERVER_NET + 
					" FROM " + DBConstants.TABLE_SERVER + 
					" WHERE " + DBConstants.FIELD_SERVER_ALIAS + " = ?",
					new ParameterizedRowMapper<Float>() {
						@Override
						public Float mapRow(ResultSet rs, int rowNum) throws SQLException {
							return getLoadAffinity(
									rs.getFloat(DBConstants.FIELD_SERVER_CPU), 
									rs.getFloat(DBConstants.FIELD_SERVER_MEM), 
									rs.getFloat(DBConstants.FIELD_SERVER_NET));
						}
					}, 
					URLMaping.INSTANCE_DIR);
		} catch (Exception e) {
			logger.error("Cannot getLatestLocalLoad", e);
			return 0;
		}
		
	}
	/**
	 * Higher load is better, it means system has more resources
	 * @param cpu in usage as fraction (from 0 to 1)
	 * @param mem in usage as fraction (from 0 to 1)
	 * @param net in usage as fraction (from 0 to 1)
	 * @return load affinity in usage as fraction (from 0 to 1)
	 */
	public static float getLoadAffinity(float cpu, float mem, float net){
		float currentLoad = -1;
		//CPU_load= (bogomips x cpu idle %) x weighting /100 ; this calculates CPU spare capacity
		float cpuLoad = (bogomips*cpu)*weighting / 100;
		//memory_load= (virtual_memory_free) x weighting /100; this calculates spare memory capacity
		float memLoad = mem*weighting/100;
		//network_load=(bandwidth_free) x weighting/100
		float netLoad = net * weighting / 100;
		//load = cpul x CPU_load + meml x memory_load + netl x network_load
		currentLoad = cpul * cpuLoad + meml * memLoad + netl * netLoad;
		
		currentLoad = currentLoad / (cpul*weighting / 100 + meml*weighting / 100 + netl*weighting / 100);
		
		//currentLoad = (45*cpu + 45*mem + 10*net)/100;
		//currentLoad = cpuLoad + memLoad + netLoad;
		//logger.info("CPULoad: " + cpuLoad + ", memLoad: " + memLoad + ", netLoad: " + netLoad + " --> currentLoad: " + currentLoad);
		return 1 - currentLoad ;
	}
	
	public static float getMaxAvailability(){
		return maxAvailability;
	}
	
	/**
	 * If a deadlock is found this method will receive a list of thread ids<br>
	 * This will also check a deadlock on the allSearchesLock
	 * @param deadlockedThreads
	 */
	public void signalDeadlock(long[] deadlockedThreads){
		boolean canLockAllSearches = false;
		Thread locker = null;
		try {
			ReentrantInfoLock lockAllSearches = SearchManager.getLockAllSearches();
			
			if(lockAllSearches.tryLock() || lockAllSearches.tryLock(lockAllSearchTimeout, TimeUnit.SECONDS)) {
				canLockAllSearches = true;
				lockAllSearches.unlock();
			} else {
				canLockAllSearches = false;
				locker = lockAllSearches.getOwner();
			}
		} catch (Exception e) {
			logger.error("Error while locking all searches!", e);
		}
		
		if(!canLockAllSearches || (deadlockedThreads != null && deadlockedThreads.length > 0) ) {
			//found a deadlock
			if(deadlockBean.isDeadlockCurrentlyDetected()) {
				deadlockBean.incrementDeadlockCount();
			} else {
				deadlockBean.setDeadlockCurrentlyDetected(true);
				deadlockBean.setDeadlockCount(1);
				deadlockBean.setFirstDeadlockFoundDate(new Date());
				
				try {
					String deadlockStatus = "";
					
					if(!canLockAllSearches) {
						deadlockStatus += "Cannot acquire lock on allSearches in SearchManager in " + lockAllSearchTimeout + " seconds \n";
					}
					if(deadlockedThreads != null && deadlockedThreads.length > 0 ) {
						deadlockStatus += "Deadlocked Threads:" + "\n";
						deadlockStatus += "-------------------" + "\n";
						
						ThreadInfo[] threadInfos = mbean.getThreadInfo(deadlockedThreads, true, true);
						for (ThreadInfo threadInfo : threadInfos) {
							deadlockStatus += threadInfo + "\n";
						}
					}
					
					if(locker != null) {
						ThreadInfo threadInfo = mbean.getThreadInfo(locker.getId(), Integer.MAX_VALUE);
						if(threadInfo != null) {
							deadlockStatus += "Slowdown Thread\n";
							deadlockStatus += ServerInfoSingleton.getThreadInfoRepresentation(threadInfo) + "\n";
							deadlockStatus += "-------------------" + "\n";
						}
					}
					
					String subject = "Possible deadlock found on " + URLMaping.INSTANCE_DIR;
					sendNotificationViaEmail(deadlockStatus, subject);
				} catch (Exception e) {
					logger.error("Error while trying to send notification email for deadlock", e);
				}
				
				saveThreadStack();
				
			}
		} else {
			//server is working fine - no deadlocks
			if(deadlockBean.isDeadlockCurrentlyDetected()) {
				deadlockBean.setDeadlockCurrentlyDetected(false);
				deadlockBean.setDeadlockCount(0);
				deadlockBean.setEmailSentCount(0);
				deadlockBean.setFirstDeadlockFoundDate(null);
				deadlockBean.setFirstEmailSentDate(null);
				
				try {
				
					sendNotificationViaEmail(
							"Acquired lock on allSearches in SearchManager in less than " + lockAllSearchTimeout + " seconds \n", 
							"Slowdown recover on " + URLMaping.INSTANCE_DIR);
					
				} catch (Exception e) {
					logger.error("Error while trying to send notification email for deadlock free", e);
				}
				
			}
		}
	}

	public static void sendNotificationViaEmail(String deadlockStatus, String subject) {
		EmailClient email = new EmailClient();
		email.addTo(DBManager.getConfigByName("lbs.notification.email"));
		email.setSubject(subject);
		email.addContent(deadlockStatus);
		email.sendAsynchronous();
	}
	
	public boolean isDeadlockCurrentlyDetected() {
		if(deadlockBean != null) {
			return deadlockBean.isDeadlockCurrentlyDetected();
		} else {
			return false;
		}
	}

	public SaveStackThread saveThreadStack() {
		return saveThreadStack(saveThreadStackTraceInterval, saveThreadStackTraceCount);
		
	}
	
	/**
	 * Saves stack traces in the database using the given parameters (in seconds)
	 * @param saveThreadStackTraceInterval
	 * @param saveThreadStackTraceCount
	 */
	public SaveStackThread saveThreadStack(final int saveThreadStackTraceInterval,
			final int saveThreadStackTraceCount) {
		SaveStackThread thread = new SaveStackThread(saveThreadStackTraceCount, saveThreadStackTraceInterval);
		thread.start();
		return thread;
	}
	
	private class ServerInfoSingletonTask extends TimerTask {

		@Override
		public void run() {
			
			checkDatabaseStatus();
			
			setEnabledLoadAlgorithm(LoadConfig.getLbsEnableLoadAlg());
			setEnabledSourceAlgorithm(LoadConfig.getLbsEnableSourceAlg());
			
		}
		
	}

	private static final String SQL_CHECK_SEARCH_TABLE = "SELECT max(" + DBConstants.FIELD_SEARCH_ID + ") from " + DBConstants.TABLE_SEARCH;
	private void checkDatabaseStatus() {
		try {
			long maxId = DBManager.getSimpleTemplate().queryForLong(SQL_CHECK_SEARCH_TABLE);
			if(maxId > 0) {
				setDatabaseWorking(true);
			} else {
				setDatabaseWorking(false);
			}
		} catch (Exception e) {
			setDatabaseWorking(false);
			logger.error("Error while running " + SQL_CHECK_SEARCH_TABLE, e);
			
		}
		
	}

	public boolean isDatabaseWorking() {
		return databaseWorking;
	}

	private void setDatabaseWorking(boolean databaseWorking) {
		ServerInfoSingleton.databaseWorking = databaseWorking;
	}
	
	public static String getThreadInfoRepresentation(ThreadInfo threadInfo) {
		StringBuilder sb = new StringBuilder("\"" + threadInfo.getThreadName() + "\""
				+ " Id=" + threadInfo.getThreadId() + " " + threadInfo.getThreadState());
		if (threadInfo.getLockName() != null) {
			sb.append(" on " + threadInfo.getLockName());
		}
		if (threadInfo.getLockOwnerName() != null) {
			sb.append(" owned by \"" + threadInfo.getLockOwnerName() + "\" Id="
					+ threadInfo.getLockOwnerId());
		}
		if (threadInfo.isSuspended()) {
			sb.append(" (suspended)");
		}
		if (threadInfo.isInNative()) {
			sb.append(" (in native)");
		}
		sb.append('\n');
		int i = 0;
		StackTraceElement[] stackTrace = threadInfo.getStackTrace();
		for (; i < stackTrace.length ; i++) {
			StackTraceElement ste = stackTrace[i];
			sb.append("\tat " + ste.toString());
			sb.append('\n');
			if (i == 0 && threadInfo.getLockInfo() != null) {
				Thread.State ts = threadInfo.getThreadState();
				switch (ts) {
				case BLOCKED:
					sb.append("\t-  blocked on " + threadInfo.getLockInfo());
					sb.append('\n');
					break;
				case WAITING:
					sb.append("\t-  waiting on " + threadInfo.getLockInfo());
					sb.append('\n');
					break;
				case TIMED_WAITING:
					sb.append("\t-  waiting on " + threadInfo.getLockInfo());
					sb.append('\n');
					break;
				default:
				}
			}

			for (MonitorInfo mi : threadInfo.getLockedMonitors()) {
				if (mi.getLockedStackDepth() == i) {
					sb.append("\t-  locked " + mi);
					sb.append('\n');
				}
			}
		}
		if (i < stackTrace.length) {
			sb.append("\t...");
			sb.append('\n');
		}

		LockInfo[] locks = threadInfo.getLockedSynchronizers();
		if (locks.length > 0) {
			sb.append("\n\tNumber of locked synchronizers = " + locks.length);
			sb.append('\n');
			for (LockInfo li : locks) {
				sb.append("\t- " + li);
				sb.append('\n');
			}
		}
		sb.append('\n');
		return sb.toString();
	}
	
}
