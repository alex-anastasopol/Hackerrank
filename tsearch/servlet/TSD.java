package ro.cst.tsearch.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.monitor.TSDTime;
import ro.cst.tsearch.threads.ASMaster;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.URLMaping;

public class TSD extends BaseServlet {

	private static final long serialVersionUID = -4729283921605922255L;
	protected static final Logger logger = Logger.getLogger(TSD.class);
	
	public static void initSearch(User currentUser, Search global, String sPath, HttpServletRequest request) throws IOException {
		SearchAttributes sa = global.getSa();

		if (sa != null && sa.getCertificationDate().getDate() == null) {
			try {
				sa.setCertificationDate();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void doRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		long startTime = System.currentTimeMillis();

		HttpSession session = request.getSession();
		User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
		UserAttributes currentUserA = currentUser.getUserAttributes();
		Search global = (Search) currentUser.getSearch(request);
	
	   //daca search-ul este fake ( a se vedea definitia in  Search->unFakeSearch(User user)), se 	    
	    if (SearchManager.isFakeSearch(global))
	 	     SearchManager.removeFakeSearch(global, currentUser); 

		String sPath = getServletConfig().getServletContext().getRealPath(request.getContextPath());

		String searchIdStr = request.getParameter("searchId");
		String query1 = request.getParameter("query1");
		
		if(query1!=null && searchIdStr!=null &&!"".equals(query1)){
			//Search.searchInit(query1, Long.parseLong(searchIdStr), sPath, request);
		}
		else{
			initSearch(currentUser, global, sPath, request);
		}
		
		TSDTime.update(System.currentTimeMillis() - startTime);
	
		String dispatcher = request.getParameter("dispatcher");
		if(global.getSearchState() == Search.STATE_FROM_PARENT_SITE && dispatcher!=null && (String.valueOf(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION).equals(dispatcher)
				||String.valueOf(URLConnectionReader.SAVE_TO_TSD_ACTION).equals(dispatcher)
				||String.valueOf(URLConnectionReader.CONTINUE_TO_NEXT_SERVER_ACTION).equals(dispatcher)) ) {
//          ASMaster.startSearch(global, ASMaster.JOIN_EXISTING, "true", "false");
			if (!global.getSa().isDataSource()) {
				ASMaster.startSearch(global, ASMaster.JOIN_EXISTING, "true", "false", currentUser);
			}
            global.setSearchState(Search.STATE_NONE);
        }
		
		//request.getParameterMap()
		response.setContentType("text/plain");
	    //PrintWriter out= response.getWriter();
	        
	    response.setStatus(response.SC_MOVED_TEMPORARILY);
	    
	    boolean extrafiles = "true".equals(request.getParameter("extrafiles"));
	    
	    String location = URLMaping.path+"/jsp/newtsdi/tsdindexpage.jsp?searchId="+global.getID()+"&userId=" +  currentUserA.getID().longValue();
	    
	    if(extrafiles) {
	    	location += "&extrafiles=true";
	    }
	    
	    String numberOfSavedDocuments = request.getParameter("NumberOfSavedDocuments");
	    if(numberOfSavedDocuments != null && numberOfSavedDocuments.matches("\\d+")) {
	    	location += "&NumberOfSavedDocuments=" + numberOfSavedDocuments;
	    }
	    
	    response.setHeader("Location",  location );
		
	}
	
}

	