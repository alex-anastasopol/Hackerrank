package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.HTTPSiteManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Tidy;

public class FLMartinTR extends TSServer {
	
	static final long serialVersionUID = 10000000;
	
	private boolean downloadingForSave;

	private static final Pattern lprevPattern = Pattern.compile("(?is)<\\s*a[^>]+doPostBack[^>]+>\\s*previous\\s*<\\s*/a\\s*>");
	private static final Pattern lnextPattern = Pattern.compile("(?is)<\\s*a[^>]+doPostBack[^>]+>\\s*next\\s*<\\s*/a\\s*>");
	private static final Pattern searchByParcelDetailsPattern = Pattern.compile("(?is)<\\s*a[^>]+href[^a-z]+([^>'\\\"]*PropertyDetails[^>'\\\"]+)[^>]*>[^<]+[0-9]+[^<]+<\\s*/a\\s*>");
	private static final Pattern searchByParcelYrPattern = 
		Pattern.compile("(?is)account\\s*details[\\s0-9a-z=\\\"]+(propertydetails[^\\\">]+)[\\\"<>0-9a-z/\\s]*<\\s*/td\\s*>(\\s*<\\s*td[^>]*>[^<]*(<\\s*a[^>]*>[^<]*<\\s*/a\\s*>)?[^<]*<\\s*/td\\s*>)+");
	//private static final Pattern fileIDPattern = Pattern.compile("(?is)[^0-9]+([0-9]{13,})");
	private static final Pattern pidMatch1 = Pattern.compile("(?is)(?is)<span[^>]*id=[\"']{1}lblHdrPropertyNumber[^>]*>[^\\d]*([0-9-\\./]*)");
	private static final Pattern pidMatch2 = Pattern.compile("(?is)<span[^>]*id=[\"']{1}\\s*lblDetTaxParcel[^>]*>[^\\d]*([0-9-\\./]*)");
	
	public FLMartinTR(
			String rsRequestSolverName,
			String rsSitePath,
			String rsServerID,
			String rsPrmNameLink,
			long searchId, int mid) {
			super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		
	}
	
	public static void splitResultRows(
			Parser p,
			ParsedResponse pr,
			String htmlString,
			int pageId,
			String linkStart,
			int action)
			throws ro.cst.tsearch.exceptions.ServerResponseException
			{
			
			p.splitResultRows(
				pr,
				htmlString,
				pageId,
				"<tr><b></b><td>",
				"<tr><td>&nbsp;</td></tr>",
				linkStart,
				action);
		}

