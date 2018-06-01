package ro.cst.tsearch.servlet;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.TSOpCode;

/**
 * Check whether a search is accessible to the current user
 * @author radu bacrau
 */
public class CheckAvailabilityServlet extends BaseServlet {

	static final long serialVersionUID = 1000000;
	
	public void doRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {		
	
		int searchDBId = Integer.valueOf(request.getParameter("searchDBId"));
		String opCodeString = request.getParameter(TSOpCode.OPCODE);
		int opCode = -1;
		HttpSession session = request.getSession();		
		User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
		
		PrintWriter out = response.getWriter();
		
		if(opCodeString!=null){
			try {
				opCode = Integer.parseInt(opCodeString);
			} catch (Exception e) {
				opCode = -1;
			}
		}

		if(opCode==TSOpCode.REUSE_SEARCH_CODE) {
			int searchStatus = DBManager.getTSRGenerationStatus(searchDBId);
			if(searchStatus == 1){
				out.print("OK-TSR");
			} else {
				DBManager.SearchAvailabitily searchAvailable =  
					DBManager.checkAvailability(searchDBId, currentUser.getUserAttributes().getID().longValue(), DBManager.CHECK_AVAILABLE, false);
				if(searchAvailable.status == DBManager.SEARCH_WAS_CLOSED) {
					out.print("OK-K");	
				} else {
					if("false".equals(request.getParameter("isReopen"))) {
						out.print("OK-K");	
					} else {
						out.print("Operation not permitted. This search is not closed.");
					}
				}
			}
		} else {
			System.err.println("++++++++++ Before checkAvailability servlet +++++++++");
			
			DBManager.SearchAvailabitily searchAvailable =  DBManager.checkAvailability(searchDBId, currentUser.getUserAttributes().getID().longValue(), DBManager.CHECK_AVAILABLE, false);
		    
			boolean searchWasUnlocked = DBManager.checkAvailability(searchDBId, currentUser.getUserAttributes().getID().longValue(), 
																	 DBManager.CHECK_OWNER, false).status == DBManager.SEARCH_WAS_UNLOCKED;
			
			System.err.println("++++++++++ After checkAvailability servlet +++++++++  searchAvailable.status = " +  searchAvailable.status );
			
			
			if (  searchAvailable.status != DBManager.SEARCH_AVAILABLE || searchAvailable.tsrInProgress ) {
				out.print("message=" + searchAvailable.getErrorMessage());    		
	    	} else {    	    
	    	    long userId = currentUser.getUserAttributes().getID().longValue();
	    	    int searchStatus = DBManager.getSearchStatus(searchDBId);
	    	    if (searchStatus == Search.SEARCH_STATUS_N)
	    	    	DBManager.setSearchOwner(searchDBId, -1, userId);
	    	    else if (searchStatus == Search.SEARCH_STATUS_T)
		    	    DBManager.setSearchOwner(searchDBId, userId);    	    
	    	    if(searchWasUnlocked) {
	    	    	out.print("OK SEARCH_WAS_UNLOCKED");
	    	    }else {
	    	    	out.print("OK");
	    	    }
	    	}
		}
		out.flush();
	}
	
}
