package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Node;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.parser.HtmlParserTidy;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
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
import ro.cst.tsearch.utils.StringUtils;

public class OHFranklinAO extends TSServer{


	boolean downloadingForSave = false;
	
	private static final long serialVersionUID = -6636728917281898065L;

	private static final Pattern patParcelID = Pattern.compile("[0-9][0-9][0-9]-[0-9][0-9][0-9][0-9][0-9][0-9]-[0-9][0-9]");
    
    private static final Pattern firstPagePattern = Pattern.compile("(?i)<a.*?href\\s*=\\s*\\\"([^\\\"]+)\\\".*Jump\\s*to\\s*first\\s*page");
    private static final Pattern prevLinkPattern = Pattern.compile("(?i)<a.*?href\\s*=\\s*\\\"([^\\\"]+)\\\".*Get\\s*previous\\s*page");
    private static final Pattern nextLinkPattern = Pattern.compile("(?i)<a.*?href\\s*=\\s*\\\"([^\\\"]+)\\\".*Get\\s*next\\s*page" );
    private static final Pattern lastPagePattern = Pattern.compile("(?i)<a.*?href\\s*=\\s*\\\"([^\\\"]+)\\\".*Jump\\s*to\\s*last\\s*page");
    
    private static final Pattern TRANSFER_HISTORY_LINK_PATTERN = Pattern.compile("(?i)<a\\s+href\\s*=\\s*\\\"([^\\\"]+)\\\">\\s*Transfer\\s*History");
    private static final Pattern TAX_INFORMATION_LINK_PATTERN = Pattern.compile("(?i)<a\\s+href\\s*=\\s*\\\"([^\\\"]+)\\\">\\s*Tax\\s*Information");
    private static final Pattern LEVY_INFO_LINK_PATTERN = Pattern.compile("(?i)<a\\s+href\\s*=\\s*\\\"([^\\\"]+)\\\">\\s*Levy\\s*Info");
    private static final Pattern TAX_DISTRIBUTION_LINK_PATTERN = Pattern.compile("(?i)<a\\s+href\\s*=\\s*\\\"([^\\\"]+)\\\">\\s*Tax\\s*Distribution");
	