	String createFinalPage(String rsResponse, String initialResponse)
	{
		StringBuffer sb=null;
		HTTPRequest req = new HTTPRequest("https://");
		String link =null;
		HTTPResponse res = null;
		String rsResponseDetail="", finalResponse=null;
		
		rsResponse = rsResponse.replaceFirst("(?is)[^0-9].*help[\\s<>&/;a-z0-9]+<\\s*/div\\s*>\\s*(<\\s*table[^>]*>)", "$1");
		rsResponse = rsResponse.replaceFirst("(?is)<\\s*/table\\s*>[\\sa-z0-9<>/]*<\\s*table[\\s!=<>@#$%&*_+a-z0-9'\\\"-]+links\\s*of\\s*interest.*", "\n</table>\n");
		rsResponse = rsResponse.replaceAll("(?is)<\\s*td[^>]*>[^<]*<\\s*a[^>]+>[^<]*<\\s*img[^>]+add\\s*to\\s*cart[^>]+>[^<]*<\\s*/a\\s*>[^<]*<\\s*div[^>]*>[^<]+cart[^<]*<\\s*/div\\s*>[^<]*<\\s*/td\\s*>","");
		rsResponse = rsResponse.replaceAll("(?is)<\\s*input[^>]+checkbox[^>]+>([^<]+<\\s*/input\\s*>)?","");
		rsResponse = rsResponse.replaceFirst("(?is)(balance\\s*due<\\s*/b\\s*>\\s*<\\s*/td\\s*>\\s*<\\s*td\\s*>)[^<]*", "$1");
		finalResponse = rsResponse;
			
        sb = new StringBuffer(rsResponse);
        Matcher searchByParcelDetailsMat = searchByParcelYrPattern.matcher(sb);
        ArrayList<String> linksSearchByParcelYr = new ArrayList<String>();
        ArrayList<String> linksSearchByParcelYrReferers = new ArrayList<String>();

        while(searchByParcelDetailsMat.find())
        {
        	link = searchByParcelDetailsMat.group(1);
        	link=link.replaceAll("(?is)amp[;]", "");//link=link.replaceAll("%2c", "");
        	String aux = searchByParcelDetailsMat.group(0);
        	if (aux.toLowerCase().contains("special"))
        	{
        		String link_aux = new String(link);
        		link_aux=link_aux.replaceFirst("(?is)[&]owner[^&]+", "");
        		link_aux=link_aux.replaceFirst("(?is)[&]page[^&]+", "");
        		linksSearchByParcelYrReferers.add(link+"&special");//linksSearchByParcelYrReferers.add("PropertyDetails.aspx?Acctno=++++++2340420010020084090000&Acctyear=2007&Acctbtyear=&Owner=SMITH%2c+ERNEST+K&Page=1");
        		linksSearchByParcelYr.add(link_aux);
        	}
        	else if (aux.toLowerCase().contains("certificate"))
        	{
        		String link_aux = new String(link);
        		link_aux=link_aux.replaceFirst("(?is)[&]owner[^&]+", "");
        		link_aux=link_aux.replaceFirst("(?is)[&]page[^&]+", "");
        		linksSearchByParcelYrReferers.add(link+"&certificate");//linksSearchByParcelYrReferers.add("http://taxcol.martin.fl.us/ITM/PropertyDetails.aspx?Acctno=++++++1340400004000075090000&Acctyear=2007&Acctbtyear=&Owner=SMITH%2c+ALEXANDER+%26+LINDA+(T%2fE)&Page=1");
        		linksSearchByParcelYr.add(link_aux);
        	}
        	else
        	{
        		linksSearchByParcelYrReferers.add("");
        		linksSearchByParcelYr.add(link);
        	}
        }

        req.setMethod( HTTPRequest.GET );
        req.setHeader("Host", "taxcol.martin.fl.us");
        for ( int j=0; j<linksSearchByParcelYr.size() ; j++ )
        {
        	link = linksSearchByParcelYr.get(j);
	        link = "https://taxcol.martin.fl.us/ITM/"+link;
        	
        	String aux = linksSearchByParcelYrReferers.get(j);
        	if (aux.equals(""))
        	{
            	req.modifyURL(link);
    	        String pageNo = link.replaceFirst("(?is).*[^a-z]page[^a-z0-9]+([0-9]+).*", "$1");
    	        int pgNo = Integer.parseInt(pageNo)-1;
        		String referer = link.replaceFirst("(?is)(.*[^a-z])page[^a-z0-9]+[0-9]+(.*)","$1"+"Page="+pgNo+"$2");
        		req.setHeader("Referer", referer);
        	}
        	else
        	{
        		link = link.replaceFirst("PropertyDetails.aspx", "PropertyAcctBill.aspx");
        		req.modifyURL(link);
        		req.setHeader("Referer", "https://taxcol.martin.fl.us/ITM/"+aux); 

//        		req.setHeader("Cookie","ASP.NET_SessionId=5phz0w55mnk3fe45iamqp4vg");
        		req.setHeader("User-Agent","Mozilla/5.0 (X11; U; SunOS i86pc; en-US; rv:1.8.1) Gecko/20061024 Firefox/2.0");
        		req.setHeader("Accept","text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,**;q=0.5");
        		req.setHeader("Accept-Language","en-us,en;q=0.5");
        		req.setHeader("Accept-Encoding","gzip,deflate");
        		req.setHeader("Accept-Charset","ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        	}

	        res = HTTPSiteManager.pairHTTPSiteForTSServer( "FLMartinTR", searchId, miServerID).process( req );
	        if (res==null)
	        	return finalResponse;
	        rsResponseDetail = res.getResponseAsString();
	      
		    if (!rsResponseDetail.matches("(?is).*(Some\\s*account\\s*data\\s*is\\s*not\\s*viewable\\s*or\\s*is\\s*unavailable\\s*at\\s*this\\s*time).*") &&
		    		!rsResponseDetail.matches("(?is).*(Your\\s*search\\s*returned\\s*no\\s*results).*") &&
		    		!rsResponseDetail.matches("(?is).*(Tax\\s*Roll\\s*Data\\s*Error).*") &&
		    		!rsResponseDetail.matches("(?is).*(Tax\\s*Roll\\s*Data\\s*Unavailable).*"))
		    {
		    	if (rsResponseDetail.matches("(?is).*current\\s*account\\s*details.*"))
		    	{
			    	rsResponseDetail = rsResponseDetail.replaceFirst("(?is)[^0-9].*(<\\s*table[^>]*>\\s*<\\s*tr[^>]*>\\s*<td[^>]*>[\\sa-z<>]+current\\s*account\\s*details)", "<hr width=\"100%\"/> "+"$1");
			    	rsResponseDetail = rsResponseDetail.replaceFirst("(?is)<\\s*/table\\s*>[\\sa-z<>]+<\\s*table[^>]*>[\\s!@#$%&*=/<>a-z0-9'\\\"]+links\\s*of\\s*interest.*", "\n</table>\n");
		    	}
		    	else if (rsResponseDetail.matches("(?is).*ad\\s*valorem\\s*taxes.*"))
		    	{
		    		rsResponseDetail = rsResponseDetail.replaceFirst("(?is)[^a-z].*<\\s*form[^>]+>\\s*(<\\s*input[^>]+>)?([^<]*<\\s*input[^>]+>[^<]*)?", "<hr width=\"100%\"/>\n");
		    		rsResponseDetail = rsResponseDetail.replaceFirst("(?is)<\\s*/form\\s*>\\s*<\\s*/body\\s*>\\s*<\\s*/html\\s*>.*", "");
		    	}

		    	finalResponse += rsResponseDetail;
		    }
        }
		
        finalResponse = finalResponse.replaceAll("(?is)<\\s*[/]?a[^>]*>", "");
        
		return finalResponse;
	}
	
	
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID)
			throws ServerResponseException
	{
			String sTmp1 = "", sTmp2 = "";
			StringBuffer sb = new StringBuffer();
			String rsResponse = Response.getResult();
			String initialResponse = rsResponse;
	        
			switch (viParseID)
			{
				case ID_SEARCH_BY_NAME:
				case ID_SEARCH_BY_ADDRESS:
				case ID_SEARCH_BY_INSTRUMENT_NO:
				case ID_SEARCH_BY_SUBDIVISION_NAME:
				    if (rsResponse.matches("(?is).*(Some\\s*account\\s*data\\s*is\\s*not\\s*viewable\\s*or\\s*is\\s*unavailable\\s*at\\s*this\\s*time).*"))
				    {
						rsResponse ="<table><th><b>Some account data is not viewable or is unavailable at this time.</b></th></table>";
						Response.getParsedResponse().setOnlyResponse(rsResponse);
				    	return;
				    }
				    else if (rsResponse.matches("(?is).*(Your\\s*search\\s*returned\\s*no\\s*results).*"))
				    {
						rsResponse ="<table><th><b>Your search returned no results.</b></th></table>";
						Response.getParsedResponse().setOnlyResponse(rsResponse);
						return;
				    }
				    else if (rsResponse.matches("(?is).*(Tax\\s*Roll\\s*Data\\s*Error).*"))
				    {
						rsResponse ="<table><th><b>Tax Roll Data Error.</b></th></table>";
						Response.getParsedResponse().setOnlyResponse(rsResponse);
						return;
				    }
				    else if (rsResponse.matches("(?is).*(Tax\\s*Roll\\s*Data\\s*Unavailable).*"))
				    {
						rsResponse ="<table><th><b>Tax Roll Data Unavailable.</b></th></table>";
						Response.getParsedResponse().setOnlyResponse(rsResponse);
						return;
				    }
				    else if (rsResponse.matches("(?is).*the\\s*system\\s*is\\s*temporarily\\s*unavailable.*"))
				    {
						rsResponse ="<table><th><b>The system is temporarily unavailable. Please try later.</b></th></table>";
						Response.getParsedResponse().setOnlyResponse(rsResponse);
						return;
				    }

				    try
				    {
				    	rsResponse = new String (rsResponse.getBytes(),"UTF-8");//ISO-8859-1
				    	initialResponse = new String (initialResponse.getBytes(),"UTF-8");//ISO-8859-1
				    }
				    catch (Exception e)
				    {
				    	e.printStackTrace();
				    }

				    sTmp1 = CreatePartialLink(TSConnectionURL.idPOST);
				    sTmp2 = CreatePartialLink(TSConnectionURL.idGET);

				    String linkNext=null, linkPrev=null, nextStr=null, prevStr=null, prevNext = "", strRefererURL="", strSubdiv="";
				    sb = new StringBuffer(initialResponse);
				    HashMap<String, String> params = HttpUtils.getFormParams( rsResponse , true);
				    
			    	String qry = Response.getQuerry();

			    	if ( viParseID == ID_SEARCH_BY_NAME 
			    			|| viParseID == ID_SEARCH_BY_ADDRESS
			    			|| viParseID == ID_SEARCH_BY_INSTRUMENT_NO
			    			|| viParseID == ID_SEARCH_BY_SUBDIVISION_NAME)
			    	{
					    Matcher lprevMat = lprevPattern.matcher(sb);
					    Matcher lnextMat = lnextPattern.matcher(sb);
					    sTmp1 = CreatePartialLink(TSConnectionURL.idPOST);
					    sTmp2 = CreatePartialLink(TSConnectionURL.idGET);
					    String strOwnerName ="",strYear="";
					    String strNo="", strName="", strType="",strDirection="",strApt="",strCity="";
					    String strTxtDBA="", strTxtYear2="";
					    if (viParseID == ID_SEARCH_BY_NAME)
					    {
						    if (qry.matches("(?is).*txtYear1.*"))
						    {
					    		strYear = qry.replaceFirst ("(?is).*txtYear1=([^&]*).*","$1");
					    		strOwnerName = qry.replaceFirst ("(?is).*txtOwner=([^&]*).*","$1");
					    		strOwnerName = strOwnerName.replaceAll("\\s", "+");
						    }
						    else
						    {
						    	strYear = qry.replaceFirst ("(?is).*[^a-z]Year=([^&]*).*","$1");
						    	strOwnerName = qry.replaceFirst ("(?is).*Owner=([^&]*).*","$1");
						    }
				    		strRefererURL = "&Search=Owner&Owner="+strOwnerName+"&Year="+strYear;
					    }
					    else if (viParseID == ID_SEARCH_BY_ADDRESS)
					    {
					    	if (qry.matches("(?is).*VIEWSTATE.*"))
					    	{
/*Search=Address&Nbr=8754&Name=Little Village&Type=RD&Direction=N&Apt=2091&City=PC&_ctlhdnHideCartText=1&__EVENTTARGET=lnkNext&
__VIEWSTATE=dDw5NTUwMDg1MTQ7dDxwPGw8Um93Q291bnQ7PjtsPGk8NT47Pj47bDxpPDA+O2k8MT47PjtsPHQ8cDxwPGw8VGV4dDs+O2w8Oz4
xmPjs+Pjs+Ozs+Oz4+Oz4+Oz4MD2qW0HWXaMjj+TbJAxU7UeCouQ==&__EVENTARGUMENT=&_ctltxtPopup=*/					    		
						    	strNo = qry.replaceFirst ("(?is).*[^a-z]Nbr=([^&]*).*","$1");
					    		strNo = strNo.replaceAll("\\s", "+");
					    		strName = qry.replaceFirst ("(?is).*[^a-z]Name=([^&]*).*","$1");
					    		strName = strName.replaceAll("\\s", "+");
					    		strType = qry.replaceFirst ("(?is).*[^a-z]Type=([^&]*).*","$1");
					    		strType = strType.replaceAll("\\s", "+");
					    		strDirection = qry.replaceFirst ("(?is).*[^a-z]Direction=([^&]*).*","$1");
					    		strDirection = strDirection.replaceAll("\\s", "+");
					    		strApt = qry.replaceFirst ("(?is).*[^a-z]Apt=([^&]*).*","$1");
					    		strApt = strApt.replaceAll("\\s", "+");
					    		strCity = qry.replaceFirst ("(?is).*[^a-z]City=([^&]*).*","$1");
					    		strCity = strCity.replaceAll("\\s", "+");
					    	}
					    	else
					    	{
/*txtPopup=&txtSitusNbr=8754&txtSitusApt=2091&txtDBA=&dropType=RD&btnSearch3=Search&txtSitusName=Little Village&txtOwner=&txtAccount=&
txtYear1=&txtSitusDirection=N&txtYear2=&dropSitusCode=PC*/
					    		strNo = qry.replaceFirst ("(?is).*[^a-z]txtSitusNbr=([^&]*).*","$1");
					    		strNo = strNo.replaceAll("\\s", "+");
					    		strName = qry.replaceFirst ("(?is).*[^a-z]txtSitusName=([^&]*).*","$1");
					    		strName = strName.replaceAll("\\s", "+");
					    		strType = qry.replaceFirst ("(?is).*[^a-z]dropType=([^&]*).*","$1");
					    		strType = strType.replaceAll("\\s", "+");
					    		strDirection = qry.replaceFirst ("(?is).*[^a-z]txtSitusDirection=([^&]*).*","$1");
					    		strDirection = strDirection.replaceAll("\\s", "+");
					    		strApt = qry.replaceFirst ("(?is).*[^a-z]txtSitusApt=([^&]*).*","$1");
					    		strApt = strApt.replaceAll("\\s", "+");
					    		strCity = qry.replaceFirst ("(?is).*[^a-z]dropSitusCode=([^&]*).*","$1");
					    		strCity = strCity.replaceAll("\\s", "+");
					    	}//PropertySummary.aspx?Search=Address&Nbr=8754&Name=Little+Village&Type=RD&Direction=N&Apt=2091&City=PC]
				    		strRefererURL = "&Search=Address&Nbr="+strNo+"&Name="+strName+"&Type="+strType+"&Direction="+strDirection+"&Apt="+strApt+"&City="+strCity;
					    }
					    else if (viParseID == ID_SEARCH_BY_INSTRUMENT_NO)
					    {
/*txtSitusNbr=&txtDBA=dba Family Name Centers&txtYear2=2007&txtSitusApt=&txtPopup=&btnSearch4=Search&txtOwner=&txtSitusName=&txtYear1=& 
txtAccount=&txtSitusDirection= */
					    	if (qry.matches("(?is).*txtDBA=.*"))
					    	{
						    	strTxtDBA = qry.replaceFirst ("(?is).*[^a-z]txtDBA=([^&]*).*","$1");
						    	strTxtDBA = strTxtDBA.replaceAll("\\s", "+");
						    	strTxtYear2 = qry.replaceFirst ("(?is).*txtYear2=([^&]*).*","$1");
					    	}
					    	else
					    	{
						    	strTxtDBA = qry.replaceFirst ("(?is).*[^a-z]DBA=([^&]*).*","$1");
						    	strTxtDBA = strTxtDBA.replaceAll("\\s", "+");
						    	strTxtYear2 = qry.replaceFirst ("(?is).*[^a-z]Year=([^&]*).*","$1");
					    	}
					    	strRefererURL = "&Search=DBA&DBA="+strTxtDBA+"&Year="+strTxtYear2;
					    }
					    else if (viParseID == ID_SEARCH_BY_SUBDIVISION_NAME)
					    {
						    if (qry.matches("(?is).*txtYear1.*"))
						    {//btnSearch5=Search&dropSubdivision=1371000&txtYear1=&txtPopup=&txtAccount=&txtDBA=&txtOwner=&txtSitusNbr=&txtSitusName=&txtYear2=&txtSitusDirection=&txtSitusApt=
					    		strSubdiv = qry.replaceFirst ("(?is).*dropSubdivision=([^&]*).*","$1");
						    }
						    else
						    {
						    	strSubdiv = qry.replaceFirst ("(?is).*[^a-z]Subdivision=([^&]*).*","$1");
						    }
				    		strRefererURL = "&Search=Subdivision&Subdivision="+strSubdiv;
					    }
					    
				    	if (lnextMat.find())
					    {
				    		nextStr = "lnkNext";//lnextMat.group(1);
				    		if (mSearch.getSearchType() != Search.AUTOMATIC_SEARCH)
				    		{
				    			linkNext = getNextPrevLink(params, sTmp1+"/ITM/PropertySummary.aspx"+strRefererURL, nextStr, false);
				    		}
					    }

					    if (mSearch.getSearchType() != Search.AUTOMATIC_SEARCH)
					    {
						    if (lprevMat.find())
						    {
						    	prevStr = "lnkPrevious";//lprevMat.group(1);
						    	linkPrev = getNextPrevLink(params, sTmp1+"/ITM/PropertySummary.aspx"+strRefererURL, prevStr, true);
						    }
					    	
		                    if( linkPrev != null ) {
		                    	prevNext = linkPrev;
		                    }

					    	if( linkNext != null ) {
		                    	prevNext += linkNext;
		                    }
	 				    }
			    	}
				    if (rsResponse.matches("(?is)[^0-9].*help[\\s0-9a-z/<>&=;'\\\"]+<\\s*[/]tr\\s*>\\s*<\\s*[/]table\\s*>\\s*<\\s*table[^>]*>.*"))
				    {
				    	rsResponse = rsResponse.replaceFirst("(?is)[^0-9].*help[\\s0-9a-z/<>&=;'\\\"]+<\\s*[/]tr\\s*>\\s*<\\s*[/]table\\s*>\\s*<\\s*table[^>]*>",
					    		"<table border=\"1\" width=\"900\" cellpadding=\"2\" cellspacing=\"1\">\n" + prevNext);
				    }
				    else
				    {
					    rsResponse = rsResponse.replaceFirst("(?is)([^0-9].*<\\s*[/]table\\s*>\\s*<\\s*table[^>]*>)([<>\\s=#;/&0-9a-z%\\\"]+search\\s+by)",
					    		"<table border=\"1\" width=\"900\" cellpadding=\"2\" cellspacing=\"1\">\n" +prevNext+"$2");
				    }
				    rsResponse = rsResponse.replaceAll("(?is)<\\s*a[^>]*>\\s*prior\\s*payments\\s*due[^<]*<\\s*/a[^>]*>","");
				    rsResponse = rsResponse.replaceFirst("(?is)<\\s*[/]table\\s*>\\s*<\\s*table[^>]*>\\s*<\\s*tr\\s*>.*","\n</table>\n");
				    rsResponse = rsResponse.replaceAll("(?is)<\\s*input[^>]*payonline[^>]*>\\s*<\\s*label[^>]+payonline[^>]*>\\s*pay\\s*online<\\s*[/]label\\s*>","");
				    rsResponse = rsResponse.replaceAll("(?is)<\\s*img[^>]+add\\s+to\\s+cart[^>]*>","");
				    rsResponse = rsResponse.replaceAll("(?is)\\s*<\\s*div[^>]*>\\s*add[^<]*to[^<]*cart\\s*<\\s*[/]div\\s*>","");
				    rsResponse = rsResponse.replaceAll("(?is)<\\s*td[^>]*>","<td>");
				    boolean changed = true;
				    int pos=0;
				    
				    String referer_aux="";
				    qry=Response.getQuerry();
				    if ( viParseID == ID_SEARCH_BY_INSTRUMENT_NO )
				    {
				    	String dbaName=qry.replaceFirst("(?is).*txtDBA=([^&]*).*", "$1");
				    	referer_aux = "&refererDBA="+dbaName;
				    }
				    while (changed)
				    {
					    changed=false;
					    sb = new StringBuffer(rsResponse);
				    	Matcher searchByParcelDetailsMat = searchByParcelDetailsPattern.matcher(sb);
				    	if (searchByParcelDetailsMat.find(pos))
				    	{
					    	changed=true;
				    		String detailsLnk = searchByParcelDetailsMat.group(1);
					    	String detailsLnkAux = detailsLnk;
					    	detailsLnk = detailsLnk.replaceAll("(?is)amp[;]", "");
					    	String start = rsResponse.substring(0, rsResponse.indexOf(detailsLnkAux));
					    	String end = rsResponse.substring(rsResponse.indexOf(detailsLnkAux)+detailsLnkAux.length());
					    	if (!detailsLnk.matches("(?is).*[^0-9][0-9]{13,}[^0-9].*"))
					    	{
					    		end = end.replaceFirst("^\\\">\\s*", "\"> <b>Tangible</b> - ");
					    		//detailsLnk = detailsLnk.replaceFirst("([0-9]{5,12})","<b>"+"$1"+" - TANGIBLE</b>");
					    	}
				    		detailsLnk = sTmp2+"/ITM/"+detailsLnk+referer_aux.replaceAll("\\s", "+");
					    	pos = rsResponse.indexOf(detailsLnkAux)+detailsLnk.length();
					    	rsResponse = start+detailsLnk+end;
				    	}
				    }
				    
				    if (viParseID==ID_SEARCH_BY_NAME || viParseID==ID_SEARCH_BY_SUBDIVISION_NAME)
				    {
				    	rsResponse = rsResponse.replaceAll("(?is)<\\s*tr[^>]*>\\s*<\\s*td[^>]*>(\\s*<\\s*b\\s*>\\s*<\\s*span[^>]*>\\s*Owner[^<]+Name\\s*<\\s*/span\\s*>)",
				    																"<tr><b></b><td>\n"+"$1");
				    }
				    else if (viParseID==ID_SEARCH_BY_ADDRESS)
				    {
				    	rsResponse = rsResponse.replaceAll("(?is)<\\s*tr[^>]*>\\s*<\\s*td[^>]*>(\\s*<\\s*b\\s*>\\s*<\\s*span[^>]*>\\s*Situs\\s*Address)",
								"<tr><b></b><td>\n"+"$1");
				    }
				    else if (viParseID==ID_SEARCH_BY_INSTRUMENT_NO)
				    {
				    	rsResponse = rsResponse.replaceAll("(?is)<\\s*tr[^>]*>\\s*<\\s*td[^>]*>(\\s*<\\s*b\\s*>\\s*<\\s*span[^>]*>\\s*DBA\\s*Name\\s*<\\s*/span\\s*>)",
								"<tr><b></b><td>\n"+"$1");
				    }
				    
				    rsResponse = rsResponse.replaceAll("(?is)<\\s*tr\\s*>\\s*<\\s*td\\s*>[&nbsp;\\s]*<\\s*/td\\s*>\\s*<\\s*/tr\\s*>","<tr><td>&nbsp;</td></tr>");
				    
	                if(mSearch.getSearchType() == Search.AUTOMATIC_SEARCH && nextStr != null )
	                {
	                	String linkNextForAutomatic = getNextLinkForAutomatic(params, sTmp1+"/ITM/PropertySummary.aspx"+strRefererURL, nextStr);
	                	Response.getParsedResponse().setNextLink( linkNextForAutomatic );
	                }
					parser.Parse(
							Response.getParsedResponse(),
							rsResponse,
							Parser.PAGE_ROWS,
							getLinkPrefix(TSConnectionURL.idGET),
							TSServer.REQUEST_SAVE_TO_TSD);
					break;
				case ID_SEARCH_BY_PARCEL:
				    if (rsResponse.matches("(?is).*(Some\\s*account\\s*data\\s*is\\s*not\\s*viewable\\s*or\\s*is\\s*unavailable\\s*at\\s*this\\s*time).*"))
				    {
						rsResponse ="<table><th><b>Some account data is not viewable or is unavailable at this time.</b></th></table>";
						Response.getParsedResponse().setOnlyResponse(rsResponse);
				    	return;
				    }
				    else if (rsResponse.matches("(?is).*(Your\\s*search\\s*returned\\s*no\\s*results).*"))
				    {
						rsResponse ="<table><th><b>Your search returned no results.</b></th></table>";
						Response.getParsedResponse().setOnlyResponse(rsResponse);
						return;
				    }
				    else if (rsResponse.matches("(?is).*(Tax\\s*Roll\\s*Data\\s*Error).*"))
				    {
						rsResponse ="<table><th><b>Tax Roll Data Error.</b></th></table>";
						Response.getParsedResponse().setOnlyResponse(rsResponse);
						return;
				    }
				    else if (rsResponse.matches("(?is).*(Tax\\s*Roll\\s*Data\\s*Unavailable).*"))
				    {
						rsResponse ="<table><th><b>Tax Roll Data Unavailable.</b></th></table>";
						Response.getParsedResponse().setOnlyResponse(rsResponse);
						return;
				    }
				    else if (rsResponse.matches("(?is).*the\\s*system\\s*is\\s*temporarily\\s*unavailable.*"))
				    {
						rsResponse ="<table><th><b>The system is temporarily unavailable. Please try later.</b></th></table>";
						Response.getParsedResponse().setOnlyResponse(rsResponse);
						return;
				    }

				    try
				    {
				    	rsResponse = new String (rsResponse.getBytes(),"UTF-8");//ISO-8859-1
				    	initialResponse = new String (initialResponse.getBytes(),"UTF-8");//ISO-8859-1
				    }
				    catch (Exception e)
				    {
				    	e.printStackTrace();
				    }

				    sTmp1 = CreatePartialLink(TSConnectionURL.idPOST);
				    sTmp2 = CreatePartialLink(TSConnectionURL.idGET);
				    
				    rsResponse = rsResponse.replaceAll("(?is)<\\s*td[^>]+>[^<]*<\\s*a[^>]+cart[^>]+>[^<]*<\\s*img[^>]+add\\s*to\\s*cart[^>]+>[^<]*<\\s*div[^>]+>[^<]+cart[^<]*<\\s*/div\\s*>[^<]*<\\s*/a\\s*>[^<]*<\\s*/td\\s*>","");
				    rsResponse = rsResponse.replaceAll("(?is)<\\s*a[^>]+>[^<]*<\\s*img[^>]+>[^<]*<\\s*div[^>]*>[^<]+cart[^<]*<\\s*/div\\s*>[^<]*<\\s*/a\\s*>","");
				    rsResponse = rsResponse.replaceAll("(?is)<\\s*input[^>]+chk[^>]+>","");
				    rsResponse = rsResponse.replaceFirst("(?is)[^0-9].*help[\\s0-9a-z/<>&=;'\\\"]+<\\s*/tr\\s*>\\s*<\\s*/table\\s*>\\s*<\\s*table[^>]*>",
				    		"<table border=\"1\" width=\"900\" cellpadding=\"2\" cellspacing=\"1\">\n"/* + prevNext*/);
				    rsResponse = rsResponse.replaceFirst("(?is)<\\s*/table\\s*>\\s*<\\s*table[^>]*>\\s*<\\s*tr\\s*>.*","\n</table>\n");
				    rsResponse = rsResponse.replaceAll("(?is)<\\s*input[^>]*payonline[^>]*>\\s*<\\s*label[^>]+payonline[^>]*>\\s*pay\\s*online<\\s*/label\\s*>","");
				    rsResponse = rsResponse.replaceAll("(?is)<\\s*td[^>]*>\\s*<\\s*a[^>]+>\\s*prior\\s*payments\\s*due[^<]+view\\s*detail[^<]*<\\s*/a\\s*>\\s*<\\s*/td\\s*>","");
				    rsResponse = rsResponse.replaceAll("(?is)<\\s*td[^>]*>","<td>");
				    boolean changed_aux = true;
				    int pos_aux=0;

				    while (changed_aux)
				    {
				    	changed_aux=false;
					    sb = new StringBuffer(rsResponse);
				    	Matcher searchByParcelDetailsMat = searchByParcelDetailsPattern.matcher(sb);
				    	if (searchByParcelDetailsMat.find(pos_aux))
				    	{
				    		changed_aux=true;
				    		String detailsLnk = searchByParcelDetailsMat.group(1);
					    	String detailsLnkAux = detailsLnk;
					    	detailsLnk = detailsLnk.replaceAll("(?is)amp[;]", "");
					    	String start = rsResponse.substring(0, rsResponse.indexOf(detailsLnkAux));
					    	String end = rsResponse.substring(rsResponse.indexOf(detailsLnkAux)+detailsLnkAux.length());
					    	if (!detailsLnk.matches("(?is).*[^0-9][0-9]{13,}[^0-9].*"))
					    	{
					    		end = end.replaceFirst("^\\\">\\s*", "\"> <b>Tangible</b> - ");
					    		//detailsLnk = detailsLnk.replaceFirst("(?is).*[^0-9]([0-9]{5,12})[^0-9].*","<b>"+"$1"+" - TANGIBLE</b>");
					    	}
					    	detailsLnk = sTmp2+"/ITM/"+detailsLnk;					    		
					    	pos_aux = rsResponse.indexOf(detailsLnkAux)+detailsLnk.length();
					    	rsResponse = start+detailsLnk+end;
				    	}
				    }

			    	rsResponse = rsResponse.replaceAll("(?is)<\\s*tr[^>]*>\\s*<\\s*td[^>]*>(\\s*<\\s*b\\s*>\\s*<\\s*span[^>]*>\\s*Owner[^<]+Name\\s*<\\s*/span\\s*>)",
							"<tr><b></b><td>\n"+"$1");
			    	
			    	rsResponse = rsResponse.replaceAll("(?is)<\\s*tr\\s*>\\s*<\\s*td\\s*>[&nbsp;\\s]*<\\s*/td\\s*>\\s*<\\s*/tr\\s*>","<tr><td>&nbsp;</td></tr>");
				    
				    parser.Parse(
							Response.getParsedResponse(),
							rsResponse,
							Parser.PAGE_ROWS,
							getLinkPrefix(TSConnectionURL.idGET),
							TSServer.REQUEST_SAVE_TO_TSD);
					break;
				case ID_DETAILS:
				    if (rsResponse.matches("(?is).*(Some\\s*account\\s*data\\s*is\\s*not\\s*viewable\\s*or\\s*is\\s*unavailable\\s*at\\s*this\\s*time).*"))
				    {
						rsResponse ="<table><th><b>Some account data is not viewable or is unavailable at this time.</b></th></table>";
						Response.getParsedResponse().setOnlyResponse(rsResponse);
						return;
				    }
				    else if (rsResponse.matches("(?is).*(Your\\s*search\\s*returned\\s*no\\s*results).*"))
				    {
						rsResponse ="<table><th><b>Your search returned no results.</b></th></table>";
						Response.getParsedResponse().setOnlyResponse(rsResponse);
						return;
				    }
				    else if (rsResponse.matches("(?is).*(Tax\\s*Roll\\s*Data\\s*Error).*"))
				    {
						rsResponse ="<table><th><b>Tax Roll Data Error.</b></th></table>";
						Response.getParsedResponse().setOnlyResponse(rsResponse);
						return;
				    }
				    else if (rsResponse.matches("(?is).*(Tax\\s*Roll\\s*Data\\s*Unavailable).*"))
				    {
						rsResponse ="<table><th><b>Tax Roll Data Unavailable.</b></th></table>";
						Response.getParsedResponse().setOnlyResponse(rsResponse);
						return;
				    }
				    else if (rsResponse.matches("(?is).*the\\s*system\\s*is\\s*temporarily\\s*unavailable.*"))
				    {
						rsResponse ="<table><th><b>The system is temporarily unavailable. Please try later.</b></th></table>";
						Response.getParsedResponse().setOnlyResponse(rsResponse);
						return;
				    }
				    rsResponse  = Tidy.tidyParse(rsResponse , null);
				    
				    try
				    {
				    	rsResponse = new String (rsResponse.getBytes(),"UTF-8");//ISO-8859-1
				    	initialResponse = new String (initialResponse.getBytes(),"UTF-8");//ISO-8859-1
				    }
				    catch (Exception e)
				    {
				    	e.printStackTrace();
				    }
				    
	                String keyCode = "File";
				    if ((!downloadingForSave && !rsResponse.contains("<hr width=\"100%\"/>")) ||
							 mSearch.getSearchType() == Search.AUTOMATIC_SEARCH)
					{
					    rsResponse = createFinalPage(rsResponse, initialResponse);
					    rsResponse=rsResponse.replaceAll("(?is)<input[^>]*type=[\"']{1}hidden[^>]*>", "");
					    //get detailed document addressing code
		                sb = new StringBuffer(rsResponse);
		              
					    Matcher acct = pidMatch1.matcher(sb);
					    if (acct.find())
					    {
					    	keyCode = acct.group(1).replaceAll("[^0-9]+", "");
					    } else {
					    	acct = pidMatch2.matcher(sb);
					    	if(acct.find()) {
					    		keyCode = acct.group(1).replaceAll("[^0-9]+", "");
					    	}
					    }
					    keyCode = keyCode.replaceAll("(?is)[\\.-/]+", "");
					}
				    else if (downloadingForSave)
				    {//keyCode = rsResponse.replaceFirst("(?is).*dummy[^0-9]+([0-9]{13,}).*", "$1");
		                sb = new StringBuffer(rsResponse);
		                Matcher acct = pidMatch1.matcher(sb);
					    if (acct.find())
					    {
					    	keyCode = acct.group(1).replaceAll("[^0-9]+", "");
					    } else {
					    	acct = pidMatch2.matcher(sb);
					    	if(acct.find()) {
					    		keyCode = acct.group(1).replaceAll("[^0-9]+", "");
					    	}
					    }
					    keyCode = keyCode.replaceAll("(?is)[\\.-/]+", "");
				    }
					
	                if (!downloadingForSave)
					{
	

	    				String qry_aux = Response.getRawQuerry();
//						String qry_aux = "";
						if (qry_aux.contains("&qry"))
						{
			
							qry_aux = qry_aux.substring(0, qry_aux.indexOf("&qry"));
							qry_aux = qry_aux.replaceAll("(?is)[%]7C", "|");
							qry_aux = qry_aux.replaceAll("(?is)[%]3D", "=");
						}

						qry_aux = "dummy=" + keyCode + "&" + qry_aux;
						String originalLink = sAction + "&" + qry_aux;
						originalLink=originalLink.replaceAll("[+]+", "");
						String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
						HashMap<String, String> data = new HashMap<String, String>();
						loadDataHash(data);
						if (isInstrumentSaved(keyCode, null, data)) 
						{
						    rsResponse += CreateFileAlreadyInTSD();
						}
						else 
						{
							
							mSearch.addInMemoryDoc(sSave2TSDLink, rsResponse);
							rsResponse = addSaveToTsdButton(rsResponse, sSave2TSDLink, viParseID);

						}

						Response.getParsedResponse().setPageLink(
							new LinkInPage(
								sSave2TSDLink,
								originalLink,
								TSServer.REQUEST_SAVE_TO_TSD));
						parser.Parse(
							Response.getParsedResponse(),
							rsResponse,
							Parser.NO_PARSE);
	    				downloadingForSave = false;
					} 
					else
					{//for html
						msSaveToTSDFileName = keyCode + ".html";

						Response.getParsedResponse().setFileName(
							getServerTypeDirectory() + msSaveToTSDFileName);

						msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();

					    parser.Parse(Response.getParsedResponse(), rsResponse, Parser.PAGE_DETAILS);
						
					}

					break;
				case ID_GET_LINK :
					if (Response.getQuerry().contains("Search=Owner"))
						ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);						
					else if (Response.getQuerry().contains("Search=Address"))
						ParseResponse(sAction, Response, ID_SEARCH_BY_ADDRESS);
					else if (Response.getQuerry().contains("Search=DBA"))
						ParseResponse(sAction, Response, ID_SEARCH_BY_INSTRUMENT_NO);
					else if (Response.getQuerry().contains("Search=Subdivision"))
						ParseResponse(sAction, Response, ID_SEARCH_BY_SUBDIVISION_NAME);
					else if (sAction.contains("PropertyDetails.aspx"))
						ParseResponse(sAction, Response, ID_DETAILS);
					else
						ParseResponse(sAction, Response, ID_SEARCH_BY_PARCEL);						
					break;
				case ID_SAVE_TO_TSD :
					if (sAction.contains("PropertySummary.aspx"))
						ParseResponse(sAction, Response, ID_SEARCH_BY_PARCEL);
					else
					{// on save
						downloadingForSave = true;
						ParseResponse(sAction, Response, ID_DETAILS);
						downloadingForSave = false;
					}
					break;
				default:
					break;
			}
		}

