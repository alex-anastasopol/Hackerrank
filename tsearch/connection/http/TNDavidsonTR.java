package ro.cst.tsearch.connection.http;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;


public class TNDavidsonTR extends HTTPSite 
{
	
	private String sid;
    private boolean loggingIn = false;
    public LoginResponse onLogin() 
    {
        loggingIn = true;
        
        HTTPRequest req = new HTTPRequest( "http://tn-davidson-taxcollector.governmax.com" );
        HTTPResponse res = process(req);
        
        String response = res.getResponseAsString();
        sid = response.replaceAll("(?s)(.+sid=)(\\w+)(.+)", "$2");
        
        loggingIn = false;
        
        return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
    }
    
    public void onBeforeRequest(HTTPRequest req) 
    {
        if( loggingIn )
        {
            return;
        }
               
        if( sid == null )
        {
            onLogin();
        }
        
        if (req.getMethod() == HTTPRequest.POST && req.getPostParameter("sid") != null) {
        	
        	req.removePostParameters("sid");
        	
            req.setPostParameter("sid", sid);
            
            String referer = "http://tn-davidson-taxcollector.governmax.com/collectmax/agency/tn-davidson-taxcollector/list_collect_v5-5.asp?r=289&l_mv=next&sid=";
            
            req.setHeader("Referer", referer + sid);
        } 
        if (req.getPostParameter("owner") != null && req.getPostParameter("owner2") != null) 
        {           
        	String text = req.getPostParameter("owner") + " " + req.getPostParameter("owner2");
        	
        	req.removePostParameters("owner");

            req.setPostParameter("owner", text);
            
            req.postParameters.remove("owner2");
        }
        else if (req.getPostParameter("streetname") != null && req.getPostParameter("streetname1") != null) 
        {           
        	String streetName = req.getPostParameter("streetname") + " " + req.getPostParameter("streetname1");
            
        	req.removePostParameters("streetname");
        	req.setPostParameter("streetname", streetName.trim());
        	
            req.postParameters.remove("streetname1");
        }
        else if (req.getPostParameter("account")!=null){
        	String account=req.getPostParameter("account").toString();
        	req.removePostParameters("account");
        	req.setPostParameter("account",account);
        }
    }
}
