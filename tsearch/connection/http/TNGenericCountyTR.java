package ro.cst.tsearch.connection.http;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.utils.InstanceManager;


public class TNGenericCountyTR extends HTTPSite {
	private String TNcountyNameTR="TNFranklinTR";
    private String sid = "";
    private boolean loggingIn = false;
    
	protected void setCountyName (String name)
	{
		TNcountyNameTR = name;
	}
	
	public void setMiServerId(long miServerId){
		super.setMiServerId(miServerId);
		TNcountyNameTR = HashCountyToIndex.getDateSiteForMIServerID(getCurrentCommunityIdAsInt(),
				(int)miServerId).getName().replaceAll(" ","");
	}
	
	protected String getSpecificCntySrvName()
	{
		if (TNcountyNameTR.equalsIgnoreCase("TNTrousdaleTR"))
			return "TNWilsonTR";
		else if (TNcountyNameTR.equalsIgnoreCase("TNMooreTR"))
			return "TNCoffeeTR";
		else if (TNcountyNameTR.equalsIgnoreCase("TNHamblenTR"))
			return "TNGreeneTR";

		return TNcountyNameTR;
	}

	
	public LoginResponse onLogin( ) 
	{
        setUserData(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + getSpecificCntySrvName());
        
        if ("".equals(sid)) {
        
        	String cntyName = InstanceManager.getManager().getCurrentInstance(searchId)
        		.getCrtSearchContext().getSa().getCountyName().replaceAll("[\\s\\p{Punct}]+", "");
        	HTTPRequest req = new HTTPRequest( "http://www." + cntyName + ".tennesseetrustee.org/" );
        	req.setMethod(HTTPRequest.GET);
        	req.setHeader("Host", "www." + cntyName + ".tennesseetrustee.org");
	        
	        loggingIn = true;
	        
	        HTTPResponse res = process( req );
	        String response = res.getResponseAsString();
	        
	        if (response.matches("(?s).*PHPSESSID[^a-z]+value.*"))
	        	sid = "PHPSESSID=" + response.replaceAll("(?s).*PHPSESSID' value='(.*?)'.*", "$1");
	        else
	        	sid="PHPSESSID=";
	        
	        loggingIn = false;   
        }

        return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

	public void onBeforeRequest(HTTPRequest req) {
        if( loggingIn == true )
        {
            return;
        }
        
        String url = req.getURL();
        url = url.replaceFirst("(?is)[/][/]search.php", "/search.php");
        String cntySrvName = getSpecificCntySrvName();
        String cntyName = InstanceManager.getManager().getCurrentInstance(searchId)
			.getCrtSearchContext().getSa().getCountyName().replaceAll("[\\s\\p{Punct}]+", "");
        
        setUserData(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + cntySrvName);
        req.setHeader("Host", "www."+cntyName+".tennesseetrustee.org");
        
        if ( url.indexOf("search.php") != -1 && url.indexOf("owner=") != -1 && (!url.contains("&p=")&& !url.contains("&r=")))
        {// normal search (by Owner OR by Address OR by Subdivision etc.)
        	loggingIn = true;
        	
            if (cntyName.equalsIgnoreCase("Scott"))
            {
            	url = url+"&"+sid;
            	req.modifyURL(url);
            }
/*            {
            	req.setHeader("Referer", "http://www."+cntyName+".tennesseetrustee.org/");
            }
            else
            	req.setHeader("Referer", "http://www."+cntyName+".tennesseetrustee.org/");*/
            req.setHeader("Referer", "http://www."+cntyName+".tennesseetrustee.org/");

            HTTPRequest req1 = new HTTPRequest("http://www."+cntyName+".tennesseetrustee.org/setLastPage.php");
            req1.setMethod(HTTPRequest.GET);
            req1.setHeader("Referer", "http://www."+cntyName+".tennesseetrustee.org/");
            req1.setHeader("Host", "www."+cntyName+".tennesseetrustee.org");
            process(req1);
            
        	loggingIn = false;
        }
        else if ( url.indexOf("card.php") != -1 ) 
        {// link details
        	req.setMethod(HTTPRequest.POST);
        	
        	url = url.replaceAll("PHPSESSID=.*?\\&", "");
        	req.modifyURL(url);
        	
        	req.setHeader( "Referer", req.getURL().replaceAll("printCard", "card") );
        }
        else if (url.contains("&p=") && url.contains("&r="))
		{// link next
        	loggingIn = true;
        	
        	req.setMethod(HTTPRequest.POST);
        	
        	if (!cntyName.equalsIgnoreCase("Scott"))
        	{
		    	url = url.replaceAll("&?PHPSESSID=.*?(\\&|$)", "&");
		    	req.modifyURL(url);
        	}
        	
			int i_page = url.indexOf("&p=");
			i_page += "&p=".length();
			int end =-1;
			if ((end=url.indexOf("&", i_page))==-1)
				end = url.length();
			String str_page = url.substring(i_page,end);
			int i_row = url.indexOf("&r=");
			i_row += "&r=".length();
			end =-1;
			if ((end=url.indexOf("&", i_row))==-1)
				end = url.length();
			String str_row = url.substring(i_row,end);

	        req.setPostParameter( "p" , str_page);
	        req.setPostParameter( "r" , str_row);
	        req.setPostParameter( "shower" , "on");
//	        req.noRedirects = false;

            if (cntyName.equalsIgnoreCase("Scott"))
            {
            	req.modifyURL("http://www.scott.tennesseetrustee.org/search.php");
            	if (!url.toUpperCase().contains("PHPSESSID"))
            	{
            		url = url.replaceFirst("&p=[^&]*", "");
            		url = url.replaceFirst("&r=[^&]*", "");
            		req.setHeader("Referer", url + "&" + sid);
            		req.setHeader("Cookie", sid);
            	    req.setHeader("Keep-Alive", "300");
            	}
            }
	        
	        loggingIn = false;
		} 
	}

	 public void onRedirect(HTTPRequest req){
		  String location = req.getRedirectLocation();
		  if(location.contains("maintenance.php")){
		    destroySession();
		    sid = "";
		   throw new RuntimeException("Redirected to " + location + ". Session needs to be destroyed");
		  }
	 }
	
}


