package ro.cst.tsearch.threads;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import ro.cst.tsearch.bean.ErrorRequestBean;

public class DeleteErrorRequest extends TimerTask
{
    private static int daysBack = 30;	
    
    public static void init() {
		
		String timeZone = "CST6CDT";	//Central Daylight Time (GMT-6)
    	int hour = 3;					//3 AM
		int interval = 24;				//hours
    	
		DeleteErrorRequest deleteErrorRequest = new DeleteErrorRequest();
		Timer timer = new Timer(DeleteErrorRequest.class.getName());
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(timeZone));
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		
		timer.schedule(deleteErrorRequest, calendar.getTime(), interval * 3600 * 1000);
	}
    
    public void run()
    {
    	try { ErrorRequestBean.deleteEntriesOlderThan(daysBack);} catch (Exception e) {e.printStackTrace();}
    }
    
}
