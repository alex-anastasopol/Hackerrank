package ro.cst.tsearch.threads;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.servers.types.GenericCountyRecorderRO.SelectLists;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.XStreamManager;

@SuppressWarnings("deprecation")
public class GenericCountyRecorderRO extends TimerTask
{
    public final static String[] stateCounty = {"AZApache", "AZCochise", "AZGraham", "AZGreenlee", "AZLa Paz", "AZNavajo", "AZSanta Cruz",
    											"COBaca", "COCheyenne", "CODolores", "COHuerfano", "COKiowa", "COLincoln", "COSaguache",
    											"COSan Juan", "COSan Miguel", "COSedgwick", "COTeller", "COWashington"};
	
	public static void init() {
		
		String timeZone = "CST";	//Central Standard Time (GMT-6)
    	int hour = 3;				//3 AM
		int interval = 24;			//hours
    	
		GenericCountyRecorderRO genericCountyRecorderRO = new GenericCountyRecorderRO();
		Timer timer = new Timer(GenericCountyRecorderRO.class.getName());
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(timeZone));
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		
		timer.schedule(genericCountyRecorderRO, calendar.getTime(), interval * 3600 * 1000);
	}
    
    public void run()
    {
    	try { 
    		
    		
    		String addressForSiteChanged = ServerConfig.getAddressForSiteChanged();
    		if(StringUtils.isEmpty(addressForSiteChanged)) {
    			return;
    		}
    		
    		boolean hasFolder = true;
    		String folderPath = ServerConfig.getModuleDescriptionFolder(BaseServlet.REAL_PATH + "WEB-INF/classes/resource/SelectLists/");
    		File folder = new File(folderPath);
    		if (!folder.exists() || !folder.isDirectory()) {
    			hasFolder = false;
    		}
    		for (String s: stateCounty) {
    			String state = "";
    			String county = "";
    			if (s.length()>2) {
    				state = s.substring(0,2);
    				county = s.substring(2);
    			}
    			String ss = s.replaceAll("\\s+", "");
    			boolean hasGoodXML = false;
    			String xml = null;
    			if (hasFolder) {
    				File file = new File(folderPath + File.separator + ss + "RO.xml");
    				if (!file.exists() || file.isDirectory()) {
    					hasGoodXML = false;
    				} else {
    					xml = FileUtils.readFileToString(file);
//    					XStreamManager xsm = XStreamManager.getInstance();
//    					Object o = xsm.fromXML(xml);
//    					if (o instanceof SelectLists) {
//    						SelectLists selectLists = (SelectLists)o;
//        					Calendar modified = selectLists.getDateModified();
//        					if (modified==null) {
//        						hasGoodXML = false;
//        					} else {
//        						Calendar today = Calendar.getInstance();
//        						if (today.get(Calendar.YEAR)==modified.get(Calendar.YEAR) && 
//        								today.get(Calendar.MONTH)==modified.get(Calendar.MONTH) &&
//        								today.get(Calendar.DAY_OF_MONTH)==modified.get(Calendar.DAY_OF_MONTH)) {
//        							hasGoodXML = true;
//        						}
//        					}
//    					}
    				}
    			}
    			if (!hasGoodXML) {
    				SelectLists newSelectLists = null;
    				HttpSite site = HttpManager.getSite(state + county + "RO", -15);
    				try {
    					newSelectLists = ((ro.cst.tsearch.connection.http2.GenericCountyRecorderRO)site).getSelectLists(state, county); 
    				} catch (RuntimeException e) {
    					e.printStackTrace();
    				} finally {
    					HttpManager.releaseSite(site);
    				}
    				if (newSelectLists!=null) {
    					XStreamManager xsm = XStreamManager.getInstance();
    					String newXml = xsm.toXML(newSelectLists);
    					if (newXml!=null) {
    						boolean hasChanged = false;
        					if (xml==null) {
        						hasChanged = true;
        					} else {
        						if (!newXml.equals(xml)) {
        							hasChanged = true;
        						}
        					}
        					if (hasChanged) {
        						File file = new File(folderPath + File.separator + ss + "RO.xml");
        	    				FileUtils.writeStringToFile(file, newXml);
        	    				HashMap<String, SelectLists> cachedSelectLists = ro.cst.tsearch.servers.types.GenericCountyRecorderRO.getCachedSelectLists();
        	    				cachedSelectLists.put(s, newSelectLists);
        	    				ro.cst.tsearch.servers.types.GenericCountyRecorderRO.setCachedSelectLists(cachedSelectLists);
        	    				//send e-mail
        	    				EmailClient email = new EmailClient();
        	    				email.addTo(ServerConfig.getAddressForSiteChanged());
        	    				email.setSubject(ss + "RO.xml has changed");
        	    				email.addContent(ss + "RO.xml has changed! Please copy it from \\web\\WEB-INF\\classes\\resource\\SelectLists" + 
        	    					" to \\src\\resource\\SelectLists");
        	    				email.sendAsynchronous();
        	    			}
    					}
    				}
    			}
    		}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
}
