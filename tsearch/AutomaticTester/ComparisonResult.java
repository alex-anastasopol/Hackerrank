package ro.cst.tsearch.AutomaticTester;

import java.util.Vector;

//the result of comparing two pages 

public class ComparisonResult {
	
	//contains the first page
	private String page1;
	
	//contains the seccond page
	private String page2;
	
	//the links from page1
	//private Vector linkListPage1 = new Vector();
	
	//the links from page2
	//private Vector linkListPage2 = new Vector();
	
	//if the links are equal
	private Vector linkDifference = new Vector();
	
	//true if the content of the link is true , false if it is not true
	private boolean isLinksContentEqual = true;
	
	//true if the page content is equal , false if it is not true 
	private boolean isPageContentEqual = true;
	
	//true if the pages are equal , false if the pages are not equal
	private boolean arePagesEqual = true;
	
	public ComparisonResult(String pg1,String pg2){
		
		page1 = pg1;
		
		page2 = pg2;
		
	}

	public void setPagesEqualFalse(boolean value){
		//set true or false if the content of the pages is equal 
		arePagesEqual = value;
	}
	
	public void setLinksContentEqualFalse(boolean value){
		//set true or false if the content of the pages are equal or false
		isLinksContentEqual = value;
	}
	
	public void setPageContentEqual(boolean value){
		//set true is the page content is equal
		isPageContentEqual = value;
	}
	/*
	public void addLinksFromPage1(String link){
		//add link from page 1 
		linkListPage1.add(link);
		
	}
	
	public void addLinksFromPage2(String link){
	   //add links from page 2	
		linkListPage2.add(link);
		
	}
	*/
	public void addlinkDifference(String difference){
		//add the difference between the links
		linkDifference.add(difference);
		
	}
	
	//getter methods 
	public boolean getPagesEqualFalse(){
		//get the boolean value that represents the fact that the page content are equal or not 
		return arePagesEqual;
	}
	
	public boolean getLinksContentEqualFalse(){
		//get the boolean value that represents the fact that the page content are equal or not 
		return isLinksContentEqual;
	}
	
	public boolean getPageContentEqual(){
		//get the boolean value that represents the fact that the page are equal or not 
		return isPageContentEqual;
	}
	/*
	public Vector getLinksFromPage1(){
		//get the vector with the links from page 1
		return linkListPage1;
		
	}
	
	public Vector getLinksFromPage2(){
		//get the vector with the links from page 2
		return linkListPage2;
		
	}
	*/
	public Vector getlinkDifference(){
	  //get the vector with the differences of the links 	
		return linkDifference;
		
	}
	
	
	
}
