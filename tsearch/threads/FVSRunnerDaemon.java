package ro.cst.tsearch.threads;

import java.util.Timer;

import org.apache.log4j.Logger;

import ro.cst.tsearch.ServerConfig;

public class FVSRunnerDaemon {
	
	protected static final Logger logger = Logger.getLogger(FVSRunnerDaemon.class);
	
	public static void initAndStartDaemons() {
		logger.debug("FVSRunnerDaemon: Starting service....");
		
		int period = ServerConfig.getFVSRunnerTimerTaskPeriod();
		Timer timerNow = new Timer("FVSRunnerTimerTask");
		timerNow.schedule(new FVSRunnerTimerTask(), 5000, period * 1000);

		logger.info("FVSRunnerDaemon: Service started.");
	}

	/**
	 * @return the logger
	 */
	public static Logger getLogger() {
		return logger;
	}

	

}
