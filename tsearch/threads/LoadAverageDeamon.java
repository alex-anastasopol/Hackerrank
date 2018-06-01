package ro.cst.tsearch.threads;

import java.util.Timer;
import java.util.TimerTask;

import ro.cst.tsearch.LoadConfig;
import ro.cst.tsearch.database.DBLoadInfo;

public class LoadAverageDeamon extends TimerTask {

	private static final long TIMER_DELAY		= 2000; 			//1 second
	
	public static void init(){
		LoadAverageDeamon loadAverageDeamon = new  LoadAverageDeamon();
		Timer timer = new Timer(LoadAverageDeamon.class.getName());
		timer.schedule(loadAverageDeamon, TIMER_DELAY, LoadConfig.getLoadAverageComputationPeriod() * 1000);
	}
	
	@Override
	public void run() {
		try {
			if(LoadConfig.getLoadAverageComputationEnable()) {
				long startTime = System.currentTimeMillis();
				DBLoadInfo.updateLoadInformationLastDay();
				DBLoadInfo.updateLoadInformationLastWeek();
				DBLoadInfo.updateLoadInformationLastMonth();
				DBLoadInfo.updateLoadInformationLastYear();
				
				DBLoadInfo.updateSearchStatisticsLastWeek();
				DBLoadInfo.updateSearchStatisticsLastMonth();
				DBLoadInfo.updateSearchStatisticsLastYear();
				
				if(DBLoadInfo.getLogger().isDebugEnabled()) {
					DBLoadInfo.getLogger().debug("run for LoadAverageDeamon took " + ((System.currentTimeMillis()-startTime)/1000) + " seconds");
				}
			} else {
				DBLoadInfo.getLogger().debug("load.average.computation.enable is not true");
			}
		} catch (RuntimeException e) {
			DBLoadInfo.getLogger().error("Error running LoadAverageDeamon", e);
		}
	}

}
