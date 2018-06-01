package ro.cst.tsearch.connection.http;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.utils.InstanceManager;



public class FLLeeTR extends HTTPSite {
    private boolean loggingIn = false;
	
	public void setMiServerId(long miServerId){
		super.setMiServerId(miServerId);
	}
	
	public LoginResponse onLogin( ) 
	{
        loggingIn = true;
        
		setUserData(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "FLLeeTR");
	
		//HTTPRequest req = new HTTPRequest( "http://www.leetc.com");
		HTTPRequest req = new HTTPRequest( "http://www.leetc.com/search_criteria.asp?searchtype=RP&c=home&r=1&page_id=searchcriteria");
        req.setMethod( HTTPRequest.GET );
        HTTPResponse res = process( req );
        
        loggingIn = false;

        return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

	public void onBeforeRequest(HTTPRequest req) {
		setUserData(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "FLLeeTR");
        if( loggingIn )
        {
            return;
        }
		
		req.noRedirects=true;
		String url = req.getURL();
		req.setHeader("Host", "www.leetc.com");

		
		if (url.contains ("search_results.asp"))
		{// Search by Parcel Number, Name and Address
/*
15:40:56.384[1903ms][total 2655ms] Status: 200[OK]
POST http://www.leetc.com/search_results.asp Load Flags[LOAD_DOCUMENT_URI  LOAD_INITIAL_DOCUMENT_URI  ] Content Size[40741] Mime Type[text/html]
   Request Headers:
      Host[www.leetc.com]
      User-Agent[Mozilla/5.0 (X11; U; SunOS i86pc; en-US; rv:1.8.1) Gecko/20061024 Firefox/2.0]
      Accept[text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,**;q=0.5]
      Accept-Language[en-us,en;q=0.5]
      Accept-Encoding[gzip,deflate]
      Accept-Charset[ISO-8859-1,utf-8;q=0.7,*;q=0.7]
      Keep-Alive[300]
      Connection[keep-alive]
      Referer[http://www.leetc.com/search_criteria.asp?searchtype=RP&c=home&r=1&page_id=searchcriteria]
      Cookie[ASPSESSIONIDAQBDSTDS=BMCCMPEAGLJPBCLCGELANHKD]
   Post Data:
      searchtype[RP]
      criteriapage[search_criteria.asp]
      resultpage[search_results.asp]
      detailproc[search_detail.asp]
      SaveAsFilename[RealEstateTaxes.txt]
      DisplayFormat[1]
      searchon[account]
      queryAddl[2007]
      query1[0144250200000D]
      noquery1[]
      noqueryAddl[2007]
      noquery2[]
      searchsubmit[]
   Response Headers:
      Cache-Control[private]
      Content-Length[40741]
      Content-Type[text/html]
      X-Powered-By[ASP.NET]
      Date[Tue, 13 Nov 2007 13:43:11 GMT]

      
		      *
Type = POST   URL=http://www.leetc.com/search_results.asp
........... HEADER PARAMETERS ............
Connection           Keep-Alive
Host            www.leetc.com
User-Agent           Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)
Accept-Language           en-us
Referer           http://www.leetc.com/search_criteria.asp?searchtype=RP&c=home&r=1&page_id=searchcriteria
Content-Type           application/x-www-form-urlencoded
Accept           image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, application/x-shockwave-flash, **
.......... POST PARAMETERS .............
queryAddl           2007
criteriapage           search_criteria.asp
SaveAsFilename           RealEstateTaxes.txt
searchtype           RP
noquery1           
query1           0144250200000D
searchsubmit           
DisplayFormat           1
searchon           account
detailproc           search_detail.asp
noqueryAddl           
resultpage           search_results.asp

		      */
			loggingIn = true;
			req.setHeader("Referer", "http://www.leetc.com/search_criteria.asp?searchtype=RP&c=home&r=1&page_id=searchcriteria");
			req.setMethod( HTTPRequest.POST );
			req.setHeader("Host", " www.leetc.com");			
			// parametrii de tip POST ce apar in plus in requestul cand se click pe butonul de  PREV sau NEXT
		/*	req.setPostParameter("Current_Page" , "");
			req.setPostParameter("Total_Page" , "");
			req.setPostParameter("total_records" , "");
			req.setPostParameter("per_page" , "");
			req.setPostParameter("PageAction" , "");  // la PageAction apare Prev, respectiv Next ca valoare a parametrului Post
			req.setPostParameter("PerPage" , "");
		*/	
			/*     -------------- ------------------  ------------------- -------------------- ------------------- ------------- */
			req.setPostParameter("searchtype", "RP");
			req.setPostParameter("criteriapage", "search_criteria.asp");
			req.setPostParameter("resultpage", "search_results.asp");
			req.setPostParameter("detailproc", "search_detail.asp");
			req.setPostParameter("SaveAsFilename","RealEstateTaxes.txt");
			req.setPostParameter("DisplayFormat", "1");
			//req.setPostParameter("searchon", "account");
			req.setPostParameter("noquery1", "");
			//req.setPostParameter("noqueryAddl", "");
			req.setPostParameter("searchsubmit","");

	        loggingIn = false;
		}
		else
			if (url.contains("search_detail.asp"))
			{//pagina de Detalii
				loggingIn = true;
				//url = url.replace("?SearchType", "&SearchType");
				req.modifyURL(url);
				req.setMethod( HTTPRequest.GET );
				req.setHeader("Referer",url);
				loggingIn = false;
			}
/*			else if (url.contains("CurrentPage") && url.contains("refNext"))
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
				else if (url.contains("CurrentPage") && url.contains("refPrev"))
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
	*/
		
		req.toString();
		
	}
}

