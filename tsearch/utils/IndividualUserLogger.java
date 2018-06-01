package ro.cst.tsearch.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Map;
import java.util.TimeZone;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.data.User;

public class IndividualUserLogger {
    private static IndividualUserLogger individualUserLoggerInstance = new IndividualUserLogger();
    
    private Hashtable openedLogHandlers = null;
    
    public static void initLog(User currentUser){
    	IndividualUserLogger loggerInstance = IndividualUserLogger.getInstance();
        PrintWriter currentSearchPrintWriter = null;

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
	    
		String sToday = String.valueOf(calendar.get(Calendar.YEAR));
		sToday += "_" + String.valueOf(calendar.get(Calendar.MONTH) + 1);
		sToday += "_" + String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
		
		FormatDate ASDate = new FormatDate(FormatDate.DISC_FORMAT_1_TIME);
		String time =  ASDate.getDate(calendar.getTime().getTime(), TimeZone.getDefault());
		time = time.replaceAll(":", "_");
        
		String currentUserId = time + currentUser.toString().replaceAll("(?i)[^@]+@", "@");
		
        try {
        	String path = ServerConfig.getFilePathUserLogs() + File.separator + currentUser.getUserAttributes().getLOGIN()  + File.separator + sToday + File.separator;
        	File logDir = new File(path);
        	if (!logDir.exists()){
        		logDir.mkdirs();
        	}
        	
        	String userLoggerPath = path + currentUserId + ".txt";
	       
        	File logFile = new File(userLoggerPath);
	        
	        currentSearchPrintWriter = new PrintWriter(new FileOutputStream(logFile, true), true);
	        
        } catch(Exception e){
            e.printStackTrace();
        }
        
        if(currentSearchPrintWriter != null){
            loggerInstance.add(currentSearchPrintWriter, currentUserId);
        }
        
    }
    
    public static void finishLog(String userLoggerPath)
    {
    	IndividualUserLogger loggerInstance = IndividualUserLogger.getInstance();
        
        PrintWriter currentPrintWriter = (PrintWriter) loggerInstance.openedLogHandlers.get(userLoggerPath);
        
        if(currentPrintWriter != null) {
            currentPrintWriter.println();
            currentPrintWriter.println();
                       
            //close the print writer
            currentPrintWriter.close();
            
            //remove the print writer from the internal hashtable
            loggerInstance.remove(userLoggerPath);
        }
    }
    
    public static void info(String info, String userToStringId){
        
        IndividualUserLogger loggerInstance = IndividualUserLogger.getInstance();
       
        PrintWriter out = loggerInstance.getPW(userToStringId);
        
        if(out != null){
            out.println(info  + " on " + SearchLogger.getCurDateTimeCST());
        }
    }
    
    public static void infoDebug(String infoDebug, User currentUser){
    	try {
    	
	        IndividualUserLogger loggerInstance = IndividualUserLogger.getInstance();
	        
	        String userToStringId = currentUser.toString().replaceAll("(?i)[^@]+@", "@");
	        
	        PrintWriter out = loggerInstance.getPW(userToStringId);
	        
	        boolean logAdded = false;
	        
	        if(out == null){
	            initLog(currentUser);
	            out = loggerInstance.getPW(userToStringId);
	            logAdded = true;
	        }
	        
	        //write the info to the file
	        if(out != null){
	            out.println(infoDebug + " on " + SearchLogger.getCurDateTimeCST());
	        }
	        
    	} catch(Throwable t){
    		t.printStackTrace();
    	}
    }
    
    public synchronized void add(PrintWriter pw, String userToStringId){
        openedLogHandlers.put(userToStringId, pw);
    }
    
    public synchronized void remove(String userToStringId){
    
        openedLogHandlers.remove(userToStringId);
    }
    
    public synchronized PrintWriter getPW(String userToStringId){
    	for (Object o: openedLogHandlers.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            if (entry.getKey().toString().endsWith(userToStringId)){
            	return (PrintWriter) openedLogHandlers.get(entry.getKey());
            }
    	}
    	
    	return null;
    }
    
    private IndividualUserLogger(){
        openedLogHandlers = new Hashtable();
    }
    
    public static IndividualUserLogger getInstance(){
        return individualUserLoggerInstance;
    }
        
 
}