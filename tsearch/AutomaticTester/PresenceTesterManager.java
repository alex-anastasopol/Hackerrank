package ro.cst.tsearch.AutomaticTester;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.titledocument.TSDManager;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.URLMaping;

public class PresenceTesterManager implements Runnable
{
    private static Vector presenceTests;
    private boolean shutdown;
    private boolean isRunning;
    private Object starter;
    private Date startDate;
    
    //counter for the reset 
    
    //set to true if automatically started
    private boolean autoStart;
    
    private static Vector tCaseVector = new Vector();
    
    private String sPath;
    private String rootPath;
    public static String testCaseFolder = BaseServlet.FILES_PATH + "TestCases" ;
    public static String finishedTestCaseFolder = testCaseFolder + File.separator + "Finished";
    public static final Logger logger = Logger.getLogger(PresenceTesterManager.class);
    
    private int stopStartHour = 20;
    private int stopEndHour = 6;
    
    //vector with test cases
    public static Vector testCaseIndexes = new Vector();

    public static final String dateFormat = "MM/dd/yyyy'<BR>'HH:mm";
    public static final Object globalNotifier = new Object();
    public static final Object globalPresenceTestNotifier = new Object();
    
    public static final String REFRESH_TESTCASE = "testcase";
    public static final String REFRESH_PRESENCETEST = "presencetest";
    
    public static User testUser = null;
    
    private static PresenceTesterManager presenceTesterManagerInstance = null;
    
    private PresenceTesterManager(long searchId)
    {
        presenceTests = new Vector(10,10);
        shutdown = false;
        isRunning = false;
        startDate = new Date();
        starter = new Object();
        autoStart = false;
        
        initPresenceTests(searchId);
    }
    
   
    public static synchronized PresenceTesterManager getInstance(long searchId)
    {
        if( presenceTesterManagerInstance == null )
        {
            presenceTesterManagerInstance = new PresenceTesterManager(searchId);            
            presenceTesterManagerInstance.rootPath = BaseServlet.FILES_PATH;
            presenceTesterManagerInstance.sPath = BaseServlet.REAL_PATH + File.separator + "title-search";    
            
            Thread ptjRunner = new Thread(presenceTesterManagerInstance);
            ptjRunner.setName( "Presence Test Scheduler" );
            ptjRunner.setPriority( Thread.MIN_PRIORITY );
            ptjRunner.start();
        }
        
        return presenceTesterManagerInstance;
    }
    
    public void initPresenceTests(long searchId)
    {
        //Iterator i = implementedSites.keySet().iterator();
         // Iterator i = implementedSites.keySet().iterator();
        //=====================================================================
    	  //instantiate an object to store data from data base	
    	int testBranch =1;
    	
    	if( testBranch == 1 ){
    	
		  DatabaseTestCaseStoring	dTcS = new DatabaseTestCaseStoring();  
			  
		  //load the data from the database and stores the data in a vector
		  dTcS.readFileFromDataBase();	  
		  
		  presenceTestCaseBattery tcb = new presenceTestCaseBattery();

		  //get a vector with test cases from the DB
		  tCaseVector = dTcS.getTestCaseFromDb();
		  
		 // tcb.setBattery(tCaseVector);         
		  
		  int lengthVec = tCaseVector.size();
		  
		  testCase tc = new testCase();
		
		   boolean tst = false;
		   
		   //=================================================================
	       //generate an searchId
		    String sPath;
	        String rootPath;
	        
		    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		    
			String sToday = String.valueOf(calendar.get(Calendar.YEAR));
			sToday += "_" + String.valueOf(calendar.get(Calendar.MONTH) + 1);
			sToday += "_" + String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
	
		    rootPath = BaseServlet.FILES_PATH;
		    sPath = BaseServlet.REAL_PATH + File.separator + "title-search";
		
            String userPath = rootPath + TSDManager.TSDDir + File.separator + sToday + File.separator;
            
            testUser = new User(userPath);
            testUser.setUserAttributes( UserManager.getTSAdminUser() );
		            
		   //===============================================================================
    	
            
            for(int i=0 ; i < lengthVec  ;i++){
            //aici ar trebui introdus codul de creat si lansat threaduri 
               tc = (testCase) tCaseVector.elementAt(i); 
               
               //int state = ( (Integer) activityList.elementAt(i)).intValue();
             
               int state = Integer.parseInt( tc.getState() );
               
               
               //int act = tc.getVtestID();
               
               //testCaseIndexes.add(new Integer(act));
               
	            PresenceTesterJob PTJ = new PresenceTesterJob(i, tc , state );
	            presenceTests.add( PTJ );
	            
	            if( PTJ.isActive() ){
		            PTJ.start();
	            }
            }
        }
        //=============================
    }
    public void addTestCase(long id){
    	
		  DatabaseTestCaseStoring	dTcS = new DatabaseTestCaseStoring();  
		  testCase te=new testCase();
		  te=dTcS.readFileFromDataBase(id);	 
		 //  int state = Integer.parseInt( t.getState() );
		  int state=1;
		  PresenceTesterJob PTJ = new PresenceTesterJob(presenceTests.size()+1, te , state );
		 
		  PTJ.setTestId(id);
		  PTJ.setServerName(te.getVServerNAME());
		  presenceTests.add(PTJ); 
		  if( PTJ.isActive() )  PTJ.start();
		  //PresenceTesterThread PTT = new PresenceTesterThread( PTJ );
		 
		  
	        
		    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		    Calendar currentDateTimeCalendar = GregorianCalendar.getInstance();
		    String sToday = String.valueOf(calendar.get(Calendar.YEAR));
			sToday += "_" + String.valueOf(calendar.get(Calendar.MONTH) + 1);
			sToday += "_" + String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
	        
		  
		  if( currentDateTimeCalendar.get( GregorianCalendar.HOUR_OF_DAY ) < stopStartHour || currentDateTimeCalendar.get( GregorianCalendar.HOUR_OF_DAY ) > stopEndHour )
          {
          	//get the index as a property of PTJ
          	
          	 state = PTJ.getState();
          	
          	if( state == 1 ){
          	
              if(  PTJ.isActive()  )
              {
                  PresenceTesterThread presenceThread = new PresenceTesterThread( PTJ );
              }
              else
              {
                  PTJ.setLastStarted( false );
                 // PTJ.updateDB( (new Date()).getTime(), PresenceStatus.STATUS_DISABLED );
              }
              
          	}
          }
          else
          {
              PTJ.setLastStarted( false );
          }
		  
    }
    public Vector getAllPresenceTests()
    {
        return presenceTests;
    }
    
