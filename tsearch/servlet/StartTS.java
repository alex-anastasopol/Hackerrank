package ro.cst.tsearch.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.AppLinks;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.SaveSearchException;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;

public class StartTS extends BaseServlet
{
	public void doRequest(HttpServletRequest request,
				  HttpServletResponse response)
	throws IOException, ServletException
	{
		//set last access param
		HttpSession session=request.getSession(true);
		User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
		Search global= (Search) currentUser.getSearch( request);		
		int searchType = global.getSearchType();
		
		if (searchType == Search.PARENT_SITE_SEARCH) {
		    
			sendRedirect(request, response, AppLinks.getParentSiteNoSaveHref(global.getSearchID())); 
			
		} else if (searchType == Search.SAVE_SEARCH) {
		    
		    try {
				//saveSearch(global);
				DBManager.saveCurrentSearch(currentUser, global, Search.SEARCH_TSR_NOT_CREATED, request);
			} catch (SaveSearchException e) {
				e.printStackTrace();
				forward(request, response, URLMaping.StartTSPage + 
		    			"?" + TSOpCode.OPCODE + "=" + TSOpCode.ERROR_OCCURS + 
						"&" + RequestParams.ERROR_BODY + "=" + e.getMessage());
				return;
			}
			
			forward(request, response, URLMaping.StartTSPage);			
		    
		} else {
			forward(request,response,URLMaping.AutomaticSearch );
		}

	}
}