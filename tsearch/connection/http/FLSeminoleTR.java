package ro.cst.tsearch.connection.http;

import java.net.URLDecoder;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.utils.InstanceManager;

public class FLSeminoleTR extends HTTPSite {
    private boolean loggingIn = false;
	
	public void setMiServerId(long miServerId){
		super.setMiServerId(miServerId);
	}
	
	public LoginResponse onLogin( ) 
	{
        loggingIn = true;
        
		setUserData(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "FLSeminoleTR");
	
		//HTTPRequest req = new HTTPRequest( "http://www.seminoletax.org/dev/NameSearch.asp" );
		HTTPRequest req = new HTTPRequest( "http://www.seminoletax.org" );
        req.setMethod( HTTPRequest.GET );
        HTTPResponse res = process( req );
        
        loggingIn = false;

        return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

	public void onBeforeRequest(HTTPRequest req) {
		setUserData(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "FLSeminoleTR");
        if( loggingIn )
        {
            return;
        }
		
		req.noRedirects=true;
		String url = req.getURL();
		req.setHeader("Host", "http://www.seminoletax.org");
/*		if (url.contains("linkNext"))
		{
			loggingIn = true;
			
			url = url.substring(0, url.indexOf("&linkNext"));
			req.removePostParameters("linkNext");
			req.modifyURL(url);

			req.setHeader("Referer", "http://www.seminoletax.org/dev/NameSearchs.asp");

			loggingIn = false;
		}
*/
//		req.setHeader("Host", "www.seminoletax.org");

		 if (url.contains("CurrentPage") && url.contains("&refNext"))
		{
			loggingIn = true;
			
			url = url.substring(0,url.indexOf("&refNext="));
			url = url.replace("?Results", "&Results");
			String refererStr = url.substring(0, url.indexOf("CurrentPage"));
			String nrPage = url.substring(url.indexOf("CurrentPage=")+"CurrentPage=".length());
			refererStr += "CurrentPage="+(Integer.parseInt(nrPage)-1);
			
			req.modifyURL(url);
			req.setHeader("Referer", refererStr);
			
	        loggingIn = false;
		}
		else if (url.contains("CurrentPage") && url.contains("&refPrev"))
		{
			loggingIn = true;
			
			url = url.substring(0,url.indexOf("&refPrev="));
			url = url.replace("?Results", "&Results");
			String refererStr = url.substring(0, url.indexOf("CurrentPage"));
			String nrPage = url.substring(url.indexOf("CurrentPage=")+"CurrentPage=".length());
			refererStr += "CurrentPage="+(Integer.parseInt(nrPage)+1);
			
			req.modifyURL(url);
			req.setHeader("Referer", refererStr);
			
	        loggingIn = false;
		}
		
		else if (url.contains ("namelist.asp") && url.contains("DataSearch"))
		{// Search by Owner Name

			loggingIn = true;
			req.setHeader("Referer", "http://www.seminoletax.org/dev/NameSearch.asp");
	        loggingIn = false;
		}
		else
			if (url.contains("RpPropSearch.asp") && url.contains("submit1"))
			{//search by Parcel ID
			
				loggingIn = true;
				req.setHeader("Referer", "http://www.seminoletax.org/dev/Prop.asp");
				loggingIn = false;
			}
			else
				if (url.contains("addresslist.asp"))
				{
					loggingIn = true;
					req.setHeader("Referer", "http://www.seminoletax.org/dev/AddressSearch.asp");
					loggingIn = false;
					
				}
		
				else if (url.contains("TBSearch.asp"))
				{ //Search by Tax Bill Number
					loggingIn = true;
					req.setHeader("Referer", "http://www.seminoletax.org/dev/TaxBillPay.asp");
					loggingIn =  false;
				}
		
		
				else if (url.contains("result.asp") && url.contains("txtAccountID"))
				{// Search by Owner Name link Details, Search by Instrument No				
							loggingIn = true;
							req.setHeader("Referer", "http://www.seminoletax.org/dev/result.asp");
							req.noRedirects = false;
							loggingIn = false;
				}
		
				/*	else if (url.contains("search4")&& url.contains("submit1"))
					{
							loggingIn = true;
							req.setHeader("Referer", "http://www.seminoletax.org/dev/addresslist.asp");
							loggingIn = false;
					}*/
		
				
	
		req.toString();
	}
}