	protected void loadDataHash(HashMap<String, String> data) {
		if (data != null) {
			data.put("type", "CNTYTAX");
		}
	}
	
	private static String getNextPrevLink ( HashMap<String, String> pageParams, String postPartialLink, String eventTarget, boolean previous ){
    	String prevNextLink = null;
    	String formName = "nextForm";

    	if (previous) {
    		//generate prev link
    		formName = "prevForm";
    	}
    	
		//build next link form
		prevNextLink = "\n<form name=\"" + formName + "\" id=\"" + formName + "\" method=\"POST\" action=\"" + postPartialLink + "\">\n";
		prevNextLink += "<input type=\"hidden\" name=\"__EVENTTARGET\" id=\"__EVENTTARGET\" value=\"" + eventTarget/*pageParams.get( "__EVENTTARGET" )*/ + "\">\n";
		pageParams.remove("__EVENTTARGET");
		
		Set<Map.Entry<String,String>> entries = pageParams.entrySet();
		Iterator<Map.Entry<String, String>> iter = entries.iterator();		
		while (iter.hasNext())
		{
			Map.Entry<String,String> entry = iter.next();
			
		    String paramName = entry.getKey();
		    if (!paramName.startsWith("__"))
		    	paramName = "_ctl"+paramName;
		    prevNextLink += "<input type=\"hidden\" name=\""+
		    	paramName + "\" id=\"" + paramName + "\" value=\"" + pageParams.get(entry.getKey()) + "\">\n";
		}
		
		prevNextLink += "</form>\n";
		prevNextLink += "<a href=\"javascript:document.getElementById('" + formName + "').submit();\">" + (previous ? "<b>Prev</b>" : "<b>Next</b>") + "</a>\n";
    	
    	return prevNextLink;
    }
	
