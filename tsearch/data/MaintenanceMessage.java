package ro.cst.tsearch.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DBUser;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;
import de.schlichtherle.io.FileReader;

public class MaintenanceMessage {

	private static volatile String message = "";
	private static volatile String formattedMessage = "";
	private static String fontColor = "#000000";
	private static String defaultMessage = "All ATS Servers are Scheduled for maintenance at 07:00 AM GMT (you have less than {timeleft=20} minutes). "
			+ "Please save your work and logout until 07:00 GMT. "
			+ "You can log back in after 10 minutes (07:10 GMT) using the address http://ats.advantagetitlesearch.com and you will be redirected on a free server.";

	private static Date messageDate = new Date();

	private static Pattern timerCode = Pattern.compile("\\{timeleft\\s*=\\s*(\\d+)\\s*\\}");

	private static MaintenancemessageTask maintenanceTask = null;

	private final static List<String> messsages = new ArrayList<String>();
	
	private final static Random rand = new Random();
	
	private static class MaintenancemessageTask extends TimerTask {

		public MaintenancemessageTask() throws IOException {
			File messagesFile = new File(BaseServlet.REAL_PATH + ServerConfig.getTipOfTheDayFilePath());
			if (messagesFile.exists() && !messagesFile.isDirectory()) {
				BufferedReader reader = null;
				try {
					reader = new BufferedReader(new FileReader(messagesFile.getAbsolutePath()));
					String line = null;
					while ((line = reader.readLine()) != null) {
						messsages.add(line);
					}
				} finally {
					if (reader != null) {
						reader.close();
					}
				}
			}
			Collections.shuffle(messsages);
		}

		public void run() {
			int size = messsages.size();
			if(size>0){
				if(org.apache.commons.lang.StringUtils.isBlank(message)||message.startsWith("<!--MAINTENANCE-->")){
					setMessage(messsages.get(rand.nextInt(size)));
				}
			}
		}
	}

	public static void init() throws IOException{
		if (maintenanceTask == null) {
			maintenanceTask = new MaintenancemessageTask();
			Timer timer = new Timer();
			timer.schedule(maintenanceTask, 10000, 1000 * ServerConfig.getTipOfTheDayChangeInterval());
		}
	}

	private MaintenanceMessage() {
	}

	public static String getDefaultMessage() {
		return defaultMessage;
	}

	public static void setColor(String color) {
		fontColor = color;
	}

	public static void setMessage(String newMessage) {
		if (newMessage == null) {
			newMessage = "";
			fontColor = "000000";
		}
		newMessage = newMessage.trim();
		message = newMessage;
		messageDate = new Date();
		if ("".equals(message)) {
			formattedMessage = messsages.get(rand.nextInt(messsages.size()));
		} else {
			formattedMessage = "<table border=\"0\" cellspacing=\"0\" width=\"100%\">"
					+ "<tr bgcolor=\"white\">"
					+ "<td align=\"left\">"
					+ "<font color=\""
					+ fontColor
					+ "\">"
					+ message
					+ "</font>" + "</td>" + "</tr>" + "</table>";
		}
	}

	public static String getMessage() {
		return message;
	}
	
	public static String getFormattedMessage(HttpServletRequest request) {
		String retMessage = getFormattedMessageOld(request);
		
		String time = retMessage.substring(0, retMessage.indexOf(":"));
		String message = retMessage.substring(retMessage.indexOf(":") + 1);
		
		int t = -1;
		
		try {
			t = Integer.parseInt(time);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(t<=0){
			return message;
		} 
		
		return "<script>\n" +
				"var booleanStopTimer = false;\n" +
		
				"function messageTimer(){\n" +
				
				"	var tipOfDay = document.getElementById('tipOfTheDay');\n" +
				"	var innerHTML = tipOfDay.innerHTML;\n" +
				"	if(booleanStopTimer){\n" +
				"		clearInterval(myMaintenanceTimer);\n" +
				"		return;\n " +
				"	}\n" +
				
				"	var ajaxRequest;\n" +
				"	try {\n" +
		            // Opera 8.0+, Firefox, Safari, Chrome
		        "   	ajaxRequest = new XMLHttpRequest();\n" +
		        "	} catch (e) {\n" +
		            // Internet Explorer Browsers
		        "		try {\n" +
		        "			ajaxRequest = new ActiveXObject(\"Msxml2.XMLHTTP\");\n" +
		        "		} catch (e) {\n" +
		        "			try {\n" +
		        "				ajaxRequest = new ActiveXObject(\"Microsoft.XMLHTTP\");\n" +
		        "			} catch (e) {}\n" +
		        "    	}\n" +
		        "	}\n" +
				"	ajaxRequest.onreadystatechange = function(){\n" +
				"		if(ajaxRequest.readyState == 4 && ajaxRequest.status == 200){\n" +
				"   		var innerHTML = ajaxRequest.responseText;\n" +
				"			if(innerHTML.indexOf(\"0:\") == 0){\n" +
				"				booleanStopTimer = true;\n" +
				"			}\n" +
				"			tipOfDay.innerHTML=innerHTML.substring(innerHTML.indexOf(\":\") + 1);\n" +
				"		}\n" +
				"	}\n" +
				
				"	ajaxRequest.open(\"GET\", \""+URLMaping.path + URLMaping.MAINTENANCEMESSAGE_SERVLET+"?timeStamp=\"+new Date().getTime(), true);\n" + //use timestamp to avoid caching
				"	ajaxRequest.send(\"\");\n" +
				"}\n"+
				"var myMaintenanceTimer = setInterval(\"messageTimer()\", 1000 * " + ServerConfig.getTipOfTheDayRehreshInterval() + ");\n" + //once a minute
				"</script>\n" +
				"<div id=tipOfTheDay>"+message+"</div>";
	}

	public static String getFormattedMessageOld(HttpServletRequest request) {
		Matcher ma = timerCode.matcher(formattedMessage);
		String retMessage = formattedMessage;
		
		int remainingMinutes = 0;
		if (ma.find()) {
			try {
				int minTimer = Integer.parseInt(ma.group(1));
				remainingMinutes = minTimer
						+ Math.round((messageDate.getTime() - (new Date())
								.getTime()) / 60 / 1000);
				if (remainingMinutes < 0)
					remainingMinutes = 0;
				retMessage = ma.replaceAll(Integer.toString(remainingMinutes));
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		HttpSession session = request.getSession(true);
		try {
			User currentUser = (User) session
					.getAttribute(SessionParams.CURRENT_USER);
			if (currentUser != null
					&& DBUser.isAboutToExpire(currentUser.getUserAttributes()
							.getID().longValue(), DBManager
							.getConfigByNameAsInt(
									"user.service.notify.user.interval.days",
									80))) {
				if (StringUtils.isEmpty(retMessage)) {
					retMessage = "<font color=\"red\">Your password is about to expire. Please change it as soon as possible or your account will be automatically invalidated.</font>"; 
				} else {
					retMessage += "<br><font color=\"red\">Your password is about to expire. Please change it as soon as possible or your account will be automatically invalidated.</font>";
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return remainingMinutes + ":" + retMessage;
	}
}
