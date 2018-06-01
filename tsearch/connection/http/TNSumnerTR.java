package ro.cst.tsearch.connection.http;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

public class TNSumnerTR extends HTTPSite 
{
    private boolean loggingIn = false;
    
    private String sid = "";
    private String query = "";
    
    public LoginResponse onLogin() 
    {        
        setUserData(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "TNSumnerTR");
        
        if ("".equals(sid)) {
        
        	HTTPRequest req = new HTTPRequest( "http://www.sumner.tennesseetrustee.org" );
	        
	        loggingIn = true;
	        
	        HTTPResponse res = process( req );
	        String response = res.getResponseAsString();
	        sid = "PHPSESSID=" + response.replaceAll("(?s).*PHPSESSID' value='(.*?)'.*", "$1");
	        
	        loggingIn = false;   
        }

        return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
    }
    
    public void onBeforeRequest(HTTPRequest req) 
    {
        if( loggingIn == true )
        {
            return;
        }
        
        String url = req.getURL();
        
        // search by Owner
        if ( url.indexOf("search.php") != -1 && url.indexOf("owner=") != -1 ) {
        	
        	loggingIn = true;
        	HTTPRequest first = new HTTPRequest( "http://www.sumner.tennesseetrustee.org" );
//        	first.setHeader( "Cookie", sid );
            process( first );
            
            req.setHeader("Host", "www.sumner.tennesseetrustee.org");                    
            req.setHeader("Referer", "http://www.sumner.tennesseetrustee.org");
            req.setHeader("Accept", "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/x-shockwave-flash, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
            loggingIn = false;
            
        	url = url.replaceAll("PHPSESSID=", sid);
        	
        	if ( url.indexOf("PHPSESSID=") == -1 )
        		url += "&" + sid;
        		
        	query = url;
        	
        	req.modifyURL(url);
        	
        // link details
        } else if ( url.indexOf("printCard.php") != -1 ) {
        	
        	req.setMethod(HTTPRequest.POST);
        	
        	url = url.replaceAll("PHPSESSID=.*?\\&", "");
        	req.modifyURL(url);
        	
//        	req.setHeader( "Cookie", sid );
        	req.setHeader( "Referer", req.getURL().replaceAll("printCard", "card") );
        	
        // link next
        } else if (url.indexOf("PHPSESSID=") != -1) {
    	
        	req.setMethod(HTTPRequest.POST);
        	
	    	url = url.replaceAll("&?PHPSESSID=.*?(\\&|$)", "");
	    	req.modifyURL(url);
	    	
	    	req.setHeader("Referer", query);
//	    	req.setHeader("Cookie", sid);
        	
            req.setPostParameter( "r", TSServer.getParameter("r", url) );
            req.setPostParameter( "p", TSServer.getParameter("p", url) );
            
            url = url.substring(0, url.indexOf("?"));
        	req.modifyURL(url);
	    }
        
        loggingIn = true;
        if( url.indexOf("addressno") != -1 )
        {
        	String addressno = TSServer.getParameter("addressno", url);
        	String address = TSServer.getParameter("address", url);
        	
        	url = url.replaceAll("(addressno=.*?)[&$]", "");
        	url = url.replaceAll("(address=.*?)[&$]", "");
        	
        	url += "&address=" + address + "+" + addressno;
        	
        	req.modifyURL(url);
        }
        loggingIn = false;
    }
}