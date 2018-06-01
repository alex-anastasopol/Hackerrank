package ro.cst.tsearch.AutomaticTester;

import java.util.Date;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.utils.DBConstants;



public class PresenceTesterJob
{
    private boolean active;
    private Date lastStarted;
    private String lastStatus;
    private String serverName;
    //private long executionInterval = 30 * 60 * 1000;
    private long executionInterval ;
    private TSServerInfo serverInformation = null;
    private boolean running;
    private double upPercentage;
    private boolean statusSet = false;
    private boolean statusDel=false;
    //the state if it is on or of
    private int state = 0;
    
    private static int counter ;
    
    
    //data for the presence test job 
    //===========================================================
    private int test_id;
    private int caseIndice;
    private Vector vID = new Vector() ;
    private Vector v = new Vector();
    
    public boolean getStatus(){
    	
    	return statusSet;
    }
    
    public void setStatus(boolean b){
    	
    	statusSet = b;
    }
    
    public int getTestId(){
    	//returns the id of the test case in the DB
    	return test_id;
    }
    public void setTestId(long id){
    	this.test_id=Integer.parseInt(""+id);
    }
    public int getCaseIndice(){
    	//returns the indice of the case
    	return caseIndice;
    }
    
    public  Vector getVID(){
    	//return the vector of indice
    	return vID;
    }
    
    public Vector getVectorWithData(){
    	//get the vector with data 
    	return v;
    }
    
    testCase tcase = new testCase();
    
    public testCase getTestCase(){
    //return the test case associated with the pressent job	
    	
    	return tcase;
    }
    
    public int getState(){
    	//returns the state of the case
    	return state;
    }
    
    
    //===========================================================
    
    public static final String UP = "#00FF00";
    public static final String DOWN = "#FF0000";
    public static final String DISABLED = "#777777";
    public static final int STATUS_ROWS_LIMIT = 240;
    
    public static final Logger logger = Logger.getLogger(PresenceTesterJob.class);
    private long searchId=-1;
    
    public PresenceTesterJob(int i , testCase tc , int st)
    //public PresenceTesterJob(long searchId, Vector v ,Vector vID, int i,testDataExecution test,XMLAutomaticTestDataReader t,int tID)
    {
       //i              -  the indices of the cases
       //dbOrder - the case_id in the data base
       //searchId - the id of the search 
       //vID          - contains the id in the data base
       //v              - contains the cases  
        
    	try
        {
            statusSet = false;
            logger.info( "Getting server info for " );
            //serverInformation = TSServersFactory.GetServerInfo( serverId,searchId );
            running = false;
            this.serverName = tc.getVServerNAME();
           // executionInterval = 30 * 60 * 1000;
            executionInterval=AutomaticTesterManager.ONE_TIME_TEST;
            
            upPercentage = 0;
            
            //getLastStatusFromDB();
            //getEnabledStatus();
            
            state = st;
            
            if( state == 1 ){
            	active = true;
            }
            else{
            	active = false;
            }
            
            tcase = tc;
            
            
           //--------method that reads the data----------
           // active = true;
            
            boolean tst = false;
			   
			  try{
			
				  test_id = tc.getVtestID();  
				  
				  caseIndice = i; 
			
				  //vID = 
	              
				  //v = 
					
				  
			   }
			  catch(Exception e){
					   
					   System.out.println("  Exception at  test.testSearch() call  ");
					   e.printStackTrace();
					   
			   }
			   
            
            
        }
        catch( Exception e )
        {
            logger.info( "Error getting server info " );
            serverInformation = null;
            e.printStackTrace();
        }
    }
    
    public synchronized void setRunning( boolean running )
    {
        this.running = running;
    }
    
    public synchronized boolean isRunning()
    {
        return running == true;
    }
    
    public void setLastStatus( String status )
    {
        lastStatus = status;
    }
    
    public String getLastExecutionStatus()
    {
        if( PresenceTesterManager.getInstance(searchId).getRunningState() && running )
        {
        	return "TESTING";
        }
        else if( active )
        {
            return "ENABLED";
        }
        else
        {
            return "DISABLED";
        }
    }
    
    public String getLastStatus()
    {
        if( lastStatus == null )
        {
            return "N/A";
        }
        else
        {
            return lastStatus;
        }
    }
    public void setInterval( long millis )
    {
        executionInterval = millis;
    }
    
    public long getInterval()
    {
        return executionInterval;
    }
    
