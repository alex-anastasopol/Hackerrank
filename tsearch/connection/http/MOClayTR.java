package ro.cst.tsearch.connection.http;

import java.util.HashMap;

import ro.cst.tsearch.connection.ATSConn;
import ro.cst.tsearch.connection.ATSConnConstants;
import ro.cst.tsearch.connection.CookieManager;
import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;


public class MOClayTR extends HTTPSite 
{
	private String sid;
	private String ascendwebSessionCode = null;
    private boolean loggingIn = false;
    
    public LoginResponse onLogin() 
    {
    	setUserData( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "MOClayTR" );
        loggingIn = true;
        try{
	        HashMap<String, String> reqHeaders = new HashMap<String, String>();
	        reqHeaders.put( "Referer" , "https://collector.claycogov.com/ascend/search.aspx");
	        
	        HTTPRequest req = new HTTPRequest( "https://collector.claycogov.com/ascend/search.aspx" );
	        req.setHeaders( reqHeaders );
	        req.noRedirects = true;
	        HTTPResponse res = process(req);
	        
	        String response = res.getResponseAsString();
	        
	        if( res.getReturnCode() / 100 == 3 ){
	        	//
	        	sid = StringUtils.getTextBetweenDelimiters("/ascend/", "/search.aspx", response);
	        	
	        	req = new HTTPRequest( "https://collector.claycogov.com/ascend/" + sid + "/search.aspx" );
	        	res = process(req);
	        	response = res.getResponseAsString();
	        	
	        	ascendwebSessionCode = StringUtils.getTextBetweenDelimiters( "name=\"ASCENDWEB_SESSION_CODE\" value=\"" , "\"", response);
	        }
        }
        catch( Exception e ){
        	e.printStackTrace();
        	sid = null;
        	ascendwebSessionCode = null;
        }
        loggingIn = false;
        
        return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
    }
    
