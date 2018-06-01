package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * @author Mihai D.
 */
public class CODouglasTR extends TSServer {
	
	static final long serialVersionUID = 10000000;
	private static final String viewDetailsUrl = "/apps/treasurer/tidi/";

	public CODouglasTR(String rsRequestSolverName, String rsSitePath,
            String rsServerID, String rsPrmNameLink, long searchId, int mid) {
        super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
    }
	
    protected void setModulesForAutoSearch(TSServerInfo serverInfo) 
    {
        
    	List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
        TSServerInfoModule m;
                             	
		FilterResponse addressFilter 	= AddressFilterFactory.getAddressHighPassFilter( searchId , 0.7d );
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , null );
		FilterResponse rejectNonRealEstateFilter = new RejectNonRealEstate(SearchAttributes.OWNER_OBJECT, searchId);
		rejectNonRealEstateFilter.setThreshold(new BigDecimal("0.65"));
		
		//search by PIN as Property Account Number
		if(hasPin()){			
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			m.clearSaKeys();
			m.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO);
			l.add(m);
		}
		//search by PIN as Parcel No
		if(hasPin()){			
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PROP_NO_IDX));
			m.clearSaKeys();
			m.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO);
			l.add(m);
		}
	
		if(hasStreet()){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.getFunction(1).setSaKey( SearchAttributes.P_STREETNO );
			m.getFunction(2).setSaKey( SearchAttributes.P_STREETNAME );
			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);
			m.addFilter(rejectNonRealEstateFilter);
			m.addFilter(LegalFilterFactory.getDefaultLegalFilter(searchId));
			l.add(m);
		}
		
		if( hasOwner() )
	    {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);			
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			m.addFilter(NameFilterFactory.getDefaultNameFilter( SearchAttributes.OWNER_OBJECT, searchId, m));
			m.addFilter(rejectNonRealEstateFilter);
			m.addFilter(addressFilter);
			m.addFilter(LegalFilterFactory.getDefaultLegalFilter(searchId));
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[] {"F M L;;","f m L;;","f L;;","m L;;", "F m;;"});
			m.addIterator(nameIterator);
			l.add(m);
	    }
	
		serverInfo.setModulesForAutoSearch(l);
	
      }

    protected void ParseResponse(String sAction, ServerResponse Response,
            int viParseID) throws ServerResponseException {

        String rsResponse = Response.getResult();
        
        if (rsResponse.contains("No parcel was found")) {
			Response.getParsedResponse().setError("No data found.");
			Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
			return;
		}
        
        if (viParseID == ID_SEARCH_BY_ADDRESS && rsResponse.contains("Property Tax Details")) {
        	viParseID = ID_DETAILS;
        }
        
        if (viParseID == ID_SEARCH_BY_PROP_NO && rsResponse.contains("Property Tax Details")) {
        	viParseID = ID_DETAILS;
        }
        
		switch (viParseID) {
			case ID_SEARCH_BY_ADDRESS:
			case ID_SEARCH_BY_PROP_NO:
				try {
					StringBuilder outputTable = new StringBuilder();
					ParsedResponse parsedResponse = Response.getParsedResponse();
	
					Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
	
					if (smartParsedResponses.size() > 0) {
						parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
						parsedResponse.setOnlyResponse(outputTable.toString());
						parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case ID_SEARCH_BY_PARCEL:
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:
	
				rsResponse = rsResponse.replaceAll("<th\\s+(.*?)>", "<td $1>")
										.replaceAll("</th>", "</td>");
				
				String accountId = StringUtils.extractParameterFromUrl(Response.getQuerry(),"propertyId");
	
				if(StringUtils.isEmpty(accountId)) {
					HtmlParser3 parser= new HtmlParser3(rsResponse);
					accountId = StringUtils.prepareStringForHTML(
										HtmlParser3.getValueFromNextCell(
												HtmlParser3.findNode(parser.getNodeList(), "Property Account Number:"),"",true)).trim();
				}
				
				if (StringUtils.isEmpty(accountId)) {
					return;
				}
				
				String details = getDetails(rsResponse, accountId);
				String filename = accountId + ".html";
	
				if (viParseID != ID_SAVE_TO_TSD) {
					String originalLink = sAction + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
	
					boolean isInstrumentSaved = false;
					try {
						//String year = StringUtils.extractParameter(details, "(?ism)Estimated\\s+(\\d{2,4})\\s+Property Taxes");
						HashMap<String, String> data = new HashMap<String, String>();
						data.put("type","CNTYTAX");
	//					data.put("year",year);
						isInstrumentSaved =  isInstrumentSaved(accountId,null,data);
					}catch(Exception e) {
						e.printStackTrace();
					}
					
					if (isInstrumentSaved){
						details += CreateFileAlreadyInTSD();
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink,viParseID);
					}
					
	
					Response.getParsedResponse().setPageLink(
							new LinkInPage(sSave2TSDLink, originalLink,
									TSServer.REQUEST_SAVE_TO_TSD));
					Response.getParsedResponse().setResponse(details);
	
				} else {
					smartParseDetails(Response, details);
	
					msSaveToTSDFileName = filename;
					Response.getParsedResponse().setFileName(
							getServerTypeDirectory() + msSaveToTSDFileName);
					Response.getParsedResponse().setResponse(details);
	
					msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
	
				}
				break;
				
			case ID_GET_LINK:
				ParseResponse(sAction, Response,rsResponse.contains("Page ")
						? ID_SEARCH_BY_ADDRESS
						: ID_DETAILS);
				break;
		}
    }
    
	private String getDetails(String response, String accountNo) {
		
		/* If from memory - use it as is */
		if(!response.contains("<html")){
			return response;
		}
						
		StringBuilder details = new StringBuilder();
		
		HtmlParser3 parser= new HtmlParser3(response);
		
		TableTag mainTable = (TableTag)parser.getNodeByAttribute("class", "Apptable", true);
		if (mainTable!=null) {
			mainTable.setAttribute("width", "99%");
			mainTable.setAttribute("border", "1");
			details.append(mainTable.toHtml());
			details.append("<br>");
		}
		
		ArrayList<ArrayList<String>> tabs = new ArrayList<ArrayList<String>>();
		Matcher ma1 = Pattern.compile("(?is)addTab\\('[^']+',\\s*'[^']+',\\s*'([^']+)',\\s*'([^']+)'\\)").matcher(response);
		while (ma1.find()) {
			ArrayList<String> list = new ArrayList<String>();
			list.add(ma1.group(1));
			list.add(ma1.group(2));
			tabs.add(list);
		}
		
		Div mainDiv = (Div)parser.getNodeById("tabContent");
		if (mainDiv!=null) {
			mainDiv.removeAttribute("style");
			mainTable.setAttribute("border", "1");
			details.append(tabs.size()>0?"<h3>" + tabs.get(0).get(0) + "</h3>":"");
			details.append(mainDiv.toHtml().replaceAll("(?s)<!--.*?-->", "")
				.replaceAll("(?is)<table", "<table width=\"99%\" border=\"1\"")
				.replaceFirst("(?is)border=\"1\"", "border=\"0\"")
				.replaceAll("(?is)Select.*?tab for details\\.", ""));
			details.append("<br>");
		}
		
		for (int i=1;i<tabs.size();i++) {
			String tabPage = "";
			HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			try {		
				tabPage = ((ro.cst.tsearch.connection.http2.CODouglasTR)site).getPage(viewDetailsUrl + tabs.get(i).get(1));
			}catch(Exception e) {
				e.printStackTrace();
			}finally {
				HttpManager.releaseSite(site);
			}
			if (!StringUtils.isEmpty(tabPage) && tabPage.contains("<table")) {
				details.append("<h3>" + tabs.get(i).get(0) + "</h3>");
				details.append(tabPage.replaceAll("(?s)<!--.*?-->", "")
					.replaceAll("(?is)<table", "<table width=\"99%\" border=\"1\"")
					.replaceFirst("(?is)border=\"1\"", "border=\"0\"")
					.replaceAll("(?is)<tr>\\s*<td[^>]*>\\s*For more detailed information.*?</td>\\s*</tr>", ""));
				details.append("<br>");
			}
		}
		
		String assessorPage = "";
		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		try {		
			assessorPage = ((ro.cst.tsearch.connection.http2.CODouglasTR)site).getPage("/apps/assessor/search/parcelDetails.do?action=print&propertyId="+accountNo);
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			HttpManager.releaseSite(site);
		}
		
		assessorPage = Tidy.tidyParse(assessorPage, null);
		if(!assessorPage.contains("Sorry the record you were looking for was not found")) {
			HtmlParser3 assesorParser= new HtmlParser3(assessorPage);
			Div assesorDiv = (Div)assesorParser.getNodeById("AppcontentPrint", true);
			NodeList tables = assesorDiv.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"),true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "ApptableThree"));
			details.append("<h2>Assesor Details</h2>");
			for (int i=0;i<tables.size();i++) {
				TableTag table = (TableTag)tables.elementAt(i);
				table.setAttribute("border", "1");
				table.setAttribute("width", "99%");
				table.setAttribute("style", "border:1px;border-collapse:collapse");
				details.append(table.toHtml()
						.replaceAll("(?is)<table[^>]*>\\s*<tr>\\s*<th[^>]*>\\s*View\\s*Map[^<]*</th>\\s*</tr>\\s*<tr>\\s*<td>.*?</td>\\s*</tr>\\s*</table>", "")
						.replaceAll("(?is)(<th[^>]*>[^<]*)<a[^>]*>Send</a>[^<]*(</th>)", "$1$2")
						.replaceAll("(?is)<td[^>]+rowspan=\"5\"[^>]*>\\s*<table.*?</table>\\s*<table.*?</table>\\s*</td>", "")
						.replaceFirst("(?is)<td[^>]+width=\"400\"[^>]*>.*?</td>", ""))
						.append("<br>");
			}
		}
		
		String detPage = details.toString();
		detPage= detPage.replaceAll("(?is)(?:<div[^>]+>\\s*)?<button(?:[A-Z=\\\"\\s\\.\\('\\)]*)title\\s*=\\s*\\\"Select to hide from print\\.\\\"[^>]*>\\s*x\\s*</button>(?:\\s*</div>)?", "");
		
		return detPage;
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		 Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			 			
			HtmlParser3 parser = new HtmlParser3(table);
			TableTag mainTable = (TableTag) parser.getNodeByAttribute("class","ApptableDisplayTag",true);
						
			TableRow[] rows = mainTable.getRows();
			for(TableRow row : rows ) {
				if(row.getColumnCount() > 0) {
					
					String link = HtmlParser3.getFirstTag(row.getColumns()[0].getChildren(), LinkTag.class, true).getLink();		
						
					link = link.replaceFirst("\\?", "?dummy=true&");
					link = CreatePartialLink(TSConnectionURL.idGET) + viewDetailsUrl + link+ "&print=true";
					String rowHtml =  row.toHtml().replaceFirst("(?is)href=\"[^\"]+\"","href=\"" + Matcher.quoteReplacement(link)+"\"");
					
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
					
					ResultMap m = parseIntermediaryRow(row);
					
					Bridge bridge = new Bridge(currentResponse,m,searchId);
					
					DocumentI document = (TaxDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				}
			}
			
		String header0 = "<tr><td colspan='6'>"+proccessLinks(response,(TableTag)parser.getNodeByAttribute("class", "ApptableTwo", true))+"</td></tr>";
		String header1 = rows[0].toHtml();
		
		//if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
			response.getParsedResponse().setHeader("<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n"+ header0 + header1 );
			response.getParsedResponse().setFooter("</table>");
	    //}
		
		outputTable.append(table);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	private String proccessLinks(ServerResponse response, TableTag linksTable) {
				
		try {
			Node[] links = linksTable.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"),true).toNodeArray();
			for(Node n : links) {
				LinkTag lnk = (LinkTag) n;
				lnk.setLink(CreatePartialLink(TSConnectionURL.idPOST) + lnk.getLink());
				if(lnk.getChildrenHTML().contains("Next&nbsp;Page")) {
					response.getParsedResponse().setNextLink( lnk.toHtml() );
				}
			}
			
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		return linksTable.toHtml();
	}

	protected ResultMap parseIntermediaryRow(TableRow row) {
		return ro.cst.tsearch.servers.functions.CODouglasTR.parseIntermediaryRow( row);
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,String detailsHtml, ResultMap map) {
		ro.cst.tsearch.servers.functions.CODouglasTR.parseAndFillResultMap(detailsHtml,map,searchId);
		return null;
	}

	public static void getTestcases() {
		HttpClient client = new HttpClient();
		String url = "http://apps.douglas.co.us/apps/treasurer/tidi/ownerAddressSearchForm.do?ownerName=Smith";
		GetMethod method = new GetMethod(url);
		
		int statusCode = 0;
		try {
			statusCode = client.executeMethod(method);
			if (statusCode != HttpStatus.SC_OK) {
				System.err.println("Method failed: " + method.getStatusLine());
		    }
			// Read the response body.
		    String response = method.getResponseBodyAsString();
		    System.out.println(response);
		    HtmlParser3 parser = new HtmlParser3(response);
		    TableTag mainTable = (TableTag)parser.getNodeByAttribute("class","ApptableDisplayTag",true);
		    for(TableRow row : mainTable.getRows()) {
		    	if(row.getColumnCount()>4) {
		    		System.out.println(row.getColumns()[2].toPlainTextString());
		    	}
		    }
		    for(int i=2; i<=20; i++) {
		    	String urlNext = "http://apps.douglas.co.us/apps/treasurer/tidi/parcelSearchResults.jsp?ResultListTag.COMMAND="+i;
		    	method = new GetMethod(urlNext);
		    	statusCode = client.executeMethod(method);
			    response = method.getResponseBodyAsString();
			    parser = new HtmlParser3(response);
			    mainTable = (TableTag)parser.getNodeByAttribute("class","ApptableDisplayTag",true);
			    for(TableRow row : mainTable.getRows()) {
			    	if(row.getColumnCount()>4) {
			    		System.out.println(row.getColumns()[2].toPlainTextString());
			    	}
			    }
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	/*public static void main(String... args) {
		getTestcases();
	}*/

}