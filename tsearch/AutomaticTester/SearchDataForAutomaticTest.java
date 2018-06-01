package ro.cst.tsearch.AutomaticTester;
import java.util.Vector;

public class SearchDataForAutomaticTest {
	/*
	 * Retains the Search Data , the vector and the PageAndIndexOfLink Vector
	 * 
	 * */
	
	
	private static Vector VectorWithSearchData = new Vector() ;
	//the vector with the Objects that contain the index and page 
	
	
	
    public static synchronized void addVectorWithSearchData(SearchRecord objectElem ){
    	/*   adds an object containing the data representing the search to the vector   
    	 *   
    	 * */
    	
    	VectorWithSearchData.add(objectElem);
    	
    }
    
    
    public static Vector getVectorWithSearchData(){
    	/*  returns the object containing the data representing the search vector  
    	 * 
    	 * */
    	
    	return VectorWithSearchData;
    }
    
 
		
 }
	
	

