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
import ro.cst.tsearch.utils.FileCopy;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.URLMaping;
import ro.cst.tsearch.data.*;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.community.*;
import ro.cst.tsearch.bean.*;
import ro.cst.tsearch.titledocument.*;
import ro.cst.tsearch.utils.*;

import com.thoughtworks.xstream.XStream;

import ro.cst.tsearch.*;

public class InitPresenceTests implements Runnable
{
    private Search s;
    public static boolean finished = false;
    private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
    private static final Logger logger = Logger.getLogger(CommAdminNotifier.class);
    private static final Object synch = new Object();
    
    private long searchId=-1;
    
    public InitPresenceTests( Search s,long searchId )
    {
    	this.searchId = searchId;
        this.s = s;
    }
    
    public void run()
    {
               
        synchronized( synch )
        {
            if( finished )
            {
                logger.info( "Already started!!!" );
                return;
            }
            
            try
            {
                if( rbc.getString( "application.name" ).trim().equals( "ats" )  )
                {
                    //if we are on ats, start the presence tester thread
                    logger.info( "App Name: ATS --> Starting Presence Tests" );
                    InstanceManager.getManager().getCurrentInstance(s.getID()).setCrtSearchContext( s );
                    PresenceTesterManager.getInstance(searchId);
                    finished = true;
                    PresenceTesterManager.getInstance(searchId).setAutoStarted( true );
                    PresenceTesterManager.getInstance(searchId).startUp();
                }
                else
                {
                    logger.info( "Not on ATS, not starting presence tests!" );
                    finished = true;                
                }
            }
            catch( Exception e )
            {
                logger.info( "Not on ATS, not starting presence tests!" );
                e.printStackTrace();
                finished = true;
            }
        }
    }
}