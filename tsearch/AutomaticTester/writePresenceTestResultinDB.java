package ro.cst.tsearch.AutomaticTester;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.Log;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;




/* class used to write the results of the presence tests in DB
 * contains method for writing in the DB the result of the presence tests
 * contains method for reading from the DB to load for display in interface 
 * */
public class writePresenceTestResultinDB {
	
	
	public static void writeLoSToDB(LineOfSearch ls){
		//method for writing in the DB the result of the presence tests
		//write a search record to DB - the search record contains the result of the presence test
			
				
				//get the data from a line of search 
				
				//get the test id
				int tID = ls.getTestID();
				
				//get the present test result
				int s = ls.getPresentTestResult()?1:0;
				
				//get the date
				Date dt = new Date();
				
				
				//enter the data in the data base
				
				  DBConnection conn = null;
				  
				  String resultat = "";

				  //determine if there is any record on the data 
				  int exist = 0;
				
				 
				 try{
				  
			
				 conn = ConnectionPool.getInstance().requestConnection();	  
				 String st = "SELECT testResult FROM "+DBConstants.TABLE_PARENTSITE_TESTS_RESULT+" WHERE test_id=" + tID;	  
				 
				  DatabaseData dbData = conn.executeSQL( st );  
				  
				  if (dbData.getRowNumber() >= 1 )
					  exist = 1;
				  }
				  catch(Exception e){
					  
					  e.printStackTrace();
				  }
				  finally
				    {
				        try
				        {
				            ConnectionPool.getInstance().releaseConnection(conn);
				        }
				        catch (Exception e){
				        	e.printStackTrace();
				        }
				    }
				  
				  //if there is no record on the data insert
				  
				  String stm = "";
				 
					  Date maDate = new Date(); 
					  Calendar c = Calendar.getInstance();
					  c.setTime(maDate);

					  resultat += c.get(Calendar.DATE);
					  resultat += "/" + c.get(Calendar.MONTH);
					  resultat += "/" + c.get(Calendar.YEAR); 
			        
				if( exist == 0 )
				  {	  
					  
					  stm = "INSERT INTO "+ DBConstants.TABLE_PARENTSITE_TESTS_RESULT 
			                                          + "("+DBConstants.FIELD_PARENTSITE_TESTS_IDRESULT_ID+","+ DBConstants.FIELD_PARENTSITE_TESTS_DATE+","+ DBConstants.FIELD_PARENTSITE_TESTS_RESULT+ ") " 
			                                          + "VALUES ( ?, ?, ? )";
			      
					  try {
							DBManager.getSimpleTemplate().update(stm,tID,resultat,s);
						} catch (Exception e) {
							e.printStackTrace();
						}
				  }
			      //if there are records on the data then update
				  else{
					
					  stm =   "UPDATE " + DBConstants.TABLE_PARENTSITE_TESTS_RESULT
					  							  + " SET date=?, testResult=? "
					  							  + " WHERE test_id=? ";
					  
					  try {
							DBManager.getSimpleTemplate().update(stm,resultat,s,tID);
						} catch (Exception e) {
							e.printStackTrace();
						}
				  }				
			}
			
		
		
		
	
	
	
	public static void writeToDB(SearchRecords sr){
	//method for writing in the DB the result of the presence tests
	//write a search record to DB - the search record contains the result of the presence test
		
		int noOfTestCases = sr.getSearchRecords().size();
		
		for( int i=0; i < noOfTestCases ; i++ ){
			//enumerate the records of the searches
		
			//get a line of search 
			LineOfSearch ls = (LineOfSearch) sr.getSearchRecords().elementAt(i);
			
			//get the data from a line of search 
			
			//get the test id
			int tID = ls.getTestID();
			
			//get the present test result
			int s = ls.getPresentTestResult()?1:0;
			
			//get the date
			Date dt = new Date();
			
			
			//enter the data in the data base
			
			  DBConnection conn = null;
			  
			 
			  
			 // String mysqlDate = new FormatDate(FormatDate.TIMESTAMP).getDate(dt);
		       
			 // String sDate = "str_to_date( '" + mysqlDate + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
			  
			  String resultat = "";

			  Date maDate = new Date(); 
			  Calendar c = Calendar.getInstance();
			  c.setTime(maDate);

			  resultat += c.get(Calendar.DATE);
			  resultat += "/" + c.get(Calendar.MONTH);
			  resultat += "/" + c.get(Calendar.YEAR); 
		        
		      String stm = " insert into "+ DBConstants.TABLE_PARENTSITE_TESTS_RESULT 
		                                                  + "("+DBConstants.FIELD_PARENTSITE_TESTS_IDRESULT_ID+","+ DBConstants.FIELD_PARENTSITE_TESTS_DATE+","+ DBConstants.FIELD_PARENTSITE_TESTS_RESULT+ ")" 
		                                                  + " VALUES ( ?, ?, ?)";
		                   

			  try {
					DBManager.getSimpleTemplate().update(stm,tID,resultat,s);
				} catch (Exception e) {
					e.printStackTrace();
				}
			
		}
		
	}
	

}
