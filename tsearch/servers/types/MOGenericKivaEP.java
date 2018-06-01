package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.connection.http3.HttpManager3;
import ro.cst.tsearch.connection.http3.HttpSite3;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

public class MOGenericKivaEP extends TSServer {

	private static final long serialVersionUID = -2754273316620692457L;
	private boolean downloadingForSave = false;

	/**
	 * Constructor
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 * @param searchId
	 * @param miServerID
	 */
	public MOGenericKivaEP(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	public String getPrefix(int miServerID){
		if (miServerID == 460103){ //MOClayEP
			return "CL";
		} else if(miServerID == 462503){//MOJacksonEP
			return "JA";
		} else if(miServerID == 466003){//MOPlatteYA
			return "PL";
		}
		String possbilePrefix = getCurrentServerName().substring(2, 4);
		
		return possbilePrefix.toUpperCase();
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
		
		if(tsServerInfoModule != null) {
			String prefix = getPrefix(miServerID);
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if(StringUtils.isNotEmpty(functionName) && "APN".equals(functionName)) {
					htmlControl.setFieldNote("Use with " + prefix + "% if the APN field is unknown");
					htmlControl.setDefaultValue(prefix + "%");
					htmlControl.getCurrentTSSiFunc().forceValue(prefix + "%");
				} else if(StringUtils.isNotEmpty(functionName) && "Domain".equals(functionName)) {
					htmlControl.setDefaultValue(prefix );
					htmlControl.getCurrentTSSiFunc().forceValue(prefix);
				}
			}
		}
		
		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		serverInfo.setModulesForAutoSearch(modules);
		
		// search only if Kansas city  
		String city = org.apache.commons.lang.StringUtils.defaultString(getSearchAttribute(SearchAttributes.P_CITY)).toUpperCase();
		if(!StringUtils.isEmpty(city)){
			if(!city.startsWith("KANSAS")){
				return;
			}			
		}
		
		// create filters
//		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d);
		FilterResponse nameFilterHybridDoNotSkipUnique = null;
		// prefix to be used for APN field
		String prefix = getPrefix(miServerID);

        // pin search
		for(String source: new String[]{"P", "S", "E"}){
			String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
			if(!StringUtils.isEmpty(pin)){
				pin = pin.replaceAll("\\p{Punct}", "").replaceAll("\\s+", "");
				if(pin.matches("\\d+")){
					pin = prefix + pin;
				}
	        	TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
	        	module.clearSaKeys();	        	
	        	module.getFunction(7).forceValue(pin);
	        	module.getFunction(9).forceValue(prefix);
	        	module.getFunction(15).forceValue(source);        	
//		        module.addFilter(addressFilter);	
				modules.add(module);
	        }
		}
		
		// address search
		for(String source: new String[]{"P", "S", "E"}){			
			String streetName = getSearchAttribute(SearchAttributes.P_STREETNAME);
			if(!StringUtils.isEmpty(streetName)){
				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));			
		        module.clearSaKeys();	        
		        module.getFunction(0).setSaKey(SearchAttributes.P_STREETNO);
		        // module.getFunction(2).setSaKey(SearchAttributes.P_STREETDIRECTION);
		        module.getFunction(3).setSaKey(SearchAttributes.P_STREETNAME);
		        module.getFunction(6).setSaKey(SearchAttributes.P_STREETUNIT);
		        module.getFunction(7).forceValue(prefix + "%");
		        module.getFunction(9).forceValue(prefix);
		        module.getFunction(15).forceValue(source);
//		        module.addFilter(addressFilter);
		        modules.add(module);	        
			}
		}
		
		// name search
		for(String source: new String[]{"P", "S", "E"}){
			
			nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter( 
					SearchAttributes.OWNER_OBJECT , searchId , null );
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
			
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(15).forceValue(source);
			module.getFunction(7).forceValue(prefix + "%");
			module.getFunction(9).forceValue(prefix);
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.getFunction(14).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			ModuleStatesIterator iterator = new ConfigurableNameIterator(searchId, new String[]{"L%F%;;"});
			iterator.setInitAgain(true);
			module.addIterator(iterator);					 
//			module.addFilters(nameFilterHybridDoNotSkipUnique, addressFilter);
			modules.add(module);
		}
		
	}
	

	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
	
		
        return super.SearchBy(module, sd);
	}
	
	/**
	 * Extract intermediate results table
	 * @param response
	 * @return
	 */
	private String getInterm(String response){
		
		int istart = response.indexOf("<table border=\"1\" cellspacing=\"0\" cellpadding=\"1\">");
		if(istart == -1){ return ""; }
		int iend = response.indexOf("</table>", istart);
		if(iend == -1){ return ""; }
		iend += "</table>".length();
		
		String interm = response.substring(istart, iend);
		
		interm = interm.replaceAll("(?i)<a href=\"index\\.cfm\\?order_column=?[^>]*>", "");
		interm = interm.replaceAll("(?i)</a></th>", "</th>");	
		interm = interm.replaceAll("<th[^>]*>", "<th align='left'>");
		interm = interm.replaceFirst("<tr[^>]*>", "<tr bgcolor='#DEDEDE'>");		
		interm = interm.replaceAll("/kivanet/2/land/summary/index.cfm\\?" + "([^>]*)>", CreatePartialLink(TSConnectionURL.idGET) + "/kivanet/2/land/summary/index.cfm&" + "$1" + "'>");
        
		return interm;

	}
	
	/**
	 * Extract details 
	 * @param response
	 * @return
	 */
	private String getDetails(String response, HttpSite site){
		
		int istart = response.indexOf("Parcel Summary");
		if(istart == -1){ return ""; }
		istart = response.lastIndexOf("<center>", istart);
		if(istart == -1){ return ""; }		
		int iend = response.lastIndexOf("</center>");
		if(iend == -1){ return ""; }
		iend += "</center>".length();
		
		Pattern p;
		Matcher ma;
		
        int st = -1, ed = -1; 
        String result1 = "";
        istart = -1;
		iend = -1;
		
		istart = response.indexOf("<table ");
		istart = response.indexOf("<table ", istart + 1);

		if (istart == -1)
			return "";

		String parcelNO = "";
		HtmlParser3 htmlparser3 = new HtmlParser3(response);
		parcelNO = htmlparser3.getValueFromNextCell(htmlparser3.getNodeList(), "APN:", "", false);
		if (org.apache.commons.lang.StringUtils.isNotBlank(parcelNO) && !parcelNO.contains("SEGMENT PARCEL")){
			parcelNO = parcelNO.replaceAll("(?is)&nbsp;", " ");
			parcelNO = parcelNO.trim();
			int tuMiServerID = (int) TSServersFactory.getSiteId(getDataSite().getStateAbrev(), getDataSite().getCountyName(), "TU");
			
			HttpSite3 quickieSite = null;
			try {
				quickieSite = HttpManager3.getSite(getCrtTSServerName(tuMiServerID), searchId);
			} catch (Exception e) { }
			
			if (quickieSite != null) {
				try {
					result1 = ((ro.cst.tsearch.connection.http3.MOGenericQuickTaxEP) quickieSite).getResponseForPIN(parcelNO);
					result1 = result1.replaceAll("(?is)</?a[^>]*>", "");
					result1 = result1.replaceAll("(?is)</?COLGROUP[^>]*>", "");
					result1 = result1.replaceAll("(?is)</?COL[^>]*>", "");
					result1 = result1.replaceAll("(?is)<table[^>]*>", "<table border = \"1\">");
					result1 = result1.replaceAll("(?is)<th[^>]*>", "<th>");
					result1 = result1.replaceAll("(?is)<tr[^>]*>", "<tr>");
					result1 = result1.replaceAll("(?is)<td[^>]*>", "<td>");
				} finally {
					// always release the HttpSite
					HttpManager3.releaseSite(quickieSite);
				}
			}
		}
		
		iend = response.lastIndexOf("</table>") + "</table>".length();

		response = response.substring(istart, iend);
		response = response.replaceAll("class=\".*?\"", "");

//		String parcelNumber = response;
//		p = Pattern.compile("(?s)<td .*?</td>.*?<td.*?\">(\\D{2}\\d+)");
//		ma = p.matcher(parcelNumber);
//		if (ma.find()) {
//			parcelNumber = ma.group(1);
//		}

		// Display Legal
		String link = response;
		p = Pattern.compile("(?s)<a  href=\"(.*?)\".*?>Display Legal");
		ma = p.matcher(link);
        
        String result = "";
		if (ma.find()) {
			
			link = "http://kivaweb.kcmo.org/kivanet/2/land/summary/" + ma.group(1);            
			result = site.process(new HTTPRequest(link)).getResponseAsString();

            // map
            istart = result.indexOf("<table ");
            istart = result.indexOf("<table ", istart + 1);
            istart = result.indexOf("<table ", istart + 1);
            iend = result.indexOf("</table>", istart + 1) + "</table>".length();

            result = result.substring(istart, iend);
		}
		response += result;
		if (org.apache.commons.lang.StringUtils.isNotBlank(result1)){
			response += "<br><br>" + result1;
		} else{
			response += "<br><br>Additional site info not found.";
		}
		response = response.replaceAll("(?s)<a  href.*?</a>", "");
		response = response.replaceAll("(?is)<\\d+\\s*:\\s*\\d+[^<]+", "");
		
		return response;
	}
	
	/**
	 * Get next or previous link
	 * @param response
	 * @param buttonText is either "<< Prev 20" or "Next 20 >>"
	 * @param linkText is either "Prev" or "Next"
	 * @return
	 */
	private String getNavLink(String response, String buttonText, String linkText){
		
		int istart = response.indexOf(buttonText);
		if(istart == -1){ return null; }
		int istart1 = response.lastIndexOf("<form", istart);
		if(istart1 == -1){
			istart1 = response.lastIndexOf("<FORM", istart);
		}
		istart = istart1;
		if(istart == -1){ return null; }
		int iend = response.indexOf("</form>", istart);
		if(iend == -1){
			iend = response.indexOf("</FORM>", istart);
		}
		if(iend == -1){ return null; }
		iend += "</form>".length();
		String nextForm = response.substring(istart, iend);
		SimpleHtmlParser.Form form = new SimpleHtmlParser(nextForm).getForm(0);
		if(form == null){ return null; }
		
		String link = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + "/kivanet/2/land/lookup/" + form.action;
		for(Map.Entry<String, String> entry: form.getParams().entrySet()){
			link += "&" + entry.getKey() + "=" + StringUtils.urlEncode(entry.getValue());
		}
		link += "\">" + linkText + "</a>";
				
		return link;
	}
	
	/**
	 * 
	 * @param response
	 * @return
	 */
	protected String getNextLink(String response){
		return getNavLink(response, "Next 20 >>", "Next");
	}
	
	/**
	 * 
	 * @param response
	 * @return
	 */
	protected String getPrevLink(String response){
		return getNavLink(response, "<< Prev 20", "Prev");
	}


	@Override
	protected void ParseResponse(String action, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String initialResponse = Response.getResult();
		String response = initialResponse;
		
		ParsedResponse pr = Response.getParsedResponse();
	
		switch(viParseID){
		case ID_SEARCH_BY_NAME:
			
			// check whether it is a details page
			if(response.contains("Parcel Summary")){
				ParseResponse(action, Response, ID_DETAILS);
				return;
			}
			
			//extract intermediate table
			String interm = getInterm(response);
			if(StringUtils.isEmpty(interm)){
				return;
			}
			
			// remove alias records
	        int noAlias = 0;
			Pattern pa = Pattern.compile("(?s)(<tr>\\s*?<td.*?(ALIAS|MASTER).*?)</tr>");
	        Matcher ma = pa.matcher(interm);
	        while (ma.find()) {
	           String s = ma.group(0);
	           if (s.indexOf("ALIAS") != -1){   
	        	   noAlias ++;
	               int x = interm.indexOf(s);
	               interm = interm.substring(0, x) + interm.substring(x + s.length()); 
	           }
	        }
	        
			// add navigation links
			String prevLink = getPrevLink(response);
			if(prevLink != null){
				interm += prevLink + "&nbsp;&nbsp;&nbsp;&nbsp;";
			}
			String nextLink = getNextLink(response);
			if(nextLink != null){
				interm += nextLink;
			}
			
			// add navigation info
			String info = StringUtils.extractParameter(response, "(Displaying \\d+ - \\d+ of  \\d+ matching records)");
			if(!StringUtils.isEmpty(info)){				
				if(noAlias != 0){
					info += " (" + noAlias + " alias entries not shown) ";
				}
				interm = info + interm;
			}
			
			Response.setResult(interm);
	        parser.Parse(pr, interm, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
            pr.setNextLink(nextLink);
			
			break;
			
		case ID_DETAILS:

			String details = response;
			
			if(response.contains("DOCTYPE HTML PUBLIC")){			
				HttpSite site = HttpManager.getSite(getCrtTSServerName(miServerID), searchId);
				try {
					details = getDetails(response, site);				
				}finally {
					HttpManager.releaseSite(site);
				}				
				if(StringUtils.isEmpty(details)){
					pr.setError("Details page not found!");
					return;
				}
			}
			String responseForParser = response;
			responseForParser = responseForParser.replaceAll("(?is)<th ", "<td ").replaceAll("(?is)</th> ", "</td>");
			
			String parcelNumber = "";
			HtmlParser3 htmlparser3 = new HtmlParser3(responseForParser);
			parcelNumber = htmlparser3.getValueFromNextCell(htmlparser3.getNodeList(), "APN:", "", false);
			parcelNumber = parcelNumber.replaceAll("(?is)&nbsp;", " ").trim();
			String streetNumber = htmlparser3.getValueFromAbsoluteCell(1, -1, HtmlParser3.findNode(htmlparser3.getNodeList(), "Frac"), "", false, true);
			streetNumber = streetNumber.replaceAll("(?is)&nbsp;", " ").trim();
			
			if (parcelNumber.contains("SEGMENT PARCEL")){
				parcelNumber += streetNumber;
			}
			
						
			if (!downloadingForSave) {

				String originalLink = action + "&dummy=" + parcelNumber + "&" + Response.getQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", "CITYTAX");

				if (isInstrumentSaved(parcelNumber, null, data)){
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details); // initialResponse
					details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);					
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parser.Parse(Response.getParsedResponse(), details, Parser.NO_PARSE);

			} else {

				// for html
				msSaveToTSDFileName = parcelNumber + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				
				String detailsforParsing = Tidy.tidyParse(details, null);
				detailsforParsing = detailsforParsing.replaceAll("(?is)\\bdata=\\\"[^\\\"]*\\\"", "");
				detailsforParsing = detailsforParsing.replaceAll("(?is)-?maxlength=\\\"[^\\\"]*\\\"", "");
				detailsforParsing = detailsforParsing.replaceAll("(?is)\\brole=\\\"[^\\\"]*\\\"", "");
				detailsforParsing = detailsforParsing.replaceAll("(?is)<colgroup>.*?</colgroup>", "");
				
				
				parser.Parse(Response.getParsedResponse(), detailsforParsing, Parser.PAGE_DETAILS);
			}			
			
			break;
			
		case ID_GET_LINK:
		case ID_SAVE_TO_TSD:
			
            if (action.equals("/kivanet/2/land/lookup/index.cfm") && viParseID != ID_SAVE_TO_TSD)
                ParseResponse(action, Response, ID_SEARCH_BY_NAME);
            else if (viParseID == ID_GET_LINK)
                ParseResponse(action, Response, ID_DETAILS);
            else {
                downloadingForSave = true;
                ParseResponse(action, Response, ID_DETAILS);
                downloadingForSave = false;
            }
            
			break;
		}				
		
	}

	@Override
	protected String getFileNameFromLink(String link) {
		 return link.substring(link.indexOf("pin=") + 4, link.length());
	}

	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action) 
    	throws ro.cst.tsearch.exceptions.ServerResponseException {

        p.splitResultRows(pr, htmlString, pageId, "<tr", "</table>", linkStart, action);

        // remove header
        Vector rows = pr.getResultRows();
        if (rows.size() > 0) {
            ParsedResponse firstRow = (ParsedResponse)rows.remove(0);           
            pr.setResultRows(rows);
            pr.setHeader(pr.getHeader() + firstRow.getResponse());
        }
        
        //insert dummy parcelid
        for (int i = 0; i < rows.size(); i++) {
            ParsedResponse row = (ParsedResponse) rows.get(i);
            PropertyIdentificationSet data = row.getPropertyIdentificationSet(0);
            String pid = data.getAtribute("ParcelID");            
            row.getPageLink().setOnlyLink(row.getPageLink().getLink().replaceFirst("\\.html", ".html&dummy=" + pid));
            row.getPageLink().setOnlyOriginalLink(row.getPageLink().getOriginalLink().replaceFirst("\\.html", ".html&dummy=" + pid));
        }
    }	
	
}
