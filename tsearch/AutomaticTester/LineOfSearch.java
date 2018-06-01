package ro.cst.tsearch.AutomaticTester;

import java.util.Date;
import java.util.Vector;

/*
 * 
 * class that mentains the record of one search 
 * element in the search process one value is the result of the comparison 
 * the field is of type boolean
 *  
 * other element represent the differences of the two pages 
 * */

public class LineOfSearch {

	
	  //-------------------------- class data ------------------------------------------------------------
	
	 private int testID ;   // the id of the search 
	
	 private Date dt = new Date(); 
	 
	  public LineOfSearch(){
		  
	  }
	 
	  public LineOfSearch(int testid){
	  //sets the testID
		  testID = testid;
	  }
	  
	   private Vector presentTest = new Vector();
		//vector that contains the results of the search
	    //the results read from the page 
	   
	   private boolean booleanValuesOfTestResults ;
	  
	  
	   
	   
	   //------------------------------------------ geter and setter methods ---------------------------------------------------------------
	    
	   public int getTestID(){
	   //returns the id of the test case	   
		   return testID;
	   }
	   
	   public void setTestID(int TID){
	   //sets the id of the test 	   
		   testID = TID;
	   }
	   
	    public Vector getPresentTest(){
	    //returns the results of the search 
	    	return presentTest;
	    }
	
	    public void setPresentTest(Vector test){
		 //set the results of the search 
		    	presentTest = test;
		 }
	    
	    public void setPresentTestResult(boolean rez){
	    //set the result	
	    	booleanValuesOfTestResults = rez;
	    }
	    
	    public boolean getPresentTestResult(){
	    //returns the result of the test
	    	return booleanValuesOfTestResults;
	    }
	   
	    public void addPresentTest( ComparisonResult cr ){
	    //adds to resent text	
	    	presentTest.add(cr);
	    }
	    
	    public void setDate(Date date){
	    	
	    	dt = date;
	    	
	    }
	
	    public Date getDate(){
	    	
	    	return dt;
	    	
	    }
	    
}
