package ro.cst.tsearch.servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Category;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.monitor.AutomaticSearchTime;
import ro.cst.tsearch.threads.ASMaster;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;

public class AutomaticSearch extends BaseServlet {
    
	private static final long serialVersionUID = -600441769469037485L;
	protected static final Category logger= Category.getInstance(AutomaticSearch.class.getName());

	public void doRequest(HttpServletRequest request, HttpServletResponse response) throws Exception
	{
	    
	    long t0 = System.currentTimeMillis();
	    String forwardTo = URLMaping.StartTSPage;
	    
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
		if ("true".equals(goBackOneLevel)){
			//  search.setP2("3");  
			//search.setGoBackType(request.getParameter("gobackselect").toString());
			ASMaster.startSearch(search, ASMaster.START_NEW, continueSearch, goBackOneLevel, currentUser);
		}
		else{
			ASMaster.startSearch(search, ASMaster.JOIN_EXISTING, continueSearch, goBackOneLevel, currentUser);
		}
		ASMaster.waitForSearch(search);
	    	    
		AutomaticSearchTime.update(System.currentTimeMillis() - t0);
	    return;
	}
}
