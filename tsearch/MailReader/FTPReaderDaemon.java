package ro.cst.tsearch.MailReader;

import java.util.Timer;

import org.apache.log4j.Logger;

import ro.cst.tsearch.ServerConfig;

public class FTPReaderDaemon {
	
	protected static final Logger logger = Logger.getLogger(FTPReaderDaemon.class);
	
	public static void initAndStartDeamons() {
		logger.debug("FTPReaderDaemon: Starting service....");
		
		int period = ServerConfig.getFTPReaderNdexTimerTaskPeriod(600);
		Timer timerNow = new Timer("FTPReaderNdexTimerTask");
		timerNow.schedule(new FTPReaderNdexTimerTask(), 5000, period * 1000);

		logger.info("FTPReaderDaemon: Service started.");
	}

	/**
	 * @return the logger
	 */
	public static Logger getLogger() {
		return logger;
	}

	

}
