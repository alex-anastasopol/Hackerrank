package ro.cst.tsearch.threads;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import ro.cst.tsearch.data.CountyCommunityManager;
import ro.cst.tsearch.database.DBManager;

public class RefreshService extends TimerTask {

	private static final Logger logger = Logger.getLogger(RefreshService.class);
	
	private static final long TIMER_DELAY		= 8000; 			//8 seconds
	
	public static void init(){
		
		String periodString = DBManager.getConfigByName("refresh.service.period");
		long period = 900;	//seconds which means 15 minutes
		try {
			period = Long.parseLong(periodString);
		} catch (Exception e) {
			logger.error("Using default value refresh.service.period = " + period, e);
		}
		period *= 1000;
		RefreshService deamon = new  RefreshService();
		Timer timer = new Timer(RefreshService.class.getName());
		timer.schedule(deamon, TIMER_DELAY, period);
	}
	
	
	@Override
	public void run() {
		try {
			if("true".equals(DBManager.getConfigByName("refresh.county_community.enabled"))) {
				CountyCommunityManager.getInstance().reloadCounties();
			}
		} catch (Exception e) {
			logger.error("Error while refreshing county_community information", e);
		}

	}

}
