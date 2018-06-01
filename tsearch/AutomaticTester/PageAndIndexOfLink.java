package ro.cst.tsearch.AutomaticTester;

public class PageAndIndexOfLink {
	/*     Object used to retain the index of the link in the page and the page
	 * 
	 * */
	
	private int Index ;          //the int representing the index 
	
	private String Page ;    //the String representing the page 
	
	
	public int getIndex(){
	//returns the index of the link	
		return Index;
	}
	
	public String getPage(){
	//returns the string containing the page 
		return Page;
	} 
	
	public void setIndex(int ind){
	//sets the index	
		Index = ind;
	}
	
	public void setPage(String pag){
	//sets the page	
		Page = pag;
	}
	
	public PageAndIndexOfLink(){
	//constructor
		
	}
	
	public PageAndIndexOfLink( int index , String page ){
	//constructor
		Index = index ;
		Page = page;
		
	}
	
	

}
