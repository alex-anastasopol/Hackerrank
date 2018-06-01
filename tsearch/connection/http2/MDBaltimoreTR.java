package ro.cst.tsearch.connection.http2;

import java.net.URLDecoder;
import java.util.Map;

import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;
import ro.cst.tsearch.servers.bean.DataSite;


public class MDBaltimoreTR extends HttpSite {

	private String sid;
	private boolean billDetails = false;
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
        String currentRequest = req.getURL();  
        
        if( currentRequest.startsWith("http:") ){
        	currentRequest = currentRequest.replace("http:" , "https:");
        }
        
        // Referer = https://egov2.baltimorecountymd.gov/obftax/(S(oieudc55safh4drl52l4xgv1))/Default.aspx
        String url = "";
        String urlRef = "";
        
        
        if (!currentRequest.contains("default.aspx")) {
        	if (!currentRequest.contains(sid)) {
        		if (!currentRequest.contains("?")) {
        			currentRequest = currentRequest.replaceFirst("(/obftax/)(Default.aspx)", "$1/$2");
        			url = currentRequest.replaceFirst("(/obftax/).*(/Default.aspx.*)", "$1" + sid + "$2");
        			req.setMethod(HTTPRequest.POST);
               	 	
        		} else {
        			url = currentRequest.substring(0, currentRequest.indexOf("Default.aspx")) + sid + "/" + currentRequest.substring(currentRequest.indexOf("Default.aspx"));
               	 	req.setMethod(HTTPRequest.GET);
               	 	billDetails = true;  
               	 	urlRef = url.substring(0, url.indexOf("?"));
        		}
        	} else {
        		 url = currentRequest;
            	 req.setMethod(HTTPRequest.POST);
        	}
        } else {
        	url = currentRequest.substring(0, currentRequest.indexOf("/default.aspx")) 
        			+ "/obftax/" + sid 
        			+ currentRequest.substring(currentRequest.indexOf("/default.aspx"));

        	req.setMethod(HTTPRequest.GET);
        }

        if (!billDetails) {
        	req.setHeader("Referer",url);
        	
   	 	} else {
        	req.setHeader("Referer",urlRef);
        }
                      
        req.setURL(url);
            
        String paramType = req.getPostFirstParameter("SearchType");
        
        if (paramType != null) {
        	if (paramType.equals("Address")){
        		String strNo = req.getPostFirstParameter("SearchStreetNumber");
        		String strName = req.getPostFirstParameter("SearchStreetName");
        		req.removePostParameters("ParcelAddress");
        		if (!strNo.equals("")) {
        			req.setPostParameter("ParcelAddress", strNo +" "+ strName);
        		} else {
        			req.setPostParameter("ParcelAddress", strName);
        		}
        	}
        	
        	if (paramType.equals("ParcelID")){
        		String PIDvalue = req.getPostFirstParameter("SearchParcel");
            	req.removePostParameters("ParcelID");
            	req.setPostParameter("ParcelID", PIDvalue);
        	}        	  	
        } 
	}

	
	@Override
	public LoginResponse onLogin() {
		DataSite dataSite = getDataSite();
				
        try{	        
			@SuppressWarnings("deprecation")
			Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
	        Protocol.registerProtocol("https", easyhttps);	        
	        
	        HTTPRequest req = new HTTPRequest(dataSite.getLink(), HTTPRequest.GET);	   
	        String response = execute(req);
	        
	        if(response.contains("Real Property Tax Search") && (StringUtils.isNotEmpty(req.getRedirectLocation()))){
	        	if (!response.contains("Real Property Tax Search Results:")) {
	        		sid = ro.cst.tsearch.utils.StringUtils.getTextBetweenDelimiters("/obftax/", "/Default.aspx", URLDecoder.decode(req.getRedirectLocation(), "UTF-8"));
	        	}
	        	
	        } else {
	        	if (sid == null) {
	        		return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Official site not functional");
	        	}
	        	
	        	if (response.contains("An unexpected error has occurred:")) {
	        		return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Official site not functional");
	        		
	        	} else {
	        		return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Page not found");
	        	}	        	 
	        }
        }
        catch(Exception e){
        	e.printStackTrace();
        	logger.error("Problem parsing form on MDBaltimoreTR:", e);
        	sid = null;
        }       
        
        return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	

	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		Map<String,String> params = ro.cst.tsearch.utils.StringUtils.extractParametersFromUrl(req.getURL());
		if (params.size() > 0) {
			if (params.get("Action").equals("BillDetails") && params.get("BillID").equals("4")) {
				destroySession();
				billDetails = false;
			}
		
		} 
//		else {
//			if (req.getPostParameters().size() > 0  && req.getPostFirstParameter("Action").equals("MainMenu")) {
//				if (!billDetails) {
//					destroySession();
//				}
//			}
//			
//		}		
	}

}
