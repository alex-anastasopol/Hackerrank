package ro.cst.tsearch.servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Category;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.AutomaticTester.*;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;
import java.io.*;

public class SaveTestCase extends BaseServlet {
    
	protected static final Category logger= Category.getInstance(AutomaticSearch.class.getName());
	public static final String testCasesFolder = "TestCases";
	public static final String errorCasesFolder = testCasesFolder + "Error" + File.pathSeparator;

	public void doRequest(HttpServletRequest request, HttpServletResponse response) throws Exception
	{       
	    HttpSession session = request.getSession(true);
        User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
		Search search = (Search) currentUser.getSearch(request);
	    
		DBManager.SearchAvailabitily searchAvailable = DBManager.checkAvailability(search.getID(),currentUser.getUserAttributes().getID().longValue(), DBManager.CHECK_OWNER, false);
    	
		if (  searchAvailable.status != DBManager.SEARCH_AVAILABLE ) {

    	    String errorBody = searchAvailable.getErrorMessage();
    	    	    		
    		request.setAttribute(RequestParams.ERROR_BODY, errorBody);
    		
    		forward(request, response, URLMaping.StartTSPage + "?" + TSOpCode.OPCODE + "=" + TSOpCode.ERROR_OCCURS);
    		
    		return;
    	}
		
		String continueSearch = request.getParameter(RequestParams.CONTINUE_SEARCH);
		String goBackOneLevel = request.getParameter(RequestParams.GO_BACK_ONE_LEVEL);
		    
		//saving the search to the designated folder, for later using as a test case
		Search.saveSearchToPath( search, BaseServlet.FILES_PATH + testCasesFolder, null );
		
		//update the test case list of the automatic test manager
		AutomaticTesterManager.getInstance().updateTestCaseList(search);
		
	    String forwardTo = URLMaping.TSDIndexedPageJSP + "?" + RequestParams.SEARCH_ID + "=" + request.getParameter(RequestParams.SEARCH_ID)
							+ "&" + TSOpCode.OPCODE + "=" + request.getParameter( TSOpCode.OPCODE )
							//+ "&chapterIndexForUpload=" + request.getParameter( "chapterIndexForUpload" )
							+ "&newID=" + request.getParameter( "newID" );
	    
		try {
		    forward(request, response, URLMaping.TSDIndexedPage);
		} catch (Exception e) {
		    e.printStackTrace();
		    forward(request, response, URLMaping.StartTSPage);
		}
	    
	    return;
	}
}
