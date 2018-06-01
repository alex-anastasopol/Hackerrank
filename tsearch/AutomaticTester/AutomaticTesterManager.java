package ro.cst.tsearch.AutomaticTester;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.math.*;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import ro.cst.tsearch.monitor.SNMPAgent;
import ro.cst.tsearch.servers.parentsite.Company;
import ro.cst.tsearch.servlet.*;
import ro.cst.tsearch.threads.*;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.utils.URLMaping;
import ro.cst.tsearch.data.*;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.community.*;
import ro.cst.tsearch.bean.*;
import ro.cst.tsearch.titledocument.*;
import ro.cst.tsearch.utils.*;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.XStream11XmlFriendlyReplacer;
import com.thoughtworks.xstream.io.xml.XppDriver;

import ro.cst.tsearch.*;

public class AutomaticTesterManager implements Runnable
{
    private Vector activeTestCaseList;
    private Vector automaticSearchJobs;
    private Vector presenceTests;
    private Hashtable testSuites;
    private boolean wasRefreshedAtCount0;
    private boolean shutdown;
    private boolean isRunning;
    private Object starter;
    private Date startDate;
    
    private String sPath;
    private String rootPath;
    public static String testCaseFolder = BaseServlet.FILES_PATH + "TestCases" ;
    public static String finishedTestCaseFolder = testCaseFolder + File.separator + "Finished";
    private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
    public static final Logger logger = Logger.getLogger(AutomaticTesterManager.class);
    private boolean hasChanged;
    private HashMap implementedSites = null;
    
    public static final int START = 0;
    public static final int STOP = 1;
       
    public static final int UPDATE_TIME = 2;
    public static final int DELETE = 3;
    public static final int ENABLE = 4;
    public static final int DISABLE = 5;
    public static final int SHOW_PRESENCETEST_GRAPHS = 6;
    public static final int SORT_TESTCASES_ASC = 7;
    public static final int SORT_TESTCASES_DESC = 8;
    
    public static final int SORT_PRESENCETEST_ASC = 9;
    public static final int SORT_PRESENCETEST_DESC = 10;
    
    public static final int SORT_TESTCASES_BY_COUNTY = 11;
    public static final int SORT_TESTCASES_BY_LAST_TEST_STATUS = 12;
    public static final int SORT_TESTCASES_BY_EXEC_STATUS = 13;
    public static final int SORT_TESTCASES_BY_TEST_SUITE  = 14;
    
    public static final int SORT_PRESENCETESTS_BY_SITENAME = 15;
    public static final int SORT_PRESENCETESTS_BY_LAST_TEST_STATUS = 16;
    public static final int SORT_PRESENCETESTS_BY_EXEC_STATUS = 17;
    
    public static final int ONE_TIME_TEST = -2;
    
    public static final int START_PRESENCE_TESTER = 18;
    public static final int STOP_PRESENCE_TESTER = 19;
    
    public static final int UPDATE_PRESENCE_TESTS_PERIOD = 20;
    
    public static final int RESET_SESSION = 21;
    
    public static final int GROUP_TESTSUITE = 22;
    public static final int MOVE_TESTSUITE = 23;
    
    public static final int UPDATE_REFFERENCE = 24;
    
    public static final String dateFormat = "MM/dd/yyyy'<BR>'HH:mm";
    public static final Object globalNotifier = new Object();
    //starter/shutdown lock 
    public static final Object SD = new Object();
    public static final Object globalPresenceTestNotifier = new Object();
    
    public static final String REFRESH_TESTCASE = "testcase";
    public static final String REFRESH_PRESENCETEST = "presencetest";
    
    private static AutomaticTesterManager automaticTesterManagerInstance = null;
    
    private AutomaticTesterManager()
    {
        activeTestCaseList = new Vector(10,10);
        automaticSearchJobs = new Vector(10,10);
        presenceTests = new Vector(10,10);
        testSuites = null;
        shutdown = false;
        isRunning = false;
        startDate = new Date();
        starter = new Object();
        wasRefreshedAtCount0 = false;
        
        //initialize the saved test case jobs
        initTestCaseList();
        
        //initialize the presence tests
        /*
        implementedSites = DBManager.getImplementedSites(BaseServlet.REAL_PATH);
        initPresenceTests();
        */
    }
  
    public void setRefreshed(boolean b){
    	wasRefreshedAtCount0 = b;
    }
    
    public boolean wasRefreshed(){
    	return wasRefreshedAtCount0;
    }
   
