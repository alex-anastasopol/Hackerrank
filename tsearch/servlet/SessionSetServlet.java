package ro.cst.tsearch.servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Category;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.AutomaticTester.*;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;
import java.io.*;
import java.util.Enumeration;

public class SessionSetServlet extends BaseServlet {
    
	protected static final Category logger= Category.getInstance(SessionSetServlet.class.getName());

	public void doRequest(HttpServletRequest request, HttpServletResponse response) throws Exception
	{       
	    HttpSession session = request.getSession(true);
        User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);

        String searchId = request.getParameter( "searchId" );
        Search currentSearch = null;
        
        try{
        	currentSearch = SearchManager.getSearch( Long.parseLong( searchId ) );
        }
        catch( Exception e ){
        	e.printStackTrace();
        	currentSearch = null;
        }
        
        if( currentSearch != null ){
        	String sortAscendingChapters = (String) request.getParameter("sortAscendingChapters");
        	String jsSortChaptersBy = (String) request.getParameter( "jsSortChaptersBy" );
        	
        	currentSearch.setJsSortOrder(jsSortChaptersBy, sortAscendingChapters);
        }
        
	    return;
	}
}
