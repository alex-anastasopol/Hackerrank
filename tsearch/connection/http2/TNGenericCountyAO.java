package ro.cst.tsearch.connection.http2;

import org.apache.commons.io.IOUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.utils.InstanceManager;

public class TNGenericCountyAO extends HttpSite {
    private boolean loggingIn = false;
    private String intermPageSearchKey = null;
	
	public LoginResponse onLogin( ) 
	{
		//login
		HTTPRequest req = new HTTPRequest( "http://www.assessment.state.tn.us/SelectCounty.asp" );
        req.setMethod( HTTPRequest.POST );
        req.setHeader( "Referer", "http://www.assessment.state.tn.us/SelectCounty.asp" );
        process( req );//HTTPResponse res = 
//        res.getResponseAsString();
		
        return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

	public void onBeforeRequest(HTTPRequest req) {
		// don't do anything during logging in
		if(status != STATUS_LOGGED_IN){
			return;
		}
		// don't do anything while we're already inside a onBeforeRequest call
		if(getAttribute("onBeforeRequest") == Boolean.TRUE){
			return;
		}
		// mark that we're treating onBeforeRequest
		setAttribute("onBeforeRequest", Boolean.TRUE);

		req.noRedirects=true;
		String url = req.getURL(); 
		
		
		if (req.getPostParameter("mradParcelID")!=null)
		{//request for final page
/*12:00:00.689[299ms][total 800ms] Status: 200[OK]
POST http://www.assessment.state.tn.us/ParcelDetail4.asp?C=001 Load Flags[LOAD_DOCUMENT_URI  LOAD_INITIAL_DOCUMENT_URI  ] Content Size[-1] Mime Type[text/html]
   Request Headers:
      Host[www.assessment.state.tn.us]
      User-Agent[Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.0.4) Gecko/2008102920 Firefox/3.0.4]
      Accept[text/html,application/xhtml+xml,application/xml;q=0.9,**;q=0.8]
      Accept-Language[en-us,en;q=0.5]
      Accept-Encoding[gzip,deflate]
      Accept-Charset[ISO-8859-1,utf-8;q=0.7,*;q=0.7]
      Keep-Alive[300]
      Connection[keep-alive]
      Referer[http://www.assessment.state.tn.us/ParcelList.asp?C=]
      Cookie[ASPSESSIONIDAACCASDB=FAANGNHDNEGEPHKHDIFBEBMG]
   Post Data:
      mradParcelID[A001104E+A+03900+000104E+CA]
      mSystem[O]
      mCounty[001]
      CountyNumber[001]
      CountyName[ANDERSON++]
      TaxYear[2009]
   Response Headers:
      Date[Thu, 04 Dec 2008 10:00:03 GMT]
      Server[Microsoft-IIS/6.0]
      X-Powered-By[ASP.NET]
      Content-Type[text/html]
      Cache-Control[private]
      Transfer-Encoding[chunked]



In ATS:
Type = POST   URL=http://www.assessment.state.tn.us/ParcelDetail4.asp?C=001
........... HEADER PARAMETERS ............
Accept-Language           en-us
Host           www.assessment.state.tn.us
Referer           http://www.assessment.state.tn.us/ParcelList.asp?C=
User-Agent           Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)
Content-Type           application/x-www-form-urlencoded
Connection           Keep-Alive
Accept           image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, application/x-shockwave-flash, **
.......... POST PARAMETERS .............
mSystem           O
mradParcelID           A001104E A 03900 000104E CA
CountyNumber           001
mCounty           001
CountyName           ANDERSON  
TaxYear           2009*/			
			loggingIn = true;
			req.setHeader("Referer", "http://www.assessment.state.tn.us/ParcelList.asp?C=");
			req.setHeader("Host", "www.assessment.state.tn.us");
			url = url + "?C=" + req.getPostParameter("CountyNumber").toString();
			req.modifyURL(url);
			
        	HTTPResponse res = null;
        	res = process (req);// the 'process' method inherited from http2.HttpSite 
            String htmlRes = res.getResponseAsString();

			if (!htmlRes.matches("(?is).*Property\\s*Owner\\s*and\\s*Mailing\\s*Address.*") ||
					htmlRes.matches("(?is).*Invalid\\s*Search.*") ||
					htmlRes.matches("(?is).*Object\\s*Moved.*"))
		    {
				lastSearchForIntermPage();
				res = process (req);

				htmlRes = res.getResponseAsString();
                res.is = IOUtils.toInputStream(htmlRes); 
                req.setBypassResponse(res); 
		    }
			else
			{
                res.is = IOUtils.toInputStream(htmlRes); 
                req.setBypassResponse(res); 
			}
			loggingIn = false;
		}
		else if (req.getPostParameter("txtOwnerName")!=null)
		{// request for intermediary page
/*
POST http://www.assessment.state.tn.us/ParcelList.asp?C= Load Flags[LOAD_DOCUMENT_URI  LOAD_INITIAL_DOCUMENT_URI  ] Content Size[-1] Mime Type[text/html]
   Request Headers:
      Host[www.assessment.state.tn.us]
      User-Agent[Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.14) Gecko/20080404 Firefox/2.0.0.14]
      Accept[text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,**;q=0.5]
      Accept-Language[en-us,en;q=0.5]
      Accept-Encoding[gzip,deflate]
      Accept-Charset[ISO-8859-1,utf-8;q=0.7,*;q=0.7]
      Keep-Alive[300]
      Connection[keep-alive]
      Referer[http://www.assessment.state.tn.us/SelectCounty.asp]
      Cookie[ASPSESSIONIDAASRBBAQ=MIJANACACGMGIMENLFOILEPD]
   Post Data:
      countylist[063]
      txtOwnerName[Johnson+B]
      txtPropertyAddress[]
      txtControlMap[]
      txtGroup[]
      txtParcel[]
      txtSubdivisionName[]
      Class[]
      txtBegSaleDate[]
      txtEndingSaleDate[]
      SortOptions[Owner]
      submit1[+++++SEARCH+++++]
   Response Headers:
      Date[Wed, 18 Jun 2008 12:27:16 GMT]
      Server[Microsoft-IIS/6.0]
      X-Powered-By[ASP.NET]
      Pragma[no-cache]
      Cache-Control[private, private]
      Content-Type[text/html]
      Expires[Mon, 16 Jun 2008 12:27:16 GMT]
      Transfer-Encoding[chunked]

Type = POST   URL=http://www.assessment.state.tn.us/ParcelList.asp?C=
........... HEADER PARAMETERS ............
Accept-Language           en-us
Host           www.assessment.state.tn.us
Referer           http://www.assessment.state.tn.us/SelectCounty.asp
User-Agent           Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)
Content-Type           application/x-www-form-urlencoded
Connection           Keep-Alive
Accept           image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, application/x-shockwave-flash, **
.......... POST PARAMETERS .............
txtControlMap           
txtSubdivisionName           
countylist           063
txtEndingSaleDate           
submit1                SEARCH     
Class           
txtPropertyAddress           
txtOwnerName           Johnson B
txtBegSaleDate           
txtParcel           
SortOptions           Owner
txtGroup           */			
			loggingIn = true;
			req.setHeader("Referer", "http://www.assessment.state.tn.us/SelectCounty.asp");
			req.setHeader("Host", "www.assessment.state.tn.us");
			url = "http://www.assessment.state.tn.us/ParcelList.asp?C=";
			req.modifyURL(url);
			
			intermPageSearchKey = Long.toString(searchId);
			Search crtSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			crtSearch.setAdditionalInfo(intermPageSearchKey, req);
			
			loggingIn = false;
		}
		
		setAttribute("onBeforeRequest", Boolean.FALSE);
	}

	/**
	 * makes the last search for intermediary page one more time,
	 * in order to refresh the connection
	 * @param 
	 */
	private void lastSearchForIntermPage()
	{
		Search crtIntermSearch = InstanceManager.getManager().getCurrentInstance(Long.parseLong(intermPageSearchKey)).getCrtSearchContext();
		HTTPRequest req = (HTTPRequest) crtIntermSearch.getAdditionalInfo(intermPageSearchKey);
		HTTPResponse res = null;
		
		if (req != null)
		{
        	process (req);//res = // the 'process' method inherited from http2.HttpSite 
//            String htmlRes = res.getResponseAsString();
		}
	}
	
	/**
	 * log a message, together with instance id and session id
	 * @param message
	 */
	private void info(String message) {
		logger.info("search=" + searchId + " :" + message);
	}

	@Override
	public void onRedirect(HTTPRequest req){
		String location = req.getRedirectLocation();
		if(location.contains("?e=newSession") || location.contains("?e=sessionTerminated")){
			setDestroy(true);
			throw new RuntimeException("Redirected to " + location + ". Session needs to be destroyed");
		}
	}
	
	
}
