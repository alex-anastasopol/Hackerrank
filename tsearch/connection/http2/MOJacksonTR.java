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

public class MOJacksonTR extends HttpSite 
{
	
	private String sid;
	private String ascendwebSessionCode = null;
	
	private static final String[] ADD_PARAM_NAMES = { "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT"};
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE"};
	
    @SuppressWarnings("deprecation")
	public LoginResponse onLogin(){

        try{
	        HashMap<String, String> reqHeaders = new HashMap<String, String>();
	        Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
	        Protocol.registerProtocol("https", easyhttps);
	        
	        reqHeaders.put( "Referer" , "https://ascendweb.jacksongov.org/ascend/");
	        
	        HTTPRequest req = new HTTPRequest( "https://ascendweb.jacksongov.org/ascend/" );
	        req.setHeaders( reqHeaders );
	        req.noRedirects = true;
	        
	        
	        
	        HTTPResponse res = process(req);
	        
	        String response = res.getResponseAsString();
	        
	        if( res.getReturnCode() / 100 == 3 ){
	        	//
	        	sid = StringUtils.getTextBetweenDelimiters("/ascend/", "/Search.aspx", response);
	        	
	        	req = new HTTPRequest( "https://ascendweb.jacksongov.org/ascend/" + sid + "/Search.aspx" );
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
        	
        	return LoginResponse.getDefaultFailureResponse();
        }
        
        return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
    }
    
    @SuppressWarnings("unchecked")
	public void onBeforeRequestExcl(HTTPRequest req) 
    {   
    	Map<String,String> addParams = (HashMap<String,String>)getAttribute("params");
        String currentRequest = req.getURL();
        currentRequest = currentRequest.replace("ascend/", "ascend/" + sid + "/");
        if( currentRequest.startsWith("http:") ){
        	currentRequest = currentRequest.replace( "http:" , "https:");
        }
        
        //Referer=https://ascendweb.jacksongov.org/ascend/(gozhwi45shfuydypkw3kgk55)/search.aspx
        
        if( currentRequest.indexOf("/result.aspx") >= 0 ){
        	req.setHeader( "Referer" , "https://ascendweb.jacksongov.org/ascend/" + sid + "/search.aspx" );
        	
        	Map<String,String> paramsDetails = (Map<String, String>)getTransientSearchAttribute("paramsDetails:");
        	req.removePostParameters( "__VIEWSTATE" );
        	if (addParams != null){
        		req.setPostParameter("__VIEWSTATE", paramsDetails.get("__VIEWSTATE"));
        	} else {
        		req.setPostParameter( "__VIEWSTATE" , "dDwtMTE4ODY1Mjc2MTt0PDtsPGk8MD47PjtsPHQ8O2w8aTwxPjtpPDM+Oz47bDx0PDtsPGk8Mz47PjtsPHQ8dDw7O2w8aTwwPjs+Pjs7Pjs+Pjt0PDtsPGk8MT47aTwzPjtpPDU+Oz47bDx0PHA8cDxsPFRleHQ7PjtsPFJlc3VsdHMgTWVzc2FnZTo7Pj47Pjs7Pjt0PHA8cDxsPFRleHQ7PjtsPDggcmVjb3JkcyByZXR1cm5lZCBmcm9tIHlvdXIgc2VhcmNoIGlucHV0Ljs+Pjs+Ozs+O3Q8O2w8aTwwPjs+O2w8dDxAMDxwPHA8bDxEYXRhS2V5cztfIUl0ZW1Db3VudDtQYWdlQ291bnQ7XyFEYXRhU291cmNlSXRlbUNvdW50Oz47bDxsPD47aTw4PjtpPDE+O2k8OD47Pj47PjtAMDxAMDxwPGw8SGVhZGVyVGV4dDs+O2w8UGFyY2VsIE51bWJlcjs+Pjs7Ozs+O0AwPHA8bDxIZWFkZXJUZXh0Oz47bDxOYW1lOz4+Ozs7Oz47QDA8cDxsPEhlYWRlclRleHQ7PjtsPExvY2F0aW9uIEFkZHJlc3M7Pj47Ozs7Pjs+Ozs7Ozs7Ozs7PjtsPGk8MD47PjtsPHQ8O2w8aTwxPjtpPDI+O2k8Mz47aTw0PjtpPDU+O2k8Nj47aTw3PjtpPDg+Oz47bDx0PDtsPGk8MD47aTwxPjtpPDI+Oz47bDx0PDtsPGk8MD47PjtsPHQ8cDxwPGw8VGV4dDtOYXZpZ2F0ZVVybDs+O2w8MDc1MDM0MzIxO2phdmFzY3JpcHQ6X19kb1Bvc3RCYWNrKCdtUmVzdWx0c2NvbnRyb2w6bUdyaWQnLCdwYXJjZWxfbnVtYmVyPTA3NTAzNDMyMScpOz4+Oz47Oz47Pj47dDxwPHA8bDxUZXh0Oz47bDxKT0hOU09OIFdBTFRFUjs+Pjs+Ozs+O3Q8cDxwPGw8VGV4dDs+O2w8VU5LTk9XTiwgVU5LTk9XTiwgTU87Pj47Pjs7Pjs+Pjt0PDtsPGk8MD47aTwxPjtpPDI+Oz47bDx0PDtsPGk8MD47PjtsPHQ8cDxwPGw8VGV4dDtOYXZpZ2F0ZVVybDs+O2w8MDc1NzM1ODA0O2phdmFzY3JpcHQ6X19kb1Bvc3RCYWNrKCdtUmVzdWx0c2NvbnRyb2w6bUdyaWQnLCdwYXJjZWxfbnVtYmVyPTA3NTczNTgwNCcpOz4+Oz47Oz47Pj47dDxwPHA8bDxUZXh0Oz47bDxKT0hOU09OIFdBTFRFUjs+Pjs+Ozs+O3Q8cDxwPGw8VGV4dDs+O2w8NDE0MyBQUk9TUEVDVCBBVkUsIEtBTlNBUyBDSVRZLCBNTyA2NDEzMDs+Pjs+Ozs+Oz4+O3Q8O2w8aTwwPjtpPDE+O2k8Mj47PjtsPHQ8O2w8aTwwPjs+O2w8dDxwPHA8bDxUZXh0O05hdmlnYXRlVXJsOz47bDwwOTU4MzM3M0I7amF2YXNjcmlwdDpfX2RvUG9zdEJhY2soJ21SZXN1bHRzY29udHJvbDptR3JpZCcsJ3BhcmNlbF9udW1iZXI9MDk1ODMzNzNCJyk7Pj47Pjs7Pjs+Pjt0PHA8cDxsPFRleHQ7PjtsPEpPSE5TT04gV0FMVEVSOz4+Oz47Oz47dDxwPHA8bDxUZXh0Oz47bDw0NTA4IFZJUkdJTklBIEFWRSwgS0FOU0FTIENJVFksIE1POz4+Oz47Oz47Pj47dDw7bDxpPDA+O2k8MT47aTwyPjs+O2w8dDw7bDxpPDA+Oz47bDx0PHA8cDxsPFRleHQ7TmF2aWdhdGVVcmw7PjtsPDA5NzA0ODA3MjtqYXZhc2NyaXB0Ol9fZG9Qb3N0QmFjaygnbVJlc3VsdHNjb250cm9sOm1HcmlkJywncGFyY2VsX251bWJlcj0wOTcwNDgwNzInKTs+Pjs+Ozs+Oz4+O3Q8cDxwPGw8VGV4dDs+O2w8Sk9ITlNPTiBXQUxURVI7Pj47Pjs7Pjt0PHA8cDxsPFRleHQ7PjtsPDQ1MDggVklSR0lOSUEgQVZFLCBLQU5TQVMgQ0lUWSwgTU8gNjQxMTA7Pj47Pjs7Pjs+Pjt0PDtsPGk8MD47aTwxPjtpPDI+Oz47bDx0PDtsPGk8MD47PjtsPHQ8cDxwPGw8VGV4dDtOYXZpZ2F0ZVVybDs+O2w8MDk3MTI0MzgwO2phdmFzY3JpcHQ6X19kb1Bvc3RCYWNrKCdtUmVzdWx0c2NvbnRyb2w6bUdyaWQnLCdwYXJjZWxfbnVtYmVyPTA5NzEyNDM4MCcpOz4+Oz47Oz47Pj47dDxwPHA8bDxUZXh0Oz47bDxKT0hOU09OIFdBTFRFUjs+Pjs+Ozs+O3Q8cDxwPGw8VGV4dDs+O2w8Tk8gQUREUkVTUyBQUk9WSURFRCBCWSBUQVhQQVlFUiwgVU5LTk9XTiwgTU87Pj47Pjs7Pjs+Pjt0PDtsPGk8MD47aTwxPjtpPDI+Oz47bDx0PDtsPGk8MD47PjtsPHQ8cDxwPGw8VGV4dDtOYXZpZ2F0ZVVybDs+O2w8MTAwOTE1MjM5O2phdmFzY3JpcHQ6X19kb1Bvc3RCYWNrKCdtUmVzdWx0c2NvbnRyb2w6bUdyaWQnLCdwYXJjZWxfbnVtYmVyPTEwMDkxNTIzOScpOz4+Oz47Oz47Pj47dDxwPHA8bDxUZXh0Oz47bDxKT0hOU09OIFdBTFRFUjs+Pjs+Ozs+O3Q8cDxwPGw8VGV4dDs+O2w8NDE0MyBQUk9TUEVDVCBBVkUsIEtBTlNBUyBDSVRZLCBNTyA2NDEzMDs+Pjs+Ozs+Oz4+O3Q8O2w8aTwwPjtpPDE+O2k8Mj47PjtsPHQ8O2w8aTwwPjs+O2w8dDxwPHA8bDxUZXh0O05hdmlnYXRlVXJsOz47bDwyMDExNDk1NTE7amF2YXNjcmlwdDpfX2RvUG9zdEJhY2soJ21SZXN1bHRzY29udHJvbDptR3JpZCcsJ3BhcmNlbF9udW1iZXI9MjAxMTQ5NTUxJyk7Pj47Pjs7Pjs+Pjt0PHA8cDxsPFRleHQ7PjtsPEpPSE5TT04gV0FMVEVSOz4+Oz47Oz47dDxwPHA8bDxUZXh0Oz47bDxOTyBBRERSRVNTIFBST1ZJREVEIEJZIFRBWFBBWUVSLCBVTktOT1dOLCBNTzs+Pjs+Ozs+Oz4+O3Q8O2w8aTwwPjtpPDE+O2k8Mj47PjtsPHQ8O2w8aTwwPjs+O2w8dDxwPHA8bDxUZXh0O05hdmlnYXRlVXJsOz47bDw0My02MDAtMDMtODQtMDAtMC0wMC0wMDA7amF2YXNjcmlwdDpfX2RvUG9zdEJhY2soJ21SZXN1bHRzY29udHJvbDptR3JpZCcsJ3BhcmNlbF9udW1iZXI9NDMtNjAwLTAzLTg0LTAwLTAtMDAtMDAwJyk7Pj47Pjs7Pjs+Pjt0PHA8cDxsPFRleHQ7PjtsPEpPSE5TT04gV0FMVEVSOz4+Oz47Oz47dDxwPHA8bDxUZXh0Oz47bDw0MTA5IE5FIFNVV0FOTkVFIERSLCBMRUVTIFNVTU1JVCwgTU8gNjQwNjQ7Pj47Pjs7Pjs+Pjs+Pjs+Pjs+Pjs+Pjs+Pjs+Pjs+Xpe2FScsxG+TOdJLPwZkIjN+A5Y=");
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
        if( sessionCode != null ){
        	req.removePostParameters( "ASCENDWEB_SESSION_CODE" );
        }
        
        String dummyReq = req.getPostFirstParameter( "dummy" );
        if( dummyReq != null ){
        	req.removePostParameters( "dummy" );
        }

//        logger.info( "VIEWSTATE = " + req.getPostFirstParameter( "__VIEWSTATE" ) );
        
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
