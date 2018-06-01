package ro.cst.tsearch.threads;

import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.data.SearchData;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.utils.URLMaping;

public class DeleteContext extends TimerTask {
    
    public static void init() {
        
        // if the delete.context.start is defined and has "false" propery, then do not run the delete context task
        boolean start = true;
        try{	
        	start = Boolean.parseBoolean( rbc.getString( "delete.context.start" ).trim() );
        }catch(Exception e){        	
        }
        if(!start){
        	System.err.println("delete.context.start=false");
        	return;
        }
        
        DeleteContext deleteContext = new DeleteContext();
        
        Timer timer = new Timer();
   	 	timer.schedule( (TimerTask) deleteContext, 60000, 
   	 	        Integer.parseInt(rbc.getString("search.verifier.interval")) * 60 * 1000);
    }

    private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
    public void run() {
        
        StringBuffer report = new StringBuffer();
        
        /*deleteOldContexts(report);*/
        findZombieSearchProcess(report);
        findZombieTSRProcess(report);
        
        /*try {
	        
            UserAttributes TSAdmin = UserManager.getUser("TSAdmin");
	        
			Properties props = System.getProperties();
			props.put("mail.smtp.host", ServerConfig.getMailSmtpHost());
			
			Session session = Session.getDefaultInstance(props,null);
			MimeMessage msg = new MimeMessage(session);
			
			InternetAddress fromAddress = new InternetAddress(MailConfig.getLoggerFromEmailAddress());
			msg.setFrom(fromAddress);
			msg.setSubject("Report from search verifier daemon.");
			msg.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(TSAdmin.getEMAIL()));
			msg.setContent(report.toString(), "text/html");
			
			Transport.send(msg);
			
        } catch (Exception e) {
            e.printStackTrace();
        }*/
		
    }
    

    
    
    private void findZombieSearchProcess(StringBuffer report) {
        
        report.append("Find zombie searches - START<br>");
        
        SearchData[] allSearches = DBManager.getSearches(-1);
        
        for (int i = 0; i < allSearches.length; i++) {
        
            ASThread thread = ASMaster.getSearch(allSearches[i].getSearchId());
            
            if (thread == null) {
                
                //zombie search record
                //DBManager.setSearchOwner(allSearches[i].getId(), 0, allSearches[i].getAbstractorId());
                DBManager.unlockSearch( allSearches[i].getId() );
                
                report.append("&nbsp;&middot;&nbsp;Search [" + allSearches[i].getId() + "] is zombie.<br>");
            
            } else {
                
                report.append("&nbsp;&middot;&nbsp;Search [" + allSearches[i].getId() + "] in progress.<br>");
            }
        }
        
        if (allSearches.length == 0)
            report.append("&nbsp;&middot;&nbsp;No zombie search process detected<br>");
        
        report.append("Find zombie searches - FINISH<br><br>");
    }
    
    private void findZombieTSRProcess(StringBuffer report) {
        
        report.append("Find zombie TSR processes - START<br>");
        
        SearchData[] allSearches = DBManager.getSearches(-2);
        
        for (int i = 0; i < allSearches.length; i++) {
        
            GPThread thread = GPMaster.getThread(allSearches[i].getSearchId());
            
            if (thread == null) {
                
                //zombie TSR record
                DBManager.setSearchOwner(allSearches[i].getId(), 0, allSearches[i].getAbstractorId());
                
                report.append("&nbsp;&middot;&nbsp;TSR process [" + allSearches[i].getId() + "] is zombie.<br>");
            
            } else {
                
                report.append("&nbsp;&middot;&nbsp;TSR process [" + allSearches[i].getId() + "] in progress.<br>");
            }
        }
        
        if (allSearches.length == 0)
            report.append("&nbsp;&middot;&nbsp;No zombie TSR process detected<br>");
        
        report.append("Find zombie TSR processes - FINISH<br><br>");
    }
}
