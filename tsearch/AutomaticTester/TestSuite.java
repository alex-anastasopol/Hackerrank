package ro.cst.tsearch.AutomaticTester;

import java.io.*;

import java.util.*;
import java.math.*;
import java.text.*;
import javax.servlet.http.*;

import org.apache.log4j.Logger;

import ro.cst.tsearch.monitor.SNMPAgent;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.parentsite.Company;
import ro.cst.tsearch.servlet.*;
import ro.cst.tsearch.threads.*;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.utils.CSTCalendar;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.URLMaping;
import ro.cst.tsearch.data.*;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.exceptions.NotImplFeatureException;
import ro.cst.tsearch.community.*;
import ro.cst.tsearch.bean.*;

import com.thoughtworks.xstream.XStream;
import ro.cst.tsearch.*;
import ro.cst.tsearch.utils.*;

public class TestSuite
{
    private String testSuiteName;
    private String testSuiteDescription;
    
    //enabled is false if and only if all the associated search jobs are not enabled
    private boolean enabled;
    
    //vector of AutomaticSearchJob
    private Vector testCaseArray;
    
    private long testSuiteId;
    
    public TestSuite( String name, String description, boolean enabled )
    {
        testSuiteName = name;
        testSuiteDescription = description;
        this.enabled = enabled;
        testSuiteId = -1;
        
        testCaseArray = new Vector();
    }
    
    public String getName()
    {
        return testSuiteName;
    }
    
    public String getDescription()
    {
        return testSuiteDescription;
    }
    
    public void setId( long id )
    {
        testSuiteId = id;
    }
    
    public long getId()
    {
        return testSuiteId;
    }
    
    /**
     * enable or disable the entire search job list
     * @param enabled
     */
    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
        
        for( int i = 0 ; i < testCaseArray.size() ; i ++ )
        {
            AutomaticSearchJob ASJ = (AutomaticSearchJob) testCaseArray.elementAt( i );
            
            if( enabled )
            {
                ASJ.start();
            }
            else
            {
                ASJ.stop();
            }
        }
    }
    
    public void addJob( AutomaticSearchJob job )
    {
        if( !testCaseArray.contains( job ) )
        {
            testCaseArray.add( job );
            
            this.enabled = this.enabled && job.isActive();
        }
    }
    
    public Vector getTestCases()
    {
        return testCaseArray;
    }
}