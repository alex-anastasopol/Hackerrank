package ro.cst.tsearch.servlet.titledocument;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;

public class ReuseSearch extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	@Override
	public void service(HttpServletRequest request, HttpServletResponse response) 
	throws ServletException, IOException {
		String opCodeString = request.getParameter(TSOpCode.OPCODE);
		String searchIdString = request.getParameter(RequestParams.SEARCH_ID);
		String fileLink = request.getParameter("f");
		User currentUser = (User) request.getSession().getAttribute(SessionParams.CURRENT_USER);
		boolean isDateDown = Boolean.parseBoolean(request.getParameter("isDateDown"));
		boolean isFVSUpdate = Boolean.parseBoolean(request.getParameter("isFVSUpdate"));
		
		int opCode = -1;
		long searchId = 0;
		try {
			opCode = Integer.parseInt(opCodeString);
			searchId = Long.parseLong(searchIdString);
		} catch (Exception e) {
			opCode = -1;
		}

		if(opCode == TSOpCode.REUSE_SEARCH_CODE){
			response.sendRedirect(URLMaping.path + URLMaping.UpdateTSRIndex + "?newSearchId=" + searchId + 
				"&searchId=" + Search.SEARCH_NONE +
				"&isDateDown="+ isDateDown +
				"&opCode=" + TSOpCode.REUSE_SEARCH_CODE);
		} else if(opCode == TSOpCode.FVS_UPDATE){
			Search global = SearchManager.getSearchFromDisk(searchId);
			if (global != null) {
	        	SearchLogger.info("</div><div><b>FVS Update</b> search was launched from Dashboard "+ SearchLogger.getTimeStamp(global.getID())
										+ ".<BR><div>", global.getID());
				Search localSearch = global.createSearchForFVSUpdate();
	
				response.sendRedirect(URLMaping.path + URLMaping.UpdateTSRIndex + "?newSearchId=" + localSearch.getID() + 
						"&searchId=" + localSearch.getID() +
						"&isFVSUpdate=" + isFVSUpdate +
						"&opCode=" + TSOpCode.FVS_UPDATE);
			} else {
				String errorBody = "Initial order (with search id " + searchId + ") cannot be opened.<br/>Please contact ATS support.<br/>We are sorry for the inconvenience.";
	    		request.setAttribute("title", "Order cannot be opened!");
				request.setAttribute("msg", errorBody);
				request.getRequestDispatcher("/jsp/simpleErrorPage.jsp").forward( request,  response);
			}
			
		} else if(opCode == TSOpCode.CLONE_SEARCH_CODE){
			Search global = SearchManager.getSearchFromDisk(searchId);
			if (global != null) {
				//clone current search
		    	Search localSearch = global.cloneSearch();
		        SearchManager.setSearch(localSearch, currentUser);
		        response.sendRedirect(URLMaping.path + URLMaping.UpdateTSRIndex + "?newSearchId=" + localSearch.getID() + 
						"&searchId=" + searchId + 
						"&opCode=" + TSOpCode.CLONE_SEARCH_CODE);
			} else {
				String errorBody = "Initial order (with search id " + searchId + ") cannot be opened.<br/>Please contact ATS support.<br/>We are sorry for the inconvenience.";
	    		request.setAttribute("title", "Order cannot be opened!");
				request.setAttribute("msg", errorBody);
				request.getRequestDispatcher("/jsp/simpleErrorPage.jsp").forward( request,  response);
			}
		} else {
			if(fileLink!=null)
				fileLink = fileLink.replaceAll(BaseServlet.FILES_PATH , "");
			response.sendRedirect( 
					"/title-search/jsp/TSDIndexPage/viewDescription.jsp?" + 
					"f=" + fileLink + 
					"&viewOrder=1&userId=" + currentUser.getUserAttributes().getID().longValue() + 
					"&viewDescrSearchId=" + searchId);
		}
		
	}
	
	

}
