package ro.cst.tsearch.AutomaticTester;

import java.io.*;

import java.util.*;

import org.apache.log4j.Logger;
import ro.cst.tsearch.*;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.utils.*;

public class AutomaticSearchJob
{
    public static final int ACTION_START = 0;
    public static final int ACTION_STOP = 1;
    public static final int ACTION_CLEAN = 2;
    public static final int ACTION_DELETE = 3;
    public static final int ACTION_INTERVAL = 4;
    
    private Date lastStartedOn;
    private Search searchEtalon;
    private String XMLfile;
    private long timeBetweenTests;
    private boolean active;
    private String lastStatus;
    private boolean crashed = false;
    
    public static final Logger logger = Logger.getLogger(AutomaticSearchJob.class);
    
    public AutomaticSearchJob( Search s, String XMLfile, boolean active )
    {
    	setCrashed(false);
        searchEtalon = s;
        SearchAttributes sa = s.getSa();
        
        //sa.setAtribute(SearchAttributes.ABSTRACTOR_FILENO, "UnknownFileNo_" + searchEtalon.getID());
		//sa.setAbstractorFileName(s);
		
        this.active = active;
        this.XMLfile = XMLfile;
        
        //set default time between tests - one time execution
        //if one time execution is set, the test is run only once and disabled immediately after the test's completion
        timeBetweenTests = AutomaticTesterManager.ONE_TIME_TEST;
        lastStartedOn = null;
        
        lastStatus = getLastStatus();
    }
    
    public void setCrashed(boolean c){
    	crashed = c;
    }
    
    public boolean getCrashed(){
    	return crashed;
    }
    public void setInterval( long millis )
    {
        timeBetweenTests = millis;
    }
    
    public long getInterval()
    {
        return timeBetweenTests;
    }
    
    public void start()
    {
        active = true;
    }
    
    public void stop()
    {
        active = false;
    }
    
    public Date getLastStarted()
    {
        return lastStartedOn;
    }
    
    public void setLastStarted(boolean reset)
    {
        lastStartedOn = reset ? null : new Date();
    }
    
    public Search getSearch()
    {
        return searchEtalon;
    }
    
    public void updateSearch( Search newSearchEtalon )
    {
        searchEtalon = newSearchEtalon;
    }
    
    public String getXMLFile()
    {
        return XMLfile;
    }
    
    public boolean isActive()
    {
        return active;
    }
    
    public void cleanReports( boolean deleteall )
    {
    	String testResultsFolder = AutomaticTesterManager.finishedTestCaseFolder + File.separator + AutomaticTesterManager.getTestFolderName(XMLfile);
    	File resultsFolder = new File( testResultsFolder );
    	if( resultsFolder.exists() )
    	{
			File[] HTMLResults = resultsFolder.listFiles();
			for( int i = 0 ; i < HTMLResults.length ; i++ )
			{
			    if( HTMLResults[i].isFile() )
			    {
			        HTMLResults[i].delete();
			    }
			}
    	}
		
    	String allCandFolder = AutomaticTesterManager.finishedTestCaseFolder + File.separator + "ALLFILES_" + AutomaticTesterManager.getTestFolderName(XMLfile);
    	File allCandFolderFile = new File( allCandFolder );
    	logger.info(" deleting " + allCandFolder);
    	if( allCandFolderFile != null )
    	{
    	    if( allCandFolderFile.exists() )
    	    {
    	        FileUtils.deleteDir( allCandFolderFile );
    	    }
    	}
    	
		if( deleteall )
		{		    
		    File xmlFile = new File( AutomaticTesterManager.testCaseFolder + File.separator + XMLfile );
		    File xmlTestFile = new File( AutomaticTesterManager.finishedTestCaseFolder + File.separator + XMLfile );
		    File testCaseFiles = new File( AutomaticTesterManager.testCaseFolder + File.separator + "ALLFILES_" + AutomaticTesterManager.getTestFolderName(XMLfile) );
		    
		    logger.info(" deleting test results folder "+resultsFolder.delete());
		    logger.info(" deleting finished test case xml " + xmlTestFile.delete());		    
		    logger.info(" deleting test case xml " + xmlFile.delete());
		    FileUtils.deleteDir( testCaseFiles );
            
            DBManager.deleteTestCase(this);
		}
    }
    
    private String getLastStatus()
    {
        String testResultsFolder = AutomaticTesterManager.finishedTestCaseFolder + File.separator + AutomaticTesterManager.getTestFolderName(XMLfile);
        File[] listing = ((new File(testResultsFolder))).listFiles();
        
        File latestTest = null;
        
        if(listing != null)
        {
	        for( int i = 0 ; i < listing.length ; i++ )
	        {
	            if( !listing[i].isDirectory() )
	            {
	                if( latestTest == null )
	                {
	                    latestTest = listing[i];
	                }
	                else
	                {
	                    if( latestTest.lastModified() < listing[i].lastModified() )
	                    {
	                        latestTest = listing[i];
	                    }
	                }
	            }
	        }
        }
        
        if( latestTest != null )
        {
	        if( latestTest.getName().indexOf( "OK_" ) >= 0 )
	        {
	            return "OK";
	        }
	        else
	        {
	            return "FAILED";
	        }
        }
        else
        {
            return "N/A";
        }
    }
    
