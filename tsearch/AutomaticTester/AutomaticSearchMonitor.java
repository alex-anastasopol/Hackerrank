package ro.cst.tsearch.AutomaticTester;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.bean.TSDIndexPage;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityUtils;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.exceptions.SaveSearchException;
import ro.cst.tsearch.propertyInformation.Family;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.ServletServerComm.CompareInstrumentsAfterRecordedDate;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servlet.TSD;
import ro.cst.tsearch.threads.ASMaster;
import ro.cst.tsearch.threads.ASThread;
import ro.cst.tsearch.utils.InstanceManager;

public class AutomaticSearchMonitor extends Thread
{
    //the test case, initially saved by the admin
    private Search testCase = null;
    private String testCaseFilename;
    private User searchUser;
    private AutomaticSearchJob job;
    
    //the search resulted from automatic search using testCase's original search attributes
    private Search candidate = null;
    private String sPath;
    
    //start date of the test
    private Date startDate;
    
    public Object notifier = new Object();
    private boolean finished;
    private boolean ok;
    private boolean interrupted;

    public static final Logger logger = Logger.getLogger(AutomaticSearchMonitor.class);
    
    public AutomaticSearchMonitor( AutomaticSearchJob job, User searchUser, String sPath)
    {
        testCaseFilename = job.getXMLFile();
        this.searchUser = searchUser;
        this.job = job;
        testCase = job.getSearch();
        CommunityAttributes ca = null;
        
        //original search attributes
        SearchAttributes originalSA = null;
        
        if( testCase == null )
        {
            logger.info( "Null Test CASE!!!!" );
            return;
        }
                
        candidate  = SearchManager.addNewSearch(searchUser, false);
        originalSA = testCase.getOrigSA();
        
        if( originalSA == null )
        {
            logger.info( "NULL original search attributes, skipping test case!" );
            job.stop();
            job.setLastStarted( false );
            synchronized( AutomaticTesterManager.globalNotifier )
            {
                AutomaticTesterManager.globalNotifier.notifyAll();
            }
            return;
        }
        
        candidate.setSa(new SearchAttributes(originalSA,candidate.getID()));
        candidate.setOrigSA(new SearchAttributes(originalSA,candidate.getID()));
        
        candidate.resetSearchStatus();
        
        candidate.setUpdate(false);
        candidate.setSearchStarted(true);
        candidate.setSearchType(Search.AUTOMATIC_SEARCH);

        
        ///////////////////////
        
        SearchAttributes sa = candidate.getSa();

		candidate.getSa().setAtribute(SearchAttributes.FROMDATE, testCase.getSa().getAtribute(SearchAttributes.FROMDATE));
		candidate.getSa().setAtribute(SearchAttributes.TODATE, testCase.getSa().getAtribute(SearchAttributes.TODATE));
		
        candidate.setAgent(searchUser.getUserAttributes());
        candidate.getSa().setOrderedBy(searchUser.getUserAttributes());

		try {
            HashCountyToIndex.setSearchServer(candidate, HashCountyToIndex.getFirstServer(
            		sa.getProductId(),
            		sa.getCommId(),
            		Integer.parseInt(sa.getCountyId()) ));
        } catch (BaseException e2) {
            e2.printStackTrace();
        }				
        this.sPath = sPath;

        startDate = new Date();
        interrupted = false;
        AutomaticTesterManager.getInstance().addNewTestSearch( this );
        start();
    }
    
