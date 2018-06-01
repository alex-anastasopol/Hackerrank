package ro.cst.tsearch.Archive;

import java.util.Calendar;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;

import ro.cst.tsearch.utils.URLMaping;

public class ArchiveDaemon extends HttpServlet{

	private static final int delay = 60*1000*1440; // 24 hours

	private static Timer timer = null;
	private static TimerTask timerTask = null;
	private static String REAL_PATH;
	
	private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
	private static final Logger logger = Logger.getLogger(ArchiveDaemon.class);

	public void init() throws ServletException{
		
		REAL_PATH = getServletConfig().getServletContext().getRealPath("/");
		super.init();
		int age = 10;
		//reading from config file the number of days after a tsr is archived
		try{
			age = Integer.parseInt(rbc.getString("archiveFilesOfAge").trim());
		} catch(Exception e){
			logger.error(e);
		}
		
		//setting the start time for daemon
		timerTask = new ArchiveDaemonTimerTask(age);
		timer = new Timer();
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, 1);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		Date startTime = c.getTime(); 
		timer.scheduleAtFixedRate(timerTask, startTime, delay);
		//for test
	//	timer.scheduleAtFixedRate(timerTask, 0, delay);
		
	}

	/**
	 * @return
	 */
	public static String getREAL_PATH() {
		return REAL_PATH;
	}

}