    public static synchronized AutomaticTesterManager getInstance()
    {
        if( automaticTesterManagerInstance == null )
        {
            automaticTesterManagerInstance = new AutomaticTesterManager();
            automaticTesterManagerInstance.rootPath = BaseServlet.FILES_PATH;
            automaticTesterManagerInstance.sPath = BaseServlet.REAL_PATH + File.separator + "title-search";
            
            //initialize the test suites
            automaticTesterManagerInstance.initTestSuites();
            
            //test the existance of the finished folder...
            File testFinished = new File( finishedTestCaseFolder );
            if( !testFinished.exists() )
            {
                testFinished.mkdir();
            }
            
            
            Thread atmRunner = new Thread(automaticTesterManagerInstance);
            atmRunner.setName( "Automatic Test Scheduler" );
            atmRunner.setPriority( Thread.MIN_PRIORITY );
            atmRunner.start();
        }
        
        return automaticTesterManagerInstance;
    }
    
    public synchronized void addNewTestSearch( AutomaticSearchMonitor toStart )
    {   
        //add another search to the list
        activeTestCaseList.add( toStart );
    }
    
    public synchronized void removeTestSearch( AutomaticSearchMonitor toRemove )
    {
        activeTestCaseList.remove( toRemove );
    }
    
    public static Search loadTestCase( String savedTestXMLName )
    {
        Search returnedSearch = null;
        XStreamManager xstream = XStreamManager.getInstance();
		
        try
        {
            //read the xml saved search
            Reader inputReader = new FileReader( testCaseFolder + File.separator + savedTestXMLName );
            StringBuffer sb = new StringBuffer();
            char[] buffer= new char[1024];
			while (true)
			{
				int bytes_read= inputReader.read(buffer);
				if (bytes_read == -1)
					break;
				
				sb.append(buffer, 0, bytes_read);
			}
			inputReader.close();
			
            returnedSearch = (Search)xstream.fromXML(sb.toString());
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        
        return returnedSearch;
    }
    
    public static Search loadCandidate( String savedTestXMLName, String savedFolder )
    {
        Search returnedSearch = null;
        
		XStreamManager xstream = XStreamManager.getInstance();
        try
        {
            //read the xml saved search
            Reader inputReader = new FileReader( AutomaticTesterManager.finishedTestCaseFolder + File.separator + "ALLFILES_" + AutomaticTesterManager.getTestFolderName( savedTestXMLName ) + File.separator + savedFolder + File.separator + savedTestXMLName );
            StringBuffer sb = new StringBuffer();
            char[] buffer= new char[1024];
			while (true)
			{
				int bytes_read= inputReader.read(buffer);
				if (bytes_read == -1)
					break;
				
				sb.append(buffer, 0, bytes_read);
			}
			inputReader.close();
            
			returnedSearch = (Search)xstream.fromXML(sb.toString());


        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        
        return returnedSearch;
    }    
    
    private void initTestCaseList()
    {
        File[] testList = listAllTestCases();

        if( testList != null )
        {
            for( int i = 0 ; i < testList.length ; i++ )
            {
            	try {
	                if( mustStartTestCase( testList[i].getName() ) )
	                {
	                    Search s = AutomaticTesterManager.loadTestCase( testList[i].getName() );
	                    
	                    if( s != null )
	                    {                        
	    	                AutomaticSearchJob searchJob = new AutomaticSearchJob( s,testList[i].getName() , enabledTestCase( testList[i].getName() ) );
	    	                automaticSearchJobs.add( searchJob );
	    	                searchJob.setCrashed(false);
	    	                searchJob.start();
	                    }
	                }
                } catch (Exception e) {
					e.printStackTrace();
				}
            }
        }
        setRefreshed(false);
    }
    
    private void initTestSuites()
    {
        //load the test cases and test suites from the database
        testSuites = DBManager.getTestCaseSuites( );
    }
    
    public Hashtable getTestSuites()
    {
        return testSuites;
    }
    
    public void addJobToTestSuite( AutomaticSearchJob ASJ, String testSuiteName, String testSuiteDescription )
    {
        if( "-1".equals( testSuiteName ))
        {
            DBManager.setTestSuiteJob( ASJ.getXMLFile(), -1 );
        }
        else
        {
            TestSuite testSuite = (TestSuite) testSuites.get( testSuiteName );
            long testSuiteId = -1;
            
            if( testSuite == null )
            {
                testSuite = new TestSuite( testSuiteName, testSuiteDescription, ASJ.isActive() );
                
                testSuiteId = DBManager.newTestSuite( testSuiteName, testSuiteDescription );
                
                testSuite.setId( testSuiteId );
                testSuites.put( testSuiteName, testSuite );
            }
            
            if( testSuite.getId() != -1 )
            {
                DBManager.setTestSuiteJob( ASJ.getXMLFile(), testSuite.getId() );
            }
           
            testSuite.addJob( ASJ );
        }
    }
    
    public void removeJobFromTestSuite( AutomaticSearchJob ASJ )
    {
        Enumeration tS = testSuites.elements();
        while( tS.hasMoreElements() )
        {
            TestSuite testSuite = (TestSuite) tS.nextElement();
            
            Vector groupedJobs = testSuite.getTestCases();
            
            groupedJobs.removeElement( ASJ );
        }
    }
    
    public TestSuite getTestSuite( AutomaticSearchJob ASJ )
    {
        Enumeration tS = testSuites.elements();
        while( tS.hasMoreElements() )
        {
            TestSuite testSuite = (TestSuite) tS.nextElement();
            
            Vector groupedJobs = testSuite.getTestCases();
            
            if( groupedJobs.contains( ASJ ) )
            {
                return testSuite;
            }
        }
        
        return null;
    }
    
    /*private void initPresenceTests()
    {
        Iterator i = implementedSites.keySet().iterator();
        
        while( i.hasNext() )
        {
            Long siteID = ( Long ) i.next();
            String serverName = (String) implementedSites.get( siteID );
            
            if( siteID.intValue() != 0 )
            {
	            PresenceTesterJob PTJ = new PresenceTesterJob( siteID.intValue(), serverName );
	            if( PTJ.getServerInfo() != null )
	            {
		            presenceTests.add( PTJ );
		            PTJ.start();
	            }
            }
        }
    }*/
    
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
        logger.info( "Automatic Search Test Case thread started" );
        
        //wait to be started
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
        
        activeTestCaseList.clear();
        startDate = new Date();

        //start all active test cases
        
        for( i = 0 ; i < automaticSearchJobs.size() ; i++ )
        {
            AutomaticSearchJob searchJob = (AutomaticSearchJob) automaticSearchJobs.elementAt(i);
            synchronized(searchJob)
            {
	            if( searchJob.isActive() )
	            {
	                searchJob.setLastStarted( true );
                    searchJob.setCrashed(false);
	                AutomaticSearchMonitor autThread = new AutomaticSearchMonitor( searchJob, testUser, sPath);
	            }
            }
        }
        
        //do presence tests for all active servers
        /*
        for( i = 0 ; i < presenceTests.size() ; i++ )
        {
            PresenceTesterJob PTJ = (PresenceTesterJob) presenceTests.elementAt( i );
            if( PTJ.isActive() )
            {
                PresenceTesterThread presenceThread = new PresenceTesterThread( PTJ );
            }
            else
            {
                PTJ.setLastStarted( false );
                PTJ.updateDB( (new Date()).getTime(), PresenceStatus.STATUS_DISABLED );
            }
        }
        */
        while( true )
        {
	        shutdown = false;
	        synchronized(AutomaticTesterManager.SD){
	        	AutomaticTesterManager.SD.notifyAll();
	        }
	        logger.info( "Automatic test scheduler started!" );
	        	        
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
		        
	            //search for a search job that has to be started
		        //the time between the job's last started time and execution interval has elapsed
		        for( i = 0 ; i < automaticSearchJobs.size() ; i++ )
		        {
		            AutomaticSearchJob searchJob = (AutomaticSearchJob) automaticSearchJobs.elementAt(i);
		            synchronized(searchJob)
		            {
			            if( searchJob.isActive() )
			            {
			                //if job is active, determine the time elapsed since the last test
			                Date lastStarted = searchJob.getLastStarted();
			                
			                //if time elapsed and job not running
			                if( lastStarted !=null )
			                {
				                if( (searchJob.getInterval() == ONE_TIME_TEST) || ( (new Date()).getTime() - lastStarted.getTime() > searchJob.getInterval() ) )
				                {
				                    //must start this test case
				                    searchJob.setLastStarted( true );
				                    searchJob.setCrashed(false);
				                    AutomaticSearchMonitor autThread = new AutomaticSearchMonitor( searchJob, testUser, sPath);
				                }
			                }
			            }
		            }
		        }

		        //schedule the presence test jobs
		        /*
		        for( i = 0 ; i < presenceTests.size() ; i++ )
		        {
		            PresenceTesterJob PTJ = (PresenceTesterJob) presenceTests.elementAt( i );
		            if( PTJ.isActive() )
		            {
		                if( !PTJ.isRunning() )
		                {
		                    if( (PTJ.getInterval() == ONE_TIME_TEST) || ( (new Date()).getTime() - PTJ.getLastStarted().getTime() > PTJ.getInterval() ) )
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
		                    
		                    if( (PTJ.getInterval() == ONE_TIME_TEST) || ( (new Date()).getTime() - lastStarted.getTime() > PTJ.getInterval() ) )
		                    {
		                       PTJ.updateDB( (new Date()).getTime(), PresenceStatus.STATUS_DISABLED );
		                       PTJ.setLastStarted( false );
		                    }
		                }
		            }
		        }
		        */
	        }
	        
	        synchronized(starter)
	        {
	            isRunning = false;
	            
	            //killing all automatic search threads that may be running
	            for( i = 0 ; i < activeTestCaseList.size() ; i++ )
	            {
	            	AutomaticSearchMonitor searchMonitor = ( AutomaticSearchMonitor ) activeTestCaseList.elementAt( i );
	            	
	            	searchMonitor.terminate();
	            }
	            setRefreshed(false);
	        }
	        
	        logger.info( "Automatic test scheduler stopped!" );
	        //shutdown complete, notify SD
	        synchronized(AutomaticTesterManager.SD){
	        	AutomaticTesterManager.SD.notifyAll();
	        }
	        //wait to be started
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
    }
    
    public File[] listAllTestCases()
    {
        File testCaseFolderFile = new File( testCaseFolder );       
        
        return testCaseFolderFile.listFiles();
    }
    
    public Vector getJobs()
    {
        return automaticSearchJobs;
    }
    
    public int getActiveJobsCount(){
    	int n = 0;
    	for (int i=0; i< automaticSearchJobs.size(); i++){
    		if (((AutomaticSearchJob)automaticSearchJobs.get(i)).isActive()) n++;
    	}
    	return n;
    }
    
    private boolean mustStartTestCase( String xmlTestCaseFileName )
    {
        return xmlTestCaseFileName.endsWith( ".xml" ) && !xmlTestCaseFileName.startsWith( "DISABLED_" );
    }
    
    private boolean enabledTestCase( String xmlTestCaseFileName )
    {
        return !xmlTestCaseFileName.startsWith( "DISABLED_" );
    }
    
    public Hashtable getStatus()
    {
        Hashtable threadStatus = new Hashtable();
        int i;
        for( i = 0 ; i < activeTestCaseList.size() ; i++ )
        {
            AutomaticSearchMonitor autThread = (AutomaticSearchMonitor) activeTestCaseList.elementAt(i);
            
            String status = autThread.hasFinished() ? "DONE" : "TESTING";
            
            threadStatus.put( autThread.getXMLFilename(), status );
            threadStatus.put( autThread.getXMLFilename() + "_DIAGS", autThread.compareSearches() );            
        }
        
        return threadStatus;
    }
    
    public static String getTestFolderName( String XMLTestCaseFileName )
    {
        int index = XMLTestCaseFileName.indexOf( ".xml" );
        String folderName = XMLTestCaseFileName;
        
        if( index >= 0 )
        {
            folderName = XMLTestCaseFileName.substring( 0, XMLTestCaseFileName.indexOf( ".xml" ));
        }
        
        if( folderName.startsWith( "DISABLED_" ) )
        {
            folderName = folderName.substring( 9 );
        }
        
        return folderName;
    }
    
    public synchronized AutomaticSearchJob getJob( String XMLFileName )
    {
        for( int i = 0 ; i < automaticSearchJobs.size() ; i++ )
        {
            AutomaticSearchJob j = (AutomaticSearchJob) automaticSearchJobs.elementAt(i);
            if( j.getXMLFile().equals( XMLFileName ) )
            {
                return j;
            }
        }
        return null;
    }
    
    public synchronized void removeJob( AutomaticSearchJob j )
    {
        automaticSearchJobs.remove( j );
        removeJobFromTestSuite( j );
    }
    
    public boolean shutdownInitiated()
    {
   		return shutdown;
    }
    
    public void updateTestCaseList(Search s)
    {
        //updates the list of test cases with this search
        
        //determine the saved search filename, to reload it from the disk
		SearchAttributes sa = s.getSa();
		SearchAttributes osa = s.getOrigSA();
		/*
		String filename = DBManager.getStateForId( Long.parseLong( sa.getAtribute( SearchAttributes.P_STATE ) ) ).getName() + "_" 
		+ DBManager.getCountyForId( Long.parseLong( sa.getAtribute( SearchAttributes.P_COUNTY ) ) ).getName() + "_" 
		+ osa.getAtribute( SearchAttributes.P_CITY ) + "_" + osa.getAtribute( SearchAttributes.OWNER_FNAME ) 
		+ osa.getAtribute( SearchAttributes.OWNER_MNAME ) + osa.getAtribute( SearchAttributes.OWNER_LNAME )
		+ osa.getAtribute( SearchAttributes.LD_INSTRNO ) + osa.getAtribute( SearchAttributes.LD_BOOKNO )
		+ osa.getAtribute( SearchAttributes.LD_PAGENO ) + osa.getAtribute( SearchAttributes.LD_BOOKPAGE )
		+ osa.getAtribute( SearchAttributes.LD_SUBDIVISION ) + osa.getAtribute( SearchAttributes.LD_LOTNO )
		+ osa.getAtribute( SearchAttributes.LD_SUBDIV_NAME ) + osa.getAtribute( SearchAttributes.LD_SUBDIV_BLOCK )
		+ osa.getAtribute( SearchAttributes.LD_SUBDIV_PHASE ) + osa.getAtribute( SearchAttributes.LD_SUBDIV_TRACT )
		+ osa.getAtribute( SearchAttributes.LD_SUBDIV_SEC ) + osa.getAtribute( SearchAttributes.LD_SUBDIV_CODE )
		+ osa.getAtribute( SearchAttributes.LD_PARCELNO ) + osa.getAtribute( SearchAttributes.LD_SUBDIV_UNIT )
		+ osa.getAtribute( SearchAttributes.P_STREETNO ) + osa.getAtribute( SearchAttributes.P_STREETDIRECTION )
		+ osa.getAtribute( SearchAttributes.P_STREETNAME ) + osa.getAtribute( SearchAttributes.P_STREETSUFIX )
		+ osa.getAtribute( SearchAttributes.P_STREETUNIT ) + osa.getAtribute( SearchAttributes.P_ZIP )
		+ osa.getAtribute( SearchAttributes.P_STREET_FULL_NAME )
		+ "__testCase.xml";
		*/
		
		String shortName = Products.getProductShortNameStringLength3(s.getID());
		String filename = sa.getAbstractorFileName() + ".xml";
		filename = filename.replaceAll( "TSR", "TC" );
		filename = filename.replaceAll( shortName, "TC" );

		
		//delete the previous saved context (if any)
		FileUtils.deleteDir( new File(AutomaticTesterManager.testCaseFolder + File.separator + "ALLFILES_" + AutomaticTesterManager.getTestFolderName( filename )) );
		
		//save the entire context of the search
		AutomaticTesterManager.copyFolder( s.getSearchDir(), AutomaticTesterManager.testCaseFolder + File.separator + "ALLFILES_" + AutomaticTesterManager.getTestFolderName( filename ) );
		
		//check to see if it already exists in the list
		AutomaticSearchJob searchJob = getJob( filename );
        if( searchJob != null )
        {
            //already there, remove it
            removeJob( searchJob );
        }
        
        //create new job and add it to the list
        
        Search newSearch = AutomaticTesterManager.loadTestCase( filename );
        if( newSearch != null )
        {
            searchJob = new AutomaticSearchJob( newSearch, filename , enabledTestCase( filename ) );
            searchJob.setCrashed(false);
            searchJob.setLastStarted( false );
            automaticSearchJobs.add( searchJob );
            searchJob.start();
            
            DBManager.insertTestcase( searchJob );
        }
        //refresh the page if needed
        synchronized(AutomaticTesterManager.globalNotifier){
        	AutomaticTesterManager.globalNotifier.notifyAll();
        }
    }
    
    public static boolean copyFolder(String source, String destination)
    {
        File sourceFolder = new File( source );
        File destFolder = new File( destination );
		if (sourceFolder.exists())
		{
			if (sourceFolder.isDirectory())
			{
			    if( !destFolder.exists() )
			    {
			        destFolder.mkdirs();
			    }
			    
				String[] children= sourceFolder.list();
				for (int i= 0; i < children.length; i++)
				{
					boolean success= copyFolder(sourceFolder.getAbsolutePath() + File.separator + children[i], destFolder.getAbsolutePath() + File.separator + children[i]);
					if (!success)
					{
						return false;
					}
				}
				
				return true;
			}
			else
			{
			    try
			    {
                    //copy the listed file in the destination folder
                    FileCopy.copy( sourceFolder.getAbsolutePath(), destFolder.getAbsolutePath() );
                }
			    catch (IOException e)
			    {
                    e.printStackTrace();
                    return false;
                }
			    
			    return true;
			}
		}
		else
		{
		    if( !destFolder.exists() )
		    {
		        destFolder.mkdirs();
		    }
			return true;
		}
    }
    
    public void removeTestSuite( TestSuite testSuite )
    {
        testSuites.remove( testSuite.getName() );
        DBManager.deleteTestSuite( testSuite );
    }
}