    public synchronized void ShutDown()
    {
        shutdown = true;
    }
    
    public boolean getRunningState()
    {
        synchronized( starter )
        {
            return isRunning;
        }
    }
    
    public void startUp()
    {
        synchronized( starter )
        {
            if (!isRunning)
            {
                starter.notifyAll();
            }
        }
    }
    
    
    public Vector getEnable(){
    	//returns a vector with the state of the cases if tey are enabled or disabled
    	
 	   DBConnection conn = null;     //modified

 	   Vector listOfActivity = new Vector();
        
 	  try
        {
            conn = ConnectionPool.getInstance().requestConnection();
            
           for(int i=0; i <  tCaseVector.size() ; i++){
            
        	testCase tcs =  (testCase)  tCaseVector.elementAt(i); 
        	
        	int counter = tcs.getVtestID();
        	
            DatabaseData dbData = conn.executeSQL( "SELECT EnabledOrDisabled FROM "+DBConstants.TABLE_PARENTSITE_TESTS+" WHERE test_id='" + counter + "'" );
            
            if( dbData.getRowNumber() >= 1 )
            {
            
                if( Integer.parseInt(dbData.getValue( 1, 0 ).toString()) == 1 )
                {
             
                	Integer INT = new Integer(1);
                	
                	listOfActivity.add( INT );
                }
                else
                {
                	
                	Integer INT = new Integer(0);
                	
                	listOfActivity.add( INT );
                }
            }
          }
           
          return listOfActivity;
        
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                ConnectionPool.getInstance().releaseConnection(conn);
            }
            catch (BaseException e)
            {
         	   e.printStackTrace();
            }
        }
        
