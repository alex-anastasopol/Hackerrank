package ro.cst.tsearch.connection.http3;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.io.IOUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;
import ro.cst.tsearch.servers.bean.DataSite;

public class MOStLouisCityTR extends HttpSite3 {
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
        req.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:26.0) Gecko/20100101 Firefox/26.0");
        req.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        req.setHeader("Accept-Language", getAcceptLanguage());
        req.setHeader("Accept-Encoding", "gzip, deflate");
        
        if (req.postParameters.containsKey("findByAddress") && req.postParameters.get("findByAddress").toString().equals("Find address")) {
        	req.setHeader("AOPortal_stlouis-mo.gov", "true");
        }
	}

	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) { 
//		System.out.println("Req = " + req.getURL() + "  =>  RESP: " + res.getContentType());
//		System.out.println("Req = " + req.getURL() + "  =>  RESP: " + res.getHeaders().toString());
		if ("gzip".equals(res.getHeader("Content-Encoding"))) {
			try {
				String respAsString = "";
				try {
					respAsString = res.getGZipResponseAsString();
				} catch (IOException e) {
					e.printStackTrace();
				}
				res.is = new ByteArrayInputStream(respAsString.getBytes());
				req.setBypassResponse(res);
			} catch (Exception e) {
				System.err.println(e);
			}
		}
	}
	
	@Override
	public String getAcceptLanguage() {
		return "en-US,en;q=0.5";
	}
	
	@Override
	public LoginResponse onLogin() {
		DataSite dataSite = getDataSite();
				
        try{	        
			@SuppressWarnings("deprecation")
			Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
	        Protocol.registerProtocol("https", easyhttps);	        
	        
	        HTTPRequest req = new HTTPRequest(dataSite.getLink(), HTTPRequest.GET);
	        req.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:26.0) Gecko/20100101 Firefox/26.0");
	        String response = execute(req);
	        
	        if ((response.contains("500 - Internal server error.") && 
	        		response.contains("There is a problem with the resource you are looking for, and it cannot be displayed.")) 
	    	     || response.contains("The Main City Website is Down")
	    	     || response.contains("The web site you are accessing has experienced an unexpected error.")) {
	        	return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Official site not functional");        	
	        }
        }
        catch(Exception e){
        	e.printStackTrace();
        	logger.error("Problem parsing form on MO St Louis City TR:", e);
        }       
        
        setAttribute(HttpSite3.SINGLE_LINE_COOKIES_IN_REDIRECT, Boolean.TRUE);
        
        return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
}
