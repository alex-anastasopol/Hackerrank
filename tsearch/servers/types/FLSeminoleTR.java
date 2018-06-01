package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
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
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.Tidy;

public class FLSeminoleTR extends TSServer {
	
	static final long serialVersionUID = 10000000;
	
	private boolean downloadingForSave;
	
	private static final Pattern lprevPattern = Pattern.compile("(?is)\\s*<\\s*a\\s+href\\s*=\\s*\\\"\\s*([^\\\"]+)[\\\"<>br/\\s]+\\s*previous\\s*page\\s*");
	private static final Pattern lnextPattern = Pattern.compile("(?is)\\s*<\\s*a\\s+href\\s*=\\s*\\\"\\s*([^\\\"]+)[\\\"<>br/\\s]+\\s*next\\s*page\\s*");
	private static final Pattern instrNoPattern = Pattern.compile("(?is)Parcel=(.*?)\"");

	
	public FLSeminoleTR(
			String rsRequestSolverName,
			String rsSitePath,
			String rsServerID,
			String rsPrmNameLink,
			long searchId, int mid) 
	{
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
			
			p.splitResultRows( pr, 	htmlString,  	pageId,   "<tr><td>",   "</table>",   linkStart,  action);
		}
	
	Pattern pattern = Pattern.compile("(?i)<table[ \n\t]+border=\"[^\"]+\"[ \n\t]+width=\"[^\"]+\"[ \n\t]+align=\"[^\"]+\"[ \n\t]+cols=\"[^\"]+\"[ \n\t]+title=\"Name[ \n\t]+Search[ \n\t]+Results\"[^>]*");
	
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID)
			throws ServerResponseException
	{
			String sTmp2 = "";
			StringBuffer sb = new StringBuffer();
			String rsResponse = Response.getResult();
			String initialResponse = rsResponse;
	        
			if (rsResponse.matches("(?is).*\\bContact\\s+Delinquent\\s+Taxes\\s+Immediately!.*")) {
				String errorMessage = "Could not retrieve details!";
				Matcher ma = Pattern.compile("<h1[^>]*>.*?\\bContact\\s+Delinquent\\s+Taxes\\s+Immediately!.*?</h1>\\s*<p[^>]*>.*?>(\\d+(?:-\\d+)+).*?<.*?</p>").matcher(rsResponse);
				if (ma.find()) {
					errorMessage = "Official site error:<br>Contact Delinquent Taxes Immediately!<br>" + ma.group(1);
				}
				Response.getParsedResponse().setError(errorMessage);
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				SearchLogger.info("<span class='error'><br>Could not retrieve details!<br><br></span>", searchId);
				return;
		    }
			
			switch (viParseID)
			{
				
				case ID_SEARCH_BY_NAME :
				case ID_SEARCH_BY_PARCEL:
				case ID_SEARCH_BY_ADDRESS:	
				    if (rsResponse.indexOf("No records match your search") != -1 )
		                return;

				    try
				    {
				    	rsResponse = new String (rsResponse.getBytes(),"ISO-8859-1");
				    	initialResponse = new String (initialResponse.getBytes(),"ISO-8859-1");
				    }
				    catch (Exception e)
				    {
				    	e.printStackTrace();
				    }
				    sTmp2 = CreatePartialLink(TSConnectionURL.idGET);
			    
				    sb = new StringBuffer(rsResponse);
				    Matcher lnextMat = lnextPattern.matcher(sb);
				    Matcher lprevMat = lprevPattern.matcher(sb);
				    String lnStr = "", lpStr = "", alnStr="<a><b>Next</b></a>", alpStr="<a><b>Prev</b></a>";;
				    
				    if (lnextMat.find())
				    {
				    	lnStr = lnextMat.group(1);
				    	
				    	alnStr="<a href=\""+sTmp2+"/dev/"+lnStr+"&refNext="+"\">"+"<b>Next</b></a>";
				    }

				   if (lprevMat.find())
				    {
				    	lpStr = lprevMat.group(1);
				    	
				    	alpStr="<a href=\""+sTmp2+"/dev/"+lpStr+"&refPrev="+"\">"+"<b>Prev</b></a>";
				    }

				    String prevNext = "";
				    if (mSearch.getSearchType() != Search.AUTOMATIC_SEARCH)
				    {
				    	prevNext = "\n<tr><th align=\"right\" colspan=\"5\">\n"+
			    							alpStr+"<b>|</b>"+alnStr
			    							+"\n</th></tr>\n";
 				    }

				    		
				   rsResponse = rsResponse.replaceAll("<\\s*tr\\s*>\\s*<\\s*td[^>]*>","\n<tr><td>\n");
				   
				   rsResponse = rsResponse.replaceFirst("(?is).*?<table[^>]+title=\\\"Name Search Results\\\"[^>]*>\\s*(<tr.+?</tr>)", 
				   "<table border=\"1\" width=\"750\" cellpadding=\"3\" cellspacing=\"1\">\n" + prevNext +
						    "$1"); 
				   rsResponse = rsResponse.replaceAll("(?is)(\\s*<\\s*td[^>]*>\\s*)<\\s*a\\s+href\\s*=\\s*([^>]+)>", 
							  "$1" + "<a href="+sTmp2+"/dev/"+"$2"+ ">");
				   rsResponse = rsResponse.replaceFirst("(?is)(\\s*<!--\\s*<\\s*/body.*)", "");
				   rsResponse = rsResponse.replaceFirst("\\s*Next\\s*Page\\s*","");
				   rsResponse = rsResponse.replaceFirst("\\s*Previous\\s*Page\\s*","");
				   rsResponse = rsResponse.replaceFirst("(?is)<form[^>]+>","");
				   
				   //old version
				   //rsResponse = rsResponse.replaceFirst("(?is)[^a-z].*Parcel\\s*Id\\s*<\\s*/b\\s*>\\s*<\\s*/font\\s*>\\s*<!--mstheme-->\\s*<\\s*/font\\s*>\\s*<\\s*/td\\s*>\\s*<\\s*/tr\\s*>",
				   //rsResponse = rsResponse.replaceFirst("(?is)<\\s* /table\\s*>\\s*<\\s*form[^>]*>.*", "\n</table>\n");
				   // rsResponse = rsResponse.replaceAll("(?is)<\\s* /?font[^>]*>", "");
				   // rsResponse = rsResponse.replaceAll("<\\s* /tr\\s*>","\n</tr>\n");
				 
				     
				   
				    if (!"<a><b>Next</b></a>".equals(alnStr))
				    {
				    	Response.getParsedResponse().setNextLink( alnStr );
				    }

					parser.Parse(
							Response.getParsedResponse(),
							rsResponse,
							Parser.PAGE_ROWS,
							getLinkPrefix(TSConnectionURL.idGET),
							TSServer.REQUEST_SAVE_TO_TSD);
					break;
				
				case ID_SEARCH_BY_TAX_BIL_NO: 
					if (rsResponse.indexOf("No records match your search") != -1)
						return;
					
					Matcher mat = pattern.matcher(rsResponse);
					
					if(mat.find()){
						rsResponse = rsResponse.substring(mat.start());
					}
					
					try
				    {
				    	rsResponse = new String (rsResponse.getBytes(),"ISO-8859-1");
				    	initialResponse = new String (initialResponse.getBytes(),"ISO-8859-1");
				    }
				    catch (Exception e)
				    {
				    	e.printStackTrace();
				    }

				    rsResponse = getDetails(rsResponse);
				    
				    
				    //StringUtils.toFile("C:\\1.xml", rsResponse);
				    parser.Parse(
				    		Response.getParsedResponse(),
				    		rsResponse,
				    		Parser.PAGE_ROWS,
				    		getLinkPrefix(TSConnectionURL.idGET),
				    		TSServer.REQUEST_SAVE_TO_TSD);
				    break;
					
				case ID_DETAILS:
				    if (rsResponse.indexOf("No records match your search") != -1 )
		                return;
				    
				   //cazul cand raspunsul este: Contact Delinquent Taxes Immediately
				   if (rsResponse.indexOf("Contact Delinquent Taxes Immediately") != -1 )
			                return;
				   			   
				   String details = "<table width=95% align=center>" +
				   					"<tr><td align=center>" +
				   					"<font style=\"font-size:xx-large;\">" +
				   					"<b>Seminole County Tax Collector</b></font>" +
		   							"<br>" +
				   					"</td></tr><tr><td>";
				   try {		
					   	String tidyResp = Tidy.tidyParse(rsResponse, null);
						org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(tidyResp, null);
						NodeList mainList = htmlParser.parse(null);
						
						NodeList nodes = mainList.extractAllNodesThatMatch(new TagNameFilter("div"),true)
								.extractAllNodesThatMatch(new HasAttributeFilter("id","maintext"), true);
						
						if(nodes.size()>0){
							NodeList divPayAmount =  nodes.elementAt(0).getChildren()
									.extractAllNodesThatMatch(new TagNameFilter("div"),true)
									.extractAllNodesThatMatch(new HasAttributeFilter("id","PayAmount"));
							
							if(divPayAmount.size()>0){
								details += divPayAmount.elementAt(0).toHtml();
							}
							
							nodes = nodes.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"));
							
							if(nodes.size()>0){
								details += nodes.toHtml();
							}
						}
				    } catch(Exception e) {
						e.printStackTrace();
					}
				   
					details = details.replaceAll("#FFFFCC", "#CCCCCC");
					details = details.replaceAll("#FFE3A5", "#FFFFFF");
					details = details.replaceAll("#FFFF66", "#CCCCCC");
					details = details.replaceAll("(?is)<a[^>]*>Address/Ownership\\s*Changes[^<]*</a>", "");
					details = details.replaceAll("(?is)<p[^>]*>\\s*<a[^>]*>Map\\s*&amp;\\s*Property\\s*Appraiser\\s*Information</a>.*?</p>", "");
					details = details.replaceAll("(?is)</?a[^>]*>", "");
					details = details.replaceAll("(?is)<h(\\d+)[^>]*>(.*?)</h\\1>", "$2");
					
					details += "</td></tr></table>";
				    //get detailed document addressing code
				   sb = new StringBuffer(rsResponse);
	               String keyCode = "File";
				   Matcher instrNoMat = instrNoPattern.matcher(sb);
				    if (instrNoMat.find())
				    {
				    	keyCode = instrNoMat.group(1);
				    }
	               
	                if ((!downloadingForSave))
					{
						String qry_aux = Response.getRawQuerry();
						qry_aux = "dummy=" + keyCode + "&" + qry_aux;
						String originalLink = sAction + "&" + qry_aux;
						String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

						HashMap<String, String> data = new HashMap<String, String>();
						data.put("type","CNTYTAX");
						
						if(isInstrumentSaved(keyCode, null, data))
						{
						    details += CreateFileAlreadyInTSD();
						}
						else 
						{
							details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
							mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
						}

						Response.getParsedResponse().setPageLink(
							new LinkInPage(
								sSave2TSDLink,
								originalLink,
								TSServer.REQUEST_SAVE_TO_TSD));
						parser.Parse(
							Response.getParsedResponse(),
							details,
							Parser.NO_PARSE);
					} 
					else
					{//for html
						msSaveToTSDFileName = keyCode + ".html";

						Response.getParsedResponse().setFileName(
							getServerTypeDirectory() + msSaveToTSDFileName);

						msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
						
						smartParseDetails(Response, details);
						
					}
	               
					break;
				
				case ID_GET_LINK :
					if (rsResponse.matches("(?is).*Parcel:.*"))
						ParseResponse(sAction, Response, ID_DETAILS);
					else
						ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
											
					break;
				case ID_SAVE_TO_TSD :
					if (sAction.equals("/PropertySearch.aspx"))
						ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
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
	
	private String getDetails(String rsResponse) {
		try {
			org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = parser.parse(null);
			NodeList mainTable = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width","757"));
			
			String htmlTable = mainTable.elementAt(0).toHtml();
			htmlTable = htmlTable.replaceAll("<\\s*tr\\s*>\\s*<\\s*td[^>]*>","\n<tr><td>\n");
			htmlTable = htmlTable.replaceAll("(?is)(\\s*<\\s*td[^>]*>\\s*)<\\s*a\\s+href\\s*=\\s*([^>]+)>", 
					  "$1" + "<a href="+CreatePartialLink(TSConnectionURL.idGET)+"/dev/"+"$2"+">");
			htmlTable = htmlTable.replaceAll("(?is)(Search&DataSearch=[^<]+)", "$1</td>");
			rsResponse = htmlTable.replaceAll("</td></td>", "</td>");
			
		} catch (Exception e) {
			logger.error("Error getting details", e);
			rsResponse = rsResponse.replaceAll("<\\s*tr\\s*>\\s*<\\s*td[^>]*>","\n<tr><td>\n");
			   
			   
		    rsResponse = rsResponse.replaceAll("(?is)(.+)(<table border=\\\"1\\\" width=\\\"102%\\\" align=\\\"center\\\" cols=\\\"0\\\" title=\\\"Name Search Results\\\".*</table>)(.*)", "$2");
		    rsResponse = rsResponse.replaceAll("(?is)(\\s*<\\s*td[^>]*>\\s*)<\\s*a\\s+href\\s*=\\s*([^>]+)>", 
					  "$1" + "<a href="+CreatePartialLink(TSConnectionURL.idGET)+"/dev/"+"$2"+">");
		    rsResponse = rsResponse.replaceAll("(?is)(Search&DataSearch=[^<]+)", "$1</td>");
		    rsResponse = rsResponse.replaceAll("</td></td>", "</td>");
		    rsResponse = rsResponse.replaceFirst("(?is)(\\s*<!--\\s*<\\s*/body.*)", "");
			
		}
		return rsResponse;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
			ro.cst.tsearch.servers.functions.FLSeminoleTR.parseAndFillResultMap(detailsHtml, map, searchId, miServerID);
		return null;
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) 
	{
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		boolean emptyInstrumNo = "".equals( sa.getAtribute( SearchAttributes.LD_INSTRNO ) );
		boolean emptyPid = "".equals( sa.getAtribute( SearchAttributes.LD_PARCELNO ) );
		TSServerInfoModule m;
		
		if( !emptyPid )
		{//search by Parcel Number
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			l.add(m);
		}

		if( !emptyInstrumNo )
        {//Search by Owners Name
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.TAX_BILL_MODULE_IDX));
			l.add(m);
        }
		
		if( hasOwner())
        {//Search by Owners Name
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(
					SearchAttributes.OWNER_OBJECT, searchId, m));
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
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
				url, "dummy=", "&");
		
		return keyCode+".html";
	}

}
