package ro.cst.tsearch.AutomaticTester;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.Vector;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityUtils;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.LinkProcessing;
import ro.cst.tsearch.servers.types.TSInterface;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.titledocument.TSDManager;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.utils.InstanceManager;



/*
 * class that contains the search and the result of the search in the form of 
 * an object Line of search that contains the data obtained during the search  
 * 
 * */
public class testDataExecution {

	
	private SearchResults result;
    //records the data for execution of search 
	
	public void setSearchResults( SearchResults sRes ){
	//sets the dat for the execution of search 	
		result = sRes;
	}
	
	public SearchResults getSearchResults(){
	//returns the result of the search and the data that was used for the search 
		return result; 
	}
	
	 
	
	
	//construct an object to store the results of the search 
	
	private static AutomaticTesterManager automaticTesterManagerInstance = null;
	
	/*
	public LineOfSearch getLineOfSearch(){
	//returns the present line of search 	
		return line;
	}
	*/
    
    public LineOfSearch testSearch(XMLAutomaticTestDataReader test ) throws ServerResponseException{
    /*method for starting the test search 
      first instantiate the data necesary for the search as an object with test data read from 
      an XML file then call the 
      testExecution() method with the data necessary for the search 
      returns the boolean result - true if the search returns the same data as the data when the data was saved as test data 
      	
    */
    	
    	//determine the data for the call to the function 
        //boolean response = false;
    	LineOfSearch response = null ;
    	
    	int viServerAction = 1;
    	
    	int indexInSearchVector = 1;
    	

		
		
		//extract a line of search 
    	LineOfSearch line = new LineOfSearch();
		
    	
		//get the module id
		int moduleID =  test.getModuleId();
		
		//get the vector with the pages and indexes
		Vector PageAndIndex = test.getPagesVector(); 
		
		//get the list with the funtions
		ArrayList L = test.getFunctionList();
    	
		//get the p1 parameter
		String p1 = test.getP1();
		
		//get the p2 parameter
		String p2 = test.getP2();
		
		//get the search ID
		long SerachID = test.getSearchID();
		
		//get the name of the server
		String classNAME = test.getClassNAME();
		
		//get the path 
		String msSiteRealPath = test.getMsSiteRealPath();
		
	  
		try {
			
		//execute the test search 
    	response = testExecution(moduleID,PageAndIndex,L,p1,p2,classNAME,msSiteRealPath,viServerAction,SerachID );
		}
		catch(Exception e){
			
			System.out.println(" Exception in    testExecution()   ");
			e.printStackTrace();
		}
    	
    	return response;
    	
    }
    
	
	/* function for producing the search from the data stored in the test cases   
	 * 
	 * Data :
	 * 
	 * moduleID              - the id of the module                         -  for the instantiation of TSServerInfoModul 
	 * PageAndIndex     - vector with the pages and the indexes of the links from pages that are hit 
	 * ArrayList L            - the list with the function parameters
	 * p1                          - first parameter of the search            -  for the instantiation of TSServerInfoModul 
	 * p2                          - seccond parameter of the search   -  for the instantiation of TSServerInfoModul 
	 * SearchID              - the id of hte search                            -  the id of the search 
	 * classNAME          - the name of he server                       -  the name of hte server for the instantiation of the TSServerInfoModul
	 * msSiteRealPAth  - the path of the folder                         -  for the instantiation of TSServerInfoModul 
	 * viServerAction     - the type of the search that is made  - for the case  
	 *                                           viServerAction == TSServer.REQUEST_SEARCH_BY - the case of the initial search
	 *                                           viServerAction == TSServer.REQUEST_GO_TO_LINK - the case of the search after the initial search  
	 * idexInSearchVector - the index in the vector with the pages and links 
	 * 
	 * */
	public LineOfSearch testExecution(int moduleID,Vector PageAndIndex,ArrayList L,String p1,String p2,String classNAME,
			                                      String msSiteRealPath, int viServerAction, long SerachIDSaved) throws ServerResponseException {
		
		boolean testResult = true;
		int indexInSearchVector = 0;
		
		//perform the search given an id , the id of the module the parameters of the function and the link 
		
		/* case A. - the base case of the procedure 
		 *    if the search has arrived at the last element in the search vector then it is the case the results should be compared with the result of the search 
		 * 	    performed when test data was saved
		 * 	case A1.
		 *    		if the results of the search are identical with the result of the saved data then the result is entered in the vector with the results of the search as ok  
		 * 
		 * 	case A2.
		 *    		if the results of the search are not identical with the result of the saved data the result false is entered and the differences are returned from the search object
		 *         		
		 * else
		 * case B. if the search has not arrived at the last element in the search vector then it is the case the search has not arrived at the last 
		 *               level in the search tree and the search continues 
		*/
		//create an object for the results	
		LineOfSearch  ls = new LineOfSearch();
		
		//the response of the server
		ServerResponse  Result = null;
		
		//the size of the function 
		int iFunctionCount = L.size() ;
		
		//obtain the module from the functionCount and moduleID
		//TSServerInfoModule module = new TSServerInfoModule(iFunctionCount, moduleID);   
		
		TSServerInfoModule module = null , moduleCall = null  ;
		
		//instantiate a search object
		//Search srch = new Search();
		
		//setFunction(int functionIndex, TSServerInfoFunction fct)
		
	
		
		//---------------- get user -----------------------
		
        String sPath;
        String rootPath;
        
	    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
	    
		String sToday = String.valueOf(calendar.get(Calendar.YEAR));
		sToday += "_" + String.valueOf(calendar.get(Calendar.MONTH) + 1);
		sToday += "_" + String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));

	    rootPath = BaseServlet.FILES_PATH;
	    sPath = BaseServlet.REAL_PATH + File.separator + "title-search";
	
        String userPath = rootPath + TSDManager.TSDDir + File.separator + sToday + File.separator;
        
     
	
        
        User testUser = PresenceTesterManager.testUser;
	
        Search  src = SearchManager.addNewSearch(testUser, false);
        
        try{
        
        src.setP1(p1);
        src.setP2(p2);
        
       long SearchID  = src.getSearchID();
            
         //---------------------------------------   
		
           CommunityAttributes ca = null;
           
           try
           {
               ca = CommunityUtils.getCommunityFromId( testUser.getUserAttributes().getCOMMID().longValue() );
           }
           catch(Exception e)
           {
               e.printStackTrace();
           }
           
         //InstanceManager.getManager().getCurrentInstance( searchId ).getCrtSearchContext().getSa();
           
           InstanceManager.getManager().getCurrentInstance(src.getID()).setCrtSearchContext(src);
           InstanceManager.getManager().getCurrentInstance(src.getID()).setCurrentUser(testUser.getUserAttributes());                
           InstanceManager.getManager().getCurrentInstance(src.getID()).setCurrentCommunity(ca);  
            
		 //obtain the search attribute
		 SearchAttributes sa = src.getSa();
		
		//declare a TSInterface in order to make a call to SearchBy function  
		TSInterface roServerInterface = null;
		
		
		//obtain the currentSearch
		Search currentSearch = InstanceManager.getManager().getCurrentInstance(SearchID).getCrtSearchContext();
		 
		//obtain the roServerInterface object 
		try{
			//not really used any more
			roServerInterface = TSServersFactory.GetServerInstance( (int)TSServersFactory.getSiteId("", "", ""), p1, p2, SearchID );
			roServerInterface.setServerForTsd(currentSearch, msSiteRealPath);
								
		}
		catch( Exception e ){
			e.printStackTrace();
		}
    		
		TSServerInfo msiServerInfoDefault = roServerInterface.getDefaultServerInfo();
		
		//TSServerInfoModule getModule(int moduleIndex)
		int number =  msiServerInfoDefault.getModuleCount();//returns miModuleCount
		
		ArrayList ts = msiServerInfoDefault.getKeyList();
		//returns a list of keys from the hashmap
		
		

		
		for(int i=0 ; i < ts.size() ; i++ ){
			
			Integer scurent = (Integer) ts.get(i);
			
			module = msiServerInfoDefault.getModule(scurent.intValue());
			
			try{
			
			int modID = module.getModuleIdx();
		
			
			if(modID == moduleID)
				moduleCall = module;
			
			}
			catch(Exception e){
				System.out.println( " Exception at getting the module " );
				e.printStackTrace();
			}
		
		}
		
		
		
		try{
		
		for(int i=0 ; i<iFunctionCount ; i++){
			
			TSServerInfoFunction fct = (TSServerInfoFunction)L.get(i);
		
			moduleCall.setFunction( i , fct );
			
			}
		}
		catch(Exception e){
			System.out.println("Exception at the setting functions to module ");
			e.printStackTrace();
			
		}
		
    	//the case of the initial search 
   	
    		//step1: obtain the response of the sever
    		
    		try{
    			//obtain the result of the search - start the search with the data saved
    			Result = roServerInterface.SearchBy( moduleCall , new SearchDataWrapper(sa) );
    			
       		}
    		catch(Exception e){
    		
    			System.out.println(" Exception in testDataExecution  at SearchBy() ");
    			e.printStackTrace();
    		}
    		    
    		
    			//extract the string representing the pege from the Result 
    			String SearchPage = Result.getParsedResponse().getResponse();
    			
    			//===========================
    			//test code 
    			
    			//String searchPageTransformed = testDataEnter.testDataEnter( SearchPage );
    			
    			//SearchPage = searchPageTransformed;
    			
        		//===========================
        	    //get the current PageAndIndexOfLink element from the vector 
        		PageAndIndexOfLink currentElcompare = (PageAndIndexOfLink) PageAndIndex.elementAt(indexInSearchVector);
        		
        		//get the index of the link
        		int indexLink = currentElcompare.getIndex();
        		
                //get the string with the current page 
        		String HTMLcompare =  currentElcompare.getPage();
    			
        		String seachIdString = SearchID + "";
        		
        		String searchToReplace = SerachIDSaved + "";
        		
        		
        		HTMLcompare =  HTMLcompare.replaceAll( searchToReplace , "" );
        		
        		
        		int pozitieSrID = HTMLcompare.indexOf(searchToReplace);
        		int pozitieSrID2 = SearchPage.indexOf(seachIdString);
        		
        		String searchPageForComparison = SearchPage.replaceAll(seachIdString, "" );
        		
        		int pozitieSrID3 = searchPageForComparison.indexOf(seachIdString);
        		
        		
        		
        		int lengthSearchPageForComparison = searchPageForComparison.length();
        		int lengthSearchPage = HTMLcompare.length();
        		
        		
        		/*
        		int indexDif = 0;
        		int x = 1;
        		
        		for(int i=0;i<lengthSearchPage;i++){
        			
        			if(  searchPageForComparison.charAt(i) != HTMLcompare.charAt(i) )
        			          indexDif = i;
        		}
        		*/
        		
    		
    			
    			//build the parameter for the next search
    			
    			//instantiate the comparison object 
    			resultCompare comp = new resultCompare();
    			
    			    			
    			//make the comparison test 
    			boolean comTest = false;
    			ComparisonResult cr = null;
    			
    			try{
    			
    		    cr = comp.resultCompare( searchPageForComparison , HTMLcompare  );
    			
    			//ads the curent result to the search record
    		
    		
    			comTest = cr.getPagesEqualFalse();
    			
    			//add the result of the 
    			ls.addPresentTest(cr);
    			
    			}
    			catch(Exception e){
    				System.out.println("Error in comparison ");
    				e.printStackTrace();	
    			}
    			
    			if( comTest == false )
    				testResult = comTest;
    			
    			
    			
    		//step2: compare the response of the server with the beginning of the Vector with pages and indexes
    		
    		
    		//if the response of the server contains the string equal with the string that is stored as test case data then
    		//the search in depth will continue until the last level of the search - the level where the final data was saved
    		
    		
    		//if(  responseString.equals( testDataString )  ){
    		//     then go in depth of the serch more with  call with the parameters TSServer.REQUEST_GO_TO_LINK 
    		//     because now the search is in depth on the browsing section    
    		//}
    		//else 
    		//{ it means that the page was not identical with the page that was obtained at the time the search data was found
    	    //  add the conclusion to the testVectorSearcData
    	    //  return from the search     
    		//}
          	
    	if(testResult == false){
    		
    		return ls; 
    	}
    	else{
    			
    			String htmlCurrentPage = SearchPage;
    			
        //the case when links are hit after the initial search 
    	 
    	for(int i=0; ( i < PageAndIndex.size()-1 ) && ( comTest == true )  ;i++) 
    	{	
    		
    		//step1: obtain the response of the server 
    		
    		//obtain the link that was hit in order to set it as a parameter 
    		
    		//if(  responseString.equals( testDataString )  ){
    		//     then go in depth of the serch more with  call with the parameters TSServer.REQUEST_GO_TO_LINK 
    		//     because now the search is in depth on the browsing section    
    		//}
    		//else 
    		//{ it means that the page was not identical with the page that was obtained at the time the search data was found
    	    //  add the conclusion to the testVectorSearcData
    	    //  return from the search     
    		//}
    		
    		indexInSearchVector = i;
    		
    	    //get the current PageAndIndexOfLink element from the vector 
    		PageAndIndexOfLink currentEl = (PageAndIndexOfLink) PageAndIndex.elementAt(indexInSearchVector);
    		
    		PageAndIndexOfLink nextEl = (PageAndIndexOfLink) PageAndIndex.elementAt(indexInSearchVector+1);
    		
            //get the string with the current page 
    	
    		
    		
    		//get the no of the link 
    	    int noLink = currentEl.getIndex();
    		
    
    	    
    	    //-----------------------------------------------------
    	    
//    	  obtain the string with the link from the page 
    		String requestParams =  LinkProcessing.getLink(noLink, searchPageForComparison);
    		
    	//	String forComp = LinkProcessing.getLink(noLink,HTML);
    		
    		boolean flag = true;
    		
    		
    			
            int pozitie = requestParams.indexOf("?");
            
            String LINK = null;
            
            try{
            
            if( pozitie > -1 ){
            
            	LINK = requestParams.substring(pozitie+1);
            	  requestParams = LINK;
            }
            
            }catch(Exception e){
            	
            	System.out.println("Exception in substring");
            	e.printStackTrace();
            }
            
          
    	    
    	    //-----------------------------------------------------
    	
    		try{
    		
    			//obtain the response from the server
    			Result = roServerInterface.GetLink(requestParams, true); 
    			
    			//=====================================================================================
    			//change the data for tests
    			
		        //test code 
    			
    		
    			
    			//extract the string representing the pege from the Result 
    			//String SearcPageForNext = Result.getResult();
    			
    		    //======================================================================================
    			
    			//extract the string representing the pege from the Result 
    			SearchPage = Result.getParsedResponse().getResponse();
        		
//    			searchPageTransformed = testDataEnter.testDataEnter( SearchPage );
    			
  //  			SearchPage = searchPageTransformed;
    			
        	    //get the current PageAndIndexOfLink element from the vector 
                currentElcompare = (PageAndIndexOfLink) PageAndIndex.elementAt(indexInSearchVector + 1);
        		
        		//get the index of the link
        	    indexLink = currentElcompare.getIndex();
        		
                //get the string with the current page 
        		HTMLcompare =  currentElcompare.getPage();
    			
        	    seachIdString = SearchID + "";
        		
        		searchToReplace = SerachIDSaved + "";
        		
        		
        		HTMLcompare =  HTMLcompare.replaceAll( searchToReplace , "" );
        		
        		
        		pozitieSrID = HTMLcompare.indexOf(searchToReplace);
        		pozitieSrID2 = SearchPage.indexOf(seachIdString);
        		
        		searchPageForComparison = SearchPage.replaceAll(seachIdString, "" );
        		
        		pozitieSrID3 = searchPageForComparison.indexOf(seachIdString);
        		
        		
        		
        		lengthSearchPageForComparison = searchPageForComparison.length();
        		lengthSearchPage = HTMLcompare.length();
    			
    			
           
        		
    			
    			//compare with the string extracted from the seach data 
    			//perform the comparison 
    			
        		cr = comp.resultCompare( searchPageForComparison , HTMLcompare );
        		
        		comTest = cr.getPagesEqualFalse();
    			                   
        		//add the result of the 
    			ls.addPresentTest(cr);
    			
    			if( comTest == false )
    				testResult = comTest;
    			//build the parameter for the next search
    			//HTMLcompare = SearcPageForNext;
    		 
    			if( comTest == false )
    				break;
    			
    		}
    		catch(Exception e){
		
    			System.out.println(" Exception in testDataExecution  at GetLink() ");
    			e.printStackTrace();
    		}
    		
    		
    		
    		//step2: compare the response of the server with the the next page 
    	  }//close the for 
    		
    	//close the else
    	
    	ls.setPresentTestResult(testResult);
    	
    	//returns the result of the search
    	return ls;
	       
	   }
    	
      }
      catch( Exception e ){
    	  e.printStackTrace();
      }
      finally{
      	SearchManager.removeSearch(src.getID(), true);    	  
      }
      
      return ls;
	}
}
