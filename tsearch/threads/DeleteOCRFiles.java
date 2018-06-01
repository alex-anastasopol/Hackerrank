package ro.cst.tsearch.threads;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.URLMaping;

public class DeleteOCRFiles extends TimerTask {

    private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);

    public static void init() {
        
        // if the delete.context.start is defined and has "false" propery, then do not run the delete context task
        boolean start = true;
        try{	
        	start = Boolean.parseBoolean( rbc.getString( "delete.ocr.files.start" ).trim() );
        }catch(Exception e){        	
        }
        if(!start){
        	System.err.println("delete.ocr.files.start=false");
        	return;
        }
        
        //default start hour
        int startHour = 2;
        Calendar cal = Calendar.getInstance();
        Date firstTime = cal.getTime();
        
        try
        {
            startHour = Integer.parseInt( rbc.getString( "commadmin.notifier.starthour" ).trim() );
            cal.set( Calendar.HOUR_OF_DAY, startHour );
            cal.set( Calendar.MINUTE, 0 );
            cal.set( Calendar.SECOND, 0 );
            cal.set( Calendar.MILLISECOND, 0 );
            
            firstTime = cal.getTime();
        }
        catch( Exception e )
        {}

        DeleteOCRFiles deleteOCRFiles = new DeleteOCRFiles();
        
        Timer timer = new Timer();
   	 	timer.schedule( (TimerTask) deleteOCRFiles, firstTime, 
   	 	        Integer.parseInt(rbc.getString("period.ocr.files.interval")) * 24 * 3600 * 1000);
    }

    public void run() {
    	
//    	try {
//			OcrFileMapper.deleteEntriesOlderThan(ServerConfig.getOCRDeleteDatabaseDays());
//		} catch (Exception e) {e.printStackTrace();}
    	
        
    	String saveOCRDir = "";
    	
		try {
			saveOCRDir = rbc.getString("save.ocr.dir").trim();
		} catch (Exception e) {}
		
		String ocrDir = BaseServlet.FILES_PATH + saveOCRDir + "/";
		
		File ocr = new File(ocrDir);
		String[] listOCRFiles = ocr.list();

		Date dateOfReference = null;
		Date dateOfDir = null;

		int interval = 1;
		
		try{
			interval = Integer.parseInt(rbc.getString("delete.ocr.files.time"));
		}
		catch( Exception e ){
			e.printStackTrace();
		}
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		calendar.add(Calendar.DATE, -interval);
		String sDateOfReference = String.valueOf(calendar.get(Calendar.YEAR));
		sDateOfReference += "_" + String.valueOf(calendar.get(Calendar.MONTH) + 1);
		sDateOfReference += "_" + String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));

		DateFormat formatedDate = new SimpleDateFormat("yyyy_MM_dd");
		try {
			dateOfReference = (Date) formatedDate.parse(sDateOfReference);
		} catch (Exception e) {
			
		}
		
		if( listOCRFiles != null ){
		
			for (int i = 0; i < listOCRFiles.length; i++){
				try {
					dateOfDir = (Date) formatedDate.parse(listOCRFiles[i]);
				} catch (Exception e) {
					// TODO: handle exception
				}
				if (dateOfDir.before(dateOfReference)){
					String ocrDirToDelete = ocrDir + listOCRFiles[i];
					File deleteOCRDir = new File(ocrDirToDelete);
					String[] children = deleteOCRDir.list();
					for (int j=0; j<children.length; j++) {
		                File file = new File(ocrDirToDelete + "/" + children[j]);
		                file.delete();
					}
					deleteOCRDir.delete();
				}
			}
		}
    }
    
}
