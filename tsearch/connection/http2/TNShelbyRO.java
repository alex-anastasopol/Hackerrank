package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.servers.bean.DataSite;

public class TNShelbyRO extends HttpSite{


	public LoginResponse onLogin() {
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}


	public void onBeforeRequest(HTTPRequest req) {
		req.noRedirects=true;
		String link = req.getURL();
		
		link = link.replaceAll("\\+\\&", "&");
		req.modifyURL(link);
	
        if (link.indexOf("imgView") != -1) 
        {
        	DataSite dataSite = getDataSite(); 
            req.setHeader("Referer", dataSite.getServerHomeLink() + "/p5.php");
        }
        else if ( link.indexOf("p4.php") > -1)
        {
        	DataSite dataSite = getDataSite(); 
        	req.setHeader("Referer", dataSite.getServerHomeLink() + "/p3.php");
        }
		
	}
	
	

}
