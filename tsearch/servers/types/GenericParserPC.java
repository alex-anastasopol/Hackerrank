package ro.cst.tsearch.servers.types;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.io.FileUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.NotFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableHeader;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.RawResponseWrapper;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class GenericParserPC extends TSServer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private boolean downloadingForSave = false;   
	private String HIDDEN_PARAM_SAVE_NAME = "savedLinkInAts";
	
	private static HashMap<String, ArrayList<String>> STATES_COUNTIES = new HashMap<String, ArrayList<String>>();
	static {
		ArrayList<String> ar_counties = new ArrayList<String>();
		ar_counties.add("Benton");
		ar_counties.add("Carroll");
		ar_counties.add("Pulaski");
		STATES_COUNTIES.put("AR", ar_counties);
	}

	public GenericParserPC(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	public GenericParserPC(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {        
        super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
    }
	
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
        String rsResponse = Response.getResult();
        ParsedResponse parsedResponse = Response.getParsedResponse();
        rsResponse = rsResponse.replace("", "");		// fix for bug #1465
        rsResponse = rsResponse.replace("[C[", " ");
        //rsResponce=StringUtils.tidyParse(rsResponce);
        String initialResponse = rsResponse;
        String linkStart;
        int istart = -1, iend = -1;
        
        String lastURIAsString = "";
        if(Response.getLastURI() != null) {
        	try {
				lastURIAsString = Response.getLastURI().getPathQuery();
			} catch (URIException e) {
				logger.error(searchId + "Problem getting lastURI path.", e);
			}
        }
        
        if(rsResponse.contains("Form error:")) {
        	String error = RegExUtils.getFirstMatch("<span class=\"error\">(.*?)</span>", rsResponse, 1);
        	if(StringUtils.isNotEmpty(error)) {
        		parsedResponse.setError("Something is wrong. Please try again.<br>Site Message is: " + error);
    			return;
        	}
        }

        if(rsResponse.contains("You must enter some search criteria to get results.")) {
        	parsedResponse.setError("Something is wrong. Please try again.<br>Site Message is: You must enter some search criteria to get results.");
			return;
        }
        
        switch (viParseID) {
        case ID_SEARCH_BY_NAME:
        {
			
			StringBuilder outputTable = new StringBuilder();
			
			Collection<ParsedResponse> smartParsedResponses = 
					smartParseIntermediary(Response, rsResponse, outputTable);
			if(smartParsedResponses != null && smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(rsResponse);
				
	            if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
	            	String header = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
	            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"5\">\n";
	            	header += parsedResponse.getHeader();
	            	
	            	parsedResponse.setHeader(header);
	            	parsedResponse.setFooter(parsedResponse.getFooter() + "\n</table>");
	            	
	            	
	            	Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
	            	if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
	            		parsedResponse.setFooter(parsedResponse.getFooter() + 
	            				CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer)numberOfUnsavedDocument));
	            	} else {
	            		parsedResponse.setFooter(parsedResponse.getFooter() + 
	            				CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1));
	            	}
	            	
	            }
	            
				
			} else {
				parsedResponse.setResultRows(new Vector<ParsedResponse>());
				parsedResponse.setError("<font color=\"red\">No results found.</font>");
				return;
			}	
		}
        	break;
        
        case ID_SEARCH_BY_ADDRESS:
        	
        	linkStart = CreatePartialLink(TSConnectionURL.idGET);
        	
        	istart = rsResponse.indexOf("<TABLE BORDER=0 BGCOLOR=#F5F5F5>");
        	if (istart == -1)
                return;
        	iend = rsResponse.indexOf("</TABLE>", istart) + 8;
            
        	String sNext = null, sPrev = null;
        	if (rsResponse.indexOf("disp_page") != -1) {
		    	sNext = "http://pacer.uspci.uscourts.gov" + rsResponse.replaceAll("(?s)(?i).*Next.*?a\\shref=\"(.*?)\">.*", "$1");
		        if (sNext.indexOf("disp_page") != -1) {
			        
		            sNext = sNext.substring(sNext.indexOf("/cgi"), sNext.length()).replaceAll("\\?", "&");
			        sNext = "<a href='" + linkStart + sNext + "'>Next</a>";
		        } else {
		            sNext = null;
		        }
		        
		        
		        int i = rsResponse.indexOf("-- Previous --");
		        int j = rsResponse.indexOf("-- Next --");
		        String text = rsResponse.substring(i, j);
		        
		        sPrev = "http://pacer.uspci.uscourts.gov" + 
		        	text.replaceAll("(?s)(?i).*-- Previous --.*?a href=\"(/cgi-bin/disp_page[^\"]*).*", "$1");
		        
		        if (sPrev.indexOf("disp_page") != -1) {
		            sPrev = sPrev.substring(sPrev.indexOf("/cgi"), sPrev.length()).replaceAll("\\?", "&");
		            sPrev = "<a href='" + linkStart + sPrev + "'>Previous</a>";
		        } else {
		            sPrev = null;
		        }
        	}
	        		
            rsResponse = rsResponse.substring(istart, iend);
            
            rsResponse = rsResponse.replaceAll("(?s)(<A HREF=)(https{0,1}:.*?>.*?</A>)", "$1" + linkStart + "$2");
            
            //
            rsResponse = rsResponse.replaceAll("(?s)<A href=\"http:.*?\">(\\w+)</A>", "$1");
            rsResponse = rsResponse.replaceAll("\\.pl\\?", ".pl&");
            // adaugam eventualele link-uri de next
            rsResponse += "<br>";
            
            if (sPrev != null) {
            	rsResponse += sPrev;
            }

            if (sNext != null) {
            	if (sPrev != null){
            		rsResponse += "&nbsp;&nbsp;&nbsp;&nbsp;";
            	}                
            	rsResponse += sNext;
            }
            
            /*
            Matcher tnwbkMatcher = tnwbkPattern.matcher( rsResponce );
            while( tnwbkMatcher.find() )
            {
                String urlToReplace = tnwbkMatcher.group( 1 );
                String caseNo = tnwbkMatcher.group( 2 );
                
                Pattern tnwbkePattern = Pattern.compile( "(?si)<tr>\\s*<td>\\s*\\d+</td>\\s*<td>[^<]*</td>\\s*<td>tnwbke</td>\\s*<td><a href=([^>]*)>" + caseNo + "</a></td>.*?</tr>" );
                Matcher tnwbkeMatcher = tnwbkePattern.matcher( rsResponce );
                if( tnwbkeMatcher.find() )
                {
                    rsResponce = rsResponce.replace( urlToReplace, tnwbkeMatcher.group( 1 ) );
                }
                else
                {
                    rsResponce = rsResponce.replace( "<A HREF=" + urlToReplace + ">" + caseNo + "</A>", caseNo );
                }
            }
            */
            
            // se parseaza.
            parser.Parse(Response.getParsedResponse(), rsResponse, Parser.PAGE_ROWS,
                    getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
            
            Response.getParsedResponse().setNextLink(sNext);
            
        	break;
        	
        
        case ID_INTERMEDIARY:
        {
        	String secondIntermediaryPage = getSecondIntermediaryPage(Response, sAction + "&" + Response.getRawQuerry());
        	if(StringUtils.isEmpty(secondIntermediaryPage)) {
        		parsedResponse.setError("Could not parse second intermediay page, the one with links");
        		Response.setError(parsedResponse.getError());
        		parsedResponse.setResponse("");
        		Response.setResult("");
        	} else {
        		parsedResponse.setResponse("");
        		Response.setResult("");
        		parsedResponse.setOnlyResponse(
        				"<b>Click on links to get more info and save a document.</b><br>" +
        				"If a documents exists, the extra info will be added to it.<br><br>" +
        				secondIntermediaryPage);
        		getSearch().setAdditionalInfo(sAction, secondIntermediaryPage);
        	}
        }
        	break;
        	
        case ID_DETAILS:
        case ID_SAVE_TO_TSD:
        {	
        	String detailedPage = getDetailedPage(rsResponse, sAction);
        	if(StringUtils.isEmpty(detailedPage)) {
        		parsedResponse.setError("Could not parse detailed page.");
        		Response.setError(parsedResponse.getError());
        		parsedResponse.setResponse("");
        		Response.setResult("");
        	} else {
        		
        		if(viParseID == ID_SAVE_TO_TSD) {
    				msSaveToTSDFileName = "keyCode.html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = "<form>" + detailedPage + CreateFileAlreadyInTSD();                
	                smartParseDetails(Response,detailedPage, sAction);
    			} else {
	        		getSearch().setAdditionalInfo(sAction, detailedPage);
	        		String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + sAction;
	        		
	        		detailedPage = addSaveToTsdButton(detailedPage, sSave2TSDLink, viParseID);
	        		
	        		parsedResponse.setResponse(detailedPage);
	        		parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink, sAction, TSServer.REQUEST_SAVE_TO_TSD));
    			}
        	}
        }
        	break;
        	
        case ID_DETAILS2:
            
            String aliasResponse = "", caseSummaryResponse = "", noticeOfBankrupcyResponse = "", statusResponse = "";
            
            String instrNum = "unknown";
            
            
	        if (rsResponse.indexOf("Docket Summary") != -1 || sAction.indexOf("reports.pl") != -1) {
	            
	        	if (rsResponse.indexOf("Can't open docket sheet for") != -1) {
	        		Response.getParsedResponse().setError(rsResponse);
					return;
	        	}
	        	
	            instrNum = rsResponse.replaceFirst("(?s).*CASENUM=([^&>]*).*", "$1");
	            
	            istart = rsResponse.indexOf("Pages");
	            istart = rsResponse.indexOf("<center>", istart);
	            rsResponse = rsResponse.substring(istart, rsResponse.length());
	            if (rsResponse.indexOf("<HR>")!=-1) rsResponse = rsResponse.substring(0, rsResponse.indexOf("<HR>"));
	            rsResponse = rsResponse.replaceAll("<pre>", "</pre><pre>") + "</pre>";
	            rsResponse = rsResponse.replaceFirst("</pre>", "");
	            rsResponse = rsResponse.replaceAll("</html>","");
	            rsResponse = rsResponse.replaceAll("</body>","");
	            int is = -1;
	            int ie = -1;
	            ie = rsResponse.lastIndexOf("</pre>");
	            is = rsResponse.lastIndexOf("</pre>")+"</pre>".length();
	            rsResponse = rsResponse.substring(0,ie) + rsResponse.substring(is,rsResponse.length());  
	            
	            aliasResponse = rsResponse;
	            
	               
				
	        } else {
	            
	        	//long searchId = InstanceManager.getCurrentInstance().getCrtSearchContext().getSearchID();
	        	
	            String host = sAction.replaceAll(".*://(.*?)/cgi.*", "$1");
        
		        instrNum = rsResponse.replaceAll("(?s).*\\.pl\\?(\\d+).*", "$1");
		        String siteResponse = "";
		        
	            // click pe alias
	            String link = "https://" + host + "/cgi-bin/qryAlias.pl?" + instrNum;
                HTTPRequest req = new HTTPRequest( link ); 
                req.setHeader("Host", host);
                req.setHeader("Referer", sAction);
                
                //siteResponse = performRequest(req);
                
                if (siteResponse.indexOf("<CENTER>") < siteResponse.length()){
                	siteResponse = siteResponse.substring(siteResponse.indexOf("<CENTER>"), siteResponse.length());
    				siteResponse = siteResponse.substring(0, siteResponse.indexOf("<HR>"));
    				siteResponse = siteResponse.replaceAll("</CENTER></FONT>", "</FONT></CENTER>");
    				
    				siteResponse = siteResponse.replace("", "");		// fix for bug #1465
    				siteResponse = siteResponse.replace("[C[", " ");
    	
    				aliasResponse = siteResponse;
                	
                }
				
		        // click pe CaseSummary
				link = "https://" + host + "/cgi-bin/qrySummary.pl?" + instrNum;
                req = new HTTPRequest( link ); 
                req.setHeader("Host", host);
                req.setHeader("Referer", sAction);
                
                //siteResponse = performRequest(req);
                
				istart = siteResponse.indexOf("</CENTER>") + 13;
				siteResponse = siteResponse.substring(istart, siteResponse.length());
				siteResponse = siteResponse.substring(0, siteResponse.indexOf("<HR>"));
				siteResponse = siteResponse.replaceAll("</CENTER></FONT>", "</FONT></CENTER>");
				
				siteResponse = siteResponse.replace("", "");		// fix for bug #1465
				siteResponse = siteResponse.replace("[C[", " ");
				
				caseSummaryResponse = siteResponse;
					        
		        // click pe NoticeOfBankruptcyCaseFiling
		        if (rsResponse.indexOf("NoticeOfFiling.pl") != -1) {
		           		        
		            link = "https://" + host + "/cgi-bin/NoticeOfFiling.pl?" + instrNum;
		            
                    req = new HTTPRequest( link ); 
                    req.setHeader("Host", host);
                    req.setHeader("Referer", sAction);
	
                    //siteResponse = performRequest(req);
                    
					istart = siteResponse.indexOf("<p align=center>");
					if (istart == -1) 
						istart = siteResponse.indexOf("<p align=\"center\">");
					siteResponse = siteResponse.substring(istart, siteResponse.length());
					siteResponse = siteResponse.substring(0, siteResponse.indexOf("<HR>"));
					siteResponse = siteResponse.replaceAll("</CENTER></FONT>", "</FONT></CENTER>");
					
					noticeOfBankrupcyResponse = siteResponse;
				}
		        
		        if (rsResponse.indexOf("StatusQry.pl") != -1) {
			        // click pe Status
			        link = "https://" + host + "/cgi-bin/StatusQry.pl?" + instrNum;
                    req = new HTTPRequest( link ); 
                    req.setHeader("Host", host);
                    req.setHeader("Referer", sAction);
    
                    //siteResponse = performRequest(req);
		
					istart = siteResponse.indexOf("<B><FONT SIZE=5>") - 9;
					if (istart< siteResponse.length()){
						siteResponse = siteResponse.substring(istart, siteResponse.length());
						siteResponse = siteResponse.substring(0, siteResponse.indexOf("<HR>"));
						siteResponse = siteResponse.replaceAll("</CENTER></FONT>", "</FONT></CENTER>");
						siteResponse = siteResponse.replaceAll("(?s)(?i)(<A\\sHREF\\s=\"/cgi-bin/show_case_doc(.*?)>)(.*?)(</a>)", "$3");
						siteResponse = siteResponse.replaceAll("(?s)(?i)(<A\\sHREF=/cgi-bin/StatusQry.pl.*?</a>)", "");
					}
		        }
				
				//click pe DocketReport
		        //removeLinks(siteResponse);
				//siteResponse += getDocketReportTable(instrNum, host, sAction);

				statusResponse = siteResponse;
        	}            
            
        	if ((!downloadingForSave))
    		{
        		rsResponse = "<table width='100%' cellpadding='0' cellspacing='0'><tr><td width='100%'>" 
        			//+ removeLinks(aliasResponse) + "<br><br>"
        			//+ removeLinks(caseSummaryResponse) + "<br><br>"
        			//+ removeLinks(noticeOfBankrupcyResponse) + "<br><br>"
        			+ statusResponse + "<br><br>"
        			+ "</td></tr></table>";
        		
                //instrNum = getInstrumentNumberFromResponse( rsResponse );
                
        		rsResponse = rsResponse.replaceAll("<IMG.*?>", "");
        		
    			String qry = Response.getQuerry();
    			qry = "dummy=" + instrNum + "&" + qry;
    			String originalLink = sAction + "&" + qry;
    			String sSave2TSDLink =
    				getLinkPrefix(TSConnectionURL.idGET) + originalLink;
    			
    			rsResponse = removeLinks(rsResponse);
    			
    			if (FileAlreadyExist(instrNum + ".html") )
    			{
    				rsResponse += CreateFileAlreadyInTSD();
    			}
    			else
    			{
    			   rsResponse = addSaveToTsdButton(rsResponse, sSave2TSDLink, viParseID);
    			   mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
    			}
    	        
    			rsResponse=ro.cst.tsearch.utils.Tidy.tidyParse(rsResponse,null);
    			Response.getParsedResponse().setPageLink(
    				new LinkInPage(
    					sSave2TSDLink,
    					originalLink,
    					TSServer.REQUEST_SAVE_TO_TSD));
    			
    			parser.Parse(
    				Response.getParsedResponse(),
    				rsResponse,
    				Parser.NO_PARSE);
    		}
    		else
    		{
    			rsResponse = "<table width='100%' cellpadding='0' cellspacing='0'><tr><td width='100%'>" 
        			+ aliasResponse + "<br><br>"
        			+ "</td></tr></table>";
    			
                String allResponse = "<table width='100%' cellpadding='0' cellspacing='0'><tr><td width='100%'>" 
                    + aliasResponse + "<br><br>"
                    + caseSummaryResponse + "<br><br>"
                    + noticeOfBankrupcyResponse + "<br><br>"
                    + statusResponse + "<br><br>"
                    + "</td></tr></table>";
                
                //instrNum = getInstrumentNumberFromResponse( allResponse );
                
    		    // for html
    			msSaveToTSDFileName = instrNum + ".html";
    			Response.getParsedResponse().setFileName(
    				getServerTypeDirectory() + msSaveToTSDFileName);
 
    			Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
    			
    			//rsResponse = removeLinks(rsResponse);
    			
    			msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD(true);

	            ParsedResponse pr = Response.getParsedResponse();
	            
	            rsResponse = rsResponse.replaceAll("<IMG.*?>", "");

	            rsResponse=ro.cst.tsearch.utils.Tidy.tidyParse(rsResponse,null);
	            parser.Parse(pr, 
	            		rsResponse, 
	            		Parser.PAGE_DETAILS,
	                    getLinkPrefix(TSConnectionURL.idGET),
	                    TSServer.REQUEST_SAVE_TO_TSD);
	            
	            
	            rsResponse = allResponse;
	            //rsResponse = removeLinks(rsResponse);
	            
	            parser.Parse(
	    				Response.getParsedResponse(),
	    				rsResponse,
	    				Parser.NO_PARSE);
	            
	            Response.getParsedResponse().setOnlyResponse(rsResponse);
    		}
        	
        	break;
        	
        case ID_GET_LINK:
        	//click pe next sau details
        	if (sAction.indexOf("disp_page") >= 0 
        			|| sAction.contains("/view?")
        			|| lastURIAsString.contains("/view?")) {
        		ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
        	} else if (sAction.indexOf("iqquerymenu") >= 0 
        			|| sAction.indexOf("reports.pl") >= 0 
        			|| sAction.contains("AsccaseDisplay.pl")
        			|| sAction.contains("ecf_redirect?court")
        			|| sAction.contains("/iquery.pl?")) {
        		ParseResponse(sAction, Response, ID_INTERMEDIARY);
        	}else if (sAction.contains("/doc1/") || sAction.contains("show_doc.pl")){
        		manageImageClick(sAction, Response);
        	}else if (sAction.contains("/SearchClaims")){
        		String afterPage = sAction.substring(sAction.indexOf("/SearchClaims.pl?") + "/SearchClaims.pl?".length());
        		if(afterPage.matches("\\d+")) {
        			manageGenericFormPage(sAction, Response);
        		} else {		
        			manageGenericAdvancedResultPage(sAction, Response);	
        		}
        	}else if (sAction.contains("/CreditorQry.pl")){
        		String afterPage = sAction.substring(sAction.indexOf("/CreditorQry.pl?") + "/CreditorQry.pl?".length());
        		if(afterPage.matches("\\d+")) {
        			manageCreditorQryFormPage(sAction, Response);
        		} else {		
        			ParseResponse(sAction, Response, ID_DETAILS);	
        		}
        	}else if (sAction.contains("/CredMatrixCase")){
        		String afterPage = sAction.substring(sAction.indexOf("/CredMatrixCase.pl?") + "/CredMatrixCase.pl?".length());
        		if(afterPage.matches("\\d+")) {
        			manageGenericFormPage(sAction, Response);
        		} else {
        			manageCredMatrixCaseResultPage(sAction, Response);
        		}
        	}else if (sAction.contains("/SchedQry")){
        		String afterPage = sAction.substring(sAction.indexOf("/SchedQry.pl?") + "/SchedQry.pl?".length());
        		if(afterPage.matches("\\d+")) {
        			manageDeadlineScheduleFirstFormPage(sAction, Response);
        		} else {		
        			manageDeadlineScheduleResultPage(sAction, Response);	
        		}
        	}else if (sAction.contains("/DktRpt")){
        		String afterPage = sAction.substring(sAction.indexOf("/DktRpt.pl?") + "/DktRpt.pl?".length());
        		if(afterPage.matches("\\d+") || afterPage.contains("caseNumber=")) {
        			manageGenericFormPage(sAction, Response);
        		} else {
        			if(rsResponse.contains("To accept charges shown below") 
        					|| (rsResponse.contains("<iframe") && rsResponse.contains("click here")) ) {
        				manageImageClick(sAction, Response);
        			} else {
        				manageGenericAdvancedResultPage(sAction, Response);
        			}
        		}
        	}else if (sAction.contains("/NoticeOfFiling.pl")){
        		manageNoticeOfFiling(sAction, Response);
        	}else if (sAction.contains("/HistDocQry")){
        		String afterPage = sAction.substring(sAction.indexOf("/HistDocQry.pl?") + "/HistDocQry.pl?".length());
        		if(afterPage.matches("\\d+")) {
        			manageGenericFormPage(sAction, Response);
        		} else {		
        			manageGenericAdvancedResultPage(sAction, Response);	
        		}
        	}else if (sAction.contains("/DoCalendar.pl")){
        		String afterPage = sAction.substring(sAction.indexOf("/DoCalendar.pl?") + "/DoCalendar.pl?".length());
        		if(afterPage.matches("\\d+")) {
        			manageGenericFormPage(sAction, Response);
        		} else {		
        			manageGenericAdvancedResultPage(sAction, Response);	
        		}
        	}else if (sAction.contains("/qryViewDocument.pl?")){
        		String afterPage = sAction.substring(sAction.indexOf("/qryViewDocument.pl?") + "/qryViewDocument.pl?".length());
        		if(afterPage.matches("\\d+")) {
        			manageGenericFormPage(sAction, Response);
        		} else {
        			
        			if(rsResponse.contains("The requested docket entry does not exist")) {
        				Response.getParsedResponse().setResponse("The requested docket entry does not exist.");
        			} else {
        				manageImageClick(sAction, Response);
        			}
        		}
        	}else if (sAction.contains("/qryDocument.pl?")){
        		String afterPage = sAction.substring(sAction.indexOf("/qryDocument.pl?") + "/qryDocument.pl?".length());
        		if(afterPage.matches("\\d+")) {
        			manageGenericFormPage(sAction, Response);
        		} else {
        			if(rsResponse.contains("Either you do not have permission to view the document, or the document does not exist in the case.")) {
        				Response.getParsedResponse().setResponse("Either you do not have permission to view the document, or the document does not exist in the case.");
        			} else if (rsResponse.contains("Error: the requested document does not exist")) {
        				Response.getParsedResponse().setResponse("Error: the requested document does not exist");
        			} else {
        				manageImageClick(sAction, Response);
        			}
        		}
        	}else if (sAction.contains("/RelTransactQry")){
        		String afterPage = sAction.substring(sAction.indexOf("/RelTransactQry.pl?") + "/RelTransactQry.pl?".length());
        		if(afterPage.matches("\\d+")) {
        			manageGenericFormPage(sAction, Response);
        		} else {		
        			manageGenericAdvancedResultPage(sAction, Response);	
        		}
        	}else if (sAction.contains("/QryRMSLocation.pl")){
        		String afterPage = sAction.substring(sAction.indexOf("/QryRMSLocation.pl?") + "/QryRMSLocation.pl?".length());
        		if(afterPage.matches("\\d+")) {
        			manageGenericFormPage(sAction, Response);
        		} else {		
        			manageGenericAdvancedResultPage(sAction, Response);	
        		}
        	}else if (sAction.contains("/ClaimHistory.pl") 
        			|| sAction.contains("/FilerQry.pl")
        			|| sAction.contains("/qryCorporateParent.pl")
        			|| sAction.contains("/qryAscCases.pl")
        			//|| sAction.contains("/DoCalendar.pl")
        			|| sAction.contains("/StatusQry.pl")) {
        		manageGenericAdvancedResultPage(sAction, Response);
        	} else if (sAction.contains("servlet=CaseSummary.jsp")) {
        		manageDocketResultPage(sAction, Response);
        	}else if (sAction.contains("/qry")
        			|| sAction.contains("=CaseSummary.jsp")){	//this is just backup
        		ParseResponse(sAction, Response, ID_DETAILS);
        	} else {
        		logger.error("Pacer: " + searchId +  ". Unimplemented Link. " + sAction);
        		Response.getParsedResponse().setResponse("Unimplemented Link. Please contact ATS support with steps to reproduce. Thank you!");
        	}
        	break;
        case ID_SEARCH_BY_MODULE19:
        	
        	String host = sAction.replaceAll(".*://(.*?)/cgi.*", "$1");
        	String link = RegExUtils.getFirstMatch("ACTION='(.*?)'", rsResponse, 1);
        	HTTPRequest req = new HTTPRequest( link );
        	req.setMethod(HTTPRequest.POST);
            req.setHeader("Host", host);
            req.setHeader("Referer", sAction);
            
            
            String caseId = RegExUtils.getFirstMatch("goDLS\\('.*?,\\s*'(.*?)'", rsResponse, 1);
            req.setPostParameter("caseid", caseId);
            req.setPostParameter("got_receipt" , "1");
            req.setPostParameter("pdf_header", "0");
            
            //HTTPResponse siteResponse = performRequest(req, true);
            
			//InputStream responseAsStream = siteResponse.getResponseAsStream();
			//RawResponseWrapper rrw = new RawResponseWrapper(siteResponse.getContentType(),(int) siteResponse.getContentLenght(),new BufferedInputStream(responseAsStream));
			//solveResponse(link,TSServer.ID_GET_LINK , "GetLink", Response, rrw, "");
			
        break;	
        /*case ID_SAVE_TO_TSD :
        	// on save
   			downloadingForSave = true;
   			ParseResponse(sAction, Response, ID_DETAILS);
   			downloadingForSave = false;
   		*/
        }
    }
	
	private void manageCredMatrixCaseResultPage(String sAction,
			ServerResponse serverResponse) throws ServerResponseException {
		String siteResponse = serverResponse.getResult();
		String host = sAction.replaceAll(".*://(.*?)/cgi.*", "$1");
		if(siteResponse.contains("<iframe") && siteResponse.contains("click here")) {
			String link = RegExUtils.getFirstMatch("<iframe src=\"(.*?)\"", siteResponse, 1);
			HTTPRequest request = new HTTPRequest("https://" + host + link);
            
			HTTPResponse retVal = null;
	    	
	    	try {
	        	HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
	        	try {
	        		retVal = site.process(request);
	        	} finally {
	        		HttpManager.releaseSite(site);
	        	}
	        } catch( Exception e ) {
	        	retVal = null;
	        	e.printStackTrace();
	        }
			if(retVal != null) {
				InputStream responseAsStream = retVal.getResponseAsStream();
				RawResponseWrapper rrw = new RawResponseWrapper(
						retVal.getContentType(),
						(int) retVal.getContentLenght(),
						retVal.getResponseAsString(),
						new BufferedInputStream(responseAsStream), null);
				
				solveResponse(link,TSServer.ID_GET_LINK , "GetLink", serverResponse, rrw, "");
			}
		} else if(siteResponse.contains("View Labels")){
			
			HtmlParser3 parser = new HtmlParser3(siteResponse);
			
			Node div = parser.getNodeById("cmecfMainContent", true);
			
			NodeList inputList = div.getChildren().extractAllNodesThatMatch(new TagNameFilter("input"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("type", "hidden"));
			for (int i = 0; i < inputList.size(); i++) {
				InputTag input = (InputTag) inputList.elementAt(i);
				String name = input.getAttribute("name");
				if(StringUtils.isNotEmpty(name)) {
					input.setAttribute("name", "___" + name);
				}
				String value = input.getAttribute("value");
				if(StringUtils.isNotEmpty(value)) {
					try {
						input.setAttribute("value", URLEncoder.encode(value, "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			}
			
			NodeList formList = div.getChildren()
				.extractAllNodesThatMatch(new TagNameFilter("form"), true);
			if(formList.size() > 0) {
				FormTag form = (FormTag) formList.elementAt(0);
				form.setAttribute("action", 
						"/title-search/URLConnectionReader?" + 
							CreatePartialLink(TSConnectionURL.idPOST) + 
							form.getAttribute("action"));
			}
			
			siteResponse = div.toHtml();
			
			
			siteResponse = siteResponse.replaceAll("(?is)(</form>)", 
				"<input type=\"hidden\" name=\"p1\" value=\"" + getSearch().getP1ParentSite() + "\">" +
	        	"<input type=\"hidden\" name=\"p2\" value=\"" + getSearch().getP2ParentSite() + "\">" +
	        	"<input type=\"hidden\" name=\"searchId\" value=\"" + (searchId) + "\">" +
	        	"$1");
			
			serverResponse.getParsedResponse().setResponse(siteResponse);
			serverResponse.setResult("");
			
		} else {
			manageGenericAdvancedResultPage(sAction, serverResponse);
		}
		
	}

	/**
	 * Parses pages like<br>/cgi-bin/RelTransactQry.pl?1323134<br>
	 * and creates a form used to search
	 * @param sAction link used to get this result
	 * @param serverResponse
	 */
	private void manageGenericFormPage(String sAction,
			ServerResponse serverResponse) {
		ParsedResponse parsedResponse = serverResponse.getParsedResponse();
		
		String siteResponse = serverResponse.getResult();
		
        String action = RegExUtils.getFirstMatch("action=\"(.*)\"", siteResponse, 1).replaceFirst("^\\.\\./", "/");
        String host = sAction.replaceAll(".*://(.*?)/cgi.*", "$1");
        String table = RegExUtils.getFirstMatch("(?is)<table(.*?)</table>", siteResponse.substring(siteResponse.indexOf("<FORM")), 0);
        String header = null;
        
        if(sAction.contains("HistDocQry.pl") 
        		|| sAction.contains("qryViewDocument.pl")
        		|| sAction.contains("qryDocument.pl")
        		|| sAction.contains("/DktRpt.pl")
        		|| sAction.contains("QryRMSLocation.pl")) {
        	table = RegExUtils.getFirstMatch("(?is)<form(?:[^>]*)>(.*?)</form>", siteResponse, 1);
        	table = table.replaceAll("(?is)<input([^>]*?)Run Query([^>]*)>", "");
        	table = table.replaceAll("(?is)<input([^>]*?)Run Report([^>]*)>", "");
        	table = table.replaceAll("(?is)<input([^>]*?)Clear([^>]*)>", "");
        	table = table.replaceAll("(?is)<input([^>]*?)Submit([^>]*)>", "");
        	
        	String tempInput = RegExUtils.getFirstMatch("(?is)<input[^>]*case_number_text_area_0[^>]*>", table, 0);
        	if(!tempInput.contains("name=")) {
        		String newTempInput = tempInput.replace("id=", "name=\"case_num\" id=");
        		table = table.replace(tempInput, newTempInput);
        		tempInput = newTempInput;
        	}
        	
        	String caseNum = StringUtils.extractParameterFromUrl(serverResponse.getQuerry(), "dummyCaseNum");
        	if(StringUtils.isNotEmpty(caseNum) && StringUtils.isNotEmpty(tempInput)) {
        		table = table.replace(tempInput, tempInput.replaceFirst("<input ", "<input value='" + caseNum + "' "));
        	}
        	
        	header = "";
        } else {
        	header = RegExUtils.getFirstMatch("(?is)<b>(.*?)</b>", siteResponse.substring(siteResponse.indexOf("<BASEFONT SIZE=3>")), 1);
        }
        
        
        table = table.replaceAll("(?is)<script.*?</script>", "");
        table = table.replaceAll("(?is) name=([\"']*)(.*?)([\"'>\\s])", " name=$1___$2$3");
        table = table.replaceAll("(?is)ONCHANGE=\"[^\"]*\"", "");
        String caseNumberId = sAction.replaceAll("(?s).*\\.pl\\?(\\d+).*", "$1");
        if(caseNumberId.contains("caseNumber=")) {
        	caseNumberId = "";
        }
        String caseNumber = RegExUtils.getFirstMatch("AddCaseNumberLine\\('(.*)'\\)", siteResponse, 1);
        
        
        
        if("cookie".equals(caseNumber)) {
        	try {
	        	HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
	        	try {
		        	Cookie[] cookies = site.getHttpClient().getState().getCookies();
		    		for (int i = 0; i < cookies.length; i++) {
		    			if(cookies[i].getName().equals("CASE_NUM")) {
		    				caseNumber = RegExUtils.getFirstMatch("^([^\\(]+)\\(", cookies[i].getValue(), 1);
		    				break;
		    			}
		    		}
	        	} finally {
	        		HttpManager.releaseSite(site);
	        		
	        	}
	        } catch( Exception e ) {
	        	
	        	e.printStackTrace();
	        }
        } else {
        	caseNumber = caseNumber.replaceFirst("-", ":");
        }
        
        if(StringUtils.isEmpty(caseNumber)) {
        	caseNumber = RegExUtils.getFirstMatch("CaseNumberSelect\\('(.*)', '(.*)'\\)", siteResponse, 1);
        	//caseNumber = caseNumber.replaceAll("-CAS$", "");
        }
        
        if(sAction.contains("/CredMatrixCase")) {
	        if(!sAction.endsWith("&")) {
				sAction += "&";	
			}
	        String dummyCaseNum = org.apache.commons.lang.StringUtils.substringBetween(sAction + "&" + serverResponse.getQuerry() + "&", "dummyCaseNum=", "&");
	        
	        if("cookie".equals(caseNumber)) {
	        	try {
					caseNumber = URLDecoder.decode(dummyCaseNum, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					logger.error("Impossible exception", e);
				}
	        }
        }
        if("cookie".equals(caseNumber)) {
			caseNumber = "";
		}
        
        table = table.replaceAll("id='all_case_ids'", "id='all_case_ids' value=\"" + caseNumberId + "\"");
        table = table.replaceAll("name=\"___case_num\"", "name=\"___case_num\" value=\"" + caseNumber + "\"");
        table = table.replaceAll("<input[^>]*class=\"calBtn\".*?>", "");
        
        
		
		
        
        StringBuilder fullResult = new StringBuilder();
        fullResult
        	.append("<FONT FACE=\"arial,helvetica\" COLOR=\"#0000cc\" SIZE=4><b>").append(header).append("</b></FONT><br>")
        	.append("<form name=\"siteForm\" action=\"/title-search/URLConnectionReader?")
        	.append(CreatePartialLink(TSConnectionURL.idPOST))
        	.append("https://").append(host).append(action)
        	.append("\" method=\"POST\">")
        	.append("<input type=\"hidden\" name=\"p1\" value=\"").append(getSearch().getP1ParentSite()).append("\">")
        	.append("<input type=\"hidden\" name=\"p2\" value=\"").append(getSearch().getP2ParentSite()).append("\">")
        	.append("<input type=\"hidden\" name=\"searchId\" value=\"").append(searchId).append("\">")
        	.append("<input type=\"hidden\" name=\"___disableEncoding\" value=\"true\">")
        	.append("<input type=\"hidden\" name=\"___lastRequestReferer\" value=\"" + sAction + "\">")
        	.append(table)
        	.append("<input type=\"submit\" class=\"button\" value=\"Run Report\">")
			.append("</form>");
        
        parsedResponse.setResponse(fullResult.toString());
		serverResponse.setResult("");
		
		if(sAction.startsWith("/http")) {
			sAction = sAction.substring(1);
			getSearch().setAdditionalInfo("Referer_https://" + host + action, sAction);
		}
		
		
	}

	/**
	 * Manages a result page that can contain links/images
	 * @param sAction
	 * @param serverResponse
	 */
	private void manageGenericAdvancedResultPage(String sAction,
			ServerResponse serverResponse) {
		String siteResponse = serverResponse.getResult();
		
		//getSearch().setAdditionalInfo(sAction, siteResponse);
		if(!siteResponse.startsWith("<div")) {
			
			int indexOfMainContentDiv = siteResponse.indexOf("<div id=\"cmecfMainContent\">");
			int indexOfLastEnlosedDivTag = siteResponse.lastIndexOf("</div");
			
			if (indexOfMainContentDiv > 0 && indexOfLastEnlosedDivTag > indexOfMainContentDiv){
				siteResponse = siteResponse.substring(indexOfMainContentDiv, indexOfLastEnlosedDivTag);
			}
			if (siteResponse.contains("cmecfMainContent") && !siteResponse.contains("Transaction Receipt")){
				siteResponse = RegExUtils.getFirstMatch("(?is)<div id=\"cmecfMainContent\">.*?</div>", siteResponse, 0);
			}
			
			if(siteResponse.isEmpty() && serverResponse.getResult().contains("<div id=\"cmecfMainContent\">")) {
				siteResponse = RegExUtils.getFirstMatch("(?is)<div id=\"cmecfMainContent\">.*</center>", serverResponse.getResult(), 0);
			}
			siteResponse = siteResponse.replaceAll("(?is)<script.*?</script>", "");
			String host = sAction.replaceAll(".*://(.*?)/cgi.*", "$1");
	        //replace the links
	        String startLink = CreatePartialLink(TSConnectionURL.idGET) + "https://" + host;   
	        siteResponse = siteResponse.replaceAll("(?is)onContextMenu\\s*=\\s*'[^']*'", "");
	        
	        siteResponse = siteResponse.replaceAll("(?is)<a href=\"javascript:DktText\\('([^']*)','([^']*)','([^']*)','([^']*)'\\)\"",
	        		"<a href=\"/cgi-bin/HistDocQry.pl?$1-L_ShowDktTxt_1-0-$2-$3-$4\""); 
	        
	        
	        siteResponse = siteResponse.replaceAll("(?is)<a href=([\\'\"])/(.*?['\"])" , "<a href=$1"+ startLink + "/$2");
	        siteResponse = siteResponse.replaceAll("(?is)<a href=/(.*?)>" , "<a href="+ startLink + "/$1>");
	        siteResponse = siteResponse.replaceAll("(?is)<a href=([\\'\"])h(.*?['\"])" , "<a href=$1"+ CreatePartialLink(TSConnectionURL.idGET) + "h$2");
	        //siteResponse = siteResponse.replaceAll("(?is)<img.*?>", "");
	        
	        siteResponse = siteResponse.replaceAll("/graphics/silverball.gif", URLMaping.IMAGES_DIR + "/silverball.gif");
	        
	        siteResponse += "<input type=\"hidden\" name=\"" + HIDDEN_PARAM_SAVE_NAME + "\" value=\"" + getHashCodeForResult(siteResponse) + "\">";
		}
        
        String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + sAction;
        getSearch().addInMemoryDoc(sSave2TSDLink, siteResponse);
        //getSearch().setAdditionalInfo(sAction, siteResponse);
        
        if(!siteResponse.contains("No claims found for this case") && 
        		!sAction.contains("DoCalendar.pl")) {	//when showing calendar
	        siteResponse = addSaveToTsdButton(siteResponse, sSave2TSDLink, ID_DETAILS);
			serverResponse.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, sAction, TSServer.REQUEST_SAVE_TO_TSD));
        }
        
        serverResponse.getParsedResponse().setResponse(siteResponse);
		
	}

	/**
	 * Parses pages like<br>/cgi-bin/CreditorQry.pl?156137<br>
	 * and creates a form used to search
	 * @param sAction link used to get this result
	 * @param serverResponse response containing everything
	 */
	private void manageCreditorQryFormPage(String sAction,
			ServerResponse serverResponse) {
		
		ParsedResponse parsedResponse = serverResponse.getParsedResponse();
		
		String siteResponse = serverResponse.getResult();
		
        String action = RegExUtils.getFirstMatch("action=\"(.*)\"", siteResponse, 1);
        String host = sAction.replaceAll(".*://(.*?)/cgi.*", "$1");
        String table = RegExUtils.getFirstMatch("(?is)<table>(.*?)</table>", siteResponse.substring(siteResponse.indexOf("<FORM")), 0);
        table = table.replaceAll("name='(crtype)'", "name='___$1'");
        String afterPage = sAction.substring(sAction.indexOf("/CreditorQry.pl?") + "/CreditorQry.pl?".length());
        
        StringBuilder fullResult = new StringBuilder();
        fullResult
        	.append("<FONT FACE=\"arial,helvetica\" COLOR=\"#0000cc\" SIZE=4><b>Creditor Selection</b></FONT><br>")
        	.append("<form name=\"manageCreditorQryFormPage\" action=\"/title-search/URLConnectionReader?")
        	.append(CreatePartialLink(TSConnectionURL.idPOST))
        	.append("https://").append(host).append(action)
        	.append("\" method=\"POST\">")
        	.append("<input type=\"hidden\" name=\"p1\" value=\"").append(getSearch().getP1ParentSite()).append("\">")
        	.append("<input type=\"hidden\" name=\"p2\" value=\"").append(getSearch().getP2ParentSite()).append("\">")
        	.append("<input type=\"hidden\" name=\"searchId\" value=\"").append(searchId).append("\">")
        	.append("<input type=\"hidden\" name=\"___CaseId\"  value=\"").append(afterPage).append("\">")
        	.append(table)
        	.append("<input type=\"submit\" class=\"button\" value=\"Run Query\">")
			.append("</form>");
        
        parsedResponse.setResponse(fullResult.toString());
		serverResponse.setResult("");
		
	}

	private void manageNoticeOfFiling(String sAction, ServerResponse Response) {
		String rsResponse = Response.getResult();
		getSearch().setAdditionalInfo(sAction, rsResponse);
		int istart = rsResponse.indexOf("<p align=center>");
		if (istart == -1) 
			istart = rsResponse.indexOf("<p align=\"center\">");
		rsResponse = rsResponse.substring(istart, rsResponse.length());
		rsResponse = rsResponse.substring(0, rsResponse.indexOf("<HR>"));
		rsResponse = rsResponse.replaceAll("</CENTER></FONT>", "</FONT></CENTER>");
		rsResponse = rsResponse.replaceAll("(?is)<img.*?>", "");
		rsResponse += "<input type=\"hidden\" name=\"" + HIDDEN_PARAM_SAVE_NAME + "\" value=\"" + getHashCodeForResult(rsResponse) + "\">";
		
		String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + sAction;
        getSearch().addInMemoryDoc(sSave2TSDLink, rsResponse);
        rsResponse = addSaveToTsdButton(rsResponse, sSave2TSDLink, ID_DETAILS);
		
        Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, sAction, TSServer.REQUEST_SAVE_TO_TSD));
        
		Response.getParsedResponse().setResponse(rsResponse);
		
	}

	private void manageImageClick(String sAction, ServerResponse serverResponse) throws ServerResponseException {
		String siteResponse = serverResponse.getResult();
		String host = sAction.replaceAll(".*://(.*?)/.*", "$1");
		if(siteResponse.contains("Multiple Documents") && siteResponse.contains("Select the document you wish to view.")) {
			HtmlParser3 parser = new HtmlParser3(siteResponse); 
			NodeList tableList = parser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if(tableList.size() > 0) {
				StringBuilder result = new StringBuilder();
				TableTag table = (TableTag) tableList.elementAt(0);
				TableRow[] rows = table.getRows();
				for (TableRow tableRow : rows) {
					if(tableRow.getChildren().extractAllNodesThatMatch(new TagNameFilter("input"), true).size() == 0) {
						NodeList linkList = tableRow.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
						for (int i = 0; i < linkList.size(); i++) {
							LinkTag link = (LinkTag) linkList.elementAt(i);
							link.removeAttribute("onContextMenu");
							link.setLink(CreatePartialLink(TSConnectionURL.idGET) + "https://" + host + link.getLink());
						}
						result.append(tableRow.toHtml());
					}
				}
				if(result.length() > 0) {
					serverResponse.getParsedResponse()
						.setResponse("<p><big>Multiple Documents</big></p><p>Select the document you wish to view.</p>" +
							"<table cellpadding=4 cellspacing=3>" + result + "</table>");
					serverResponse.setResult("");
					
				}
			}
			
		} else if(siteResponse.contains("To accept charges shown below")){
			String link = RegExUtils.getFirstMatch("(?is)ACTION='(.*?)'", siteResponse, 1);
			String caseId = RegExUtils.getFirstMatch("(?is)goDLS\\('.*?,\\s*'(.*?)'", siteResponse, 1);
            String receipt = RegExUtils.getFirstMatch("(?is)<center>(.*)</center>", siteResponse, 0);
            
            String claimId = RegExUtils.getFirstMatch(
            		"goDLS\\('([^']*)','([^']*)','([^']*)','([^']*)','([^']*)','([^']*)','([^']*)','([^']*)','([^']*)','([^']*)'\\)", siteResponse, 8);
            String claimNum = RegExUtils.getFirstMatch(
            		"goDLS\\('([^']*)','([^']*)','([^']*)','([^']*)','([^']*)','([^']*)','([^']*)','([^']*)','([^']*)','([^']*)'\\)", siteResponse, 9);
            String magicNum = RegExUtils.getFirstMatch(
            		"goDLS\\('([^']*)','([^']*)','([^']*)','([^']*)','([^']*)','([^']*)','([^']*)','([^']*)','([^']*)','([^']*)'\\)", siteResponse, 7);
            
            if(link.startsWith("/")) {
            	link = "https://" + host + link ;
            }
            boolean origLinkHasQuestionMark = link.contains("?");
            link = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + link;
            
            if(StringUtils.isNotEmpty(caseId)) {
            	if(origLinkHasQuestionMark) {
            		link += "&caseid=" + caseId;
            	} else {
            		link += "?caseid=" + caseId;
            	}
            }
            
            if(StringUtils.isNotEmpty(claimId)) {
            	link += "&claim_id=" + claimId;
            }
            if(StringUtils.isNotEmpty(claimNum)) {
            	link += "&claim_num=" + claimNum;
            }
            if(StringUtils.isNotEmpty(magicNum)) {
            	link += "&magic_num=" + magicNum;
            }
            
            link += "&got_receipt=1&pdf_header=0\" target=\"_blank\">View Document</a>" ;
            
            
            siteResponse = 
            	"<P>To accept charges shown below, click on the 'View Document' button, otherwise click the 'Back' button on your browser.</P>" 
            	+ receipt
            	+ link;
            serverResponse.getParsedResponse()
				.setResponse(siteResponse);
			serverResponse.setResult("");
			
		} else if(siteResponse.contains("<iframe") && siteResponse.contains("click here")) {
			String link = RegExUtils.getFirstMatch("<iframe src=\"(.*?)\"", siteResponse, 1);
			HTTPRequest request = new HTTPRequest("https://" + host + link);
            
			HTTPResponse retVal = null;
	    	
	    	try {
	        	HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
	        	try {
	        		retVal = site.process(request);
	        	} finally {
	        		HttpManager.releaseSite(site);
	        	}
	        } catch( Exception e ) {
	        	retVal = null;
	        	e.printStackTrace();
	        }
			if(retVal != null) {
				InputStream responseAsStream = retVal.getResponseAsStream();
				RawResponseWrapper rrw = new RawResponseWrapper(retVal.getContentType(),(int) retVal.getContentLenght(),new BufferedInputStream(responseAsStream));
				solveResponse(link,TSServer.ID_GET_LINK , "GetLink", serverResponse, rrw, "");
			}
		} else {
			serverResponse.getParsedResponse().setResponse("Could not parse response!");
			serverResponse.setResult("");
		}
		
	}

	private void manageDocketResultPage(String sAction, ServerResponse serverResponse) {
		String siteResponse = serverResponse.getResult();
		
		getSearch().setAdditionalInfo(sAction, siteResponse);
		if(siteResponse.contains("<HTML")) {
			
			int startIndex = siteResponse.indexOf("<NOBR>");
			int endIndex = siteResponse.indexOf("Billable Pages:");
			if(startIndex > 0) {
				int tempIndex = siteResponse.substring(0, startIndex).lastIndexOf("<center>");
				if(tempIndex > 0) {
					startIndex = tempIndex; 
				} else {
					serverResponse.getParsedResponse().setResponse("Could not parse response!");
					logger.error(searchId + " Problem parsing manageDocketResultPage (first center) from " + sAction);
					serverResponse.setResult("");
					return;
				}
			} else {
				serverResponse.getParsedResponse().setResponse("Could not parse response!");
				logger.error(searchId + " Problem parsing manageDocketResultPage (start) from " + sAction);
				serverResponse.setResult("");
				return;
			}
			if(endIndex > 0) {
				endIndex = siteResponse.indexOf("</CENTER>", endIndex);
				if(endIndex > 0) {
					siteResponse = siteResponse.substring(startIndex, endIndex + "</CENTER>".length());		
				} else {
					serverResponse.getParsedResponse().setResponse("Could not parse response!");
					logger.error(searchId + " Problem parsing manageDocketResultPage (last center) from " + sAction);
					serverResponse.setResult("");
					return;
				}
			} else {
				serverResponse.getParsedResponse().setResponse("Could not parse response!");
				logger.error(searchId + " Problem parsing manageDocketResultPage (receipt) from " + sAction);
				serverResponse.setResult("");
				return;
			}
			
			
			
			
			siteResponse = siteResponse.replaceAll("(?is)<script.*?</script>", "");
			String host = sAction.replaceAll(".*://(.*?)/.*", "$1");
	        //replace the links
	        String startLink = CreatePartialLink(TSConnectionURL.idGET) + "https://" + host;   
	        siteResponse = siteResponse.replaceAll("onContextMenu\\s*=\\s*'[^']*'", "");
	        //siteResponse = siteResponse.replaceAll("<a href=\\'(.*?')" , "<a href='"+ startLink + "$1"); 
	        
	        siteResponse = siteResponse.replaceAll("(?is)<a href=([\\'\"])/(.*?['\"])" , "<a href=$1"+ startLink + "/$2");
	        siteResponse = siteResponse.replaceAll("(?is)<a href=/(.*?)>" , "<a href="+ startLink + "/$1>");
	        siteResponse = siteResponse.replaceAll("(?is)<a href=([\\'\"])h(.*?['\"])" , "<a href=$1"+ CreatePartialLink(TSConnectionURL.idGET) + "h$2");
	        
	        
	        siteResponse = siteResponse.replaceAll("(?is)<img.*?>", "");
	        
	        String formText = RegExUtils.getFirstMatch("(?is)<form.*?</form>", siteResponse, 0);
			if(StringUtils.isNotEmpty(formText)) {
				HtmlParser3 miniParser = new HtmlParser3(formText);
				NodeList formList = miniParser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("form"), true);
				if(formList.size() > 0) {
					FormTag form = (FormTag) formList.elementAt(0);
					String toShow = "";
					String newLinkToDocument = CreatePartialLink(TSConnectionURL.idPOST) + "https://" + sAction.replaceAll(".*://(.*/).*", "$1") + "/" + form.extractFormLocn() + "?";
					NodeList children = form.getChildren();
					
					for (int i = 0; i < children.size(); i++) {
						Node node = children.elementAt(i);
						if(node instanceof InputTag) {
							InputTag input = (InputTag) node;
							try {
								newLinkToDocument += input.getAttribute("name") + "=" + URLEncoder.encode(input.getAttribute("value"),"UTF-8") + "&";
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
							if("submit".equals(input.getAttribute("type"))) {
								toShow += " " + input.getAttribute("value") + " ";
							}
						} else {
							toShow += node.toPlainTextString();
						}
					}
					
					newLinkToDocument = newLinkToDocument.substring(0, newLinkToDocument.length() - 1);
					
					siteResponse = siteResponse.replace(formText, toShow + "<a href='"+ newLinkToDocument + "'>View Document</a><br>");
				}
			}
	        
	        siteResponse += "<input type=\"hidden\" name=\"" + HIDDEN_PARAM_SAVE_NAME + "\" value=\"" + getHashCodeForResult(siteResponse) + "\">";
		}
        
        String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + sAction;
        getSearch().addInMemoryDoc(sSave2TSDLink, siteResponse);
        siteResponse = addSaveToTsdButton(siteResponse, sSave2TSDLink, ID_DETAILS);
		
		serverResponse.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, sAction, TSServer.REQUEST_SAVE_TO_TSD));
        
        serverResponse.getParsedResponse().setResponse(siteResponse);
        
		
	}
	
	/**
	 * Parses pages like <br>/cgi-bin/SchedQry.pl?228027823493456-L_444_0-1<br>
	 * /cgi-bin/SchedQry.pl?851524079266464-L_ShowDktAndRelated_1-0-156137-6-18
	 */
	private void manageDeadlineScheduleResultPage(String sAction, ServerResponse serverResponse) {
		String siteResponse = serverResponse.getResult();
		
		getSearch().setAdditionalInfo(sAction, siteResponse);
		if(!siteResponse.startsWith("<div")) {
			siteResponse = RegExUtils.getFirstMatch("(?is)<div id=\"cmecfMainContent\">.*?</div>", siteResponse, 0);
			siteResponse = siteResponse.replaceAll("(?is)<script.*?</script>", "");
			String host = sAction.replaceAll(".*://(.*?)/cgi.*", "$1");
	        //replace the links
	        String startLink = CreatePartialLink(TSConnectionURL.idGET) + "https://" + host;   
	        siteResponse = siteResponse.replaceAll("(?is)onContextMenu\\s*=\\s*'[^']*'", "");
	        siteResponse = siteResponse.replaceAll("(?is)<a href=([\\'\"])/(.*?['\"])" , "<a href=$1"+ startLink + "/$2");
	        siteResponse = siteResponse.replaceAll("(?is)<a href=/(.*?)>" , "<a href="+ startLink + "/$1>");
	        siteResponse = siteResponse.replaceAll("(?is)<a href=([\\'\"])h(.*?['\"])" , "<a href=$1"+ CreatePartialLink(TSConnectionURL.idGET) + "h$2");
	        //siteResponse = siteResponse.replaceAll("(?is)<img.*?>", "");
	        
	        siteResponse = siteResponse.replaceAll("/graphics/silverball.gif", URLMaping.IMAGES_DIR + "/silverball.gif");
	        
	        siteResponse += "<input type=\"hidden\" name=\"" + HIDDEN_PARAM_SAVE_NAME + "\" value=\"" + getHashCodeForResult(siteResponse) + "\">";
		}
        
        String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + sAction;
        getSearch().addInMemoryDoc(sSave2TSDLink, siteResponse);
        getSearch().setAdditionalInfo(sAction, siteResponse);
        siteResponse = addSaveToTsdButton(siteResponse, sSave2TSDLink, ID_DETAILS);
		
		serverResponse.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, sAction, TSServer.REQUEST_SAVE_TO_TSD));
        
        serverResponse.getParsedResponse().setResponse(siteResponse);
        
		
	}
	
	private void manageDeadlineScheduleFirstFormPage(String sAction,
			ServerResponse serverResponse) {
		
		ParsedResponse parsedResponse = serverResponse.getParsedResponse();
		
		String siteResponse = serverResponse.getResult();
		
        String action = RegExUtils.getFirstMatch("action=\"(.*)\"", siteResponse, 1);
        String host = sAction.replaceAll(".*://(.*?)/cgi.*", "$1");
        String table = RegExUtils.getFirstMatch("(?is)<table>(.*?)</table>", siteResponse.substring(siteResponse.indexOf("<FORM")), 0);
        table = table.replaceAll("name='(sort\\d)'", "name='___$1'");
                
        StringBuilder fullResult = new StringBuilder();
        fullResult
        	.append("<FONT FACE=\"arial,helvetica\" COLOR=\"#0000cc\" SIZE=4><b>Deadlines/Hearings</b></FONT><br>")
        	.append("<form name=\"manageDeadlineScheduleFirstFormPage\" action=\"/title-search/URLConnectionReader?")
        	.append(CreatePartialLink(TSConnectionURL.idPOST))
        	.append("https://").append(host).append(action)
        	.append("\" method=\"POST\">")
        	.append("<input type=\"hidden\" name=\"p1\" value=\"").append(getSearch().getP1ParentSite()).append("\">")
        	.append("<input type=\"hidden\" name=\"p2\" value=\"").append(getSearch().getP2ParentSite()).append("\">")
        	.append("<input type=\"hidden\" name=\"searchId\" value=\"").append(searchId).append("\">")
        	.append(table)
        	.append("<input type=\"submit\" class=\"button\" value=\"Run Query\">")
			.append("</form>");
        
        parsedResponse.setResponse(fullResult.toString());
		serverResponse.setResult("");
		
	}

	private String getDetailedPage(String rsResponse, String sAction) {
		if(!rsResponse.contains("<html>") && !rsResponse.contains("<HTML>")) {
			if(!isParentSite() && rsResponse.contains("type='checkbox' name='docLink'")) {
				rsResponse = rsResponse.replaceFirst("<td.*?>.*?</td>", "");
			}
			return rsResponse;
		}
		try {
			
			
			Pattern centerTagPattern = Pattern.compile("(?is)<center>(.*)</center>");
			Matcher centerTagMatcher = centerTagPattern.matcher(rsResponse);
						
			StringBuilder result = new StringBuilder();
			
			while(centerTagMatcher.find()) {
				result//.append("<div name=\"centered\" align=\"center\">")
					.append(centerTagMatcher.group().replaceAll("(?is)<hr>", "<br>"));
					//.append("</div><br>");
			}
			String hashCode = getHashCodeForResult(result.toString());
			result.append("<input type=\"hidden\" name=\"")
				.append(HIDDEN_PARAM_SAVE_NAME)
				.append("\" value=\"")
				.append(hashCode)
				.append("\">");
		
			return result.toString();
		} catch (Exception e) {
			logger.error(searchId + ": Error parsing detailed page for link:" + sAction, e);
		}
		return "";
	}

	private String getHashCodeForResult(String details) {
		details = details.replaceAll("(?is)<a\\s+[^>]*>([^<]+)<\\s*/\\s*a\\*>", "$1");
		details = details.replaceAll("(?is)<img.*?>", "");
		details = details.replaceAll("<input type=\"hidden\" name=\"" + HIDDEN_PARAM_SAVE_NAME + "\" value=\"\\d*\">", "");
		
		Pattern centerTagPattern = Pattern.compile("(?is)<center>(.*?)</center>");
		Matcher centerTagMatcher = centerTagPattern.matcher(details);
		
		String lastMatch = null;
		while(centerTagMatcher.find()) {
			lastMatch = centerTagMatcher.group();
		}
		if(lastMatch != null && lastMatch.contains("Transaction Receipt")) {
			details = details.replace(lastMatch, "");
		}
		
		
		return Integer.toString(details.hashCode());
	}

	private String getSecondIntermediaryPage(ServerResponse fullServerResponse, String sAction) {
		
		String rsResponse = fullServerResponse.getResult();
		
		if(!rsResponse.contains("<html>")) {
			return rsResponse;
		}
		try {
			String host = sAction.replaceAll(".*://(.*?)/.*", "$1");
			
			
			if(sAction.contains("ecf_redirect?court")) {
				URI lastURI = fullServerResponse.getLastURI();
				if(lastURI != null) {
					host = lastURI.getHost();
				}
			}
			
			HtmlParser3 parser = new HtmlParser3(rsResponse);
			String linkStart = CreatePartialLink(TSConnectionURL.idGET);
			NodeList tableNodeList = parser.getNodeList()
				.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			
			
			if(!sAction.endsWith("&")) {
				sAction += "&";	
			}
			String dummyCaseNum = org.apache.commons.lang.StringUtils.substringBetween(sAction, "dummyCaseNum=", "&");
			String dummyDisposition = org.apache.commons.lang.StringUtils.substringBetween(sAction, "dummyDisposition=", "&");
			
			if(tableNodeList.size() > 0) {
				TableTag tableTag = (TableTag) tableNodeList.elementAt(0);
				NodeList linkList = tableTag.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
				for (int i = 0; i < linkList.size(); i++) {
					LinkTag linkTag = (LinkTag) linkList.elementAt(i);
					String newLink = null;
					if(dummyCaseNum == null) {
						newLink = linkStart + "https://" + host + linkTag.extractLink();
					} else {
						newLink = linkStart + "https://" + host + linkTag.extractLink() + "&dummyCaseNum=" + dummyCaseNum;
					}
					if(dummyDisposition != null) {
						newLink = newLink + "&dummyDisposition=" + dummyDisposition;
					} 
					
					if(linkTag.extractLink().contains("DoCalendar.pl")) {
						newLink = "#";
						if(linkTag.getChildCount() == 1) {
							Node node = linkTag.getChild(0);
							if(node instanceof TextNode) {
								TextNode textNode = (TextNode)node;
								textNode.setText(textNode.getText() + " - Not Implemented - Go to document server to follow the link");
							}
						}
					}
					
					linkTag.setLink(newLink);
				}
				tableTag.removeAttribute("align");
				tableTag.removeAttribute("valign");
				return tableTag.toHtml();
			}
		
		} catch (Exception e) {
			logger.error("Error parsing SecondIntermediaryPage the one with links", e);
		}
		
		return "Could not parse information!";
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String responseHtml, StringBuilder outputTable) {
		Collection<ParsedResponse> responses = new Vector<ParsedResponse>();
		if(responseHtml == null || response == null) {
			return responses;
		}

		try {
			HtmlParser3 parser = new HtmlParser3(responseHtml);
			
			NodeList mainTableList = parser.getNodeList()
				.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("align", "center"));

			String linkPrefix = CreatePartialLink(TSConnectionURL.idGET);
			int numberOfUncheckedElements = 0;
			int maxColumn = 0;
			boolean foundTableHeaderWithLineNoClass = false;
			
			boolean showTitle = isParentSite() && response.getQuerry().contains("show_title=Yes");
			
			String querry = response.getQuerry().toLowerCase();
			
			String courtType = RegExUtils.getFirstMatch("court_type=(\\w+)", querry, 1);
			
			String status = null;
			String assetbased = null;
			
			if("bk".equals(courtType)) {
				Matcher ma = Pattern.compile("status=(\\w+)").matcher(querry);
				if (ma.find()) {
					status = ma.group(1);
				}
				if (StringUtils.isEmpty(status)) {
					status = (String)getSearch().getAdditionalInfo("status");
				} else { 
					getSearch().setAdditionalInfo("status", status);
				}
				ma = Pattern.compile("assetbased=(\\w+)").matcher(querry);
				if (ma.find()) {
					assetbased = ma.group(1);
				}
				if (StringUtils.isEmpty(assetbased)) {
					assetbased = (String)getSearch().getAdditionalInfo("assetbased");
				} else { 
					getSearch().setAdditionalInfo("assetbased", assetbased);
				}
			}
			
			for (int i = 0; i < mainTableList.size(); i++) {
				TableTag table = (TableTag) mainTableList.elementAt(i);
				TableRow[] rows = table.getRows();
				for (int j = 0; j < rows.length; j++) {
					TableRow row = rows[j];
					boolean goodRow = true;
					if("bk".equals(courtType)) {	//do it just for bankruptcy 
						if (!"all".equals(status)) {
							if (row.getColumnCount()==8) {
								String dateClosed = row.getColumns()[6].toPlainTextString().trim();
								if (dateClosed.length()==0) {
									String disposition = row.getColumns()[7].toPlainTextString();
									Matcher matcher = Pattern.compile("\\d{2}/\\d{2}/\\d{4}").matcher(disposition);
									if (matcher.find())
										dateClosed = matcher.group(0);
								}
								if ( (dateClosed.length()!=0 && "open".equals(status)) || (dateClosed.length()==0 && "closed".equals(status)) )
									goodRow = false;
							}
						}
					}
					ResultMap resultMap = new ResultMap();
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setParentSite(response.isParentSiteSearch());
					StringBuilder innerRow = new StringBuilder();
					TableColumn[] columns = row.getColumns();
					String className = null;
					if(columns.length > 0) {
						className = columns[0].getAttribute("class");
					}
					if (columns.length >= 6 && "line_no".equals(className)) {
						
						String documentNumber = "";
						String serverDocType = "";
						String disposition = "";
						int posDateFiled = 5;
						if("mdl".equals(courtType)) {
							posDateFiled = 6;
							documentNumber = columns[4].toPlainTextString().trim();
							serverDocType = columns[3].toPlainTextString().trim();	
						} else {
							documentNumber = columns[3].toPlainTextString().trim();
							serverDocType = columns[2].toPlainTextString().trim();	
							
							if(columns.length > 7) {	//the column is not present anymore
								disposition = columns[7].toPlainTextString();
								disposition = disposition.replaceAll("(?is)&nbsp;", " ");
								disposition = disposition.replaceAll("\\s{2,}", " ").trim();
							}
							
						}
						
						if("cr".equals(courtType)) {
							posDateFiled = 4;
						}
												
						resultMap.put(
								SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(),
								documentNumber);
						resultMap.put(
								SaleDataSetKey.DOCUMENT_TYPE.getKeyName(),
								serverDocType);
						resultMap.put(
								SaleDataSetKey.RECORDED_DATE.getKeyName(),
								columns[posDateFiled].toPlainTextString().trim());
						resultMap.put(
								OtherInformationSetKey.REMARKS.getKeyName(),
								disposition);
						resultMap.put(
								OtherInformationSetKey.SRC_TYPE.getKeyName(),
								"PC");
						parseNames(resultMap, searchId, 
								new String[]{columns[1].toPlainTextString().trim()}, 
								new String[0]);
						
						
						NodeList linkList = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
						LinkTag linkTag = (LinkTag) linkList.elementAt(0);
						String tempLink = linkTag.extractLink().trim().replaceAll("\\s", "%20");
						
						if(!isParentSite()) {
							tempLink = tempLink.replace("iqquerymenu.pl", "qrySummary.pl");
							
							if(getSearch().getInMemoryDoc(linkPrefix + tempLink)==null){
		        				getSearch().addInMemoryDoc(linkPrefix + tempLink, currentResponse);
							}	
							
						} else {
							tempLink += "&dummyCaseNum=" + URLEncoder.encode(linkTag.toPlainTextString().trim(), "UTF-8");
						}
						
						tempLink += "&dummyDisposition=" + URLEncoder.encode(disposition, "UTF-8");
												
						String linkString = linkPrefix + tempLink;
						linkTag.setLink(linkString);
						
						for (int linkIndex = 1; linkIndex < linkList.size(); linkIndex++) {
							LinkTag extraLink = (LinkTag) linkList.elementAt(linkIndex);
							extraLink.setLink(linkPrefix + extraLink.extractLink().trim().replaceAll("\\s", "%20"));
						}
						
						LinkInPage linkInPage = new LinkInPage(
								linkString, 
								linkString, 
		    					TSServer.REQUEST_SAVE_TO_TSD);
						
						if("bk".equals(courtType)) {	//do it just for bankruptcy 
							if (!"all".equals(assetbased)) {
								//documents which are good after the first filter (Open/Closed)
								//are filtered by Asset Based (Yes/No)
								if (goodRow) { 				
									Matcher matcher = Pattern.compile("(?i).*iqquerymenu.pl\\?(\\d+)").matcher(tempLink);
									if (matcher.find()) {
										String link = tempLink.replaceFirst("(?i)/cgi-bin.*", "");
										link += "/cgi-bin/qryAlias.pl?" + matcher.group(1);
										//get Alias page
										String aliasPage = getContentsFromLink(link);
										getSearch().setAdditionalInfo("/" + link, aliasPage);
										aliasPage = aliasPage.replaceAll("(?i)</?b>", "").replaceAll("\\s", "");
										int index = aliasPage.toLowerCase().indexOf("asset:yes");
										if ( (index==-1 && "yes".equals(assetbased)) || (index!=-1 && "no".equals(assetbased)) )
											goodRow = false;
									}
								}
							}
						}
												
						HashMap<String, String> data = new HashMap<String, String>();
						data.put("type", serverDocType);
						String checkbox = null;
						if (isInstrumentSaved(documentNumber, null, data) ) {
			    			checkbox = "saved";
			    		}  
						else {
							if (!goodRow) {
				    			checkbox = "";
				    		} else {
				    			numberOfUncheckedElements++;
				    			checkbox = "<input type='checkbox' name='docLink' value='" + linkInPage.getLink().replace("iqquerymenu.pl", "qrySummary.pl") + "'>";
		            			currentResponse.setPageLink(linkInPage);
				    		}
						}
						for (int indexColumn = 1; indexColumn < columns.length; indexColumn++) {
							
							innerRow.append(columns[indexColumn].toHtml());
						}

						if(maxColumn < columns.length) {
							maxColumn = columns.length;
						}
						
						Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
	    				RegisterDocumentI document = (RegisterDocumentI)bridge.importData();
	    				document.setNote((String)resultMap.get(OtherInformationSetKey.REMARKS.getKeyName()));
	    				currentResponse.setDocument(document);
	    				if (!goodRow) {
	    					String stringInnerRow = innerRow.toString();
	    					stringInnerRow = stringInnerRow.replaceAll("(?i)<a.*?>(.*?)</a>", "$1");
	    					innerRow = new StringBuilder(stringInnerRow);
	    				}
	    				if(isParentSite()) {
	    					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr>" + innerRow + "</tr>");
	    				} else {
	    					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<table BORDER='1' CELLPADDING='2'><tr>" + innerRow + "</tr></table>");
	    				}
						
						
						for (int indexColumn = columns.length; indexColumn < maxColumn; indexColumn++) {
							innerRow.append("<td>&nbsp;</td>");
						}
						
						currentResponse.setOnlyResponse("<tr><td align=\"center\">" + checkbox + columns[0].toPlainTextString() + "</td>" + innerRow + "</tr>" );
						
						
						responses.add(currentResponse);
						
						if(showTitle && j+1 < rows.length) {
							currentResponse = new ParsedResponse();
							currentResponse.setParentSite(response.isParentSiteSearch());
							String showTitleHtml = rows[++j].toHtml();
							if(maxColumn == 8 && showTitleHtml.contains("colspan=6")) {
								showTitleHtml = showTitleHtml.replace("colspan=6", "colspan=\"7\"");
							}
							
							
							currentResponse.setOnlyResponse(showTitleHtml);
							currentResponse
									.setAttribute(
											ParsedResponse.SERVER_NAVIGATION_LINK,
											true);
							responses.add(currentResponse);
						}
						

					} else {

						
						if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
							if(columns.length == 1 && StringUtils.isEmpty(columns[0].toPlainTextString().replace("&nbsp;", ""))) {
								//empty row, ignore
							} else {
								row.getChildren().keepAllNodesThatMatch(
										new NotFilter(new TagNameFilter("img")),
										true);
								
								
								if(maxColumn < row.getHeaderCount()) {
									maxColumn = row.getHeaderCount();
								}
								
								if(row.getHeaderCount() > 0 && !foundTableHeaderWithLineNoClass) {
									if("line_no".equals(row.getHeaders()[0].getAttribute("class"))) {
										foundTableHeaderWithLineNoClass = true;
										
										InputTag inputTag = new InputTag();
										inputTag.setText(SELECT_ALL_CHECKBOXES);
										
										((TableHeader)row.getHeaders()[0]).setChildren(new NodeList(inputTag));
									}
								}
								
								String rowAsString = row
										.toHtml()
										.replaceAll(
												"<a\\s+[^>]+>([^<]+)<\\s*/\\s*a\\s*>",
												"$1");
								
								for (int indexColumn = row.getHeaderCount(); indexColumn < maxColumn && row.getHeaderCount() != 1; indexColumn++) {
									rowAsString = rowAsString.replace("</tr>", "<th>&nbsp;</th></tr>");
								}
								
								if(isParentSite()) {
									//table header
									response.getParsedResponse().setHeader(rowAsString);
								}
							
							
							}
						}
						
					}
				}
				
			}
			
			if(mainTableList.size() == 0) {
				response.getParsedResponse().setHeader("");
				ParsedResponse currentResponse = new ParsedResponse();
				currentResponse.setParentSite(response.isParentSiteSearch());
				currentResponse.setOnlyResponse("<tr><td colspan=\"" + maxColumn + "\" align=\"center\">" + 
						RegExUtils.getFirstMatch("<div class=err_txt>(.*?)</div>", responseHtml, 1) + 
						"</td></tr>");
				currentResponse
						.setAttribute(
								ParsedResponse.SERVER_NAVIGATION_LINK,
								true);
				if(isParentSite()) {
					responses.add(currentResponse);
				}
			}
			
			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
				Node receiptNode = parser.getNodeById("receipt");
				if(receiptNode != null) {
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setParentSite(response.isParentSiteSearch());
					currentResponse.setOnlyResponse("<tr><td colspan=\"" + maxColumn + "\" align=\"center\">" + receiptNode.toHtml() + "</td></tr>");
					currentResponse
							.setAttribute(
									ParsedResponse.SERVER_NAVIGATION_LINK,
									true);
					responses.add(currentResponse);
				}
			
			}
			
			Node divWithNextNode = parser.getNodeById("page_select0");
			if(divWithNextNode != null && divWithNextNode instanceof Div && divWithNextNode.getChildren() != null) {
				NodeList linkList = divWithNextNode.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
				for (int j = 0; j < linkList.size(); j++) {
					LinkTag link = (LinkTag) linkList.elementAt(j);
					link.removeAttribute("onClick");
					link.setLink(linkPrefix + link.getLink());
					if(link.toPlainTextString().contains("Next")) {
						response.getParsedResponse().setNextLink("<a href='" + link.getLink() + "'>Next</a>");
					}
				}
				if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
					response.getParsedResponse().setHeader("<tr><td colspan=\"" + maxColumn + 
							"\" align=\"center\">" + divWithNextNode.toHtml() + "</td></tr>");
					response.getParsedResponse().setFooter(response.getParsedResponse().getHeader());
				}
				
			}
			
			SetAttribute(TSServer.NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
		} catch (Exception e) {
			logger.error("SearchId: " + searchId + ": Error while parsing intermediary response", e);
			e.printStackTrace();
		}
		return responses;
			
	}
	
	
	public DocumentI smartParseDetails(ServerResponse response,
			String detailsHtml, String sAction) {
		DocumentI document = super.smartParseDetails(response, detailsHtml);
		
		Search search = getSearch();
		
		DocumentsManagerI documentsManagerI = search.getDocManager();
		
		try {
			documentsManagerI.getAccess();
			DocumentI alreadySavedDoc = documentsManagerI.getDocument(document);
			if(alreadySavedDoc != null) {
				
				String htmlIndex = DBManager.getDocumentIndex(alreadySavedDoc.getIndexId());
				
				HtmlParser3 parser = new HtmlParser3(htmlIndex);
				
				NodeList hiddenList = parser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("input"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("type", "hidden"))
					.extractAllNodesThatMatch(new HasAttributeFilter("name", HIDDEN_PARAM_SAVE_NAME));
				
				boolean alreadyContainsInfo = false;
				String hashCode = null;
				for (int i = 0; i < hiddenList.size(); i++) {
					if(hashCode == null) {
						hashCode = getHashCodeForResult(detailsHtml);
					}
					if(hashCode.equals(((InputTag)hiddenList.elementAt(i)).getAttribute("value"))) {
						alreadyContainsInfo = true;
						break;
					}
				}
				if(!alreadyContainsInfo) {
					SearchLogger.info("Document was already saved, adding extra details to present one.<br/>", searchId);
					htmlIndex = htmlIndex.replace("</html>", "").replace("</body>", "");
					detailsHtml += "</body></html>";
					alreadySavedDoc.setIndexId( DBManager.addDocumentIndex(htmlIndex + "<hr>" + detailsHtml, search ) );
				}
			}
		} catch (Exception e) {
			logger.error("Error performing merge with previous save documents", e);
		} finally {
			documentsManagerI.releaseAccess();
		}
		
		return document;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap map) {
		map.put(
				OtherInformationSetKey.SRC_TYPE.getKeyName(),
				"PC");
		String instr="";
        String grantor="";
        String date = "";
        
        if(!isParentSite() && detailsHtml.matches("<tr.*</tr>")) {
        	HtmlParser3 parser = new HtmlParser3(detailsHtml);
        	
        	NodeList columnList = parser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("td"), true);
        	if(columnList.size() == 7) {
        		grantor = columnList.elementAt(0).toPlainTextString().trim().replaceAll("\\([^\\)]*\\)$", "");
        		instr = columnList.elementAt(2).toPlainTextString().trim();
        		date = columnList.elementAt(4).toPlainTextString().trim();
        		
        		
        		map.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(),instr);
        		map.put(SaleDataSetKey.GRANTOR.getKeyName(),grantor);
        		map.put(SaleDataSetKey.RECORDED_DATE.getKeyName(),date);
        		
        		String names[] = null;
				if(NameUtils.isCompany(grantor)) {
					names = new String[]{"", "", grantor, "", "", ""};
				} else {
					names = StringFormats.parseNameNashville(grantor, true);
				}
				String[] type = GenericFunctions.extractAllNamesType(names);
				String[] otherType = GenericFunctions.extractAllNamesOtherType(names);
				String[] suffixes = GenericFunctions.extractNameSuffixes(names);
				
				ArrayList<List> grantorList = new ArrayList<List>();
				GenericFunctions.addOwnerNames(grantor, names, suffixes[0], suffixes[1], type, otherType, 
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), grantorList);
				
				
				
				try {
					map.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantorList, true));
					ArrayList<String> line = new ArrayList<String>();
			        line.add("");
			        line.add("");
			       
			        line.add("Bankruptcy Court");
			        List<List> body = new ArrayList<List>();
			        body.add(line);
			        ResultTable rt = GenericFunctions1.addNamesMOJacksonRO(body);
			        map.put("GranteeSet", rt);
				} catch (Exception e) {
					logger.error("Problem with storing owner", e);
				}
        	}
        	
        	if(instr.contains("-cv-")) {
        		map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "District Court Civil");
        	} else if(instr.contains("-bk-")) {
        		map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "Bankruptcy Court");
        	}
			map.put(SaleDataSetKey.GRANTEE.getKeyName(), "Bankruptcy Court");
        	
        } else if(detailsHtml.contains("<h3 align=\"left\">Notice of Bankruptcy Case Filing</h3>")) {
			date = RegExUtils.getFirstMatch("and filed on (\\d{2}/\\d{2}/\\d{4})", detailsHtml, 1);
			grantor = RegExUtils.getFirstMatch("(?is)<p id=\"3\">\\s*(.*?)<br>", detailsHtml, 1);
			grantor = grantor.replaceAll("(?is)</?b>", "").trim();
			instr = RegExUtils.getFirstMatch("The case was assigned case number\\s*(.*?)\\s+to", detailsHtml, 1);
			
			manageDetailedParsedData(map, instr, grantor, date);
			
			map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "Bankruptcy Court");
			map.put(SaleDataSetKey.GRANTEE.getKeyName(), "Bankruptcy Court");
		} else if(detailsHtml.contains("Claims Register Summary")) {
			
			int index = detailsHtml.indexOf("Claims Register Summary");
			
			date = RegExUtils.getFirstMatch("(?is)Date Filed:</b>\\s*(\\d{2}/\\d{2}/\\d{4})", detailsHtml.substring(index), 1);
			grantor = RegExUtils.getFirstMatch("(?is)Case Name:</b>\\s*(.*?)\\s*<br>", detailsHtml.substring(index), 1);
			instr = RegExUtils.getFirstMatch("(?is)Case Number:</b>\\s*(.*?)\\s*<br>", detailsHtml.substring(index), 1);
			
			manageDetailedParsedData(map, instr, grantor, date);
			
			map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "Bankruptcy Court");
			map.put(SaleDataSetKey.GRANTEE.getKeyName(), "Bankruptcy Court");
			
		} else if(detailsHtml.contains("Detailed Description of Claim")) {
			
			int index = detailsHtml.indexOf("<FONT SIZE=-1 COLOR=DARKBLUE> Search Criteria: </FONT>");
			if(index == -1) {
				index = detailsHtml.indexOf("<FONT SIZE=-1 COLOR=DARKBLUE>Search Criteria:</FONT>");
			}
			
			date = RegExUtils.getFirstMatch("(?is)<I>Date Filed:</I>\\s*(\\d{2}/\\d{2}/\\d{4})", detailsHtml, 1);
			grantor = "";
			instr = RegExUtils.getFirstMatch("(?is)<FONT SIZE=-1 COLOR=DARKBLUE>\\s*(.*?)\\s*</FONT>", detailsHtml.substring(index + 1), 1);
			
			manageDetailedParsedData(map, instr, grantor, date);
			
			map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "Bankruptcy Court");
			map.put(SaleDataSetKey.GRANTEE.getKeyName(), "Bankruptcy Court");
		} else if(detailsHtml.contains("Query RMS/FRC Location of Case File(s)") || detailsHtml.contains("Case File Location")) {
			
			String detailsHtmlTemp = RegExUtils.getFirstMatch("(?is)<TABLE border=1>\\s*(.*?)\\s*</TABLE>", detailsHtml, 1);
			
			if(detailsHtmlTemp.contains("<TD")) {
			
				date = "";
				grantor = RegExUtils.getFirstMatch("(?is)<TD>\\s*(.*?)\\s*</TD>\\s*<TD>\\s*(.*?)\\s*</TD>\\s*<TD>\\s*(.*?)\\s*</TD>", detailsHtmlTemp, 3);
				instr = RegExUtils.getFirstMatch("(?is)<TD>\\s*(.*?)\\s*</TD>\\s*<TD>\\s*(.*?)\\s*</TD>\\s*<TD>\\s*(.*?)\\s*</TD>", detailsHtmlTemp, 1);
			
			} else {
				int index = detailsHtml.indexOf("<FONT SIZE=-1 COLOR=DARKBLUE> Search Criteria: </FONT>");
				if(index == -1) {
					index = detailsHtml.indexOf("<FONT SIZE=-1 COLOR=DARKBLUE>Search Criteria:</FONT>");
				}
				
				date = "";
				grantor = "";
				instr = RegExUtils.getFirstMatch("(?is)<FONT SIZE=-1 COLOR=DARKBLUE>\\s*(.*?)\\s*</FONT>", detailsHtml.substring(index + 1), 1);
			}
			
			manageDetailedParsedData(map, instr, grantor, date);
			
			if(detailsHtml.contains("Query RMS/FRC Location of Case File(s)")) {
				map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "Bankruptcy Court");
				map.put(SaleDataSetKey.GRANTEE.getKeyName(), "Bankruptcy Court");
			} else {
				map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "District Court Civil");
				map.put(SaleDataSetKey.GRANTEE.getKeyName(), "District Court");
			}
			
			
		} else if(response.getQuerry().contains("CredMatrixCase.pl")) {
			
			date = "";
			grantor = "";
			instr = RegExUtils.getFirstMatch("(?is)Case Number:\\s*(.*?)\\s*</b>", detailsHtml, 1).replaceAll("&nbsp;?", "");
			
			manageDetailedParsedData(map, instr, grantor, date);
			
			map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "Bankruptcy Court");
			map.put(SaleDataSetKey.GRANTEE.getKeyName(), "Bankruptcy Court");
		} else if(response.getQuerry().contains("HistDocQry.pl") && response.getQuerry().contains("L_ShowDktTxt")) {
			
			int index = detailsHtml.indexOf("<FONT SIZE=-1 COLOR=DARKBLUE> Search Criteria: </FONT>");
			if(index == -1) {
				index = detailsHtml.indexOf("<FONT SIZE=-1 COLOR=DARKBLUE>Search Criteria:</FONT>");
			}
			
			date = "";
			grantor = "";
			instr = RegExUtils.getFirstMatch("(?is)<FONT SIZE=-1 COLOR=DARKBLUE>\\s*(.*?)\\s*</FONT>", detailsHtml.substring(index + 1), 1);
			
			if(instr.contains("Type:")) {
				instr = instr.substring(0, instr.indexOf("Type:"));
			}
			
			manageDetailedParsedData(map, instr, grantor, date);
			
			map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "Bankruptcy Court");
			map.put(SaleDataSetKey.GRANTEE.getKeyName(), "Bankruptcy Court");
			
		} else if(detailsHtml.contains("CIVIL DOCKET FOR CASE #:")) {
			int index = detailsHtml.indexOf("CIVIL DOCKET FOR CASE #:");
			if(index < 0) {
				index = 0;
			}
			date = RegExUtils.getFirstMatch("(?is)Date Filed:\\s*(\\d{2}/\\d{2}/\\d{4})", detailsHtml.substring(index), 1);
			grantor = RegExUtils.getFirstMatch("(?is)<td[^>]*>\\s*<br>\\s*(.*?)\\s*<br>", detailsHtml.substring(index), 1).replaceAll("&nbsp;?", "");
			
			instr = RegExUtils.getFirstMatch("(?is)CIVIL DOCKET FOR CASE #:\\s*(.*?)\\s*</h3>", detailsHtml.substring(index), 1).replaceAll("&nbsp;?", "");
			
			manageDetailedParsedData(map, instr, grantor, date);
			
			map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "District Court Civil");
			map.put(SaleDataSetKey.GRANTEE.getKeyName(), "District Court");
			
		} else {
		
			map.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), ro.cst.tsearch.utils.Tidy.tidyParse(detailsHtml,null));
			
			String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
	        String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
	        ArrayList counties_list = STATES_COUNTIES.get(crtState);
	        if (counties_list!=null && counties_list.contains(crtCounty)) {
	        	String query = response.getQuerry() + "&";
				String dummyDisposition = org.apache.commons.lang.StringUtils.substringBetween(query, "dummyDisposition=", "&");
				try {
					dummyDisposition = URLDecoder.decode(dummyDisposition, "UTF-8");
					map.put(OtherInformationSetKey.REMARKS.getKeyName(), dummyDisposition);
				} catch (UnsupportedEncodingException e) {
					logger.error("Decode exception", e);
				}
	        }
	        
			try {
				GenericFunctions1.parsePCJacksonMO(map, searchId);
	
			} catch (Exception e) {
				logger.error(searchId + ": Error while parsing data the old way.", e);
			}
			
			if(StringUtils.isEmpty((String)map.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName()))) {
				if(detailsHtml.contains("Court of Appeals Docket #:")) {
					instr = RegExUtils.getFirstMatch("(?is)Court of Appeals Docket #:\\s*</b>\\s*(.*?)\\s*<", detailsHtml, 1);
					date = RegExUtils.getFirstMatch("(?is)Docketed:\\s*</b>\\s*(\\d{2}/\\d{2}/\\d{4})", detailsHtml, 1);
					grantor = "";
					
					manageDetailedParsedData(map, instr, grantor, date);
					
					map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "Appelate Court");
					map.put(SaleDataSetKey.GRANTEE.getKeyName(), "Appelate Court");
				}
			}
			
		}
		return null;
	}
	
	private void manageDetailedParsedData(ResultMap m, String instr,
			String grantor, String date) {
		instr=instr.trim();
        grantor=grantor.replaceAll("(?is)\\band\\b", "&").replaceAll("&nbsp;?","").trim();
        date=date.trim();
		m.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(),instr);
        m.put(SaleDataSetKey.GRANTOR.getKeyName(),grantor);
        m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(),date);
        String[] u = StringFormats.parseNameNashville(grantor);
        if (u[2].length() > 0 && u[0].length() == 0 && u[1].length() == 0){// for B 4582, il cook ex
        	u[1] = u[2];
        	u[2] = "";
        }
        if (u[0].length() > 1 && u[1].length() == 0){// for B 4582, il kane ex
        	u[1] = u[0];
        	u[0] = "";
        }
        if (u[3].length() > 1 && u[4].length() == 0){// for B 4582, il kane ex
        	u[4] = u[3];
        	u[3] = "";
        }
        m.put("PropertyIdentificationSet.OwnerFirstName", u[2]);
        m.put("PropertyIdentificationSet.OwnerMiddleName", u[0]);
        m.put("PropertyIdentificationSet.OwnerLastName", u[1]);
        m.put("PropertyIdentificationSet.SpouseFirstName", u[5]);
        m.put("PropertyIdentificationSet.SpouseMiddleName", u[3]);
        m.put("PropertyIdentificationSet.SpouseLastName", u[4]);
        ArrayList<String> line = new ArrayList<String>();
        @SuppressWarnings("rawtypes")
		List<List> body = new ArrayList<List>();
        line.add(u[2]);
        line.add(u[0]);
        line.add(u[1]);
        body.add(line);
        line = new ArrayList<String>();
        line.add(u[5]);
        line.add(u[3]);
        line.add(u[4]);
        body.add(line);
        ResultTable rt = GenericFunctions1.addNamesMOJacksonRO(body);
        m.put("GrantorSet", rt);
        String grantee = (String)m.get("SaleDataSet.Grantee");
        if (grantee!=null){
	        line = new ArrayList<String>();
	        line.add("");
	        line.add("");
	       
	        line.add(grantee);
	        body.clear();
	        body.add(line);
	        rt = GenericFunctions1.addNamesMOJacksonRO(body);
	        m.put("GranteeSet", rt);
        }
		
	}

	@SuppressWarnings("rawtypes")
	private void parseNames(ResultMap m, long searchId, String[] grantorList, String[] granteeList) throws Exception {
		
		ArrayList<List> grantor = new ArrayList<List>();
		ArrayList<List> grantee = new ArrayList<List>();
		
		String grantorAsString = parseNameList(grantorList, grantor, "");
		String granteeAsString = parseNameList(granteeList, grantee, "");
		
		if(StringUtils.isNotEmpty(granteeAsString)) {
			m.put("SaleDataSet.Grantee", granteeAsString.substring(0,granteeAsString.length() - 1));
		}
		if(StringUtils.isNotEmpty(grantorAsString)) {
			m.put("SaleDataSet.Grantor", grantorAsString.substring(0,grantorAsString.length() - 1));
		} 
		m.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
		m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
		
		GenericFunctions1.setGranteeLanderTrustee2(m, searchId, true);
		
	}
	
	@SuppressWarnings("rawtypes")
	protected static String parseNameList(String[] grantorList,
			ArrayList<List> grantor, String grantorAsString) {
		String[] suffixes;
		String[] type;
		String[] otherType;
		for (String grantorString : grantorList) {
			grantorString = cleanName(grantorString);
			if(StringUtils.isNotEmpty(grantorString)) {
				String names[] = null;
				if(NameUtils.isCompany(grantorString)) {
					names = new String[]{"", "", grantorString, "", "", ""};
				} else {
					names = StringFormats.parseNameNashville(grantorString, true);
				}
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);
				grantorAsString += grantorString + "/";
				
				GenericFunctions.addOwnerNames(grantorString, names, suffixes[0], suffixes[1], type, otherType, 
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), grantor);
			}
		}
		return grantorAsString;
	}
	
	private static String cleanName(String grantorString) {
		if(grantorString != null) {
			grantorString = grantorString.trim().replaceAll("\\([^\\)]*\\)$", "");
			return grantorString.replaceAll("\\s{2,}", " ");
		}
		return "";
	}
	
	/**
	 * @param rsResponce
	 * @return
	 */
	public String removeLinks(String rsResponce) {
		return rsResponce.replaceAll("(?i)</?a[^>]*>", "");
	}
	
	public static void main(String[] args) {
		GenericParserPC genericParserPC = new GenericParserPC(-2);
		try {
			StringBuilder sb = new StringBuilder();
			genericParserPC.smartParseIntermediary(new ServerResponse(), 
					FileUtils.readFileToString(new File("D:\\workspace2\\TS_main\\docs\\datasource\\GenericPC\\response_all_courts_beach_dana.txt")), 
					sb);
			System.out.println(sb);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public Object getCachedDocument(String key) {
		return getSearch().getAdditionalInfo(key);
	}
	
	@Override
	public String cleanHtmlBeforeSavingDocument(String htmlContent) {
		return removeLinks(super.cleanHtmlBeforeSavingDocument(htmlContent));
	}
	
	public String getContentsFromLink(String link){
		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		try {
			return site.process(new HTTPRequest(link)).getResponseAsString();
		} catch (RuntimeException e){
			logger.warn("Could not bring link:" + link, e);
		} finally {
			HttpManager.releaseSite(site);
		}
		return "";
	}
	
	@Override
	public DocumentI smartParseDetails(ServerResponse response, String detailsHtml, boolean fillServerResponse){
		DocumentI document = null;
		try {
			ResultMap map = new ResultMap();
			parseAndFillResultMap(response,detailsHtml, map);
			map.removeTempDef();//this is for removing tmp items. we remove them here to not remove them in every place when we parse something.
			Bridge bridge = new Bridge(response.getParsedResponse(),map,searchId);
			document = bridge.importData();
			document.setNote((String)map.get(OtherInformationSetKey.REMARKS.getKeyName()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(fillServerResponse) {
			response.getParsedResponse().setResponse(detailsHtml);
			if(document!=null) {
				response.getParsedResponse().setDocument(document);
			}
		}
		response.getParsedResponse().setSearchId(this.searchId);
		return document;
	}

}