    private static String getNextLinkForAutomatic ( HashMap<String, String> pageParams, String postPartialLink, String eventTarget) {
    	String nextLinkForAutomatic = null;
    	
    	//build Next link for Automatic
		nextLinkForAutomatic =   "<a href=\""+postPartialLink + "&";
		nextLinkForAutomatic += "__EVENTTARGET="+eventTarget;
		pageParams.remove("__EVENTTARGET");
		
		Set<Map.Entry<String,String>> entries = pageParams.entrySet();
		Iterator<Map.Entry<String, String>> iter = entries.iterator();		
		while (iter.hasNext())
		{
			Map.Entry<String,String> entry = iter.next();
			
		    String paramName = entry.getKey();
		    nextLinkForAutomatic += "&"+paramName+"="+pageParams.get(entry.getKey());
/*		    if (!paramName.startsWith("__"))
		    	paramName = "_ctl"+paramName;
		    nextLinkForAutomatic += "<input type=\"hidden\" name=\""+
		    	paramName + "\" id=\"" + paramName + "\" value=\"" + pageParams.get(entry.getKey()) + "\">\n";*/
		}
		
		nextLinkForAutomatic += "\">Next</a>";
    	
    	return nextLinkForAutomatic;
    }
	
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) 
	{
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		boolean emptyStreet = "".equals( sa.getAtribute( SearchAttributes.P_STREETNAME ) );
		boolean emptyPid = "".equals( sa.getAtribute( SearchAttributes.LD_PARCELNO ) );
		TSServerInfoModule m;
		
		if( !emptyPid )
		{//Search by Account Number / Parcel ID
			
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			l.add(m);
		}
		
		if( !emptyStreet )
		{//Search by Property Address
	    	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
	    	m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_ST_NAME_FAKE);
	    	m.addFilter(AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d));
	    	l.add(m);
		}
		
		if( hasOwner() )
        {//Search by Owner Name
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			
			m.clearSaKeys();
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			
			m.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(
					SearchAttributes.OWNER_OBJECT, searchId, m));
			
			m.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
			
			m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
			.getConfigurableNameIterator(m, searchId, new String[] {"L;F;","L;f;", "L;M;", "L;m;"});
			
			m.addIterator(nameIterator);
			
			l.add(m);
        }

		serverInfo.setModulesForAutoSearch(l);
	}

	protected String getFileNameFromLink(String url)
	{
		String keyCode = "File";
		if (url.contains("dummy="))
			keyCode = org.apache.commons.lang.StringUtils.substringBetween(
				url,
				"dummy=",
				"&");
		
		return keyCode+".html";
	}
	
	@Override
	public TSConnectionURL getTSConnection() {
		TSConnectionURL conn = super.getTSConnection();
		conn.setRemoveParamValueQuotes(false);
		return conn;
	} 
}