	public OHFranklinAO(
			String rsRequestSolverName,
			String rsSitePath,
			String rsServerID,
			String rsPrmNameLink, long searchId, int mid) {
			super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	protected String getDetails(String response){
			
			// if from memory - use it as is
			if(!response.contains("<html")){
				return response;
			}
			
			String details = response.replaceAll("(?is).*<!--\\s*content\\s*table\\s*-->(.*)<!--\\s*End\\s*content\\s*table\\s*-->.*", "$1");

			String extraDetails = getTablesFromLinks(response);
			details += "<br>" + extraDetails;
			
			details = details.replaceAll("(?is)<a href=\"[^>]+?>[^>]*?Click here</a>", "");
			details = details.replaceAll("(?is)<a.*?href[^>]+>([^<]*)</a>", "$1");//<a target="_new" href="/pdf/conveyancecodes.pdf">Conveyance Type</a>
			details = details.replaceAll("(?is)<img[^>]+>", " ");
			details = details.replaceAll("(?is)(<td.*?align\\s*=\\s*\\\"center\\\")[^>]*>([^<]+)", "$1><b>$2</b>");
			details = details.replaceAll("(?i)<td.*?class\\s*=\\s*\\\"hdr center\\\"[^>]*>([^<]+)", "<td align=\"center\"><b>$1</b>");
			details = details.replaceAll("(?is)&amp;", "&");
			details = details.replaceAll("(?i)<span[^>]+>.*?Franklin\\s+County\\s+Treasurer\\s*</span\\s*>", "");
			details = details.replaceAll("(?i)class=\\\"border\\d+\\s+padding\\d+[^\\\"]*\\\"", "border=\"1\"");
			
		return details;
	}
	
	protected String getTablesFromLinks(String response){
		
		String tableFromLink = "";
		String extraDetails = "";
		String url = "http://franklincountyoh.metacama.com/";
		Matcher linkMatcher = TRANSFER_HISTORY_LINK_PATTERN.matcher(response);
		if (linkMatcher.find()){
			url = "http://franklincountyoh.metacama.com/" + linkMatcher.group(1);
			HTTPRequest req = new HTTPRequest(url);
	    	req.setMethod(HTTPRequest.GET);
	    	
	    	HTTPResponse res = null;
        	HttpSite site = HttpManager.getSite("OHFranklinAO", searchId);
			try
			{
				res = site.process(req);
			} finally 
			{
				HttpManager.releaseSite(site);
			}
			tableFromLink = res.getResponseAsString();
			Node form = HtmlParserTidy.getNodeById(tableFromLink, "sumtab", "div");
			String table = "";
			try {
				table = HtmlParserTidy.getHtmlFromNode(form);
			} catch (TransformerException e) {
				e.printStackTrace();
			}
			extraDetails += "<b><h3>Transfer History</b></h3>" + table;
		}
		
		/*linkMatcher.reset();
		linkMatcher = TAX_INFORMATION_LINK_PATTERN.matcher(response);
		if (linkMatcher.find()){
			url = "http://franklincountyoh.metacama.com/" + linkMatcher.group(1);
			tableFromLink = "";
			HTTPRequest req = new HTTPRequest(url);
	    	req.setMethod(HTTPRequest.GET);
	    	
	    	HTTPResponse res = null;
        	HttpSite site = HttpManager.getSite("OHFranklinAO", searchId);
			try
			{
				res = site.process(req);
			} finally 
			{
				HttpManager.releaseSite(site);
			}
			tableFromLink = res.getResponseAsString();
			Node form = HtmlParserTidy.getNodeById(tableFromLink, "sumtab", "div");
			String table = "";
			try {
				table = HtmlParserTidy.getHtmlFromNode(form);
			} catch (TransformerException e) {
				e.printStackTrace();
			}
			extraDetails += "<br><br><b><h3>Tax Information</h3></b>" + table;
		}*/
		
		linkMatcher.reset();
		linkMatcher = LEVY_INFO_LINK_PATTERN.matcher(response);
		if (linkMatcher.find()){
			url = "http://franklincountyoh.metacama.com/" + linkMatcher.group(1);
			tableFromLink = "";
			HTTPRequest req = new HTTPRequest(url);
	    	req.setMethod(HTTPRequest.GET);
	    	
	    	HTTPResponse res = null;
        	HttpSite site = HttpManager.getSite("OHFranklinAO", searchId);
			try
			{
				res = site.process(req);
			} finally 
			{
				HttpManager.releaseSite(site);
			}
			tableFromLink = res.getResponseAsString();
			if (tableFromLink.matches("(?is).*There\\s*is\\s*no\\s*Levy\\s*information.*")){
				tableFromLink = "There is no Levy information for this parcel ";
			} else {
				Node form = HtmlParserTidy.getNodeById(tableFromLink, "levy", "span");
				String table = "";
				try {
					table = HtmlParserTidy.getHtmlFromNode(form);
				} catch (TransformerException e) {
					e.printStackTrace();
				}
				tableFromLink = table;
			}
			extraDetails += "<br><br><b><h3>Levy Info</h3></b>" + tableFromLink; //table;
		}
		
		/*linkMatcher.reset();
		linkMatcher = TAX_DISTRIBUTION_LINK_PATTERN.matcher(response);
		if (linkMatcher.find()){
			url = "http://franklincountyoh.metacama.com/" + linkMatcher.group(1);
			tableFromLink = "";
			HTTPRequest req = new HTTPRequest(url);
	    	req.setMethod(HTTPRequest.GET);
	    	
	    	HTTPResponse res = null;
        	HttpSite site = HttpManager.getSite("OHFranklinAO", searchId);
			try
			{
				res = site.process(req);
			} finally 
			{
				HttpManager.releaseSite(site);
			}
			tableFromLink = res.getResponseAsString();
			Node form = HtmlParserTidy.getNodeById(tableFromLink, "sumtab", "div");
			String table = "";
			try {
				table = HtmlParserTidy.getHtmlFromNode(form);
			} catch (TransformerException e) {
				e.printStackTrace();
			}
			extraDetails += "<br><br><b><h3>Tax Distribution</h3></b>" + table;
		}*/
		return extraDetails;
	}
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
	
		String rsResponce = Response.getResult();
		String sTmp = CreatePartialLink(TSConnectionURL.idGET);
		
		rsResponce = rsResponce.replaceAll("[\\x00]+", "");
		
		switch(viParseID){
		
			case ID_SAVE_TO_TSD :
				
					downloadingForSave = true;
					ParseResponse(sAction, Response, ID_DETAILS);
					downloadingForSave = false;
				
			break;
		
			case ID_DETAILS:
				
				String details = getDetails(rsResponce);
				
				String qry = Response.getRawQuerry();
				
				String	keyNumber= getParcelID(rsResponce);
					
				if(keyNumber == null){
					keyNumber = "unknown";
				}
                
				if ((!downloadingForSave)) {
					
					qry = "dummy=" + keyNumber + "&" + qry;
					String originalLink = sAction + "&" + qry;
					String sSave2TSDLink =
						getLinkPrefix(TSConnectionURL.idGET) + originalLink;

					if (FileAlreadyExist(keyNumber + ".html") ) {
						details += CreateFileAlreadyInTSD();
					} else {
                        mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
					}

					Response.getParsedResponse().setPageLink(
						new LinkInPage(
							sSave2TSDLink,
							originalLink,
							TSServer.REQUEST_SAVE_TO_TSD));
					
					parser.Parse(Response.getParsedResponse(),details,Parser.NO_PARSE);
				}else {

					//for html
					msSaveToTSDFileName = keyNumber + ".html";

					Response.getParsedResponse().setFileName(
						getServerTypeDirectory() + msSaveToTSDFileName);

					msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
					
					parser.Parse(Response.getParsedResponse(), details, Parser.PAGE_DETAILS);
					
				}
				
				//parser.Parse(Response.getParsedResponse(), rsResponce, Parser.NO_PARSE,
	                 //   getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
				
			break;
		
			case ID_GET_LINK:
				
				
				if (sAction.indexOf("/do/searchByParcelId?taxDistrict") >=0 ){
					if (rsResponce.contains("Click on the Parcel ID to view the details for that property")) {
						ParseResponse(sAction,Response, ID_SEARCH_BY_NAME);
					} else {
						ParseResponse(sAction,Response, ID_DETAILS);
					}
				} else if (sAction.indexOf("/do/selectDisplay?parcelid") >=0) {
					ParseResponse(sAction,Response, ID_DETAILS);
				} else if (sAction.indexOf("DisplaySelectionList.jsp") >=0){
					ParseResponse(sAction,Response, ID_SEARCH_BY_NAME);
				} else if (sAction.indexOf("/scripts/map_select_subdivisions.pl") >= 0 || sAction.indexOf("/scripts/map_select_condominiums.pl") >= 0){
					ParseResponse(sAction, Response, ID_DETAILS1);
				}
				
				
			break;
			
			case ID_SEARCH_BY_ADDRESS:
			case ID_SEARCH_BY_PARCEL:
			case ID_SEARCH_BY_NAME:
						
				if(rsResponce.indexOf("Owner Information")>=0){
					ParseResponse(sAction, Response, ID_DETAILS);
					break;
				}
				
				if (rsResponce.indexOf("No entries were found that match your search criteria") >= 0) {
					Response.getParsedResponse().setError("<font color=\"red\">No results found that match your search criteria.</font>");
	                return;
	            }
				
				boolean hasNext  = false;
				boolean hasPrevious = false;
				String linkFirst = "";
				String linkPrevious = "";
				String linkNext = "";
				String linkLast = "";
				String contents = "";
				
				String header = rsResponce.replaceAll("(?is).*<td[^>]*>\\s*(\\d+\\s+entries\\s+were\\s+found)\\s*</td\\s*>\\s*<td[^>]*>([^<]+).*", 
														"<b>$1</b>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>$2</b>");
				Node tbl = HtmlParserTidy.getNodeById(rsResponce, "sumtab", "div");
				String html = "";
				try {
					html = HtmlParserTidy.getHtmlFromNode(tbl);
				} catch (TransformerException e) {
					e.printStackTrace();
				}
				
				contents = getContents(html);
				
				if (!StringUtils.isEmpty(header) && header.contains("entries")){
					contents = header + contents;
				}
				
				tbl = HtmlParserTidy.getNodeById(rsResponce, "cards", "div");
				String prevNextTable = "";
				try {
					prevNextTable = HtmlParserTidy.getHtmlFromNode(tbl);
				} catch (TransformerException e) {
					e.printStackTrace();
				}
				prevNextTable = prevNextTable.replaceAll("(?is)</td>", "</td>\r\n");
				
				contents = contents.replaceAll("(?i)<a\\s+href\\s*=\\s*\"(/do/[^\"]+)\">", "<a href=\"" + sTmp + "$1\">");
				
				String footer = "<br/>";
                
				Matcher firstLinkMatcher = firstPagePattern.matcher(prevNextTable);
                linkFirst = "";
                if( firstLinkMatcher.find() )
                {
                    linkFirst = "<a href=\"" +sTmp + firstLinkMatcher.group(1) + "\">" + "<b>First</b></a>";
                }
                
                Matcher prevLinkMatcher = prevLinkPattern.matcher(prevNextTable);
                linkPrevious = "";
                if( prevLinkMatcher.find() )
                {
                    linkPrevious = "<a href=\"" +sTmp + prevLinkMatcher.group(1) + "\">" + "<b>Previous</b></a>";
                    hasPrevious = true;
                }
                
                Matcher nextLinkMatcher = nextLinkPattern.matcher(prevNextTable);
                linkNext = "";
                if( nextLinkMatcher.find() )
                {
                    linkNext = "<a href=\"" +sTmp + nextLinkMatcher.group(1) + "\">" + "<b>Next</b></a>";
                    hasNext = true;
                }
                
                Matcher lastLinkMatcher = lastPagePattern.matcher(prevNextTable);
                linkLast = "";
                if( lastLinkMatcher.find() )
                {
                    linkLast = "<a href=\"" +sTmp + lastLinkMatcher.group(1) + "\">" + "<b>Last</b></a>";
                }
                
                if (hasPrevious){
                	footer += linkFirst + "&nbsp;&nbsp;&nbsp;&nbsp;" + linkPrevious + "&nbsp;&nbsp;&nbsp;&nbsp;";
                }
                if (hasNext){
                	footer += linkNext + "&nbsp;&nbsp;&nbsp;&nbsp;" + linkLast;
                }
                
                if (mSearch.getSearchType() != Search.AUTOMATIC_SEARCH){
    				contents += footer;
    			}
                
                if (mSearch.getSearchType() == Search.AUTOMATIC_SEARCH)
    		    {
    		    	Response.getParsedResponse().setNextLink(linkNext);
    		    }
                
				parser.Parse(Response.getParsedResponse(), contents, Parser.PAGE_ROWS,
	                    getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
	
			break;
			case ID_SEARCH_BY_SUBDIVISION_NAME:
			case ID_SEARCH_BY_CONDO_NAME:
				
				String subdName = "";
				Pattern frameLink = Pattern.compile("(?i)<iframe\\s+src\\s*=\\s*\\\"([^\\\"]+)\\\"");
				Matcher frameMatcher = frameLink.matcher(rsResponce);
				if (frameMatcher.find()){
					String url = frameMatcher.group(1);
					HTTPRequest req = new HTTPRequest(url);
			    	req.setMethod(HTTPRequest.GET);
			    	
			    	HTTPResponse res = null;
		        	HttpSite site = HttpManager.getSite("OHFranklinAO", searchId);
					try
					{
						res = site.process(req);
					} finally 
					{
						HttpManager.releaseSite(site);
					}
					subdName = res.getResponseAsString();
					subdName = subdName.replaceAll("(?is).*(<table[^>]*>\\s*<tr\\s*>\\s*<td\\s*>\\s*<p\\s+class\\s*=\\s*\\\"title\\\"\\s*>.*?</p>\\s*<br>).*", "$1</td></tr></table>");
					subdName = subdName.replaceAll("(?i)(<a\\s+href\\s*=\\s*\\\")(/scripts[^\\\"]+)\\\"", "$1" + sTmp + "$2\"");
					subdName = subdName.replaceAll("(?i)(<a\\s+href\\s*=\\s*\\\")(map_select[^\\\"]+)\\\"", "$1" + sTmp + "/scripts/$2\"");
					subdName = subdName.replaceAll("(?is)<a\\s+href[^>]+>\\s*(Newest\\s*Entries)\\s*</a>", "$1");
					
				}
				parser.Parse(Response.getParsedResponse(), subdName, Parser.NO_PARSE,
	                    getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
				break;
				
			case ID_DETAILS1:
				rsResponce = rsResponce.replaceAll("(?is).*(<table[^>]*>\\s*<tr\\s*>\\s*<td\\s*>\\s*<p\\s+class\\s*=\\s*\\\"title\\\"\\s*>.*</a\\s*>\\s*</td\\s*>\\s*</tr\\s*>\\s*</table\\s*>).*", "$1");
				rsResponce = rsResponce.replaceAll("(?i)(<td[^>]*.*?</td\\s*>\\s*<td[^>]*.*?</td\\s*>)\\s*<td[^>]*.*?</td\\s*>", "$1");
				rsResponce = rsResponce.replaceAll("(?i)(<a\\s+href\\s*=\\s*\\\")(/scripts[^\\\"]+)\\\"", "$1" + sTmp + "$2\"");
				rsResponce = rsResponce.replaceAll("(?i)(<a\\s+href\\s*=\\s*\\\")(map_select[^\\\"]+)\\\"", "$1" + sTmp + "/scripts/$2\"");
				
				if (rsResponce.indexOf("Subdivisions") >= 0) {
					rsResponce = rsResponce.replaceAll("(?i)(<a\\s+href\\s*=\\s*\\\")#([^\\\"]+)\\\"[^>]*>", 
														"$1javascript:void(0)\" onclick=\"window.open('"+ sTmp + "/rpt/subd/$2\\.rpt', 'popup')\">");
				} else if (rsResponce.indexOf("Condominiums") >= 0){
					rsResponce = rsResponce.replaceAll("(?i)(<a\\s+href\\s*=\\s*\\\")#([^\\\"]+)\\\"[^>]*>", 
							"$1javascript:void(0)\" onclick=\"window.open('"+ sTmp + "/rpt/condo/$2\\.rpt', 'popup')\">");
				}
				
				rsResponce = rsResponce.replaceAll("(?is)<a\\s+href[^>]+>\\s*(Newest\\s*Entries)\\s*</a>", "$1");
				
				parser.Parse(Response.getParsedResponse(), rsResponce, Parser.NO_PARSE,
	                    getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
				break;
			
		}

	}

	private String getContents(String response){
		
		String contents = "";
		// cleanup
		contents = response.replaceAll("(?is)</?script[^>]*>", "");
		contents = contents.replaceAll("(?is)<tr[^>]+>\\s*<td[^>]+>\\s*<div\\s+id\\s*=\\s*\\\"\\s*cards\\s*\\\"\\s*>.*?</div\\s*>\\s*</td\\s*>\\s*</tr\\s*>", "");
		contents = contents.replaceAll("(?is)</?div[^>]*>", "");
		contents = contents.replaceAll("(?is)<tr[^>]*>\\s*<td[^>]*>\\s*<table.*</table\\s*>\\s*</td\\s*>\\s*</tr\\s*>", "");
		contents = contents.replaceAll("(?is)<td.*?class=\\\"hdr\\\">(.[^<]+)</td>", "<th>$1</th>");
		contents = contents.replaceAll("(?is)<tr valign=\\\"top\\\">\\s*", "<tr>");

		return contents;
	}

	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException
    {
		String pin;
		String taxDistrict = "", parcelNo = "";
		int ServerInfoModuleID = module.getModuleIdx();
		
		if (ServerInfoModuleID == 2) //search by pin 
		{
			pin = module.getFunction(0).getParamValue();
			if (pin != null && pin.length() > 10) {
				taxDistrict = pin.replaceAll("(?is)(\\d{3})-?(\\d{6}).*", "$1");
				parcelNo = pin.replaceAll("(?is)(\\d{3})-?(\\d{6}).*", "$2");
				module.getFunction(0).setParamValue(taxDistrict);
				module.getFunction(1).setParamValue(parcelNo);
				
			} else {
				pin = module.getFunction(1).getParamValue();
				if (pin != null && pin.length() > 10) {
					taxDistrict = pin.replaceAll("(?is)(\\d{3})-?(\\d{6}).*", "$1");
					parcelNo = pin.replaceAll("(?is)(\\d{3})-?(\\d{6}).*", "$2");
					module.getFunction(0).setParamValue(taxDistrict);
					module.getFunction(1).setParamValue(parcelNo);
					
				} 
			}
		}
		return super.SearchBy(module, sd);
    }
	
    protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
        List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
        TSServerInfoModule m;

        SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
        
        String parcelID = sa.getAtribute( SearchAttributes.LD_PARCELNO );
        String taxDistrict = parcelID.replaceAll("(?is)(\\d{3})-?(\\d{6}).*", "$1");
        String parcelNo = parcelID.replaceAll("(?is)(\\d{3})-?(\\d{6}).*", "$2");
        
        PinFilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
        FilterResponse addressFilter 	= AddressFilterFactory.getAddressHighPassFilter( searchId , 0.7d );
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , null );
        
        //search by parcel ID
        if(hasPin()){
            m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
            m.clearSaKeys();
            m.setData(0, taxDistrict);
            m.setData(1, parcelNo);
            m.addFilter(pinFilter);
            l.add(m);
        }
        
        //search by address
        if(hasStreet()){
            m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
            
            m.getFunction( 1 ).setSaKey( SearchAttributes.P_STREETNAME );
            m.addFilter(addressFilter);
            m.addFilter(nameFilterHybrid);
            m.addFilter(pinFilter);
            l.add(m);
        }
        
        //search by owner
        if(hasOwner()){            
        	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
            m.clearSaKeys();
            m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
            m.addFilter(nameFilterHybrid);
            m.addFilter(addressFilter);
            m.addFilter(pinFilter);
            
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(m, searchId, new String[] {"L;f;","L;m;","L;F;","L;M;"});
			m.addIterator(nameIterator);
            
            l.add(m);
        }
        
        serverInfo.setModulesForAutoSearch(l);  
    }
    
	
	//cand nu am ParcelID in link il iau din pagina
	String getParcelID(String str){
		Matcher m = patParcelID .matcher(str);
		if(m.find()){
			return m.group(0);
		}
		return null;
	}
    
    public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart,
            int action) throws ro.cst.tsearch.exceptions.ServerResponseException {
        
        p.splitResultRows(
                pr,
                htmlString,
                pageId,
                "<tr><td",
                "</tr>", linkStart, action);
    }
    
}