    public void setLastStatus( String status )
    {
        lastStatus = status;
    }
    
    public String getLastExecutionStatusString()
    {
    	if (crashed){
    		return "<FONT color=\"#FF0000\">CRASHED</font>";
    	} else if( AutomaticTesterManager.getInstance().getRunningState() 
    			&& lastStartedOn == null && active ){
        	return "<FONT color=\"#00AA00\">TESTING</FONT>";
        }
        else if( active )
        {
            return "ENABLED";
        }
        else
        {
            return "<FONT color=\"#000000\">DISABLED</FONT>";
        }
    }
    
    
    /**
     * replaces this testcase with the result of the last execution
     * it will be used as a testcase from now on
     */
    public void replaceWithLastCandidate()
    {
        String testResultsFolder = AutomaticTesterManager.finishedTestCaseFolder + File.separator + "ALLFILES_" + AutomaticTesterManager.getTestFolderName(XMLfile);
        File[] listing = ((new File(testResultsFolder))).listFiles();
        
        File latestTest = null;
        
        int i;
        
        //find the result of the last test
        if(listing != null)
        {
            for( i = 0 ; i < listing.length ; i++ )
            {
                if( listing[i].isDirectory() && !".".equals( listing[i].getName() ) && !"..".equals( listing[i].getName() ) )
                {
                    if( latestTest == null )
                    {
                        latestTest = listing[i];
                    }
                    else
                    {
                        if( latestTest.lastModified() < listing[i].lastModified() )
                        {
                            latestTest = listing[i];
                        }
                    }
                }
            }
        }
        
        //get the listing for the last test
        File[] lastTestListing = latestTest.listFiles();
        if( lastTestListing != null )
        {  
            for( i = 0 ; i < lastTestListing.length ; i ++ )
            {
                if( lastTestListing[i].isFile() )
                {
                    if( lastTestListing[i].getName().equals( XMLfile )  )
                    {
                        //xml
                        FileCopy.copy( lastTestListing[i], new File( AutomaticTesterManager.testCaseFolder + File.separator + XMLfile ) );
                    }
                    else
                    {
                        FileCopy.copy( lastTestListing[i], new File( AutomaticTesterManager.testCaseFolder + File.separator + "ALLFILES_" + AutomaticTesterManager.getTestFolderName(XMLfile) + File.separator + lastTestListing[i].getName() ) );
                    }
                }
                else if( lastTestListing[i].isDirectory() )
                {
                    FileCopy.copy( lastTestListing[i], new File( AutomaticTesterManager.testCaseFolder + File.separator + "ALLFILES_" + AutomaticTesterManager.getTestFolderName(XMLfile) + File.separator + lastTestListing[i].getName() ) );
                }
            }
            
            Search newSearch = AutomaticTesterManager.loadTestCase( XMLfile );
            searchEtalon = newSearch;
        }
    }
    
    public String getLastStatusString()
    {
            return lastStatus;
    }
    
    public String getAbstractorFileName()
    {
    	String shortName = Products.getProductShortNameStringLength3(searchEtalon.getID());
        String abstractorFileName = searchEtalon.getSa().getAbstractorFileName();
        abstractorFileName = abstractorFileName.replaceAll( "TSR", "TC" );
        abstractorFileName = abstractorFileName.replaceFirst( shortName , "TC");
		
        if( "".equals( abstractorFileName ) )
        {
            abstractorFileName = XMLfile.replaceAll( "\\.xml", "" );
        }
        
        return abstractorFileName;
    }
    
    public String getViewSearchOrder(long searchId)
    {
    	if ( this.getXMLFile()  == null || AutomaticTesterManager.getTestFolderName( this.getXMLFile()) == null )
    	{
        	return "&nbsp;";
    	}
    	
    	String test_path = new String (AutomaticTesterManager.testCaseFolder + File.separator + "ALLFILES_" +
    			AutomaticTesterManager.getTestFolderName( this.getXMLFile() ) +
    			File.separator + "orderFile.html");
    	String path = new String ("TestCases" + File.separator + "ALLFILES_" +
    			AutomaticTesterManager.getTestFolderName( this.getXMLFile() ) +
    			File.separator + "orderFile.html");
    	
    	File orderFile = new File (test_path);

    	if (orderFile.exists() == false)
    	{
    		return "&nbsp;";
    	}

    	String ret="<A target=\"_blank\" href=\"/title-search/fs?f="+path+"&searchId="+searchId+"\"><img src=\"/title-search/web-resources/images/ico_order_1.gif\"></A>"; 
    	
    	return ret;
    }
    
}



