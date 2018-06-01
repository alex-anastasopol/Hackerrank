package ro.cst.tsearch.AutomaticTester;

import static ro.cst.tsearch.utils.DBConstants.TABLE_CITY;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilderFactory;

import ro.cst.tsearch.data.Payrate;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FormatDate;


/*Extract data from the table with the result from the tests
 * 
 * */

public class readPresenceTestResultfromDB {
	
	
	private Vector testID = new Vector();           //the id of the test
	
	private Vector dateOfTest = new Vector();  //the date of the test
	
	private Vector testResult = new Vector();     //the result of the test

	
	//gets the vector with the id of the test
	public Vector getTestID(){
		
		return testID;
	}
	
	//gets the vector with the data of the test
	public Vector getDateOfTest(){
		
		return dateOfTest;
	}
	
	//fets the vector with the result of the test
	public Vector getTestResult(){
		
		return testResult;
	}
	
	
	public void  readFromDB(){
		//method for reading from the DB to load for display in interface	
		
		
	    String sql ="select "+ DBConstants.FIELD_PARENTSITE_TESTS_IDRESULT_ID+", "+DBConstants.FIELD_PARENTSITE_TESTS_DATE+ ", " + DBConstants.FIELD_PARENTSITE_TESTS_RESULT + 
        " from "+ DBConstants.TABLE_PARENTSITE_TESTS_RESULT ;

    DBConnection conn = null;
    DatabaseData data;

    try {

        conn = ConnectionPool.getInstance().requestConnection();
        data = conn.executeSQL(sql);
     
    	
        for (int i = 0; i<data.getRowNumber(); i++){
        	
        	//get the id
			int testId = ((Integer)data.getValue(1,i)).intValue();
			
			//read from the data base
			String dt = ((String)data.getValue(2,i));
            
			//read the result of the test 
			String resultVal =  ((String)data.getValue(3,i));
			
	        //adds the data of the test 
	        
	        testID.add(testId);
	        
	        dateOfTest.add(dt);
	        
	        testResult.add(resultVal); 
				
        	
        }

    } 
    catch (Exception e) 
    {
    	e.printStackTrace();
    } 
    finally
    {
        try
        {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        catch(BaseException e)
        {
        
        }           
    }

	}
}
