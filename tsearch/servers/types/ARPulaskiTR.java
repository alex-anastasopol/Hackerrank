package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.htmlparser.Node;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.w3c.dom.Document;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.HtmlParserTidy;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.CityFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
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
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * @author Mihai Dediu
  */
public class ARPulaskiTR extends TSServer {
	
	static final long serialVersionUID = 10000000;
	static String detailsPage = "";
	
	public ARPulaskiTR( String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
			super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,searchId,mid);
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) 
	{
		TSServerInfoModule m;
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		
		FilterResponse cityFilter 		= CityFilterFactory.getCityFilter(searchId, 0.6d);
		FilterResponse addressFilter 	= AddressFilterFactory.getAddressHighPassFilter( searchId , 0.7d );
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , null );

		boolean emptyStreet = "".equals( sa.getAtribute( SearchAttributes.P_STREETNAME ) );
				
		if(hasPin()){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			m.clearSaKeys();
			String pid = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO);
			pid = pid.replaceAll("-|\\.", "");
			m.setData(0, pid);
			l.add(m);		
		}
			
		if(!emptyStreet){
			m = new TSServerInfoModule(serverInfo
					.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			m.clearSaKeys();
			m.getFunction(0).setSaKey( SearchAttributes.P_STREET_FULL_NAME_EX );
			
			m.addFilter(cityFilter);
			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);
			l.add(m);
		}
		
		if( hasOwner() ) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			
			
			
			GenericNameFilter nameFilter = (GenericNameFilter )NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
			nameFilter.setUseSynonymsForCandidates(false);
			m.addFilter(nameFilter);

			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[] {"L;F M;","L;f m;","L;f;","L;m;"});
			m.addIterator(nameIterator);
			nameFilter = (GenericNameFilter )NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
			nameFilter.setUseSynonymsForCandidates(false);
			m.addFilter(nameFilter);
			l.add(m);
        }

		serverInfo.setModulesForAutoSearch(l);
	}
	
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID)
			throws ServerResponseException
	{
			String rsResponse = Response.getResult();
			ParsedResponse parsedResponse = Response.getParsedResponse();
			
			switch (viParseID)
			{
				case ID_SEARCH_BY_NAME:
				case ID_SEARCH_BY_ADDRESS:
				case ID_SEARCH_BY_PARCEL:
				    if (rsResponse.indexOf("No Records Found") != -1 ) {
				    	parsedResponse.setResultRows(new Vector<ParsedResponse>());
						parsedResponse.setError("<font color=\"red\">No results found.</font>");
		                return;
				    }
				    
				    
				    StringBuilder outputTable = new StringBuilder();
					Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
					
					if(smartParsedResponses.size() > 0) {
						parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
						parsedResponse.setOnlyResponse(outputTable.toString());
						parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
		            } else {
		            	parsedResponse.setResultRows(new Vector<ParsedResponse>());
						parsedResponse.setError("<font color=\"red\">No results found.</font>");
						return;
		            }
					break;
					
				case ID_DETAILS:			    
				case ID_SAVE_TO_TSD :
					
					if(rsResponse.contains("THIS PARCEL CAN NOT BE VIEWED ONLINE")) {
						Response.getParsedResponse().setResponse( "THIS PARCEL CAN NOT BE VIEWED ONLINE" );
						return;
					}
					
					String instrumentNo = StringUtils.extractParameterFromUrl(Response.getRawQuerry(), "STDID");
					String details = getDetails(rsResponse, instrumentNo);						
					String filename = instrumentNo + ".html";
					
					if (viParseID==ID_DETAILS) {
						String originalLink = sAction + "&" + Response.getRawQuerry();
						String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

						if (FileAlreadyExist(filename) ) {
							details += CreateFileAlreadyInTSD();
						}
						else {
							mSearch.addInMemoryDoc(sSave2TSDLink, details);
							
							details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
						}

						Response.getParsedResponse().setPageLink(
							new LinkInPage(
								sSave2TSDLink,
								originalLink,
								TSServer.REQUEST_SAVE_TO_TSD));
						
						parser.Parse( Response.getParsedResponse(), details, Parser.NO_PARSE);
						Response.getParsedResponse().setResponse( cleanResponseAfterParsing(details,false) );
						
					}  else {
						smartParseDetails(Response,details);
						
						msSaveToTSDFileName = filename;
						Response.getParsedResponse().setFileName( getServerTypeDirectory() + msSaveToTSDFileName);	
						Response.getParsedResponse().setResponse( cleanResponseAfterParsing(details,true) );
						
						msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
						
					}
					break;
				case ID_GET_LINK :
					ParseResponse(sAction, Response, rsResponse.contains("Total Items")
																? ID_SEARCH_BY_NAME
																: ID_DETAILS);
					break;
			}
		}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String responseHtml, StringBuilder outputTable) {
		Collection<ParsedResponse> responses = new Vector<ParsedResponse>();
		if(responseHtml == null || response == null) {
			return responses;
		}

		try {
			responseHtml = responseHtml.replaceAll("<!---\\s*SITEMS\\s*\\d+\\s*--->", "");
			HtmlParser3 parser = new HtmlParser3(responseHtml);

			Node tableNode = parser.getNodeById("HdrTbl1", true);
			if(tableNode == null || !(tableNode instanceof TableTag)) {
				return responses;
			}
			String linkPrefix = CreatePartialLink(TSConnectionURL.idPOST);
			
			TableTag table = (TableTag)tableNode;
			TableRow[] rows = table.getRows();
			String footer = "";
			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				String onClick = row.getAttribute("onclick");
				row.removeAttribute("onclick");
				TableColumn[] columns = row.getColumns();
				LinkTag linkTag = new LinkTag();

				linkTag.setLink(linkPrefix + "/cgi-bin/webshell.asp?XEVENT=READREC&STDID=" + RegExUtils.getFirstMatch("RecSel\\('(.*?)\\'.*?\\)", onClick, 1));
				linkTag.setChildren(new NodeList(new org.htmlparser.nodes.TextNode("View")));
				NodeList children = new NodeList(linkTag);				
				columns[0].setChildren(children);
				
				
				
				
				ParsedResponse currentResponse = new ParsedResponse();
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
				currentResponse.setOnlyResponse(row.toHtml());
				currentResponse.setPageLink(new LinkInPage(linkTag.getLink(),linkTag.getLink(),TSServer.REQUEST_SAVE_TO_TSD));
				
				ResultMap m = ro.cst.tsearch.servers.functions.ARPulaskiTR.parseIntermediaryRow( row, searchId ); 
				Bridge bridge = new Bridge(currentResponse, m, searchId);
				
				DocumentI document = (TaxDocumentI)bridge.importData();				
				currentResponse.setDocument(document);
				
				responses.add(currentResponse);
				
			}
			
			response.getParsedResponse().setHeader("<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + 
					"<tr><th>Select</th><th>Parcel #</th><th>Account#</th><th>Name</th><th>Year</th><th>Property Address</th><th>Mailing Address 2</th><th>Mailing City</th><th>Mailing State</th></tr>");
			
			Pattern p = Pattern.compile("<br>\\s*Page (\\d+) of (\\d+)\\s*&nbsp;&nbsp;&nbsp;\\s*<br>\\s*Total Items=\\s*\\d+\\s*");
			Matcher m = p.matcher(responseHtml); 
			if(m.find()) {
				try {
					int currentPage = Integer.parseInt(m.group(1));
					int maxPages = Integer.parseInt(m.group(2));
					int next = (currentPage+1 > maxPages)?0:currentPage+1;
					int prev = (currentPage-1 < 1)?0:currentPage-1;	
												
					String lnk = linkPrefix + "/cgi-bin/webshell.asp?" + response.getQuerry().replaceAll("&NEXTGRP=\\d+", "") + "&XEVENT=STDLIST"+"&XNGRPS=" + maxPages + "&NEXTGRP=";
					
					String nextLink =  (next==0)?"#":lnk+next;
					String prevLink =  (prev==0)?"#":lnk+prev;
					
					footer = m.group() + "<br>" + 
							((prev==0)?"Previous&nbsp;":"<a href='"+prevLink + "'>Previous</a> ") +
							"| " +
							((next==0)?"Next":"<a href='"+nextLink + "'>Next</a>") + 
							"<br><br>" + footer;
					if(!"#".equalsIgnoreCase(nextLink)) {
						response.getParsedResponse().setNextLink( "<a href='"+nextLink+"'>Next</a>" );
					}
					
					
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
			
			
			
			response.getParsedResponse().setFooter(footer + "</table>");

			
			outputTable.append(table);
			
			
		} catch (Exception e) {
			logger.error("SearchId: " + searchId + ": Error while parsing intermediary response", e);
			e.printStackTrace();
		}
		return responses;
	}
	
	private String getDetails(String response, String instrumentNo) {
		
		/* If from memory - use it as is */
		if(!response.contains("<html")){
			return response;
		}
		
		String rsResponse = "";
		
		if(detailsPage.isEmpty()) {
			HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			try {
				detailsPage = ((ro.cst.tsearch.connection.http2.ARPulaskiTR)site).getDetailsHtmlTemplate();
			}finally {
				HttpManager.releaseSite(site);
			}
		}
		
		
		
		response += "parent.PutMvals(\"DUMMY_CNT\",\"?1?2?3?4?5?6?7?8?9?10?11?12?13?14?15?16?17?18?19?20\",20,0);".replaceAll("\\?",""+(char)65533);
		response = response.replaceAll("parent.PutLastPullVal\\(\\s*(parent\\.MainForm\\..*?),\".*?\",\"(.*?)\"\\);","$1.value=\"$2\";");				
		
		String htmlDetails = detailsPage.replaceAll("style=\".*?visibility:hidden;.*?\"", "")
										.replaceAll("<table([^>]*?)\r?\n", "<table$1>\n")
										.replaceAll("<input type=text[^>]*?name=\"GD\"[^>]*?size=\"(.*?)\"[^>]*ShowF\\('(.*?)'\\)[^>]*>","<input size=\"$1\" name=\"$2\" type=\"text\">")
										.replaceAll("<input type=text[^>]name=\"(.*?)\"[^>]size=\"(.*?)\"[^>]*>","<input size=\"$2\" name=\"$1\" type=\"text\"/>")
										.replaceAll("<button[^>]*>.*?</button>","")
										.replaceAll("(Orig\\.|Curr\\.|Transactions)","")
										.replaceAll("Bill History", "<br><b>Bill History</b><br>")
										.replaceAll("Gflds.splice\\(20,0,\"PERP_1\"","Gflds.splice\\(20,0,\"DUMMY_CNT\",\"PERP_1\"")
										.replaceAll("Gflds.splice\\(18,0,\"TXF_21\"","Gflds.splice\\(18,0,\"DUMMY_CNT\",\"TXF_21\"")
										.replaceAll("<span id=\"L_PANTITLE_1\">Personal</span>","");
		
		
		String personal = "", property = "", legal ="";
		Document doc = Tidy.tidyParse(htmlDetails);
		try {
			personal = HtmlParserTidy.getHtmlFromNode(HtmlParserTidy.getNodeById(doc, "layer1" , "div"));
			property = HtmlParserTidy.getHtmlFromNode(HtmlParserTidy.getNodeById(doc, "layer2" , "div"));
			legal = HtmlParserTidy.getHtmlFromNode(HtmlParserTidy.getNodeById(doc, "layer3" , "div"));
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		
		rsResponse = "<h3>Personal</h3>"+ personal + "<h3>Property</h3>" + property + "<h3>Legal</h3>" + legal;
		
		String billHistoryTable = fillTableFromGridData(gridToTable(personal, response, true,"G_1A", "P_4"),response,htmlDetails,0);
		rsResponse = rsResponse.replaceAll("(?ism)<div[^>]*id=\"G_1A\"[^>]*>.*?</div>", "<div id=\"G_1A\">"+billHistoryTable+"</div><br>");
		
		String installmentInfoTable = fillTableFromGridData(gridToTable(personal, response, true,"G_1B", "P_29"),response,htmlDetails,14);					
		rsResponse = rsResponse.replaceAll("(?ism)<div[^>]*id=\"G_1B\"[^>]*>.*?</div>", "<div id=\"G_1B\">"+installmentInfoTable+"</div><br>");
		
		String otherInfo = "<br><b>Receipts:</b><br>"+getReceiptsTable(billHistoryTable, instrumentNo,getCurrentServerName(),searchId);
		otherInfo += "<br>" + getOriginalChargePage(instrumentNo,getCurrentServerName(),searchId);
		otherInfo += "<br>" + getCurrentChargePage(instrumentNo,getCurrentServerName(),searchId);
		rsResponse = rsResponse.replaceAll("<h3>Property</h3>",Matcher.quoteReplacement(otherInfo) + "<br><h3>Property</h3>");
				
		
		legal = legal.replaceAll("<input[^>]*?value=\"\\d+\" type=\"text\">","<input type=\"text\" name=\"DUMMY_CNT\" size=\"4\" >"); 
		String legalInfoTable = fillTableFromGridData(gridToTable(legal, response, false,"G_3A", "PERP_1"),response,htmlDetails,20);					
		rsResponse = rsResponse.replaceAll("(?ism)<div[^>]*id=\"G_3A\"[^>]*>.*?</div>", "<div id=\"G_3A\">"+legalInfoTable+"</div><br>");
		
		property = property.replaceAll("<input[^>]*?value=\"\\d+\" type=\"text\">","<input type=\"text\" name=\"DUMMY_CNT\" size=\"4\" >"); 
		String propertyTable = fillTableFromGridData(gridToTable(property, response, false,"G_2A", "P_59"),response,htmlDetails,18);					
		rsResponse = rsResponse.replaceAll("(?ism)<div[^>]*id=\"G_2A\"[^>]*>.*?</div>", "<div id=\"G_2A\">"+propertyTable+"</div><br>");
									
		Pattern findTextValues = Pattern.compile("(STDID|(?:(?:TXF|P)_\\d+)).value=\\\"(.*?)\\\";");
		Matcher findTextValueMat =findTextValues.matcher(response);
		while(findTextValueMat.find()) {
			
			try {
				String name = findTextValueMat.group(1);
				String value = findTextValueMat.group(2);
				rsResponse = rsResponse.replaceAll("<input type=\"text\" name=\""+name+"\" size=\"(.*?)\">","<input type=\"text\" name=\""+name+"\" size=\"$1\" value=\""+Matcher.quoteReplacement(value)+"\">")
									   .replaceAll("<textarea([^>]*?)name=\""+name+"\"([^>]*?)>.*?</textarea>","<textarea$1name=\""+name+"\"$2>"+Matcher.quoteReplacement(value)+"</textarea>")
									   .replaceAll("(?ism)<select[^>]*?size=\"([^\"]*?)\"[^>]*?name=\""+name+"\"[^>]*?>.*?<option value=\""+Matcher.quoteReplacement(value).replaceAll("\\(","\\(")+"\">(.*?)</option>.*?</select>","<input type=\"text\" name=\""+name+"\" size=\"$1\" value=\"$2\">");
				
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		rsResponse =  rsResponse.replaceAll("<input ","<input readonly ")
								.replaceAll("<select.*?</select>","")
								.replaceAll("onmouse(out|over)=\"[^\"]*?\"","");
		
		return rsResponse;
	}
	
	private String cleanResponseAfterParsing(String details,boolean saveToTsd) {
		details = details.replaceAll("<input readonly type=\"text\" name=\"[^\"]*\" size=\"[^\"]*\" value=\"([^\"]*)\">", "$1")
		 .replaceAll("<input readonly type=\"text\" name=\"[^\"]*\" size=\"[^\"]*\">", "")
		 .replaceAll("(?ism)<div id=\"G_1A\">\\s*<table border=0 cellpadding=0 cellspacing=0 >","<div id=\"G_1A\">\n<table border=\"0\" cellpadding=\"3\" cellspacing=\"1\">")
		 .replaceAll("(?ism)<textarea[^>]*>(.*?)</textarea>","<div style=\"width: 500px; word-wrap: break-word;\">$1</div>");
		
		if(saveToTsd) {
			details = details.replaceAll("<input[^>]*?value=\"Save To TSRI\">","");
		}
		
		return details;

	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,String detailsHtml, ResultMap map) {
		ro.cst.tsearch.servers.functions.ARPulaskiTR.parseAndFillResultMap(detailsHtml,map,searchId);
		return null;
	}
	
	public static void splitResultRows(Parser p,ParsedResponse pr,String htmlString,int pageId,String linkStart,int action)
		throws ro.cst.tsearch.exceptions.ServerResponseException {
		p.splitResultRows( pr, htmlString, pageId, "<tr><td>", "</table>", linkStart,action);
	}
	
	private static String gridToTable(String html, String initialResponse,boolean hasTotalsOnLastRow, String gridId,  String firstElementId) {
		return ro.cst.tsearch.servers.functions.ARPulaskiTR.gridToTable(html, initialResponse,hasTotalsOnLastRow, gridId,  firstElementId);
	}
	
	private static String fillTableFromGridData(String table, String initialResponse, String htmlDetails, int gridJsIndex) {
		return ro.cst.tsearch.servers.functions.ARPulaskiTR.fillTableFromGridData(table, initialResponse, htmlDetails, gridJsIndex);
	}
	
	private static String getReceiptsTable(String billHistoryTable, String instrumentNo, String serverName, long searchId) {
		return ro.cst.tsearch.servers.functions.ARPulaskiTR.getReceiptsTable(billHistoryTable, instrumentNo, serverName, searchId);
	}
	
	private static String getOriginalChargePage(String instrumentNo, String serverName, long searchId) {
		return ro.cst.tsearch.servers.functions.ARPulaskiTR.getChargePage( instrumentNo, "original", serverName, searchId);
	}
	
	private static String getCurrentChargePage(String instrumentNo, String serverName, long searchId) {
		return ro.cst.tsearch.servers.functions.ARPulaskiTR.getChargePage( instrumentNo, "current", serverName, searchId);
	}
	
}

