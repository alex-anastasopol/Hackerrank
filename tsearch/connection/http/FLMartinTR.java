package ro.cst.tsearch.connection.http;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.utils.InstanceManager;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Set;

public class FLMartinTR extends HTTPSite {
    private boolean loggingIn = false;
    String viewstate = "";
    String eventvalidation= "";
	
	public void setMiServerId(long miServerId){
		super.setMiServerId(miServerId);
	}

	public LoginResponse onLogin( ) 
	{
        loggingIn = true;
        
        viewstate = "";
        eventvalidation = "";
        
        setUserData(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "FLMartinTR");
	
		HTTPRequest req = new HTTPRequest( "https://taxcol.martin.fl.us/ITM/PropertySearch.aspx" );
		req.setHeader("Host", "taxcol.martin.fl.us");
        req.setMethod( HTTPRequest.GET );
        HTTPResponse res = process( req );
        String resp = res.getResponseAsString();
        
        if (resp.matches("(?s).*__VIEWSTATE.*"))
        		viewstate = resp.replaceFirst("(?s).*__VIEWSTATE[^>]+value[^'\\\"]+['\\\"]([^'\\\"]+).*", "$1");
        if (resp.matches("(?s).*__EVENTVALIDATION.*"))
        		eventvalidation = resp.replaceFirst("(?s).*__EVENTVALIDATION[^>]+value[^'\\\"]+['\\\"]([^'\\\"]+).*", "$1");

        loggingIn = false;

        return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void onBeforeRequest(HTTPRequest req) {
		setUserData(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "FLMartinTR");
		if( loggingIn )
        {
            return;
        }
//		req.noRedirects=true;
		String url = req.getURL();
		
		req.setHeader("Host", "taxcol.martin.fl.us");
		
		if ( req.getPostFirstParameter("txtAccount") != null && !"".equals(req.getPostFirstParameter("txtAccount").toString()) && !url.contains("dummy"))
		{// Search by Account Number
			loggingIn = true;
			
			req.setHeader("Referer","https://taxcol.martin.fl.us/ITM/PropertySearch.aspx");
			try
			{
				req.setPostParameter("__VIEWSTATE", viewstate); 
//				req.setPostParameter("__EVENTVALIDATION", eventvalidation);
			} catch (Exception e) {
	            e.printStackTrace();
	        }
/*
Type = POST   URL=http://taxcol.martin.fl.us/ITM/PropertySearch.aspx
........... HEADER PARAMETERS ............
Accept-Language           en-us
Host           taxcol.martin.fl.us
Referer           http://taxcol.martin.fl.us/ITM/PropertySearch.aspx
User-Agent           Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)
Content-Type           application/x-www-form-urlencoded
Connection           Keep-Alive
Accept           image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, application/x-shockwave-flash, **
.......... POST PARAMETERS .............
dropType            
txtSitusName                  __EVENTVALIDATION           
txtSitusNbr           
txtPopup           
txtDBA           
btnSearch1           Search
__VIEWSTATE           dDwtMTY0ODE3NjYxMjt0PDtsPGk8MD47aTwxPjs
txtAccount           3038420040340010030000
txtSitusDirection           
txtYear2           
dropSubdivision            
txtYear1           
txtOwner           
txtSitusApt           
dropSitusCode            

 * 
POST http://taxcol.martin.fl.us/ITM/PropertySearch.aspx
   Request Headers:
      Host[taxcol.martin.fl.us]
      User-Agent[Mozilla/5.0 (X11; U; SunOS i86pc; en-US; rv:1.8.1) Gecko/20061024 Firefox/2.0]
      Accept[text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,**;q=0.5]
      Accept-Language[en-us,en;q=0.5]
      Accept-Encoding[gzip,deflate]
      Accept-Charset[ISO-8859-1,utf-8;q=0.7,*;q=0.7]
      Keep-Alive[300]
      Connection[keep-alive]
      Referer[http://taxcol.martin.fl.us/ITM/PropertySearch.aspx]
      Cookie[ASP.NET_SessionId=yx4ypjapseymwr55jopwnv45]
   Post Data:
      __VIEWSTATE[dDwtMTY0ODE3Nj
      txtPopup[]
      txtAccount[3038420040340010030000]
      btnSearch1[Search]
      txtOwner[]
      txtYear1[]
      txtSitusNbr[]
      txtSitusName[]
      dropType[+]
      txtSitusDirection[]
      txtSitusApt[]
      dropSitusCode[+]
      txtDBA[]
      txtYear2[]
      dropSubdivision[+]
   Response Headers:
      Date[Wed, 16 Apr 2008 09:08:30 GMT]
      Server[Microsoft-IIS/6.0]
      X-Powered-By[ASP.NET]
      X-AspNet-Version[1.1.4322]
      Location[/ITM/PropertySummary.aspx?Search=Property&Account=3038420040340010030000]
      Cache-Control[private]
      Content-Type[text/html; charset=utf-8]
      Content-Length[193]

GET   URL=http://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Property&Account=3038420040340010030000
........... HEADER PARAMETERS ............
Accept-Language           en-us
Host           taxcol.martin.fl.us
Referer           http://taxcol.martin.fl.us/ITM/PropertySearch.aspx
User-Agent           Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)
Content-Type           application/x-www-form-urlencoded
Connection           Keep-Alive
Accept           image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, application/x-shockwave-flash, **
.......... POST PARAMETERS .............

GET http://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Property&Account=3038420040340010030000
   Request Headers:
      Host[taxcol.martin.fl.us]
      User-Agent[Mozilla/5.0 (X11; U; SunOS i86pc; en-US; rv:1.8.1) Gecko/20061024 Firefox/2.0]
      Accept[text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,**;q=0.5]
      Accept-Language[en-us,en;q=0.5]
      Accept-Encoding[gzip,deflate]
      Accept-Charset[ISO-8859-1,utf-8;q=0.7,*;q=0.7]
      Keep-Alive[300]
      Connection[keep-alive]
      Referer[http://taxcol.martin.fl.us/ITM/PropertySearch.aspx]
      Cookie[ASP.NET_SessionId=yx4ypjapseymwr55jopwnv45]
   Response Headers:
      Date[Wed, 16 Apr 2008 09:08:31 GMT]
      Server[Microsoft-IIS/6.0]
      X-Powered-By[ASP.NET]
      X-AspNet-Version[1.1.4322]
      Cache-Control[private]
      Content-Type[text/html; charset=utf-8]
      Content-Length[6975]
 */
			req.setPostParameter("dropType", " ");
			req.setPostParameter("dropSitusCode", " ");
			req.setPostParameter("dropSubdivision", " ");
//			req.toString();
			
			process(req);//HTTPResponse resp =
//			String respStr = resp.getResponseAsString();
			String strTxtAccount = req.getPostParameter("txtAccount").elementAt(0).toString();

			req.removePostParameters("txtPopup");
			req.removePostParameters("txtAccount");
			req.removePostParameters("btnSearch1");
			req.removePostParameters("txtOwner");
			req.removePostParameters("txtYear1");
			req.removePostParameters("txtSitusNbr");
			req.removePostParameters("txtSitusName");
			req.removePostParameters("dropType");
			req.removePostParameters("txtSitusDirection");
			req.removePostParameters("txtSitusApt");
			req.removePostParameters("dropSitusCode");
			req.removePostParameters("txtDBA");
			req.removePostParameters("txtYear2");
			req.removePostParameters("dropSubdivision");
			req.removePostParameters("__VIEWSTATE");
			req.removePostParameters("__EVENTVALIDATION");
			
			req.modifyURL("https://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Property&Account="
					+strTxtAccount);
			req.setMethod(HTTPRequest.GET);
			req.setHeader("Host", "taxcol.martin.fl.us");
			req.setHeader("Referer", "https://taxcol.martin.fl.us/ITM/PropertySearch.aspx");
//			req.toString();
			
			loggingIn = false;
		}
		else if ( req.getPostFirstParameter("__EVENTTARGET") != null && 
				(req.getPostFirstParameter("__EVENTTARGET").toString().equals("lnkNext") ||
						req.getPostFirstParameter("__EVENTTARGET").toString().equals("lnkPrevious")))
		{// Previous, Next links
			loggingIn = true;
/* Next, search by Owner Name
POST http://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Owner&Owner=Smith&Year=2007
........... HEADER PARAMETERS ............
Connection           Keep-Alive
Host           taxcol.martin.fl.us
User-Agent           Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)
Accept-Language           en-us
Referer           http://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Owner&Owner=Smith&Year=2007
Content-Type           application/x-www-form-urlencoded
Accept           image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, application/x-shockwave-flash, **
.......... POST PARAMETERS .............
__EVENTARGUMENT           
_ctlchkPayOnline5           
_ctlchkPayOnline4           
_ctltxtPopup           
__EVENTVALIDATION           /wEWBAL0gquCCgKS8aq2CAL7v97RBgKSw9qBBbRAh91IsxXQfDTR0Z3vkrLlk1cS
__VIEWSTATE           /wEPDwUJNTEwODMyODQyDxYCHghSb3dDb3VudAIHFgRmDw8
_ctlhdnHideCartText           1
__EVENTTARGET           lnkNext
 * 
1) POST http://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Owner&Owner=Smith&Year=2007
   Request Headers:
      Host[taxcol.martin.fl.us]
      User-Agent[Mozilla/5.0 (X11; U; SunOS i86pc; en-US; rv:1.8.1) Gecko/20061024 Firefox/2.0]
      Accept[text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,**;q=0.5]
      Accept-Language[en-us,en;q=0.5]
      Accept-Encoding[gzip,deflate]
      Accept-Charset[ISO-8859-1,utf-8;q=0.7,*;q=0.7]
      Keep-Alive[300]
      Connection[keep-alive]
      Referer[http://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Owner&Owner=Smith&Year=2007]
      Cookie[ASP.NET_SessionId=pislcg45gsagy455xslss045]
   Post Data:
      __EVENTTARGET[lnkNext]
      __EVENTARGUMENT[]
      __VIEWSTATE[%2FwEPDwUJNTEwODMyODQyDxYCHghSb3dDb3VudAIHFgRmDw8W
      txtPopup[]
      hdnHideCartText[1]
      __EVENTVALIDATION[%2FwEWBAL0gquCCgKS8aq2CAL7v97RBgKSw9qBBbRAh91IsxXQfDTR0Z3vkrLlk1cS]
   Response Headers:
      Date[Fri, 01 Feb 2008 12:07:00 GMT]
      Server[Microsoft-IIS/6.0]
      X-Powered-By[ASP.NET]
      X-AspNet-Version[2.0.50727]
      Cache-Control[private]
      Content-Type[text/html; charset=utf-8]
      Content-Length[22612]
      
2) POST http://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Owner&Owner=Smith&Year=2007
   Request Headers:
      Host[taxcol.martin.fl.us]
      User-Agent[Mozilla/5.0 (X11; U; SunOS i86pc; en-US; rv:1.8.1) Gecko/20061024 Firefox/2.0]
      Accept[text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,**;q=0.5]
      Accept-Language[en-us,en;q=0.5]
      Accept-Encoding[gzip,deflate]
      Accept-Charset[ISO-8859-1,utf-8;q=0.7,*;q=0.7]
      Keep-Alive[300]
      Connection[keep-alive]
      Referer[http://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Owner&Owner=Smith&Year=2007]
      Cookie[ASP.NET_SessionId=pislcg45gsagy455xslss045]
   Post Data:
      __EVENTTARGET[lnkNext]
      __EVENTARGUMENT[]
      __VIEWSTATE[%2FwEPDwUJNTEwODMyODQyDxYCHghSb3dDb3VudAIHFgRmDw8WAh4E
      txtPopup[]
      hdnHideCartText[0]
      txtName1[SMITH%2C+AARON+%26+BRANDY]
      txtAcctNo1[++++++++++++++++500002324052]
      txtAcctYear1[2007]
      txtAcctBtYear1[]
      txtAcctPmtInd1[U]
      txtAcctPmtDate1[20080201]
      txtDetLnkAcctPayAmt1[00000000244]
      txtItemDispAcctNo1[2007+5000-0232405%2F2]
      txtName2[SMITH%2C+AARON+HOME+IMPROVEMENTS]
      txtAcctNo2[++++++++++++++++100020001012]
      txtAcctYear2[2007]
      txtAcctBtYear2[]
      txtAcctPmtInd2[U]
      txtAcctPmtDate2[20080201]
      txtDetLnkAcctPayAmt2[00000001911]
      txtItemDispAcctNo2[2007+1000-2000101%2F2]
      __EVENTVALIDATION[%2FwEWBwLd5vC2DQKS8aq2CAL7v97RBgKOw4rODAKSw9qBBQKEh8HYDwKOw%2FbyA40IBu%2F4DlNuQF9wD%2Bfs%2Bd3UxqiD]
   Response Headers:
      Date[Fri, 01 Feb 2008 12:08:25 GMT]
      Server[Microsoft-IIS/6.0]
      X-Powered-By[ASP.NET]
      X-AspNet-Version[2.0.50727]
      Cache-Control[private]
      Content-Type[text/html; charset=utf-8]
      Content-Length[26047]
*************** Prev, Search by Owner Name
3)
POST http://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Owner&Owner=Smith&Year=2007
   Request Headers:
      Host[taxcol.martin.fl.us]
      User-Agent[Mozilla/5.0 (X11; U; SunOS i86pc; en-US; rv:1.8.1) Gecko/20061024 Firefox/2.0]
      Accept[text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,**;q=0.5]
      Accept-Language[en-us,en;q=0.5]
      Accept-Encoding[gzip,deflate]
      Accept-Charset[ISO-8859-1,utf-8;q=0.7,*;q=0.7]
      Keep-Alive[300]
      Connection[keep-alive]
      Referer[http://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Owner&Owner=Smith&Year=2007]
      Cookie[ASP.NET_SessionId=ukp0a255huwtkr45qeuj0x45]
   Post Data:
      __EVENTTARGET[lnkPrevious]
      __EVENTARGUMENT[]
      __VIEWSTATE[dDw5NTUwMDg1MTQ7dDxwPGw8Um93Q291bnQ7PjtsP
      txtPopup[]
      hdnHideCartText[0]
      txtName3[SMITH%2C+ALINE+A]
      txtAcctNo3[++++++4838411900030201010000]
      txtAcctYear3[2007]
      txtAcctBtYear3[]
      txtAcctPmtInd3[U]
      txtAcctPmtDate3[20080208]
      txtDetLnkAcctPayAmt3[00000230386]
      txtItemDispAcctNo3[2007+48-38-41-190-003-02010.10000]
      txtName4[SMITH%2C+ALLAN+A+%26+FRANCES+B]
      txtAcctNo4[+++++++639410010020018150000]
      txtAcctYear4[2007]
      txtAcctBtYear4[]
      txtAcctPmtInd4[U]
      txtAcctPmtDate4[20080208]
      txtDetLnkAcctPayAmt4[00000180184]
      txtItemDispAcctNo4[2007+6-39-41-001-002-00181.50000]
      txtName6[SMITH%2C+ALOIS+B]
      txtAcctNo6[++++++3438420380130012050000]
      txtAcctYear6[2007]
      txtAcctBtYear6[]
      txtAcctPmtInd6[U]
      txtAcctPmtDate6[20080208]
      txtDetLnkAcctPayAmt6[00000025219]
      txtItemDispAcctNo6[2007+34-38-42-038-013-00120.50000]
      txtName7[SMITH%2C+ALOIS+B]
      txtAcctNo7[++++++3438420380200020030000]
      txtAcctYear7[2007]
      txtAcctBtYear7[]
      txtAcctPmtInd7[U]
      txtAcctPmtDate7[20080208]
      txtDetLnkAcctPayAmt7[00000025219]
      txtItemDispAcctNo7[2007+34-38-42-038-020-00200.30000]
   Response Headers:
      Date[Fri, 08 Feb 2008 14:18:37 GMT]
      Server[Microsoft-IIS/6.0]
      X-Powered-By[ASP.NET]
      X-AspNet-Version[1.1.4322]
      Cache-Control[private]
      Content-Type[text/html; charset=utf-8]
      Content-Length[27399]

2)
POST http://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Owner&Owner=Smith&Year=2007
   Request Headers:
      Host[taxcol.martin.fl.us]
      User-Agent[Mozilla/5.0 (X11; U; SunOS i86pc; en-US; rv:1.8.1) Gecko/20061024 Firefox/2.0]
      Accept[text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,**;q=0.5]
      Accept-Language[en-us,en;q=0.5]
      Accept-Encoding[gzip,deflate]
      Accept-Charset[ISO-8859-1,utf-8;q=0.7,*;q=0.7]
      Keep-Alive[300]
      Connection[keep-alive]
      Referer[http://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Owner&Owner=Smith&Year=2007]
      Cookie[ASP.NET_SessionId=ukp0a255huwtkr45qeuj0x45]
   Post Data:
      __EVENTTARGET[lnkPrevious]
      __EVENTARGUMENT[]
      __VIEWSTATE[dDw5NTUwMDg1MTQ7dDxwPGw8Um93Q291bnQ
      txtPopup[]
      hdnHideCartText[0]
      txtName2[SMITH%2C+AKINS%2C+%26+ASSOC+PA]
      txtAcctNo2[++++++++++++++++100010141286]
      txtAcctYear2[2007]
      txtAcctBtYear2[]
      txtAcctPmtInd2[U]
      txtAcctPmtDate2[20080208]
      txtDetLnkAcctPayAmt2[00000020607]
      txtItemDispAcctNo2[2007+1000-1014128%2F6]
      txtName5[SMITH%2C+ALEITA+%26+ROBERT]
      txtAcctNo5[++++++1638410040000033070000]
      txtAcctYear5[2007]
      txtAcctBtYear5[]
      txtAcctPmtInd5[U]
      txtAcctPmtDate5[20080208]
      txtDetLnkAcctPayAmt5[00000249988]
      txtItemDispAcctNo5[2007+16-38-41-004-000-00330.70000]
      txtName6[SMITH%2C+ALEXANDER+%26+LINDA+%28T%2FE%29]
      txtAcctNo6[++++++1340400004000075090000]
      txtAcctYear6[2007]
      txtAcctBtYear6[]
      txtAcctPmtInd6[U]
      txtAcctPmtDate6[20080208]
      txtDetLnkAcctPayAmt6[00000003549]
      txtItemDispAcctNo6[2007+13-40-40-000-400-00750.90000]
      txtName7[SMITH%2C+ALEXIS+J+%26]
      txtAcctNo7[++++++++++++++++100006358212]
      txtAcctYear7[2007]
      txtAcctBtYear7[]
      txtAcctPmtInd7[U]
      txtAcctPmtDate7[20080208]
      txtDetLnkAcctPayAmt7[00000006248]
      txtItemDispAcctNo7[2007+1000-0635821%2F2]
   Response Headers:
      Date[Fri, 08 Feb 2008 14:19:43 GMT]
      Server[Microsoft-IIS/6.0]
      X-Powered-By[ASP.NET]
      X-AspNet-Version[1.1.4322]
      Cache-Control[private]
      Content-Type[text/html; charset=utf-8]
      Content-Length[23916]

1)
16:21:33.078[665ms][total 827ms] Status: 200[OK]
POST http://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Owner&Owner=Smith&Year=2007
   Request Headers:
      Host[taxcol.martin.fl.us]
      User-Agent[Mozilla/5.0 (X11; U; SunOS i86pc; en-US; rv:1.8.1) Gecko/20061024 Firefox/2.0]
      Accept[text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,**;q=0.5]
      Accept-Language[en-us,en;q=0.5]
      Accept-Encoding[gzip,deflate]
      Accept-Charset[ISO-8859-1,utf-8;q=0.7,*;q=0.7]
      Keep-Alive[300]
      Connection[keep-alive]
      Referer[http://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Owner&Owner=Smith&Year=2007]
      Cookie[ASP.NET_SessionId=ukp0a255huwtkr45qeuj0x45]
   Post Data:
      __EVENTTARGET[lnkPrevious]
      __EVENTARGUMENT[]
      __VIEWSTATE[dDw5NTUwMDg1MTQ7dDxwPGw8Um93Q291bnQ7PjtsPGk8Nz47Pj47bDxpPDA%2
      txtPopup[]
      hdnHideCartText[0]
      txtName1[SMITH%2C+AARON+%26+BRANDY]
      txtAcctNo1[++++++++++++++++500002324052]
      txtAcctYear1[2007]
      txtAcctBtYear1[]
      txtAcctPmtInd1[U]
      txtAcctPmtDate1[20080208]
      txtDetLnkAcctPayAmt1[00000000244]
      txtItemDispAcctNo1[2007+5000-0232405%2F2]
      txtName2[SMITH%2C+AARON+HOME+IMPROVEMENTS]
      txtAcctNo2[++++++++++++++++100020001012]
      txtAcctYear2[2007]
      txtAcctBtYear2[]
      txtAcctPmtInd2[U]
      txtAcctPmtDate2[20080208]
      txtDetLnkAcctPayAmt2[00000001911]
      txtItemDispAcctNo2[2007+1000-2000101%2F2]
   Response Headers:
      Date[Fri, 08 Feb 2008 14:21:46 GMT]
      Server[Microsoft-IIS/6.0]
      X-Powered-By[ASP.NET]
      X-AspNet-Version[1.1.4322]
      Cache-Control[private]
      Content-Type[text/html; charset=utf-8]
      Content-Length[20888]

URL=http://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Owner&Owner=Smith&Year=2007
........... HEADER PARAMETERS ............
Connection           Keep-Alive
Host           taxcol.martin.fl.us
User-Agent           Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)
Accept-Language           en-us
Referer           http://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Owner&Owner=Smith&Year=2007
Content-Type           application/x-www-form-urlencoded
Accept           image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, application/x-shockwave-flash, **
.......... POST PARAMETERS .............
chkPayOnline2           
txtDetLnkAcctPayAmt1           00000000244
chkPayOnline1           
txtAcctNo2                           100020001012
txtAcctPmtInd2           U
__VIEWSTATE           dDw5NTUwMDg1MTQ7dDxwPGw8Um93Q291bnQ7PjtsPGk8Nz
txtItemDispAcctNo2           2007 1000-2000101/2
__EVENTARGUMENT           
txtAcctYear1           2007
hdnHideCartText           0
txtAcctPmtDate1           20080208
__EVENTTARGET           lnkPrevious
txtDetLnkAcctPayAmt2           00000001911
txtAcctNo1                           500002324052
txtAcctPmtDate2           20080208
txtItemDispAcctNo1           2007 5000-0232405/2
txtName1           SMITH, AARON & BRANDY
txtAcctBtYear2           
txtAcctYear2           2007
txtPopup           
txtAcctPmtInd1           U
txtAcctBtYear1           
txtName2           SMITH, AARON HOME IMPROVEMENTS

1')
POST http://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Owner&Owner=Smith&Year=2007
   Request Headers:
      Host[taxcol.martin.fl.us]
      User-Agent[Mozilla/5.0 (X11; U; SunOS i86pc; en-US; rv:1.8.1) Gecko/20061024 Firefox/2.0]
      Accept[text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,**;q=0.5]
      Accept-Language[en-us,en;q=0.5]
      Accept-Encoding[gzip,deflate]
      Accept-Charset[ISO-8859-1,utf-8;q=0.7,*;q=0.7]
      Keep-Alive[300]
      Connection[keep-alive]
      Referer[http://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Owner&Owner=Smith&Year=2007]
      Cookie[ASP.NET_SessionId=ukp0a255huwtkr45qeuj0x45]
   Post Data:
      __EVENTTARGET[lnkPrevious]
      __EVENTARGUMENT[]
      __VIEWSTATE[dDw5NTUwMDg1MTQ7dDxwPGw8Um93Q291bnQ7PjtsPGk8Nz47Pj47bDxpPDA%2BO
      txtPopup[]
      hdnHideCartText[1]
   Response Headers:
      Date[Fri, 08 Feb 2008 14:22:57 GMT]
      Server[Microsoft-IIS/6.0]
      X-Powered-By[ASP.NET]
      X-AspNet-Version[1.1.4322]
      Cache-Control[private]
      Content-Type[text/html; charset=utf-8]
      Content-Length[26911]

Next, Search by Address
POST http://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Address&Nbr=&Name=Little+Village&Type=&Direction=&Apt=&City=
   Request Headers:
      Host[taxcol.martin.fl.us]
      User-Agent[Mozilla/5.0 (X11; U; SunOS i86pc; en-US; rv:1.8.1) Gecko/20061024 Firefox/2.0]
      Accept[text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,**;q=0.5]
      Accept-Language[en-us,en;q=0.5]
      Accept-Encoding[gzip,deflate]
      Accept-Charset[ISO-8859-1,utf-8;q=0.7,*;q=0.7]
      Keep-Alive[300]
      Connection[keep-alive]
      Referer[http://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Address&Nbr=&Name=Little+Village&Type=&Direction=&Apt=&City=]
      Cookie[ASP.NET_SessionId=ukp0a255huwtkr45qeuj0x45]
   Post Data:
      __EVENTTARGET[lnkNext]
      __EVENTARGUMENT[]
      __VIEWSTATE[dDw5NTUwMDg1MTQ7dDxwPGw8Um93Q291bnQ7PjtsPGk8Nz47Pj47bDxpPDA%2
      txtPopup[]
      hdnHideCartText[1]
   Response Headers:
      Date[Fri, 08 Feb 2008 15:03:29 GMT]
      Server[Microsoft-IIS/6.0]
      X-Powered-By[ASP.NET]
      X-AspNet-Version[1.1.4322]
      Cache-Control[private]
      Content-Type[text/html; charset=utf-8]
      Content-Length[20129]

Next, Search by dba Name

Type = POST   URL=http://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=DBA&Year=2007
........... HEADER PARAMETERS ............
Connection           Keep-Alive
Host           taxcol.martin.fl.us
User-Agent           Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)
Accept-Language           en-us
Referer           http://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=DBA&Year=2007
Content-Type           application/x-www-form-urlencoded
Accept           image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, application/x-shockwave-flash, **
.......... POST PARAMETERS .............
txtAcctNo1                 5538410000430003020000
DBA           dba Family Name Centers
txtDetLnkAcctPayAmt1           00001680973
txtItemDispAcctNo1           2007 55-38-41-000-043-00030.20000
chkPayOnline1           
txtName1           FAMILY GOLF CENTERS
__VIEWSTATE           dDw5NTUwMDg1MTQ7dDxwPGw8Um93Q291bnQ7PjtsPGk8Nz
__EVENTARGUMENT           
txtPopup           
txtAcctYear1           2007
txtAcctBtYear1           
txtAcctPmtInd1           U
hdnHideCartText           1
txtAcctPmtDate1           20080212
__EVENTTARGET           lnkNext

Next, Search by Property Address
*/

			if (req.getPostParameter("Search")!=null)
			{
				url += "?Search="+req.getPostParameter("Search").firstElement().toString().replaceAll("\\s", "+");
				req.removePostParameters("Search");
			}
//			Search by Name
			if (req.getPostParameter("Owner")!=null)
			{
				url += "&Owner="+req.getPostParameter("Owner").firstElement().toString().replaceAll("\\s", "+");
				req.removePostParameters("Owner");
			}
			if (req.getPostParameter("Year")!=null)
			{
				url += "&Year="+req.getPostParameter("Year").firstElement().toString().replaceAll("\\s", "+");
				req.removePostParameters("Year");
			}
			
			req.modifyURL(url);
			req.setHeader("Referer", url);
			
			HashMap params = req.getPostParameters();
			Set<Object> keys = params.keySet();
			Object[] keysAux = (Object[])keys.toArray();
			String[] keysV = new String[keysAux.length];
			for (int j =0; j<keysAux.length; j++)
			{
				keysV[j] = keysAux[j].toString();
			}
			
			for (int i=0; i< keysV.length; i++)
			{
				String paramName =  keysV[i];
				
			    if (paramName.startsWith("_ctl"))
			    {
			    	String value = (String) req.getPostFirstParameter(paramName).toString();
			    	req.removePostParameters(paramName);
			    	paramName = paramName.substring(4);
			    	req.setPostParameter(paramName, value);
			    }
			}
//			req.toString();
			
			loggingIn = false;
		}
		else if (url.contains("PropertyDetails.aspx") && !url.contains("dummy"))
		{// Account Number, Search for Details
			loggingIn = true;
			
			url = url.replace("?Acctyear", "&Acctyear");
			String strAccount = url.replaceFirst("(?is).*acctno[^a-z0-9]+([a-z0-9]+).*","$1");
			String referer = "";
			
			if (url.contains("refererDBA"))
			{
				referer = url.replaceFirst("(?is).*&refererDBA=(.*)[&]?.*", "$1");
				url = url.replaceFirst("(?is)(.*)&refererDBA[^&]+(.*)", "$1"+"$2");
				referer="https://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=DBA&DBA="+referer+"&Year=";
				req.setHeader("Referer", referer);
			}
			else if (req.getHeader("Referer")!=null && req.getHeader("Referer").matches("(?is).*special.*"))
			{
				referer = req.getHeader("Referer");
				referer = referer.replaceFirst("(?is)[^a-z]special", "");
				req.setHeader("Referer", referer);
			}
			else if (req.getHeader("Referer")!=null && req.getHeader("Referer").matches("(?is).*certificate.*"))
			{
				referer = req.getHeader("Referer");
				referer = referer.replaceFirst("(?is)[^a-z]certificate", "");
				req.setHeader("Referer", referer);
			}
			else
			{
				referer = "https://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Property&Account="+strAccount;
				req.setHeader("Referer", referer);
			}
			
			req.modifyURL(url);
			req.setHeader("Host", "taxcol.martin.fl.us");
//			req.toString();
			
			loggingIn = false;
		}
		else if ( req.getPostFirstParameter("txtOwner") != null && !"".equals(req.getPostFirstParameter("txtOwner").toString()) && !url.contains("dummy"))
		{// Search by Owner Name
			loggingIn = true;
			
			req.setHeader("Referer","https://taxcol.martin.fl.us/ITM/PropertySearch.aspx");
			try
			{
				req.setPostParameter("__VIEWSTATE", viewstate); 
				req.setPostParameter("__EVENTVALIDATION", eventvalidation);
			} catch (Exception e) {
	            e.printStackTrace();
	        }
			
			req.setPostParameter("dropType", " ");
			req.setPostParameter("dropSitusCode", " ");
			req.setPostParameter("dropSubdivision", " ");
//			req.toString();
			
			process(req);//HTTPResponse resp = 
//			String respStr = resp.getResponseAsString();
		try{
			String strTxtOwner = URLEncoder.encode(req.getPostParameter("txtOwner").elementAt(0).toString(),"UTF-8");
			String strYear = URLEncoder.encode(req.getPostParameter("txtYear1").elementAt(0).toString(),"UTF-8");
			String exact = URLEncoder.encode(req.getPostParameter("chkExactNameSearch").elementAt(0).toString(),"UTF-8");
			if ("on".equals(exact)) 
				exact = "&Exact=1";
			else
				exact = "";

			req.removePostParameters("txtPopup");
			req.removePostParameters("txtAccount");
			req.removePostParameters("txtOwner");
			req.removePostParameters("chkExactNameSearch");
			req.removePostParameters("txtYear1");
			req.removePostParameters("btnSearch2");
			req.removePostParameters("txtSitusNbr");
			req.removePostParameters("txtSitusName");
			req.removePostParameters("dropType");
			req.removePostParameters("txtSitusDirection");
			req.removePostParameters("txtSitusApt");
			req.removePostParameters("dropSitusCode");
			req.removePostParameters("txtDBA");
			req.removePostParameters("txtYear2");
			req.removePostParameters("dropSubdivision");
			req.removePostParameters("__VIEWSTATE");
			req.removePostParameters("__EVENTVALIDATION");
//			req.toString();
			
			req.modifyURL("https://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Owner&Owner="
					+strTxtOwner.replaceAll("\\s", "+")+"&Year="+strYear+exact);
			req.setMethod(HTTPRequest.GET);
			req.setHeader("Host", "taxcol.martin.fl.us");
			req.setHeader("Referer", "https://taxcol.martin.fl.us/ITM/PropertySearch.aspx");
			
			loggingIn = false;
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		}
		else if (req.getPostFirstParameter("txtSitusName") != null && !"".equals(req.getPostFirstParameter("txtSitusName").toString()) && !url.contains("dummy"))
		{// Search by Property Address
			loggingIn = true;

			req.setHeader("Referer","https://taxcol.martin.fl.us/ITM/PropertySearch.aspx");
			try
			{
				req.setPostParameter("__VIEWSTATE", viewstate); 
				req.setPostParameter("__EVENTVALIDATION", eventvalidation);
			} catch (Exception e) {
	            e.printStackTrace();
	        }

			req.setPostParameter("dropSubdivision", " ");
//			req.toString();

			HTTPResponse resp = process(req);
			resp.getResponseAsString();//String respStr = 
			try {
				String txtSitusNbrStr = URLEncoder.encode(req.getPostParameter("txtSitusNbr").elementAt(0).toString(), "UTF-8");
				String txtSitusNameStr = URLEncoder.encode(req.getPostParameter("txtSitusName").elementAt(0).toString(), "UTF-8");
				String dropTypeStr = URLEncoder.encode(req.getPostParameter("dropType").elementAt(0).toString(), "UTF-8");
				String txtSitusDirectionStr = URLEncoder.encode(req.getPostParameter("txtSitusDirection").elementAt(0).toString(), "UTF-8");
				String txtSitusAptStr = URLEncoder.encode(req.getPostParameter("txtSitusApt").elementAt(0).toString(), "UTF-8");
				String dropSitusCodeStr = URLEncoder.encode(req.getPostParameter("dropSitusCode").elementAt(0).toString(), "UTF-8");
				if (" ".equals(dropTypeStr))
				{
					dropTypeStr = "";
				}
				if (" ".equals(dropSitusCodeStr))
				{
					dropSitusCodeStr = "";
				}

				req.removePostParameters("txtPopup");
				req.removePostParameters("txtAccount");
				req.removePostParameters("txtOwner");
				req.removePostParameters("chkExactNameSearch");
				req.removePostParameters("txtYear1");
				req.removePostParameters("btnSearch3");
				req.removePostParameters("txtSitusNbr");
				req.removePostParameters("txtSitusName");
				req.removePostParameters("dropType");
				req.removePostParameters("txtSitusDirection");
				req.removePostParameters("txtSitusApt");
				req.removePostParameters("dropSitusCode");
				req.removePostParameters("txtDBA");
				req.removePostParameters("txtYear2");
				req.removePostParameters("dropSubdivision");
				req.removePostParameters("__VIEWSTATE");
				req.removePostParameters("__EVENTVALIDATION");

				req.modifyURL("https://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Address&Nbr="
						+ txtSitusNbrStr + "&Name=" + txtSitusNameStr.replaceAll("\\s", "+") + "&Type=" + dropTypeStr + "&Direction=" + txtSitusDirectionStr
						+ "&Apt=" + txtSitusAptStr + "&City=" + dropSitusCodeStr);
				req.setMethod(HTTPRequest.GET);
				req.setHeader("Host", "taxcol.martin.fl.us");
				req.setHeader("Referer", "https://taxcol.martin.fl.us/ITM/PropertySearch.aspx");
				// req.toString();
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			loggingIn = false;			
		}
		else if(req.getPostFirstParameter("txtDBA") != null && !"".equals(req.getPostFirstParameter("txtDBA").toString()) && !url.contains("dummy"))
		{// Search by DBA Name
			loggingIn = true;
			
			req.setHeader("Referer","https://taxcol.martin.fl.us/ITM/PropertySearch.aspx");
			try
			{
				req.setPostParameter("__VIEWSTATE", viewstate); 
				req.setPostParameter("__EVENTVALIDATION", eventvalidation);
			} catch (Exception e) {
	            e.printStackTrace();
	        }
			
			req.setPostParameter("dropType", " ");
			req.setPostParameter("dropSitusCode", " ");
			req.setPostParameter("dropSubdivision", " ");
//			req.toString();
			
			HTTPResponse resp = process(req);
			resp.getResponseAsString();//String respStr = 
			String strTxtDBA = req.getPostParameter("txtDBA").elementAt(0).toString();
			String strYear = req.getPostParameter("txtYear2").elementAt(0).toString();

			req.removePostParameters("txtPopup");
			req.removePostParameters("txtAccount");
			req.removePostParameters("txtOwner");
			req.removePostParameters("chkExactNameSearch");
			req.removePostParameters("txtYear1");
			req.removePostParameters("btnSearch4");
			req.removePostParameters("txtSitusNbr");
			req.removePostParameters("txtSitusName");
			req.removePostParameters("dropType");
			req.removePostParameters("txtSitusDirection");
			req.removePostParameters("txtSitusApt");
			req.removePostParameters("dropSitusCode");
			req.removePostParameters("txtDBA");
			req.removePostParameters("txtYear2");
			req.removePostParameters("dropSubdivision");
			req.removePostParameters("__VIEWSTATE");
			req.removePostParameters("__EVENTVALIDATION");
//			req.toString();
			
			req.modifyURL("https://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=DBA&DBA="
					+strTxtDBA.replaceAll("\\s", "+")+"&Year="+strYear);
			req.setMethod(HTTPRequest.GET);
			req.setHeader("Host", "taxcol.martin.fl.us");
			req.setHeader("Referer", "https://taxcol.martin.fl.us/ITM/PropertySearch.aspx");
//			req.toString();
			
			loggingIn = false;
		}
		else if(req.getPostFirstParameter("dropSubdivision") != null && !"".equals(req.getPostFirstParameter("dropSubdivision").toString()) && !url.contains("dummy"))
		{// Search by DBA (Subdivision) Name
			loggingIn = true;
			
			req.setHeader("Referer","https://taxcol.martin.fl.us/ITM/PropertySearch.aspx");
			try
			{
				req.setPostParameter("__VIEWSTATE", viewstate); 
				req.setPostParameter("__EVENTVALIDATION", eventvalidation);
			} catch (Exception e) {
	            e.printStackTrace();
	        }
			
			req.setPostParameter("dropType", " ");
			req.setPostParameter("dropSitusCode", " ");
//			req.toString();
			
			HTTPResponse resp = process(req);
			resp.getResponseAsString();//String respStr = 
			String strSubdiv = req.getPostParameter("dropSubdivision").elementAt(0).toString();

			req.removePostParameters("txtPopup");
			req.removePostParameters("txtAccount");
			req.removePostParameters("txtOwner");
			req.removePostParameters("chkExactNameSearch");
			req.removePostParameters("txtYear1");
			req.removePostParameters("btnSearch5");
			req.removePostParameters("txtSitusNbr");
			req.removePostParameters("txtSitusName");
			req.removePostParameters("dropType");
			req.removePostParameters("txtSitusDirection");
			req.removePostParameters("txtSitusApt");
			req.removePostParameters("dropSitusCode");
			req.removePostParameters("txtDBA");
			req.removePostParameters("txtYear2");
			req.removePostParameters("dropSubdivision");
			req.removePostParameters("__VIEWSTATE");
			req.removePostParameters("__EVENTVALIDATION");
//			req.toString();
			
			req.modifyURL("https://taxcol.martin.fl.us/ITM/PropertySummary.aspx?Search=Subdivision&Subdivision="
					+strSubdiv.replaceAll("\\s", "+"));
			req.setMethod(HTTPRequest.GET);
			req.setHeader("Host", "taxcol.martin.fl.us");
			req.setHeader("Referer", "https://taxcol.martin.fl.us/ITM/PropertySearch.aspx");
//			req.toString();

			loggingIn =false;
		}
		else if (url.contains("dummy"))
		{
/*
Type = GET   URL=http://taxcol.martin.fl.us/ITM/PropertyDetails.aspx?Acctno=++++++3038420010030008050000?dummy=File&Acctyear=2007&Acctbtyear=&
Owner=COMMERCIAL+TREND+DEVELOP+INC&Page=1&parentSite=true
........... HEADER PARAMETERS ............
Accept-Language           en-us
Host           taxcol.martin.fl.us
User-Agent           Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)
Connection           Keep-Alive
Accept           image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, application/x-shockwave-flash, **
.......... POST PARAMETERS .............
*/
//			req.toString();
			return;
		}
	}
}

