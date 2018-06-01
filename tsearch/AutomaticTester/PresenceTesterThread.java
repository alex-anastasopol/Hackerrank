package ro.cst.tsearch.AutomaticTester;

import java.util.Vector;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

public class PresenceTesterThread extends Thread
{
    private PresenceTesterJob presenceJob;
    public static final Logger logger = Logger.getLogger(PresenceTesterThread.class);
    
    public PresenceTesterThread ( PresenceTesterJob j )
    {
        presenceJob = j;
        
        start();
    }
    
    public void run()
    {
    
    	
        presenceJob.setRunning( true );
        
        synchronized( AutomaticTesterManager.globalPresenceTestNotifier )
        {
            AutomaticTesterManager.globalPresenceTestNotifier.notifyAll();
        }
        /*
        TSServerInfo serverInfo = presenceJob.getServerInfo();
        String serverName = serverInfo.getServerAddress();
        String serverLink = serverInfo.getServerLink();
        
        if( serverLink.indexOf( " " ) >= 0 || serverLink.indexOf( "\n" ) >= 0 || serverLink.indexOf( "\t" ) >=0 )
        {
            serverLink = serverLink.replaceAll( "[ \n\t]" , "");
        }
        
        String serverIP = serverInfo.getServerIP();
        String status = "";
        
        logger.info( "Get " + serverLink );
        
        HashMap conparams = null, reqprops = null;
        ATSConn c;

        conparams = new HashMap();
        reqprops = new HashMap();
       
      
        reqprops.put("Accept-Encoding", "gzip, deflate");
        reqprops.put("Accept-Language","en-us");
        reqprops.put("Connection","Keep-Alive");
        reqprops.put("Host", serverName);
        reqprops.put("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)");
     
        logger.info( " Presence test for  " + serverName + " get page " + serverLink);
        // first page        
		c = new ATSConn(presenceJob.getServerId(), serverLink, ATSConnConstants.GET, conparams, reqprops,Search.FROM_PRESENCE_TESTER_SEARCH_ID);
		c.setFollowRedirects(true);
		c.doConnection();
        */
		//===================================== Make the test execution =========================================== 
		
		//extract the data 
     
        int caseIndex = presenceJob.getTestId();
        
        int dbOrderIndex = presenceJob.getTestCase().getVtestID();
        
        //long searchId = presenceJob.
        
       // Vector vIDdataFromPTJ = presenceJob.getTestCase().get
        
        //Vector vdataFromPTJ = presenceJob.getVectorWithData();
        
        //create an object with the data from the XML file 
  
        testDataExecution test = new testDataExecution();	
        
        //perform the test search
		 
		   //create an object with the data from the XML file 
	    XMLAutomaticTestDataReader t = new XMLAutomaticTestDataReader();
		   
		   //extracts a document from the data base vector
		   //Document d = (Document) vdataFromPTJ.elementAt(caseIndex);
	    Document d = presenceJob.getTestCase().getDocumentReadTestID();
	    	
	    	
		int tID = presenceJob.getTestCase().getVtestID();
		   //get the id of the test case
		   
		LineOfSearch lsrch = new LineOfSearch(tID);
		   //instantiate an object for the search data
		   
			//instantiate the object that reads data from file 
		t.testDataReaderFromDOM(d);
			
	    String status = "";
		
	    boolean tst = false;
	    
		try{
			
			lsrch = test.testSearch(t);     
		    //it should be executed in threads of type job and t shoul be a parameter pf the constructor 
			
			lsrch.setTestID(tID);
			
			tst = lsrch.getPresentTestResult();
			
		}
		catch(Exception e){
			
			status = PresenceStatus.STATUS_DOWN;
			e.printStackTrace();
				
			}
						

		if( tst == false )
		{
			
		    status = PresenceStatus.STATUS_DOWN;
		}
		else
		{
		    status = PresenceStatus.STATUS_UP;
		}
		
		writePresenceTestResultinDB.writeLoSToDB(lsrch);
		
	    presenceJob.setLastStatus( status );
		
		presenceJob.setLastStarted( false );
		
        if( presenceJob.getInterval() == AutomaticTesterManager.ONE_TIME_TEST )
        {
            presenceJob.stop();
        }
		
        //presenceJob.updateDB( presenceJob.getLastStarted().getTime(), status );
		presenceJob.setRunning( false );
        synchronized( AutomaticTesterManager.globalPresenceTestNotifier )
        {
            AutomaticTesterManager.globalPresenceTestNotifier.notifyAll();
        }
    }
}