package ro.cst.tsearch.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Hashtable;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;

public class IndividualLogger
{
    private static IndividualLogger individualLoggerInstance = new IndividualLogger();
    
    private Hashtable openedLogHandlers = null;
    
    public static void initLog(  long searchId)
    {
        IndividualLogger loggerInstance = IndividualLogger.getInstance();
        PrintWriter currentSearchPrintWriter = null;
        
        Long searchID = null;
        
        try
        {			
	        Search currentSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
	        File logFile = new File(ServerConfig.getFilePathServerLogs() + File.separator + currentSearch.getRelativePath() + File.separator + "log" + currentSearch.getSearchID() + ".txt" );
	        
	        currentSearchPrintWriter = new PrintWriter( new FileOutputStream( logFile, true ), true );
	        
	        searchID = new Long( currentSearch.getSearchID() );
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
        
        if( currentSearchPrintWriter != null )
        {
            //add the newly created log file to the hashtable
            loggerInstance.add( currentSearchPrintWriter, searchID );
            
//            currentSearchPrintWriter.println( "<HTML><BODY>" ); 
        }
        
    }
    
    public static void finishLog( long searchId )
    {
        IndividualLogger loggerInstance = IndividualLogger.getInstance();
        
        Search currentSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
        if(currentSearch==null)
        	return;
        Long searchID = new Long( currentSearch.getSearchID() );
        
        PrintWriter currentPrintWriter = ( PrintWriter ) loggerInstance.openedLogHandlers.get( searchID );
        
        if( currentPrintWriter != null )
        {
            currentPrintWriter.println();
            currentPrintWriter.println();
            
//            currentPrintWriter.println( "</BODY></HTML>" ); 
            
            //close the print writer
            currentPrintWriter.close();
            
            //remove the print writer from the internal hashtable
            loggerInstance.remove( searchID );
        }
    }
    
    public static void info( String info,long searchId )
    {
        //log the string to the opened file
        
        IndividualLogger loggerInstance = IndividualLogger.getInstance();
       
        PrintWriter out = loggerInstance.getPW( searchId );
        
        if( out != null )
        {
            out.println( info ); 
        }
    }
    
    /**
     * logs information not related to automatic search
     * for logging inside automatic search info(String) function should be used
     * the function is more time consuming than the original info(String) function as it has to open and close the file stream after each call
     * @param infoDebug
     */
    public static void infoDebug( String infoDebug ,long searchId)
    {
    	try {
    	
	        IndividualLogger loggerInstance = IndividualLogger.getInstance();
	        Long searchID = new Long(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID());
	        PrintWriter out = loggerInstance.getPW( searchID );
	        
	        boolean logAdded = false;
	        
	        if( out == null )
	        {
	            initLog(searchId);
	            out = loggerInstance.getPW( searchID );
	            logAdded = true;
	        }
	        
	        //write the info to the file
	        
	        if( out != null )
	        {
	            out.println( infoDebug );
	        }
	        
	        if( logAdded )
	        {
	            finishLog(searchId);
	        }
        
    	} catch(Throwable t) {
    		t.printStackTrace();
    	}
    }
    
    public synchronized void add( PrintWriter pw, Long searchID )
    {
        openedLogHandlers.put( searchID, pw );
    }
    
    public synchronized void remove( Long searchID )
    {
        openedLogHandlers.remove( searchID );
    }
    
    public synchronized PrintWriter getPW( Long searchID )
    {
        return (PrintWriter) openedLogHandlers.get( searchID );
    }
    
    private IndividualLogger()
    {
        openedLogHandlers = new Hashtable();
    }
    
    public static IndividualLogger getInstance()
    {
        return individualLoggerInstance;
    }
    
    public static String getIndividualLog( Search s )
    {
    	if(s == null || s.isFakeSearch()) {
    		return null;
    	}
        try
        {
	        File logFile = new File(ServerConfig.getFilePathServerLogs() + File.separator + s.getRelativePath() + File.separator + "log" + s.getSearchID() + ".txt" );
	        
	        if( logFile.exists() )
	        {
	            String urlPath = URLMaping.path + File.separator + "fs?f=" + s.getRelativePath() + "log" + s.getSearchID() + ".txt";
	            urlPath = urlPath.replace( '\\', '/' );
	            
	            return urlPath;
	        }
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
        
        return null;
    }
    
    public static String logFileUrl( String folderPath )
    {
        String log = "";
        
        try
        {
	        File[] allFiles = (new File( folderPath )).listFiles();
	        if( allFiles != null )
	        {
	            for( int i = 0 ; i < allFiles.length ; i++ )
	            {
	                if( allFiles[i].getName().indexOf( "log" ) >= 0 && allFiles[i].getName().indexOf( ".txt" ) >= 0  && !allFiles[i].isDirectory() )
	                {
	                    //found the log file
	                   log = allFiles[i].getName();
	                   break;
	                }
	            }
	        }
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
        
        //return the log file
        return log;
    }
}