    public void start()
    {
        active = true;
        //setEnabledStatus();
    }
    
    public void stop()
    {
        active = false;
        //setEnabledStatus();
    }
    
    public Date getLastStarted()
    {
    	if( lastStarted != null )
    		return lastStarted;
    	else
    		return (new Date());
    }
    
    public void setLastStarted(boolean reset)
    {
        lastStarted = reset ? null : new Date();
    }
    
    public boolean isActive()
    {
        return active;
    }
    
    public TSServerInfo getServerInfo()
    {
        return serverInformation;
    }
    
    
    public String getServerName()
    {
        return serverName;
    }
    
    public Vector getDBStatusHistory()
    {
        Vector statusHistory = new Vector(8,8);
	    DBConnection conn = null;
	    try
	    {
	        conn = ConnectionPool.getInstance().requestConnection();
	        //DatabaseData dbData = conn.executeSQL("SELECT TEST_ID, TEST_DATE, STATUS FROM "+DBConstants.TABLE_PRESENCE_TEST+" WHERE SERVER_ID='" + serverId + "' ORDER BY TEST_DATE ASC");
	        DatabaseData dbData = conn.executeSQL("SELECT test_id, date, testResult FROM "+DBConstants.TABLE_PARENTSITE_TESTS_RESULT +" WHERE test_id='" + test_id + "' ORDER BY date ASC");
	        int rows = dbData.getRowNumber();
	        
	        for( int i = 0 ; i < rows ; i++ )
	        {
	            long testId = Long.parseLong(dbData.getValue(1,i).toString());
	            String val = (String)dbData.getValue(2,i);
	           //long testDate = Long.parseLong(val);
	            String testStatus = dbData.getValue(3,i).toString();
	            
	            //PresenceStatus pStatus = new PresenceStatus( testDate, testStatus, testId );
	            PresenceStatus pStatus = new PresenceStatus( val, testStatus, testId );
	            
	            statusHistory.add( pStatus );
	        }
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
	            logger.error(e);
	        }
	    }
        return statusHistory;
    }
    /*
    public void updateDB( long testDate, String testStatus )//update the DB status
    {                                                                                                 //modified
	    DBConnection conn = null;
	    long total_rows = 0;
	    try
	    {
	        conn = ConnectionPool.getInstance().requestConnection();
	        
	        //check to see if the STATUS_ROWS_LIMIT has been reached
	        //DatabaseData dbData = conn.executeSQL( "SELECT COUNT( TEST_ID ) FROM "+DBConstants.TABLE_PRESENCE_TEST+" WHERE SERVER_ID='" + serverId + "'" );
	        DatabaseData dbData = conn.executeSQL( "SELECT COUNT( test_id ) FROM "+DBConstants.TABLE_PARENTSITE_TESTS_RESULT+" WHERE ='" + test_id + "'" );
	        //if number of rows >= STATUS_ROWS_LIMIT, delete the oldest one
	        if( dbData.getRowNumber() >= 1 )
	        {
                int numberOfEntries = Integer.parseInt(dbData.getValue( 1, 0 ).toString());
                if( numberOfEntries >= PresenceTesterJob.STATUS_ROWS_LIMIT )
                {
                	//dbData = conn.executeSQL("SELECT MIN( TEST_DATE ) FROM "+DBConstants.TABLE_PRESENCE_TEST+" WHERE SERVER_ID='" + serverId + "' " );
                	dbData = conn.executeSQL("SELECT MIN( LastTestDate ) FROM "+DBConstants.TABLE_PARENTSITE_TESTS_RESULT+" WHERE test_id='" + test_id + "' " );
                	
                	//conn.executeSQL( "DELETE FROM "+DBConstants.TABLE_PRESENCE_TEST+" WHERE TEST_DATE = "+ dbData.getValue(1, 0) + " AND SERVER_ID='" + serverId + "'" );
                	conn.executeSQL( "DELETE FROM "+DBConstants.TABLE_PARENTSITE_TESTS_RESULT+" WHERE LastTestDate = "+ dbData.getValue(1, 0) + " AND test_id='" + test_id + "'" );
                
                }
	        }
	        
	        
	        //dbData = conn.executeSQL("INSERT INTO "+DBConstants.TABLE_PRESENCE_TEST+" ( SERVER_ID, TEST_DATE, STATUS ) VALUES ( '" + serverId +  "', '" + testDate + "', '" + testStatus + "' )");
	        dbData = conn.executeSQL("INSERT INTO "+DBConstants.TABLE_PARENTSITE_TESTS_RESULT+" ( test_id, LastTestDate, LastTestResult ) VALUES ( '" + test_id +  "', '" + testDate + "', '" + testStatus + "' )");
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
	            logger.error(e);
	        }
	    }
	    calculateUpPercentage();
    }
    */
    
   public void deleteTestCase(){//delete a test case from data base
    	
    	DBConnection conn = null;
    	
        try
	    {
	        conn = ConnectionPool.getInstance().requestConnection();
	        //DatabaseData dbData = conn.executeSQL("SELECT STATUS FROM "+DBConstants.TABLE_PRESENCE_TEST+" WHERE SERVER_ID='" + serverId + "' AND TEST_DATE = ( SELECT MAX(TEST_DATE) FROM "+DBConstants.TABLE_PRESENCE_TEST+" WHERE SERVER_ID = '" + serverId + "' )");
	        System.out.println("@@id=="+test_id);
	        DatabaseData dbData = conn.executeSQL("DELETE FROM "+DBConstants.TABLE_PARENTSITE_TESTS+" WHERE test_id=" + test_id +"");
	        
	        
	        
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
	            logger.error(e);
	        }
	    }
    	
    	
    }
    
    public String getLastStatusFromDB()//get the last data from databases
    {                                                               //get the result of the last test from the DB as the last date 
    																//modified
	    DBConnection conn = null;
	    try
	    {
	        conn = ConnectionPool.getInstance().requestConnection();
	        //DatabaseData dbData = conn.executeSQL("SELECT STATUS FROM "+DBConstants.TABLE_PRESENCE_TEST+" WHERE SERVER_ID='" + serverId + "' AND TEST_DATE = ( SELECT MAX(TEST_DATE) FROM "+DBConstants.TABLE_PRESENCE_TEST+" WHERE SERVER_ID = '" + serverId + "' )");
	        DatabaseData dbData = conn.executeSQL("SELECT testResult FROM "+DBConstants.TABLE_PARENTSITE_TESTS_RESULT+" WHERE test_id='" + test_id + "' AND date = ( SELECT MAX(date) FROM "+DBConstants.TABLE_PARENTSITE_TESTS_RESULT+" WHERE test_id = '" + test_id + "' )");
	        if (dbData.getRowNumber() >= 1 )
	        {
	            lastStatus = (String)dbData.getValue(1,0);
	            return lastStatus;
	        }
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
	            logger.error(e);
	        }
	    }
	   // calculateUpPercentage();
	    return "";
    }
    
    private void getEnabledStatus()//get the status of the button from the data base
    {                                                       //get the status of the test
        DBConnection conn = null;     //modified
        
        try
        {
            conn = ConnectionPool.getInstance().requestConnection();
            //DatabaseData dbData = conn.executeSQL( "SELECT ENABLED FROM "+DBConstants.TABLE_PRESENCE_TEST_SERVERS+" WHERE SERVER_ID='" + serverId + "'" );
            DatabaseData dbData = conn.executeSQL( "SELECT EnabledOrDisabled FROM "+DBConstants.TABLE_PARENTSITE_TESTS+" WHERE test_id='" + test_id + "'" );
            
            if( dbData.getRowNumber() >= 1 )
            {
                statusSet = true;
                if( Integer.parseInt(dbData.getValue( 1, 0 ).toString()) == 1 )
                {
                    start();
                }
                else
                {
                    stop();
                }
            }
            else
            {
                statusSet = false;
                stop();
            }
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
                logger.error(e);
            }
        }
    }
    
    public void setEnabledStatus(boolean value)//set the status of the button 
    {                                                       //set the status of the case
        DBConnection conn = null;
        statusSet = true;
        
        try
        {
        	conn = ConnectionPool.getInstance().requestConnection();
            conn.executeSQL( "UPDATE "+DBConstants.TABLE_PARENTSITE_TESTS+" SET EnabledOrDisabled=" + value + " WHERE test_id='" + test_id + "'" );
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
                logger.error(e);
            }
        }
    }
    
    public double getUpPercentage()
    {
        return upPercentage;
    }

	public boolean getStatusDel() {
		return statusDel;
	}

	public void setStatusDel(boolean statusDel) {
		this.statusDel = statusDel;
	}
	public void setServerName(String name){
		this.serverName=name;
	}
}