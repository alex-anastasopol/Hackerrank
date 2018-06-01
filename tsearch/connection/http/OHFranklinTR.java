package ro.cst.tsearch.connection.http;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

public class OHFranklinTR extends HTTPSite{

	static String siteStartLink = "http://oh-franklin-treasurer.governmax.org" ;
	static String baseLink = "http://oh-franklin-treasurer.governmax.org/propertymax/";
	String sid = "";
	
	public LoginResponse onLogin() {
	   setUserData( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "OHFranklinTR" );
		
	   HTTPRequest req = new HTTPRequest( OHFranklinTR.siteStartLink );
       req.setMethod( HTTPRequest.GET );
       req.noRedirects = false;        
       HTTPResponse res = process( req );
       String strdata = res.getResponseAsString();         

       if(res.getReturnCode() == 200){        	
           if(strdata!=null){
           	
           	int stop=strdata.indexOf(" WIDTH=\"100%\"");
           	int start=strdata.indexOf("SRC=\"");

           	if(start<stop && start>0 && stop>0){
           		
           		strdata = strdata .substring(start+"SRC=\"".length(),stop-1);
           		
           		if (sid.equals("")){  //prima intrare aici
           			int poz=0;
           			if((poz=strdata.indexOf("sid="))>0 ){
           				sid=strdata.substring(poz+4);
           			}
           		}
           		else{
           			int poz=0;
           			if((poz=strdata.indexOf("sid="))>0 ){
           				strdata=strdata.substring(0,poz+5);
           			}
           			strdata+=sid;
           		}
           		
           		String link = OHFranklinTR.baseLink + strdata;
           		req = new HTTPRequest( link );
                   req.setMethod( HTTPRequest.GET );
                   req.noRedirects = false;
                   
                   res = process( req );
                   
                   strdata = res.getResponseAsString(); 
                   
                   if(res.getReturnCode() == 200){

                	   return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
                   }
           	}
           }
           
       }    
       return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
	}	
	
	public void onBeforeRequest(HTTPRequest req) {
		 setUserData( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "OHFranklinTR" );
		
		if(!sid.equals("")){ //nu sunt inca in login
			
			if(req.getMethod() == HTTPRequest.POST){
				req.setPostParameter("sid", sid);
			}
			else{
				//metoda GET
			}
		}
		
	}
	
}