    public void run()
    {
        synchronized( AutomaticTesterManager.globalNotifier )
        {
            AutomaticTesterManager.globalNotifier.notifyAll();
        }
        CommunityAttributes ca = null;
        //not finished
        ok = true;
        synchronized(notifier)
        {
            finished = false;
        }
        
        try
        {
            ca = CommunityUtils.getCommunityFromId( searchUser.getUserAttributes().getCOMMID().longValue() );
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        
        //start the search thread
        logger.info( "Starting test case: " + testCaseFilename + " status " + candidate.getTS_SEARCH_STATUS() );
                      
        //saving search
        //save the search
        try
        {
            InstanceManager.getManager().getCurrentInstance(candidate.getID()).setCrtSearchContext(candidate);
            InstanceManager.getManager().getCurrentInstance(candidate.getID()).setCurrentUser(searchUser.getUserAttributes());                
            InstanceManager.getManager().getCurrentInstance(candidate.getID()).setCurrentCommunity(ca);
            
            SearchAttributes candSa = candidate.getSa(); 
            
            candSa.setObjectAtribute( SearchAttributes.INSTR_LIST, new ArrayList() );
            candSa.setObjectAtribute( SearchAttributes.RO_CROSS_REF_INSTR_LIST, new ArrayList<Instrument>());
            
            TSD.initSearch(searchUser, candidate, sPath, null);
            
            ASMaster.startTestCaseSearch( candidate, searchUser, sPath );
            
            //wait for the search to finish
            ASMaster.waitForSearch( candidate );
            
            //remove the thread from the list
            ASMaster.removeSearch( candidate );
            TSD.initSearch(searchUser, candidate, sPath, null);
           
            //perform the tests
        }
        catch(Exception e)
        {
       		job.setCrashed(true);
            e.printStackTrace();
        }         
        
        //notify all waiting threads that search is finished
        synchronized(notifier)
        {
            notifier.notifyAll();
            finished = true;
        }
        
        if(!interrupted && !job.getCrashed())
        {
            //if thread hasn't been stopped by the scheduler perform the comparison between the original and the candidate
            //if the test would be performed when the thread has been interrupted, the comparison results would be inconsistent
            //as the search results are incomplete
            compareSearches();
        }
        
        AutomaticTesterManager.getInstance().removeTestSearch( this );
        synchronized( job )
        {
            if( job.getInterval() == AutomaticTesterManager.ONE_TIME_TEST )
            {
                job.stop();
            }
            job.setLastStarted(false);
        }

        synchronized( AutomaticTesterManager.globalNotifier )
        {
            AutomaticTesterManager.globalNotifier.notifyAll();
        }
    }
    
    public AutomaticSearchJob getJob(){
    	return job;
    }
    
    public Date getStartDate(){
    	return startDate;
    }
    
    public boolean hasFinished()
    {
        synchronized(notifier)
        {
            return finished;
        }
    }
    
    public String getXMLFilename()
    {
        return testCaseFilename;
    }
    
    public Hashtable compareSearches()
    {
        //compara cele doua searchuri
        Hashtable searchCompare = new Hashtable();
        
        Vector documentErrors = new Vector(5,5);
        
        //daca threadul a terminat
        if( hasFinished() )
        {
            SearchAttributes original = testCase.getSa();
            SearchAttributes candidateSa = candidate.getSa();
            
            //comparing Property Address
            searchCompare.put( SearchAttributes.P_STREETNO, compareKeys( original, candidateSa, SearchAttributes.P_STREETNO, "Street Number" ) );
            searchCompare.put( SearchAttributes.P_STREETDIRECTION, compareKeys( original, candidateSa, SearchAttributes.P_STREETDIRECTION, "Street Direction" ) );
            searchCompare.put( SearchAttributes.P_STREETNAME, compareKeys( original, candidateSa, SearchAttributes.P_STREETNAME, "Street Name" ) );
            searchCompare.put( SearchAttributes.P_STREETSUFIX, compareKeys( original, candidateSa, SearchAttributes.P_STREETSUFIX, "Street Suffix" ) );
            searchCompare.put( SearchAttributes.P_STREETUNIT, compareKeys( original, candidateSa, SearchAttributes.P_STREETUNIT, "Street Unit" ) );
            searchCompare.put( SearchAttributes.P_CITY, compareKeys( original, candidateSa, SearchAttributes.P_CITY, "City" ) );
            searchCompare.put( SearchAttributes.P_STATE, compareKeys( original, candidateSa, SearchAttributes.P_STATE, "State" ) );
            searchCompare.put( SearchAttributes.P_COUNTY, compareKeys( original, candidateSa, SearchAttributes.P_COUNTY, "County" ) );
            searchCompare.put( SearchAttributes.P_ZIP, compareKeys( original, candidateSa, SearchAttributes.P_ZIP, "Zipcode" ) );
            searchCompare.put( SearchAttributes.P_STREET_FULL_NAME, compareKeys( original, candidateSa, SearchAttributes.P_STREET_FULL_NAME, "Street Full name" ) );
            searchCompare.put( SearchAttributes.P_STREET_FULL_NAME_EX, compareKeys( original, candidateSa, SearchAttributes.P_STREET_FULL_NAME_EX, "Street Full name ex" ) );
            searchCompare.put( SearchAttributes.P_STREET_FULL_NAME_NO_SUFFIX, compareKeys( original, candidateSa, SearchAttributes.P_STREET_FULL_NAME_NO_SUFFIX, "Street Full name no suffix" ) );
            
            //Comparing property owner
            searchCompare.put( SearchAttributes.OWNER_FNAME, compareKeys( original, candidateSa, SearchAttributes.OWNER_FNAME, "Owner first name" ) );
            searchCompare.put( SearchAttributes.OWNER_MNAME, compareKeys( original, candidateSa, SearchAttributes.OWNER_MNAME, "Owner middle name" ) );
            searchCompare.put( SearchAttributes.OWNER_LNAME, compareKeys( original, candidateSa, SearchAttributes.OWNER_LNAME, "Owner last name" ) );
            
            //comparing Legal Description
            searchCompare.put( SearchAttributes.LD_INSTRNO, compareKeys( original, candidateSa, SearchAttributes.LD_INSTRNO, "Instrument number" ) );
            searchCompare.put( SearchAttributes.LD_BOOKNO, compareKeys( original, candidateSa, SearchAttributes.LD_BOOKNO, "Book number" ) );
            searchCompare.put( SearchAttributes.LD_PAGENO, compareKeys( original, candidateSa, SearchAttributes.LD_PAGENO, "Page number" ) );
            searchCompare.put( SearchAttributes.LD_BOOKPAGE, compareKeys( original, candidateSa, SearchAttributes.LD_BOOKPAGE, "Book Page" ) );
            searchCompare.put( SearchAttributes.LD_SUBDIVISION, compareKeys( original, candidateSa, SearchAttributes.LD_SUBDIVISION, "Subdivision" ) );
            searchCompare.put( SearchAttributes.LD_LOTNO, compareKeys( original, candidateSa, SearchAttributes.LD_LOTNO, "Lot number" ) );
            searchCompare.put( SearchAttributes.LD_SUBDIV_NAME, compareKeys( original, candidateSa, SearchAttributes.LD_SUBDIV_NAME, "Subdivision name" ) );
            searchCompare.put( SearchAttributes.LD_SUBDIV_BLOCK, compareKeys( original, candidateSa, SearchAttributes.LD_SUBDIV_BLOCK, "Subdivision block" ) );
            searchCompare.put( SearchAttributes.LD_SUBDIV_PHASE, compareKeys( original, candidateSa, SearchAttributes.LD_SUBDIV_PHASE, "Subdivision phase" ) );
            searchCompare.put( SearchAttributes.LD_SUBDIV_TRACT, compareKeys( original, candidateSa, SearchAttributes.LD_SUBDIV_TRACT, "Subdivision tract" ) );
            searchCompare.put( SearchAttributes.LD_SUBDIV_SEC, compareKeys( original, candidateSa, SearchAttributes.LD_SUBDIV_SEC, "Subdivision sec" ) );
            searchCompare.put( SearchAttributes.LD_SUBDIV_CODE, compareKeys( original, candidateSa, SearchAttributes.LD_SUBDIV_CODE, "Subdivision code" ) );
            searchCompare.put( SearchAttributes.LD_PARCELNO, compareKeys( original, candidateSa, SearchAttributes.LD_PARCELNO, "Parcel number" ) );
            searchCompare.put( SearchAttributes.LD_SUBDIV_UNIT, compareKeys( original, candidateSa, SearchAttributes.LD_SUBDIV_UNIT, "Subdivision unit" ) );

            
            //comparing total document number
            //TSDIndexPage originalTsdIndex = testCase.getTSDIndexPage();
            //TSDIndexPage candidateTsdIndex = candidate.getTSDIndexPage();
            
            //logger.info( "Original chapters: " + ( originalTsdIndex.getChaptersCount()  ) );
            //logger.info( "New downloaded chapters: " + ( candidateTsdIndex.getChaptersCount()  ) );
            
            String[] docCount = new String[4];
            
            docCount[0] = "Document Count";
            //docCount[1] = "" + ( originalTsdIndex.getChaptersCount()  );
            
           // docCount[2] = "" + ( candidateTsdIndex.getChaptersCount()  );
            if( docCount[1].equals(docCount[2]) )
            {
                docCount[3] = "OK";
            }
            else
            {
                ok = false;
                docCount[3] = "FAILED";
            }
            searchCompare.put( "DOCUMENT COUNT", docCount );
            
            //compare the documents one by one
            //compareDocuments( documentErrors, originalTsdIndex, candidateTsdIndex );
            
            //generate the result report
            if(ok)
            {
                job.setLastStatus( "OK" );
            }
            else
            {
            	job.setLastStatus( "FAILED" );
            }
            
            generateTestReport( searchCompare, documentErrors );
            
        }
        
        return searchCompare;
    }
    
    private void compareDocuments( Vector resultsVector, TSDIndexPage original, TSDIndexPage candidate )
    {
        Vector originalChapters = new Vector(); //original.getAllChapters();
        Vector candidateChapters = new Vector(); //candidate.getAllChapters();
        
        //check the candidate against the original
        for( int i = 0 ; i < candidateChapters.size() ; i++ )
        {
            String[] candidateChapter = (String[]) candidateChapters.elementAt( i );
            
            //check if the candidate instrument number can be found in the original chapter list
           ok = false;
        }
        
        //check the original against the candidate
        for( int i = 0 ; i < originalChapters.size() ; i++ )
        {
            String[] originalChapter = (String[]) originalChapters.elementAt( i );
            
            //check if the candidate instrument number can be found in the original chapter list
                      
        }
    }
    
    private boolean checkForInstrumentNumber( String candidateInstrumentNumber, Vector originalChapters )
    {
        if ( candidateInstrumentNumber.indexOf( "patriots" ) >= 0 )
        {
            return true;
        }
        for( int i = 0 ; i < originalChapters.size() ; i++ )
        {
            String[] originalChapter = (String[]) originalChapters.elementAt( i );
            
            
        }
        
        return false;
    }
    
    public void terminate()
    {
        ASThread thread =  ASMaster.getSearch( candidate );
        if(thread != null)
        {
            thread.setStarted( false );
            interrupted = true;
        }
    }
    
    private String[] compareKeys( SearchAttributes sa1, SearchAttributes sa2, String keyName, String description )
    {
        String[] statusStrings = new String[4];
        
        statusStrings[0] = description;
        
        //original
        statusStrings[1] = sa1.getAtribute( keyName );
        
        //candidate
        statusStrings[2] = sa2.getAtribute( keyName );
        if( sa1.getAtribute( keyName ).equalsIgnoreCase( sa2.getAtribute( keyName ) ) )
        {
            statusStrings[3] = "OK";
        }
        else
        {
            ok = false;
            statusStrings[3] = "FAILED";
        }
        
        return statusStrings;
    }

    private void generateTestReport( Hashtable results, Vector documentErrors )
    {
        File originalXMLFile = new File( AutomaticTesterManager.testCaseFolder + File.separator + testCaseFilename );
        SimpleDateFormat sdf = new SimpleDateFormat("dd_MM_yyyy_HH_mm");
        Date now = new Date();
        String candidateFolder = ( ok ? "OK" : "FAILED" ) + "_" + sdf.format(startDate) + "_" + sdf.format(now);
        
        //if the search hasn't been deleted
        if( !originalXMLFile.exists() )
        {
            return;
        }
        
        try
        {            
//            FileUtils.deleteDir( new File(AutomaticTesterManager.finishedTestCaseFolder + File.separator + "ALLFILES_" + AutomaticTesterManager.getTestFolderName( testCaseFilename )) );
            
            AutomaticTesterManager.copyFolder( candidate.getSearchDir(), AutomaticTesterManager.finishedTestCaseFolder + File.separator + "ALLFILES_" + AutomaticTesterManager.getTestFolderName( testCaseFilename ) + File.separator + candidateFolder );
            
            //save the latest test in the test cases folder
            
            candidate.getSa().setAbstractorFileName( testCase );
            
            Search.saveSearchToPath( candidate, AutomaticTesterManager.finishedTestCaseFolder + File.separator + "ALLFILES_" + AutomaticTesterManager.getTestFolderName( testCaseFilename ) + File.separator + candidateFolder , testCaseFilename );
            
            Search.saveSearchToPath( candidate, AutomaticTesterManager.finishedTestCaseFolder , testCaseFilename );
        }
        catch (SaveSearchException e1)
        {
            e1.printStackTrace();
        }
        
        File reportsFolder = new File( AutomaticTesterManager.finishedTestCaseFolder + File.separator + testCaseFilename.substring( 0, testCaseFilename.indexOf( ".xml" ) ) );

        String reportFilename = ( ok ? "OK" : "FAILED" ) + "_" + sdf.format(startDate) + "_" + sdf.format(now) + ".html";
        
        String reportContents = "<HTML><HEAD><TITLE>Test Results on " + now + "</TITLE></HEAD><TABLE align=\"center\" width=\"100%\">";
        
        reportContents += "<TR><TD><B>Attribute Code</B></TD><TD><B>Attribute Name</B></TD><TD><B>Original Value</B></TD><TD><B>Refference Value</B></TD><TD><B>Status</B></TD></TR>";
        
        if( !reportsFolder.exists() )
        {
            reportsFolder.mkdir();
        }
        
        try
        {
            PrintWriter pw = new PrintWriter(new FileWriter( new File( reportsFolder + File.separator + reportFilename ) ) );
            pw.print( reportContents );
            
            Enumeration keys = results.keys();
			for(; keys.hasMoreElements() ;)
			{
				String attributeName = (String)keys.nextElement();
				if( !attributeName.equals( "DOCUMENT COUNT" ) )
				{
					String[] testValue = (String[])results.get( attributeName );
					/*
					pw.println( "<TR><TD align=\"left\">" + attributeName + "</TD>" );
					
					for( int i = 0 ; i < testValue.length ; i++ )
					{
					    pw.println( "<TD align=\"left\">" + testValue[i] + "</TD>" );
					}
					
					pw.println("</TR>");
					*/
					printReportRow( pw, testValue, attributeName );
				}
			}
			
			String[] docCountReport = (String[]) results.get( "DOCUMENT COUNT" );
			if( docCountReport != null )
			{
			    printReportRow( pw, docCountReport, "DOCUMENT COUNT" );
			}
			pw.print("<TR><TD height=\"10\">&nbsp;</TD></TR>");
			pw.println( "</TABLE>" );
			
			//print the document errors
			if( documentErrors.size() == 0 )
			{
				pw.print("<TABLE align=\"center\" width=\"100%\"><TR><TD><B>Document Discrepancy:</B></TD><TD><B>NONE</B></TD><TD>&nbsp;</TD><TD>&nbsp;</TD></TR>");
			}
			else
			{
				pw.print("<TABLE align=\"center\" width=\"100%\"><TR><TD><B>Document Discrepancy:</B></TD><TD>&nbsp;</TD><TD>&nbsp;</TD><TD>&nbsp;</TD></TR>");
				pw.print("<TR><TD><B>Intstrument No.</B></TD><TD><B>Present in original</B></TD><TD><B>Present in refference</B></TD><TD><B>Error</B></TD></TR>");
				
				for( int i = 0 ; i < documentErrors.size() ; i++ )
				{
				    String[] report = (String[]) documentErrors.elementAt( i );
				    
				    printReportRow( pw, report, null );
				}
			}
			
			//print the search status
		    pw.println("</TABLE>");
			pw.println("<TABLE align=\"center\" width=\"100%\"><TR><TD><B>Search status:</B></TD><TD>&nbsp;</TD></TR>");
			pw.println("<TR><TD><B>Server Name</B></TD><TD><B>Server Status</B></TD></TR>");
			for (int j = 0; j < candidate.getSearchStatus().size(); j++)
			{
			    pw.println("<TR><TD>" + ((String[])candidate.getSearchStatus().elementAt(j))[0].toString() + "</TD><TD>" + ((String[])candidate.getSearchStatus().elementAt(j))[1].toString() + "</TD></TR>");
			}
			
			pw.println( "</TABLE></HTML>" );
            pw.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private void printReportRow( PrintWriter pw, String[] testValue, String attributeName ) throws IOException
    {
        if( attributeName != null )
        {
            pw.println( "<TR><TD align=\"left\">" + attributeName + "</TD>" );
        }
		
		for( int i = 0 ; i < testValue.length ; i++ )
		{
		    pw.println( "<TD align=\"left\">" + testValue[i] + "</TD>" );
		}
		
		pw.println("</TR>");        
    }
}