    public void onBeforeRequest(HTTPRequest req) 
    {
    	setUserData( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "MOClayTR" );
        if( loggingIn )
        {
            return;
        }
               
        if( sid == null || ascendwebSessionCode == null )
        {
            onLogin();
        }
        
        String currentRequest = req.getURL();
        currentRequest = currentRequest.replace("ascend/", "ascend/" + sid + "/");
        if( currentRequest.startsWith("http:") ){
        	currentRequest = currentRequest.replace( "http:" , "https:");
        }
        
        if( currentRequest.indexOf("/result.aspx") >= 0 ){
        	req.setHeader( "Referer" , "https://collector.claycogov.com/ascend/" + sid + "/search.aspx" );
        	
           	req.removePostParameters( "__VIEWSTATE" );
           	req.setPostParameter( "__VIEWSTATE" , "dDwtMTE4ODY1Mjc2MTt0PDtsPGk8MD47PjtsPHQ8O2w8aTwxPjtpPDM+Oz47bDx0PDtsPGk8MT47aTwzPjs+O2w8dDxwPGw8aHJlZjs+O2w8aHR0cDovL3d3dy5nb29nbGUuY29tOz4+Ozs+O3Q8dDw7O2w8aTwwPjs+Pjs7Pjs+Pjt0PDtsPGk8MT47aTwzPjtpPDU+Oz47bDx0PHA8cDxsPFRleHQ7PjtsPFJlc3VsdHMgTWVzc2FnZTo7Pj47Pjs7Pjt0PHA8cDxsPFRleHQ7PjtsPDUgcmVjb3JkcyByZXR1cm5lZCBmcm9tIHlvdXIgc2VhcmNoIGlucHV0Ljs+Pjs+Ozs+O3Q8O2w8aTwwPjs+O2w8dDxAMDxwPHA8bDxEYXRhS2V5cztfIUl0ZW1Db3VudDtQYWdlQ291bnQ7XyFEYXRhU291cmNlSXRlbUNvdW50Oz47bDxsPD47aTw1PjtpPDE+O2k8NT47Pj47PjtAMDxAMDxwPGw8SGVhZGVyVGV4dDs+O2w8UGFyY2VsIE51bWJlcjs+Pjs7Ozs+O0AwPHA8bDxIZWFkZXJUZXh0Oz47bDxOYW1lOz4+Ozs7Oz47QDA8cDxsPEhlYWRlclRleHQ7PjtsPExvY2F0aW9uIEFkZHJlc3M7Pj47Ozs7Pjs+Ozs7Ozs7Ozs7PjtsPGk8MD47PjtsPHQ8O2w8aTwxPjtpPDI+O2k8Mz47aTw0PjtpPDU+Oz47bDx0PDtsPGk8MD47aTwxPjtpPDI+Oz47bDx0PDtsPGk8MD47PjtsPHQ8cDxwPGw8VGV4dDtOYXZpZ2F0ZVVybDs+O2w8MDAwOTkwMDk5Mjg4MTk7amF2YXNjcmlwdDpfX2RvUG9zdEJhY2soJ21SZXN1bHRzY29udHJvbDptR3JpZCcsJ3BhcmNlbF9udW1iZXI9MDAwOTkwMDk5Mjg4MTknKTs+Pjs+Ozs+Oz4+O3Q8cDxwPGw8VGV4dDs+O2w8S09FUkJFUiwgTkFOQ1kgSjs+Pjs+Ozs+O3Q8cDxwPGw8VGV4dDs+O2w8UEVSU09OQUwgUFJPUEVSVFkgU0lUVVMsIEdsYWRzdG9uZSwgTU8gOz4+Oz47Oz47Pj47dDw7bDxpPDA+O2k8MT47aTwyPjs+O2w8dDw7bDxpPDA+Oz47bDx0PHA8cDxsPFRleHQ7TmF2aWdhdGVVcmw7PjtsPDAwMDk5MDA5OTUwNTkwO2phdmFzY3JpcHQ6X19kb1Bvc3RCYWNrKCdtUmVzdWx0c2NvbnRyb2w6bUdyaWQnLCdwYXJjZWxfbnVtYmVyPTAwMDk5MDA5OTUwNTkwJyk7Pj47Pjs7Pjs+Pjt0PHA8cDxsPFRleHQ7PjtsPEtPRVJCRVIsIE1BUkdBUkVUIExPVUlTRTs+Pjs+Ozs+O3Q8cDxwPGw8VGV4dDs+O2w8UEVSU09OQUwgUFJPUEVSVFkgU0lUVVMsIEthbnNhcyBDaXR5LCBNTyA7Pj47Pjs7Pjs+Pjt0PDtsPGk8MD47aTwxPjtpPDI+Oz47bDx0PDtsPGk8MD47PjtsPHQ8cDxwPGw8VGV4dDtOYXZpZ2F0ZVVybDs+O2w8MTMzMTcwMDA5MDA0MDA7amF2YXNjcmlwdDpfX2RvUG9zdEJhY2soJ21SZXN1bHRzY29udHJvbDptR3JpZCcsJ3BhcmNlbF9udW1iZXI9MTMzMTcwMDA5MDA0MDAnKTs+Pjs+Ozs+Oz4+O3Q8cDxwPGw8VGV4dDs+O2w8S09FUkJFUiwgRlJBTksgTTs+Pjs+Ozs+O3Q8cDxwPGw8VGV4dDs+O2w8MDAwMTAxIE5XIDgyTkQgU1QsIEthbnNhcyBDaXR5LCBNTyA7Pj47Pjs7Pjs+Pjt0PDtsPGk8MD47aTwxPjtpPDI+Oz47bDx0PDtsPGk8MD47PjtsPHQ8cDxwPGw8VGV4dDtOYXZpZ2F0ZVVybDs+O2w8MTMzMTcwMDA5MDI0MDA7amF2YXNjcmlwdDpfX2RvUG9zdEJhY2soJ21SZXN1bHRzY29udHJvbDptR3JpZCcsJ3BhcmNlbF9udW1iZXI9MTMzMTcwMDA5MDI0MDAnKTs+Pjs+Ozs+Oz4+O3Q8cDxwPGw8VGV4dDs+O2w8S09FUkJFUiwgTUFSR0FSRVQgTCAgVFJVU1Q7Pj47Pjs7Pjt0PHA8cDxsPFRleHQ7PjtsPDAwMDEwNCBOVyA4MVNUIFNULCBLYW5zYXMgQ2l0eSwgTU8gOz4+Oz47Oz47Pj47dDw7bDxpPDA+O2k8MT47aTwyPjs+O2w8dDw7bDxpPDA+Oz47bDx0PHA8cDxsPFRleHQ7TmF2aWdhdGVVcmw7PjtsPDEzNjE0MDAwOTAwNzAwO2phdmFzY3JpcHQ6X19kb1Bvc3RCYWNrKCdtUmVzdWx0c2NvbnRyb2w6bUdyaWQnLCdwYXJjZWxfbnVtYmVyPTEzNjE0MDAwOTAwNzAwJyk7Pj47Pjs7Pjs+Pjt0PHA8cDxsPFRleHQ7PjtsPEtPRVJCRVIsIE5BTkNZIEo7Pj47Pjs7Pjt0PHA8cDxsPFRleHQ7PjtsPDAwNzAyNCBOIENIRVJSWSBTVCwgR2xhZHN0b25lLCBNTyA7Pj47Pjs7Pjs+Pjs+Pjs+Pjs+Pjs+Pjs+Pjs+Pjs+8YAxFhW/2nHBVDVhkhy5X83d7G4=");
        }
        
        String sessionCode = req.getPostFirstParameter( "ASCENDWEB_SESSION_CODE" );
        if( sessionCode != null ){
        	req.removePostParameters( "ASCENDWEB_SESSION_CODE" );
        }
        
        String dummyReq = req.getPostFirstParameter( "dummy" );
        if( dummyReq != null )
        {
        	req.removePostParameters( "dummy" );
        }
        
        String nameReq = req.getPostFirstParameter( "mSearchControl:mName" );
        if (nameReq !=null && !"".equals(nameReq))
        {
        	req.removePostParameters( "mSearchControl:mName" );
        	nameReq = nameReq.replaceAll("\\s+", "");
            req.setPostParameter( "mSearchControl:mName" , nameReq);
        }
        String pidReq = req.getPostFirstParameter( "mSearchControl:mParcelID" );
        if (pidReq !=null && !"".equals(pidReq))
        {
        	req.removePostParameters( "mSearchControl:mParcelID" );
        	pidReq = pidReq.replaceAll("\\s+", "");
            req.setPostParameter( "mSearchControl:mParcelID" , pidReq);
        }
        
        logger.info( "VIEWSTATE = " + req.getPostFirstParameter( "__VIEWSTATE" ) );
        
        req.setPostParameter( "ASCENDWEB_SESSION_CODE" , ascendwebSessionCode);
        
        req.URL = currentRequest;
    }
}
