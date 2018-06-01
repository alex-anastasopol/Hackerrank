package ro.cst.tsearch.AutomaticTester;

import java.util.Vector;

import org.w3c.dom.Document;


/*
 * The class SearchResults contains the results of a search
 *  The vector listOfSearchLines contains on each element vectors resulted from a search 
 *  performed with a Test Case
 *  
 * */

public class SearchResults {
	
	private Vector vDatabaseTestCaseForProcessing = new Vector();  
	//the data for the search
	
	private Vector listOfSearchLines = new Vector();
	//contains the list of the lines of search performed at one search session
	
	Document dom;
	
public void setSerchResults(Vector enter){
		
		vDatabaseTestCaseForProcessing = enter; 
	}

	
	public Vector getListOfSearchLines(){
	//retrurns the vector with the searches	
		return listOfSearchLines;
	}
	
	public void enterData(){
		
		//LineOfSearch los = new LineOfSearch(dom);  
		
	}
	
	public void process(){
		
		
	}

	
}
