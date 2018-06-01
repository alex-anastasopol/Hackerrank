package ro.cst.tsearch.utils;

import ro.cst.tsearch.*;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.*;

import java.io.*;
import java.util.*;

public class FileLogger
{
    public static final String SQL_FILE_PATH = BaseServlet.FILES_PATH + "sql.log";
    public static final String SEARCH_OWNER_LOG = BaseServlet.FILES_PATH + "change_search_owner.log";
    
    private static FileLogger fileLoggerInstance = new FileLogger();
    private Hashtable openedHandlers = null;
    
    public static FileLogger getInstance()
    {
        return fileLoggerInstance;
    }
    
    public static void startLog( String filename )
    {
        try
        {
            FileLogger loggerInstance = FileLogger.getInstance();
            PrintWriter currentSearchPrintWriter = null;
            
            FileUtils.CreateOutputDir(filename);
            
            if( loggerInstance.getPW( filename ) == null )
            {
                try
                {
                    File logFile = new File( filename );
                    /*
                    if( logFile.exists() && SEARCH_OWNER_LOG.equals( filename ) )
                    {
                        //if we start the log for search owner log, store the old file when creating a new one
                        logFile.renameTo( new File( BaseServlet.FILES_PATH + "change_search_owner" + System.currentTimeMillis() + ".log" ) );
                    }
                    */
                    logFile.delete();
                    logFile.createNewFile();
                    currentSearchPrintWriter = new PrintWriter( new FileOutputStream( logFile, true ), true );
                }
                catch( Exception e )
                {
                    e.printStackTrace();
                }
                
                if( currentSearchPrintWriter != null )
                {
                    //add the newly created log file to the hashtable
                    loggerInstance.add( currentSearchPrintWriter, filename );
                }
            }
        }
        catch(Exception ignored) {}
    }
    
    public static void info( String info, String filename )
    {
        //log the string to the opened file
        
        try
        {
            FileLogger loggerInstance = FileLogger.getInstance();
            
            PrintWriter out = loggerInstance.getPW( filename );
    
            if( out == null )
            {
                FileLogger.startLog( filename );
                out = loggerInstance.getPW( filename );
            }
            
            if( out != null )
            {
                out.println( info ); 
            }
        }
        catch( Exception ignored ) {}
    }
    
    public synchronized void add( PrintWriter pw, String filename )
    {
        openedHandlers.put( filename, pw );
    }
    
    public synchronized void remove( String filename )
    {
        openedHandlers.remove( filename );
    }
    
    public synchronized PrintWriter getPW( String filename )
    {
        return (PrintWriter) openedHandlers.get( filename );
    }
    
    private FileLogger()
    {
        openedHandlers = new Hashtable();
        startLog( SEARCH_OWNER_LOG );
    }
    
}