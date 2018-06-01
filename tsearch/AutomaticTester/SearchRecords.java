package ro.cst.tsearch.AutomaticTester;

import java.util.Vector;

public class SearchRecords {
//the class contains the record for the search 
	
	
   private Vector recordData = new Vector() ;
   //vector that records the contents and results of the search 
   
   private  Vector resultOfTest = new Vector();
   //the vector that contains the result of the search  true / false 
   
	public Vector getSearchRecords(){
	//gets the search record	
		return recordData;
	}
	
	public void addSearchRecord(LineOfSearch lsr){
	//adds a search record to the vector	
		
		recordData.add( lsr );
	}
	
    public void addTestValue(boolean val){
    //add the value of the test to the vector with the results ok/	
    	resultOfTest.add(val);
    }

	
	//process the result for statistical purposes 
	public void process(){
		
		
	} 
	
	
}
