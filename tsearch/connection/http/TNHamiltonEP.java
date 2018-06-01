package ro.cst.tsearch.connection.http;

import java.util.Vector;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;


public class TNHamiltonEP extends HTTPSite {

	public LoginResponse onLogin() 
	{

		HTTPRequest req = new HTTPRequest( "http://propertytax.chattanooga.gov/" );
		process(req);
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	public void onBeforeRequest(HTTPRequest req) 
	{
		
		String link = req.getURL();
        
	    String textQuery, cmap = null, group = null, parcel = null;
	    
	    if (link.indexOf("both.idc") != -1) {
	        
	    	cmap = req.getPostFirstParameter("Map_no");
            if (cmap != null)
	    	cmap += generateSpace( 4 - cmap.length() );
	    	
	    	group = req.getPostFirstParameter("Group_no");
            if (group != null)
	    	group += generateSpace( 2 - group.length() );
	    	
	    	parcel = req.getPostFirstParameter("Parcel_no");
            if (parcel != null)
	    	parcel += generateSpace( 10 - parcel.length() );

	        if (cmap != null && group != null & parcel != null) {
	        	textQuery = cmap + " " + group + " " + parcel;
	        	req.setPostParameter("text_query", textQuery.toUpperCase());
	        }
	        
	    } else {
	    	
	    	textQuery = req.getPostFirstParameter("text_query");
	    	if (textQuery != null)
            {
                req.removePostParameters( "text_query" );
	    		req.setPostParameter("text_query", textQuery.toUpperCase());
            }
	    }
	}
	
	private String generateSpace(int n)
	{
	    String ret = "";
	    for( int i = 0 ; i < n ; i ++ )
	    {
	        ret += " ";
	    }
	    
	    return ret;
	}
}
