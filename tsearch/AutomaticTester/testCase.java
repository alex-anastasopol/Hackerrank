package ro.cst.tsearch.AutomaticTester;

import java.util.Vector;

import org.w3c.dom.Document;

public class testCase {
	
	
  
    
    private Document documentReadTestID;
    //contains the id's of the test cases
    
    //vector used for the id's of the extracted data 
    private int vtestID;  
    
    //the string that contains the names of the servers
    private String VServerNAME ;
    
    //the string that contains the status of the case
    private String state;
    
    
    //==================================
    
    //setters of the test case values
    
    void setDocumentReadTestID(Document docRead){
    	//get the document with the data 
    	documentReadTestID = docRead;
    }
    
    void setVtestID(int testID){
    	//set the testID
    	vtestID = testID;
    }
    
    void setVServerNAME(String VServerName){
    	//set the server name 
    	VServerNAME = VServerName;
    }
    
    void setState(String st){
    	//set the state 
    	state = st;
    }
    
    
    //=====================================
    
    //getters of the test case values
    
    //get the ID
    public Document getDocumentReadTestID(){
    	
    	return documentReadTestID;
    }
    
    public int getVtestID(){
    	//get the testID
    	return vtestID;
    }
    
    public String getVServerNAME(){
    //get the name of the server 	
    	return VServerNAME;	
    }
    
    public String getState(){
    //get the state	
    	return state;
    }
    
    
}
