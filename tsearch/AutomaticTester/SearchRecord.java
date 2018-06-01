package ro.cst.tsearch.AutomaticTester;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;

/**
 * 
 *  Search helper class, store temporary parameters/fields for a search here.
 *  <br>
 *  Objects stored here are transient for the Search.
 * 
 */

public class SearchRecord {
	/*   Contains the result of the search in the form of Module , page and Index and SearchResult Vector
	 *    The class will be used to instatiate objects that would be used to store data 
	 * */
	
	//private String Module = new String(); ;
	//vector that contains the data with the Initial State from where the search proceds  
	
	
	//class variables
	
    private Vector pageAndIndex = new Vector();
    //vector that contains the data with the objects , th e array with the html pages and the links
    
    private String SearchResults = new String();
	//string with the data from the search 
    
    /**
     * saves the last module searched by
     */
    private TSServerInfoModule module; 
    
    //----------------------------------------------------------------------
    //parameters for the instatntiation of TSInterface 
    
    private String p1;
    //paramater for the instantiation of TSInterface
    
    private String p2;
    //parameter for the instantiation of TRInterface
    
    private long SerachID;
    //parameter that contains the Search ID
    
    private String classNAME;
    //the name of the server 
    
    private String msSiteRealPath;
    //the path to the work directory
    
    //----------------------------------------------------------------------
    
    //constructor
    public SearchRecord(){
    }
    
    
    
    //class methods
    
    
    public void setParameterP1(String parameter){
    //sets the paramter p1
    	p1 = parameter;
    }
    
    public String getParameterP1(){
    //gets the paramter p1	
    	return p1;
    }
    
    
    public void setParameterP2(String paramter){
    //sets the parameter p2	
    	p2 = paramter;
    } 
    
    public String getParameterP2(){
    //gets the paramter p2
    	return p2;
    }
    
    
    public void setSearchID(long SearchID){
    //sets the parameter SearchID	
    	SerachID = SearchID;
    }
    
    public long getSearchID(){
    //sets the search ID
    	return SerachID;
    }
    
    public void setServerName(String server){
    //sets the server name 
    	classNAME = server;
    }
    
    public String getServerName(){
    //gets the server name 
    	return classNAME;
    }
    
    public void setMsSiteRealPath(String MsSiteRealPath){
    //sets the name of the path to the server 
    	msSiteRealPath = MsSiteRealPath;
    }
    
    public String getMsSiteRealPath(){
    //gets the name of the path to the server
    	return msSiteRealPath;
    }
    
    /**
     * 
     * @return last module function list
     * 
     */
    
    public List<TSServerInfoFunction> getFunctionList(){
    	if(module == null)
    		return new ArrayList<TSServerInfoFunction>();
    	
    	return module.getFunctionList();
    }
    
    /**
     * 
     * @return moduleIndex - as in {@link TSServerInfo}
     * 
     * <br><br>
     * e.g. TSServerInfo.NAME_MODULE_IDX
     */
    public int getModuleID(){
    	if(module == null)
    		return -1;
    	
    	return this.module.getModuleIdx();
    }
    
  
    public void addPageAndIndex(PageAndIndexOfLink ob){
    //sets the data of the vector with the PageAndIndex object	
    	pageAndIndex.add(ob);
    }
    
    public void setPageAndIndexIndex(int index){
    //sets the index of the last element in the vector	
    	
    	if( pageAndIndex != null && pageAndIndex.size() >= 1 ){
	    	PageAndIndexOfLink x =	  (PageAndIndexOfLink)pageAndIndex.lastElement();
	        x.setIndex(index);
    	}
        
    }
    
    public Vector getPageAndIndex(){
    //returns the object contianing the page and index 	
    	
    	return pageAndIndex;
    }
    
    
    public PageAndIndexOfLink getPageAndIndexOfLinkLastElement() {
    //returns a pointer to the last object in the string
    	
    	return (PageAndIndexOfLink)pageAndIndex.lastElement();
    }
    
    
    public void setSearchResults(String str){
    //sets the string record with the search result	
    	SearchResults = str;
    }
    
    public String getSearchResults(){
    //returns the string with the search results 	
    	return SearchResults;
    }


	/**
	 * @return the last module searched by
	 */
	public TSServerInfoModule getModule() {
		return module;
	}

	/**
	 * @param module
	 *            set last module searched by
	 */

	public void setModule(TSServerInfoModule module) {
		this.module = module;
	}
    
}
