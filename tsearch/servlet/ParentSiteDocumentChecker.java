package ro.cst.tsearch.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;

public class ParentSiteDocumentChecker extends BaseServlet
{
    public void doRequest(HttpServletRequest request, HttpServletResponse response)
    {
        String searchId = request.getParameter( "searchId" );
        if( searchId == null ){
            searchId = "0";
        }
        
        long searchIdLong = 0;
        try
        {
            searchIdLong = Long.parseLong( searchId );
        }catch( NumberFormatException nfe ) {}
        
        String checkDocumentLink = request.getParameter( "checkDocument" );
        
        String beginWith = request.getParameter( "beginWith" );
        if( beginWith == null ){
            beginWith = "";
        }
        
        Search currentSearch = SearchManager.getSearch( searchIdLong );
        
        if( currentSearch == null ){
            return;
        }
        
        currentSearch.clickDocument( checkDocumentLink, beginWith );
    }
}