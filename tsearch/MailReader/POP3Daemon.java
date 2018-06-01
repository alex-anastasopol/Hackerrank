package ro.cst.tsearch.MailReader;

import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import ro.cst.tsearch.utils.URLMaping;

public class POP3Daemon {
	
	// log object for this daemon
	private static Logger logger = Logger.getLogger(POP3Daemon.class);

	private static int delay = 60000;// 60 secs
	
	//private static int delay = 30000;// 30 secs

	//private static int delay = 60*1000*1440; // 24 hours

	private static Timer timer = null;
	private static TimerTask timerTask = null;
	
	private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
	
	
	public static boolean init(String[] args){
		
		String host = null;
		String username = null;
		String password = null;
		String strLeaveCopyOnServer = null;
		String strDelay = null;
		try{
			host = rbc.getString("mail.pop3.host").trim();
			username = rbc.getString("mail.pop3.username").trim();
			password = rbc.getString("mail.pop3.password").trim();
			strLeaveCopyOnServer = rbc.getString("mail.pop3.leavecopyonserver").trim().toUpperCase();
			strDelay = rbc.getString("mail.pop3.interval").trim();
		} catch(Exception e){
			logger.debug("Exception reading properties file !");
			logger.debug(e.getMessage());
			return false;
		}

		boolean leaveCopyOnServer = strLeaveCopyOnServer.equals("TRUE")
									|| strLeaveCopyOnServer.equals("YES");
		try{
			delay = Integer.parseInt(strDelay) * 1000;
		} catch(NumberFormatException nfe){
			logger.debug("Invalid interval in properties file !");
			return false;
		}
		
		logger.debug("POP3Daemon#init host:" + host);
		logger.debug("POP3Daemon#init username:" + username);
		logger.debug("POP3Daemon#init password:" + password);
		logger.debug("POP3Daemon#init leaveCopyOnServer:" + leaveCopyOnServer);
		logger.debug("POP3Daemon#init interval:" + delay/1000 + " seconds");		
		
		timerTask = new POP3DaemonTimerTask(host,username,password,leaveCopyOnServer);
		return true;
	}

	public static boolean start(){
		logger.debug("POP3Daemon#start");
		timer = new Timer();
		timer.scheduleAtFixedRate(timerTask, 0, delay);
		//timer.scheduleAtFixedRate(new POP3DaemonTimerTask("mail.cst.ro","razvan","2004apr"),0, delay);
		return true;
	}

	public static boolean stop(){
		timer.cancel();
		return true;
	}
	

	public static void main(String[] args) {
		logger.debug("POP3Daemon#main - Start");
		
		if (init(args))
			start();		
		
		logger.debug("POP3Daemon#main - End");
	}
	
	public static Logger getLogger() {
		return logger;
	}

}
