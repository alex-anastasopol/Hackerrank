/**
 * 
 */
package ro.cst.tsearch.servers.types;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.HTTPSiteInterface;
import ro.cst.tsearch.connection.http.HTTPSiteManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
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
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author radu bacrau
 *
 */
public class FLHillsboroughTR extends TSServer {

	public static final long serialVersionUID = 10000000L;
	
	
	private static final Pattern folioPattern = Pattern.compile("(\\d{3,6}+)-?(\\d{4}+)$");
	private static final Pattern pinPattern = Pattern.compile("(\\w)-(\\w{2})-(\\w{2})-(\\w{2})-(\\w{3})-(\\w{6})-(\\w{5})[.](\\w)$");
	private static final Pattern hiddenPattern = Pattern.compile("<INPUT TYPE=HIDDEN NAME=\"([^\"]+)\" VALUE=\"([^\"]*)\">");
	private static final Pattern yearLinkPattern = Pattern.compile("property_detail\\.asp\\?pmid=(\\d+)");

	private boolean downloadingForSave = false;
	
	/**
	 * @param searchId
	 */
	public FLHillsboroughTR(long searchId) {
		super(searchId);		
	}

	/**
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 * @param searchId
	 */
	public FLHillsboroughTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,mid);
	}

	/**
	 * Get year details
	 * @param pmid
	 * @return year details or empty string if error 
	 */
	private String getYearDetails(String pmid){
		
		HTTPSiteInterface site = HTTPSiteManager.pairHTTPSiteForTSServer("FLHillsboroughTR", searchId, miServerID);		
		String link  = "http://www.hillstax.org/taxapp/property_detail.asp?pmid=" + pmid;
		HTTPRequest req = new HTTPRequest(link);
		HTTPResponse res = site.process(req);
		String page = res.getResponseAsString();
		
		int istart = page.indexOf("Property Appraiser website for this property"); if(istart == -1){ return ""; }
		istart = page.indexOf("<form>", istart); if(istart == -1){ return ""; } istart += "<form>".length();
		int iend = page.indexOf("</form>", istart); if(iend == -1){ return ""; }
		
		String part1 = page.substring(istart, iend);
		
		istart = page.indexOf("<div", iend); if(istart == -1){ return ""; }
		//iend = page.indexOf("<p class=\"indented\" style=\"margin-top: 0; margin-bottom: 0\"><font face=\"Arial\"><strong>Taxes E-Mail:</strong>", istart);
		iend = page.indexOf("<hr color=\"#00573c\" width=\"95%\"", istart);
		if(iend == -1){ return ""; }
		
		String part2 = page.substring(istart, iend);
		
		return part1 + part2;
		
	}
	
	/**
	 * Extract details from a details page
	 * @param response
	 * @return details or null if no results found
	 */
	private String extractDetails(String response){
		
		// check if anything was found
		if(response.contains("<title>Not Found</title>") || response.contains("Unable to retrieve record or record not found")){
			return null;
		}		
		
		// find the property details 
		String pMid = StringUtils.extractParameter(response, "P/MID:\\s*(\\d+)");
		String year = StringUtils.extractParameter(response, "Year:\\s*(\\d+)");
		String folio = StringUtils.extractParameter(response, "Folio:\\s*(\\d+-\\d+)");
		String pin = StringUtils.extractParameter(response, "Pin:\\s*([A-Z0-9.-]+)");
		
		// check that we have at least folio
		if("".equals(folio)){
			return null;
		}
		
		String property = "<b>P/MID:</b>&nbsp;" + pMid + "<br/>" +  
		                  "<b>Year:</b>&nbsp;"  + year + "<br/>" + 
		                  "<b>Folio:</b>&nbsp;" + folio + "<br/>" + 
		                  "<b>Pin:</b>&nbsp;"   + pin + "<br/>";
		
		int istart = response.indexOf("Current Owner"); if(istart == -1){ return null; }
		istart = response.lastIndexOf("<div", istart); if(istart == -1){ return null; }
		int iend = response.indexOf("</div>", istart); if(iend == -1){ return null;} iend += "</div>".length();
		if(iend <= istart){ return null; }
		
		String retVal = property + response.substring(istart, iend);
		retVal = retVal.replace("<p><b>Select a tax year below for more information or to pay online</b></p>", "");
		retVal = retVal.replaceAll("(?i)</?a[^>]*>", "");
		
		// remove some formatting
		retVal = retVal.replaceAll("(?i)<(/?t[r])[^>]*>","<$1>");
		retVal = retVal.replaceAll("(?i)<table[^>]+>","<br/><table border=\"0\" cellspacing=\"1\">");
	    
		retVal = "<b>Summary:</b><br/><br/>" + retVal + "<br/><b>Details:</b><br/><br/>";
		
		
		// add the years details
		Matcher yearLinkMatcher = yearLinkPattern.matcher(response);		
		String yearDetails = "";
		while(yearLinkMatcher.find()){
			String pmid = yearLinkMatcher.group(1);			
			yearDetails += getYearDetails(pmid);						
		}
		
		// put everything into a table 
		retVal = "<table align=\"center\" border=\"1\" cellspacing=\"0\">" +
		         "<tr align=\"center\"><td align=\"center\">" + retVal + /*"</td></tr>" +
		         "<tr align=\"center\"><td align=\"center\">" + */ yearDetails + "</td></tr></table>";

		// format 
		retVal = retVal.replaceAll("<div[^>]*>","<div>");
		retVal = retVal.replaceAll("<hr[^>]*>", "");		
		
		
		// remove all links
		retVal = retVal.replaceAll("(?i)</?a[^>]*>", "");
		
		// remove back-to-top links
		retVal = retVal.replace("Back to Top", "");
		
		// remove images
		retVal = retVal.replaceAll("<img[^>]+>", "");
		
		return retVal;
		
	}
	
	/**
	 * 
	 * @param page
	 * @return
	 */
	private String extractIntermTable(String page){
		
		// check if anything was found
		if(page.contains("No records returned")){ return null; }
		
		// isolate the details table
		int istart = page.indexOf("Up to 1000 records will be returned"); if(istart == -1){ return null; }
		istart = page.indexOf("<table", istart); if(istart == -1){ return null; } 
		if(page.contains("The Street Search will return up to 1000 matching properties")){
			istart += "<table".length();
			istart = page.indexOf("<table", istart); if(istart == -1){ return null; }
		}
		int iend = page.indexOf("</table>", istart); if(iend == -1){ return null; } iend += "</table>".length();		
		String newPage = page.substring(istart, iend);
		
		// remove the row with the navigation
		istart = newPage.indexOf("<TR><TD ALIGN=LEFT VALIGN=MIDDLE COLSPAN=");
		if(istart != -1){		
			iend = newPage.indexOf("</FORM></TD></TR>", istart); 
			if(iend != -1){ 
				iend += "</FORM></TD></TR>".length();			
				newPage = newPage.substring(0, istart) + newPage.substring(iend);
			}
		}
				
		// group 2 rows together		
		istart = newPage.indexOf("<tbody>"); if(istart == -1){ return newPage; } istart += "<tbody>".length();
		istart = newPage.indexOf("<tr", istart); if(istart == -1){ return newPage; }
		iend = newPage.lastIndexOf("</tr>"); if(iend == -1){ return newPage; } iend += "</tr>".length();
		
		String prefix = newPage.substring(0, istart);
		prefix = prefix.replaceFirst("(?i)<table[^>]*>","<table border=\"1\" cellspacing=\"0\" style=\"border-collapse: collapse\">");
		prefix = prefix.replaceFirst("<tr[^>]*>", "<TR bgcolor=\"#AAAAAA\">");
		prefix = prefix.replaceAll("<td[^>]+>", "<td>");
		
		StringBuffer sb = new StringBuffer(prefix);
		String suffix = newPage.substring(iend) ;
		String allRows = newPage.substring(istart, iend);
		allRows = allRows.replaceAll("<tr[^>]+>", "<tr>");
				
		istart = 0;
		boolean odd = true;
		while(true){
			iend = allRows.indexOf("</tr>", istart); if(iend == -1){ break; } iend += "</tr>".length();
			iend = allRows.indexOf("</tr>", iend); if(iend == -1){ break; } iend += "</tr>".length();
			String crtRow = allRows.substring(istart, iend);
			//<a href="property_information.asp?pmid=1440654">
			/*
			if(!crtRow.contains("Real Estate")){
				crtRow = crtRow.replaceFirst("(?i)<a href=\"property_information\\.asp\\?pmid=(\\d+)\">", "");
				crtRow = crtRow.replaceFirst("(?i)</a>", "");
			}
			*/
			String color = odd ? "#FFFFFF" : "#FFFFCC"; odd = ! odd;
			crtRow = crtRow.replaceFirst("<tr[^>]*>", "<TR bgcolor=\"" + color + "\">");			
			crtRow = crtRow.replaceFirst("<tr[^>]*>", "<tr bgcolor=\"" + color + "\">");
			sb.append(crtRow);
			
			istart = iend;
		}
		sb.append(suffix);
		return sb.toString();
		
	}
	
	/**
	 * 
	 * @param page
	 * @param linkStart
	 * @return
	 */
	private String [] extractNextPrevLinks(String  page, String linkStart){

		// check if it has prev and next links
		boolean hasPrev = page.contains("<INPUT TYPE=Submit NAME=\"fpdbr_0_PagingMove\" VALUE=\"  Prev  \">");
		boolean hasNext = page.contains("<INPUT TYPE=Submit NAME=\"fpdbr_0_PagingMove\" VALUE=\"  Next  \">");
		if(!hasPrev && !hasNext){
			return new String [] {"", ""};
		}
		
		// extract action
		String action = StringUtils.extractParameter(page, "<FORM NAME=\"fpdbr_0\" ACTION=\"([^\"]+)\"");
		if(StringUtils.isEmpty(action)){
			return new String [] {"", ""}; 
		}

		// isolate hidden params
		String link = "";		
		Matcher hiddenMatcher = hiddenPattern.matcher(page);
		while(hiddenMatcher.find()){
			String name = hiddenMatcher.group(1);
			String value = hiddenMatcher.group(2);
			try{
				link += "&" + URLEncoder.encode(name, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8");
			}catch(UnsupportedEncodingException e){
				throw new RuntimeException(e);
			}
		}
		
		// create prev link
		String prevLink = "";
		if(hasPrev){
			prevLink = "<a href='" + linkStart + action + "&postParams=true" + link + "&fpdbr_0_PagingMove=++Prev++'>Previous</a>";			
		}
		
		// create next link
		String nextLink = "";
		if(hasNext){
			nextLink = "<a href='" + linkStart + action + "&postParams=true" + link + "&fpdbr_0_PagingMove=++Next++'>Next</a>";			
		}
		
		// return
		return new String [] {prevLink, nextLink};
	}
	
	
	/**
	 * Called by the parser through reflection
	 */
	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action)
    throws ro.cst.tsearch.exceptions.ServerResponseException {

		// split the rows
		String prefix = htmlString.contains("<b>Location</b>") ? "Address" : "Name";
		
		p.splitResultRows(pr, htmlString, pageId, "<TR", "</tr>", linkStart, action, prefix, "");
		
        // remove table header
        Vector rows = pr.getResultRows();        
        if (rows.size()>0){ 
            ParsedResponse firstRow = (ParsedResponse)rows.remove(0); 
            pr.setResultRows(rows);
            pr.setHeader(pr.getHeader() + firstRow.getResponse()); 
        }
    }

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String response = Response.getResult();
		String linkStart = CreatePartialLink(TSConnectionURL.idGET);
		
		String detailsPage, intermPage;
		
		switch(viParseID){
			
			case ID_DETAILS:
			case ID_SEARCH_BY_PARCEL:
			case ID_SEARCH_BY_PROP_NO:
				String instr = "";
				if(response.contains("<html>")){
					detailsPage = extractDetails(response);
					instr = StringUtils.extractParameter(response, "Folio:\\s*(\\d+[-]?\\d+)");	
					if(detailsPage == null){
						return;
					}
				}else{
					detailsPage = response;
					instr = StringUtils.extractParameter(detailsPage, "<b>Folio:</b>&nbsp;(\\d+-\\d+)<br/>");
				}
				 			
				if("".equals(instr)){
					logger.error("Instrument number not found!");
					return;
				}
				if(!downloadingForSave) {
					String qry = Response.getRawQuerry();
	                qry = "dummy=" + instr + "&" + qry;
	                String originalLink = sAction + "&" + qry;
	                String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
	                
	                HashMap<String, String> data = new HashMap<String, String>();
	                data.put("type","CNTYTAX");
					if (isInstrumentSaved(instr,null,data)){
	                	detailsPage += CreateFileAlreadyInTSD();
	                }else {	                	
	                    mSearch.addInMemoryDoc(sSave2TSDLink, detailsPage);
	                    detailsPage = addSaveToTsdButton(detailsPage, sSave2TSDLink, viParseID);
	                }               
	                LinkInPage lip = new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD);
	                Response.getParsedResponse().setPageLink(lip);
	                Response.getParsedResponse().setResponse(detailsPage);
				} else {
					msSaveToTSDFileName = instr + ".html";				
	                parser.Parse(Response.getParsedResponse(), detailsPage, Parser.PAGE_DETAILS, 
	                		getLinkPrefix(TSConnectionURL.idPOST), TSServer.REQUEST_SAVE_TO_TSD);
				}
				break;
				
			case ID_SEARCH_BY_NAME:
			case ID_SEARCH_BY_ADDRESS:
				
				// extract the intermediate results table
				intermPage = extractIntermTable(response);
				if(intermPage == null){return;}				
				
				// rewrite details links
				intermPage = intermPage.replaceAll("<a href=\"property_information\\.asp\\?pmid=(\\d+)\">", 
						"<a href='" + linkStart +"/taxapp/property_information.asp&pmid=$1'>");
				
				// extract prev/next links
				String [] links = extractNextPrevLinks(response, linkStart);
				if(!StringUtils.isEmpty(links[0])){
					intermPage += links[0] + "&nbsp;&nbsp;";
				}
				if(!StringUtils.isEmpty(links[1])){
					intermPage += links[1] + "&nbsp;&nbsp;";
				}
				
				// call the parser				
				String suffix = (viParseID == ID_SEARCH_BY_NAME) ? "Name" : "Address";
	        	parser.Parse(Response.getParsedResponse(), intermPage, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD, suffix, "");
			
	        	// set next link
	        	Response.getParsedResponse().setNextLink(links[1]);
	        	
				break;		

			case ID_GET_LINK:
				
				if(Response.getQuerry().matches("pmid=\\d+")){
					ParseResponse(sAction, Response, ID_DETAILS);
				} else if(Response.getQuerry().contains("postParams=true")){
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				}
				break;
				
			case ID_SAVE_TO_TSD :
				
				downloadingForSave = true;
	            ParseResponse(sAction, Response, ID_DETAILS);
	            downloadingForSave = false;
				break;
		}
		
	}
	
	@Override
	protected String getFileNameFromLink(String link){
		String pmid = StringUtils.extractParameter(link, "&pmid=(\\d+)");
		if("".equals(pmid)){
			pmid = StringUtils.extractParameter(link, "dummy=(\\d+-?\\d+)");
			if("".equals(pmid)){
				throw new RuntimeException("pmid not found!");
			}		
		}
		return pmid + ".html";
	}
	
	/**
	 * Extract folio components
	 * @param folio: looks like 123456-1234 or 1234567890
	 * @return array with fist 6 digits then lasst 4 digits of folio. null if error
	 */
	private static String [] extractFolios(String folio){
		
		Matcher folioMatcher = folioPattern.matcher(folio);
		if(folioMatcher.matches()){
			return new String[]{folioMatcher.group(1), folioMatcher.group(2)};
		}
		return null;
	}
	
	private static String [] extractPinParts(String pin){
		
		Matcher pinMatcher = pinPattern.matcher(pin);
		if(pinMatcher.matches()){
			return new String[]{pinMatcher.group(1), pinMatcher.group(2), pinMatcher.group(3), pinMatcher.group(4),
					pinMatcher.group(5), pinMatcher.group(6), pinMatcher.group(7), pinMatcher.group(8)};
		}
		return null;
	}

	@Override
    public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException
    {
		// split the folio into its parts for folio search
        if(module.getModuleIdx() == TSServerInfo.PARCEL_ID_MODULE_IDX){
            String [] folios = extractFolios(module.getFunction(12).getParamValue());
    		if(folios == null){
    			return ServerResponse.createErrorResponse("Invalid Folio!");
    		}
        	module.getFunction(0).setParamValue(folios[0]);
        	module.getFunction(1).setParamValue(folios[1]);
        }
        // split the PIN into its parts for PIN search
        if(module.getModuleIdx() == TSServerInfo.PROP_NO_IDX){
        	String[] pinParts = extractPinParts(module.getFunction(12).getParamValue());
        	if(pinParts == null){
    			return ServerResponse.createErrorResponse("Invalid PIN!");
    		}
        	module.getFunction(2).setParamValue(pinParts[0]);
        	module.getFunction(3).setParamValue(pinParts[1]);
        	module.getFunction(4).setParamValue(pinParts[2]);
        	module.getFunction(5).setParamValue(pinParts[3]);
        	module.getFunction(6).setParamValue(pinParts[4]);
        	module.getFunction(7).setParamValue(pinParts[5]);
        	module.getFunction(8).setParamValue(pinParts[6]);
        	module.getFunction(9).setParamValue(pinParts[7]);
        }
        
        // continue default behaviour
        return super.SearchBy(module, sd);
    }

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
	
		String year = "" + (1900 + new Date().getYear() - 1);

		FilterResponse nonRealEstate = new RejectNonRealEstate(SearchAttributes.OWNER_OBJECT, searchId);
		nonRealEstate.setThreshold(new BigDecimal("0.65"));
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		
		// P1 : search by PIN	
		if(hasPin()){
			String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
			
			if(folioPattern.matcher(pin).matches()) {
				// folio number
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.getFunction(12).setSaKey(SearchAttributes.LD_PARCELNO);  
				modules.add(module);
			} else if(pinPattern.matcher(pin).matches()) {
				// PIN
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PROP_NO_IDX));
				module.clearSaKeys();
				module.getFunction(12).setSaKey(SearchAttributes.LD_PARCELNO);  
				modules.add(module);
			} else {
				SearchLogger.logWithServerName("Will not search with PIN = " + pin + " because it doesn't have the required format.",
						searchId, SearchLogger.ERROR_MESSAGE, getDataSite());
			}
		}

		// P2: search by address
		String strSuf = getSearchAttribute(SearchAttributes.P_STREETSUFIX);
		
		if(hasStreetNo() && hasStreet()){
			
			// search with suffix and direction if present
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).setSaKey(SearchAttributes.P_STREETNO);
			module.getFunction(1).setSaKey(SearchAttributes.P_STREETDIRECTION_ABBREV);
			module.getFunction(2).setSaKey(SearchAttributes.P_STREETNAME);
			module.getFunction(3).setSaKey(SearchAttributes.P_STREETSUFIX);			
			module.getFunction(4).setDefaultValue(year);
			module.addFilter(nonRealEstate);
			module.addFilter(addressFilter);
			modules.add(module);
			
			// search without suffix
			if(!StringUtils.isEmpty(strSuf)){
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
				module.clearSaKeys();
				module.getFunction(0).setSaKey(SearchAttributes.P_STREETNO);
				module.getFunction(2).setSaKey(SearchAttributes.P_STREETNAME);
				module.getFunction(4).setDefaultValue(year);
				module.addFilter(addressFilter);
				modules.add(module);				
			}
		}
		
		// P3: search by owners
		if(getSearch().getSa().hasOwner()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();			
			module.getFunction(1).setDefaultValue(year);

			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(
					SearchAttributes.OWNER_OBJECT, searchId, module));
			
			module.addFilter(nonRealEstate);
			
			module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
			.getConfigurableNameIterator(module, searchId, new String[] {"L;F;","L;f;", "L;M;", "L;m;"});
			module.addIterator(nameIterator);
			
			modules.add(module);	
		}
		
				
		// set list for automatic search 
		serverInfo.setModulesForAutoSearch(modules);	
		
	}
}
