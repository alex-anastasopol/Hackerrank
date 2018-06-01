package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.httpclient.protocol.Protocol;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.utils.StringUtils;


public class MOClayTR extends HttpSite 
{
	private String sid;
	private String ascendwebSessionCode = null;
	
	private static final String[] ADD_PARAM_NAMES = { "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT"};
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE"};
    
    @SuppressWarnings("deprecation")
	public LoginResponse onLogin(){
        try{
        	
 	        Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
 	        Protocol.registerProtocol("https", easyhttps);
 	        
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
	        
	        //isolate parameters
			Map<String,String> addParams = isolateParams(response, "Form1");
			
			// check parameters
			if (!checkParams(addParams)){
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
			}
			setAttribute("params", addParams);
        }
        catch( Exception e ){
        	e.printStackTrace();
        	sid = null;
        	ascendwebSessionCode = null;
        }
        
        return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
    }
    
    @SuppressWarnings("unchecked")
	public void onBeforeRequestExcl(HTTPRequest req){

        if(sid == null || ascendwebSessionCode == null){
            onLogin();
        }
        
        Map<String,String> addParams = (HashMap<String,String>)getAttribute("params");
        
        String currentRequest = req.getURL();
        currentRequest = currentRequest.replace("ascend/", "ascend/" + sid + "/");
        if( currentRequest.startsWith("http:") ){
        	currentRequest = currentRequest.replace( "http:" , "https:");
        }
        
        if( currentRequest.indexOf("/result.aspx") >= 0 ){
        	req.setHeader( "Referer" , "https://collector.claycogov.com/ascend/" + sid + "/search.aspx" );
        	
        	Map<String,String> paramsDetails = (Map<String, String>)getTransientSearchAttribute("paramsDetails:");
        	req.removePostParameters( "__VIEWSTATE" );
        	if (addParams != null){
        		req.setPostParameter("__VIEWSTATE", paramsDetails.get("__VIEWSTATE"));
        	} else {
        		req.setPostParameter( "__VIEWSTATE" , "dDwtMTE4ODY1Mjc2MTt0PDtsPGk8MD47PjtsPHQ8O2w8aTwxPjtpPDM+Oz47bDx0PDtsPGk8MT47aTwzPjs+O2w8dDxwPGw8aHJlZjs+O2w8aHR0cDovL3d3dy5nb29nbGUuY29tOz4+Ozs+O3Q8dDw7O2w8aTwwPjs+Pjs7Pjs+Pjt0PDtsPGk8MT47aTwzPjtpPDU+Oz47bDx0PHA8cDxsPFRleHQ7PjtsPFJlc3VsdHMgTWVzc2FnZTo7Pj47Pjs7Pjt0PHA8cDxsPFRleHQ7PjtsPDUgcmVjb3JkcyByZXR1cm5lZCBmcm9tIHlvdXIgc2VhcmNoIGlucHV0Ljs+Pjs+Ozs+O3Q8O2w8aTwwPjs+O2w8dDxAMDxwPHA8bDxEYXRhS2V5cztfIUl0ZW1Db3VudDtQYWdlQ291bnQ7XyFEYXRhU291cmNlSXRlbUNvdW50Oz47bDxsPD47aTw1PjtpPDE+O2k8NT47Pj47PjtAMDxAMDxwPGw8SGVhZGVyVGV4dDs+O2w8UGFyY2VsIE51bWJlcjs+Pjs7Ozs+O0AwPHA8bDxIZWFkZXJUZXh0Oz47bDxOYW1lOz4+Ozs7Oz47QDA8cDxsPEhlYWRlclRleHQ7PjtsPExvY2F0aW9uIEFkZHJlc3M7Pj47Ozs7Pjs+Ozs7Ozs7Ozs7PjtsPGk8MD47PjtsPHQ8O2w8aTwxPjtpPDI+O2k8Mz47aTw0PjtpPDU+Oz47bDx0PDtsPGk8MD47aTwxPjtpPDI+Oz47bDx0PDtsPGk8MD47PjtsPHQ8cDxwPGw8VGV4dDtOYXZpZ2F0ZVVybDs+O2w8MDAwOTkwMDk5Mjg4MTk7amF2YXNjcmlwdDpfX2RvUG9zdEJhY2soJ21SZXN1bHRzY29udHJvbDptR3JpZCcsJ3BhcmNlbF9udW1iZXI9MDAwOTkwMDk5Mjg4MTknKTs+Pjs+Ozs+Oz4+O3Q8cDxwPGw8VGV4dDs+O2w8S09FUkJFUiwgTkFOQ1kgSjs+Pjs+Ozs+O3Q8cDxwPGw8VGV4dDs+O2w8UEVSU09OQUwgUFJPUEVSVFkgU0lUVVMsIEdsYWRzdG9uZSwgTU8gOz4+Oz47Oz47Pj47dDw7bDxpPDA+O2k8MT47aTwyPjs+O2w8dDw7bDxpPDA+Oz47bDx0PHA8cDxsPFRleHQ7TmF2aWdhdGVVcmw7PjtsPDAwMDk5MDA5OTUwNTkwO2phdmFzY3JpcHQ6X19kb1Bvc3RCYWNrKCdtUmVzdWx0c2NvbnRyb2w6bUdyaWQnLCdwYXJjZWxfbnVtYmVyPTAwMDk5MDA5OTUwNTkwJyk7Pj47Pjs7Pjs+Pjt0PHA8cDxsPFRleHQ7PjtsPEtPRVJCRVIsIE1BUkdBUkVUIExPVUlTRTs+Pjs+Ozs+O3Q8cDxwPGw8VGV4dDs+O2w8UEVSU09OQUwgUFJPUEVSVFkgU0lUVVMsIEthbnNhcyBDaXR5LCBNTyA7Pj47Pjs7Pjs+Pjt0PDtsPGk8MD47aTwxPjtpPDI+Oz47bDx0PDtsPGk8MD47PjtsPHQ8cDxwPGw8VGV4dDtOYXZpZ2F0ZVVybDs+O2w8MTMzMTcwMDA5MDA0MDA7amF2YXNjcmlwdDpfX2RvUG9zdEJhY2soJ21SZXN1bHRzY29udHJvbDptR3JpZCcsJ3BhcmNlbF9udW1iZXI9MTMzMTcwMDA5MDA0MDAnKTs+Pjs+Ozs+Oz4+O3Q8cDxwPGw8VGV4dDs+O2w8S09FUkJFUiwgRlJBTksgTTs+Pjs+Ozs+O3Q8cDxwPGw8VGV4dDs+O2w8MDAwMTAxIE5XIDgyTkQgU1QsIEthbnNhcyBDaXR5LCBNTyA7Pj47Pjs7Pjs+Pjt0PDtsPGk8MD47aTwxPjtpPDI+Oz47bDx0PDtsPGk8MD47PjtsPHQ8cDxwPGw8VGV4dDtOYXZpZ2F0ZVVybDs+O2w8MTMzMTcwMDA5MDI0MDA7amF2YXNjcmlwdDpfX2RvUG9zdEJhY2soJ21SZXN1bHRzY29udHJvbDptR3JpZCcsJ3BhcmNlbF9udW1iZXI9MTMzMTcwMDA5MDI0MDAnKTs+Pjs+Ozs+Oz4+O3Q8cDxwPGw8VGV4dDs+O2w8S09FUkJFUiwgTUFSR0FSRVQgTCAgVFJVU1Q7Pj47Pjs7Pjt0PHA8cDxsPFRleHQ7PjtsPDAwMDEwNCBOVyA4MVNUIFNULCBLYW5zYXMgQ2l0eSwgTU8gOz4+Oz47Oz47Pj47dDw7bDxpPDA+O2k8MT47aTwyPjs+O2w8dDw7bDxpPDA+Oz47bDx0PHA8cDxsPFRleHQ7TmF2aWdhdGVVcmw7PjtsPDEzNjE0MDAwOTAwNzAwO2phdmFzY3JpcHQ6X19kb1Bvc3RCYWNrKCdtUmVzdWx0c2NvbnRyb2w6bUdyaWQnLCdwYXJjZWxfbnVtYmVyPTEzNjE0MDAwOTAwNzAwJyk7Pj47Pjs7Pjs+Pjt0PHA8cDxsPFRleHQ7PjtsPEtPRVJCRVIsIE5BTkNZIEo7Pj47Pjs7Pjt0PHA8cDxsPFRleHQ7PjtsPDAwNzAyNCBOIENIRVJSWSBTVCwgR2xhZHN0b25lLCBNTyA7Pj47Pjs7Pjs+Pjs+Pjs+Pjs+Pjs+Pjs+Pjs+Pjs+8YAxFhW/2nHBVDVhkhy5X83d7G4=");
        	}
        } else if ( currentRequest.indexOf("/parcelinfo.aspx") >= 0 ) {
        	req.setMethod(HTTPRequest.POST);
        	req.setHeader( "Referer" , "https://ascendweb.jacksongov.org/ascend/" + sid + "/result.aspx" );
        	
        	Map<String,String> paramsReceipts = (Map<String, String>)getTransientSearchAttribute("paramsReceipts:");
        	for (String par: paramsReceipts.keySet()) {
        		req.setPostParameter(par, paramsReceipts.get(par));
        	}
        } else {
	        if (addParams != null){
		        req.removePostParameters( "__VIEWSTATE" );
		    	req.setPostParameter("__VIEWSTATE", addParams.get("__VIEWSTATE"));
	        }
        }
        
        String sessionCode = req.getPostFirstParameter( "ASCENDWEB_SESSION_CODE" );
        if(sessionCode != null){
        	req.removePostParameters( "ASCENDWEB_SESSION_CODE" );
        }
        
        String dummyReq = req.getPostFirstParameter( "dummy" );
        if(dummyReq != null){
        	req.removePostParameters( "dummy" );
        }
        
        // Remove white spaces.
        String[] params = new String[] {"mSearchControl:mLastName", "mSearchControl:mFirstName", 
        		"mSearchControl:mMiddleName", "mSearchControl:mParcelID"};
        for (String param : params) {
        	String value = req.getPostFirstParameter( param );
        	 if (value !=null && StringUtils.isNotEmpty(value)){
        		 req.removePostParameters( param );
        		 req.setPostParameter( param , value.replaceAll("\\s+", "") );
             }
        }
        
        logger.info( "VIEWSTATE = " + req.getPostFirstParameter( "__VIEWSTATE" ) );
        
        req.setPostParameter( "ASCENDWEB_SESSION_CODE" , ascendwebSessionCode);
        
        req.setURL(currentRequest);
    }
    
    public static Map<String, String> isolateParams(String page, String form){
		Map<String, String> params = new SimpleHtmlParser(page).getForm(form).getParams();
		Map<String,String> addParams = new HashMap<String,String>();
		for (String key : ADD_PARAM_NAMES) {
			String value = "";
			if (params.containsKey(key)) {
				value = params.get(key);
			}
			addParams.put(key, value);
		}
		
		return addParams;
	}
	
	public static HTTPRequest addParams(Map<String, String> addParams, HTTPRequest request){
		Iterator<String> i = addParams.keySet().iterator();
		while(i.hasNext()){
			String k = i.next();
			request.setPostParameter(k, addParams.get(k));
		}		
		return request;
	}
	
	public static boolean checkParams(Map<String, String> addParams){
		for(String key: REQ_PARAM_NAMES){
			if(StringUtils.isEmpty(addParams.get(key))){
				return false;
			}
		}
		return true;
	}
}
