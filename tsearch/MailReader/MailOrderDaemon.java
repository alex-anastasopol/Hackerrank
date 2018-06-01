package ro.cst.tsearch.MailReader;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import ro.cst.tsearch.MailConfig;

public class MailOrderDaemon {
	
	protected static Logger logger = Logger.getLogger(MailOrderDaemon.class);
	
	private static int interval = 0;
	private static int delay = 0;

	private static Timer timer = null;
	private static TimerTask timerTask = null;
	
	public static boolean init(String[] args){
		
		String host = null;
		String username = null;
		String password = null;
		String attachmentPath = null;
		String supportAddress = null;
		String searchOrdersAddress = null;
		String succeededFolderName = null;
		String failedFolderName = null;
		String protocol = null;
		int maxMessages = 0;
		boolean replyEnabled = false;
		String ordersAddress = null;
		
		try {		
			host = MailConfig.getMailOrderHost();
			username = MailConfig.getMailOrderUsername();
			password = MailConfig.getMailOrderPassword();
			attachmentPath = MailConfig.getMailOrderAttachpath();
			supportAddress = MailConfig.getMailOrderSupportAddress();
			searchOrdersAddress = MailConfig.getMailOrderSearchordersAddress();
			succeededFolderName = MailConfig.getMailOrderSucceededfolder();
			failedFolderName = MailConfig.getMailOrderFailedfolder();
			replyEnabled = MailConfig.getMailOrderReplyenabled();
			ordersAddress = MailConfig.getMailOrderOrdersAddress();
			delay = MailConfig.getMailOrderDelay() * 1000;
			interval = MailConfig.getMailOrderInterval() * 1000;			
			maxMessages = MailConfig.getMailOrderMaxmessages();
			protocol = MailConfig.getMailOrderProtocol();
			
		} catch(Exception e){
			
			if(e instanceof NumberFormatException){
				logger.error("MailOrderDaemon#init !!! Invalid delay/interval/maxMessages in properties file !", e);
			}else{
				logger.error("MailOrderDaemon#init !!! Exception reading properties file !", e);
			}
			
			return false;
		}
		
		logger.info("MailOrderDaemon#init host:" + host);
		logger.info("MailOrderDaemon#init username:" + username);
		logger.info("MailOrderDaemon#init password:" + password);
		logger.info("MailOrderDaemon#init delay:" + delay/1000 + " seconds");		
		logger.info("MailOrderDaemon#init interval:" + interval/1000 + " seconds");
		logger.info("MailOrderDaemon#init attachmentPath:" + attachmentPath);
		logger.info("MailOrderDaemon#init supportAddress:" + supportAddress);
		logger.info("MailOrderDaemon#init searchOrdersAddress:" + searchOrdersAddress);
		logger.info("MailOrderDaemon#init succeededfolder:" +  succeededFolderName);
		logger.info("MailOrderDaemon#init failedfolder:" + failedFolderName);
		logger.info("MailOrderDaemon#init maxmessages:" + maxMessages);
		logger.info("MailOrderDaemon#init replyEnabled:" + replyEnabled);
		logger.info("MailOrderDaemon#init ordersAddress:" + ordersAddress);
		
		timerTask = new MailOrderDaemonTimerTask(
							host,
							username, 
							password, 
							attachmentPath,
							supportAddress,
							searchOrdersAddress,
							ordersAddress,
							succeededFolderName,
							failedFolderName,
							protocol,
							maxMessages,
							replyEnabled
					);
		return true;
	}

	public static boolean start(){
		logger.info("MailOrderDaemon#start");
		timer = new Timer();
		timer.scheduleAtFixedRate(timerTask, delay, interval);
		return true;
	}

	public static boolean stop(){
		timer.cancel();
		return true;
	}

	public static void main(String[] args) {
		logger.info("MailOrderDaemon#main - Start");
		
		if (init(args))
			start();		
		
		logger.info("MailOrderDaemon#main - End");
	}
	
	public static Logger getLogger() {
		return logger;
	}
}