        return null;
        
 } 
    
    public void run()
    {
        //start all searches
        int i;
        
	    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
	    
		String sToday = String.valueOf(calendar.get(Calendar.YEAR));
		sToday += "_" + String.valueOf(calendar.get(Calendar.MONTH) + 1);
		sToday += "_" + String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
        
        String userPath = rootPath + TSDManager.TSDDir + File.separator + sToday + File.separator;
        
        User testUser = new User(userPath);
        testUser.setUserAttributes( UserManager.getTSAdminUser() );
        logger.info( "Presence Tester Manager thread started" );
        
        //wait to be started if not started automatically
        
        if( !autoStart )
        {
            synchronized(starter)
            {
    	        try
    	        {
    	            starter.wait();
    	        }
    	        catch( Exception e )
    	        {
    	            e.printStackTrace();
    	        }
    	        
    	        isRunning = true;
            }
        }
        else
        {
            isRunning = true;
        }
        
        startDate = new Date();
       
        
        //do presence tests for all active servers if we are not in the inactivity interval

        Calendar currentDateTimeCalendar = GregorianCalendar.getInstance();
        for( i = 0 ; i < presenceTests.size() ; i++ )
        {
            PresenceTesterJob PTJ = (PresenceTesterJob) presenceTests.elementAt( i );
            //test if we are in the inactivity time interval ( stopStartHour - stopEndHour )
            if( currentDateTimeCalendar.get( GregorianCalendar.HOUR_OF_DAY ) < stopStartHour || currentDateTimeCalendar.get( GregorianCalendar.HOUR_OF_DAY ) > stopEndHour )
            {
            	//get the index as a property of PTJ
            	
            	int state = PTJ.getState();
            	
            	if( state == 1 ){
            	
                if(  PTJ.isActive()  )
                {
                    PresenceTesterThread presenceThread = new PresenceTesterThread( PTJ );
                }
                else
                {
                    PTJ.setLastStarted( false );
                   // PTJ.updateDB( (new Date()).getTime(), PresenceStatus.STATUS_DISABLED );
                }
                
            	}
            }
            else
            {
                PTJ.setLastStarted( false );
            }
        }
        
        while( true )
        {
	        shutdown = false;

	        logger.info( "Automatic Presence tester scheduler started!" );
	        	        
	        while(!shutdown)
	        {
		        //sleep for 500 millis
		        try
		        {
		         	Thread.sleep( 500 );
		        }
		        catch(Exception e)
		        {
		            e.printStackTrace();
		        }
		           
                 currentDateTimeCalendar = GregorianCalendar.getInstance();
                //test if we are in the inactivity time interval ( stopStartHour - stopEndHour )
                if( currentDateTimeCalendar.get( GregorianCalendar.HOUR_OF_DAY ) >= stopStartHour && currentDateTimeCalendar.get( GregorianCalendar.HOUR_OF_DAY ) <= stopEndHour )
                {
                    //we're not supposed to perform presence tests during this interval
                    continue;
                }
                
		        //schedule the presence test jobs
		        for( i = 0 ; i < presenceTests.size() ; i++ )
		        {
		            PresenceTesterJob PTJ = (PresenceTesterJob) presenceTests.elementAt( i );
		            if( PTJ.isActive() )
		            {
		                if( !PTJ.isRunning() )
		                {
		                    if( (PTJ.getInterval() == AutomaticTesterManager.ONE_TIME_TEST) || ( (currentDateTimeCalendar.getTime()).getTime() - PTJ.getLastStarted().getTime() > PTJ.getInterval() ) )
		                    {
		                        PresenceTesterThread PTT = new PresenceTesterThread( PTJ );
		                    }
		                }
		            }
		            else
		            {
		                if( !PTJ.isRunning() )
		                {
		                    Date lastStarted = PTJ.getLastStarted();
		                    
		                    if( (PTJ.getInterval() == AutomaticTesterManager.ONE_TIME_TEST) || ( (currentDateTimeCalendar.getTime()).getTime() - lastStarted.getTime() > PTJ.getInterval() ) )
		                    {
		                       //PTJ.updateDB( (new Date()).getTime(), PresenceStatus.STATUS_DISABLED );
		                       PTJ.setLastStarted( false );
		                    }
		                }
		            }
		        }
	        }
	        
	        logger.info( "Automatic test scheduler stopped!" );

	        //wait to be started
	        synchronized(starter)
	        {
	            isRunning = false;
		        try
		        {
		            starter.wait();
		        }
		        catch( Exception e )
		        {
		            e.printStackTrace();
		        }
		        
		        isRunning = true;
	        }

        }
    }
          
    public boolean shutdownInitiated()
    {
        return shutdown == true;
    }
    
    public void setAutoStarted( boolean autoStartValue )
    {
        autoStart = autoStartValue;
    }
    
    public void setStopStartHour( int startHour )
    {
        if( startHour >= 0 && startHour <= 24 )
        {
            stopStartHour = startHour;
        }
    }
    
    public void setStopEndHour( int stopEndHour )
    {
        if( stopEndHour >= 0 && stopEndHour <= 24 )
        {
            this.stopEndHour = stopEndHour;
        }
    }
    
    public int getStopStartHour()
    {
        return stopStartHour;
    }
    
    public int getStopEndHour()
    {
        return stopEndHour;
    }
}