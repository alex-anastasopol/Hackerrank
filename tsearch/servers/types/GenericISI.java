package ro.cst.tsearch.servers.types;


import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.connection.http2.ISIConn;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.MultiplePinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
 * Generic class for ILXxxxxxxIS
 */
public class GenericISI extends TSServer {

	public static final long serialVersionUID = 10000000L;
	private boolean downloadingForSave = false;
	
	
	private static final Pattern countInfoPattern   = Pattern.compile("(Showing \\d+-\\d+ of \\d+)");
	private static final Pattern crtPagePattern     = Pattern.compile("<span>(\\d+)</span>");
	private static final Pattern formActionPattern  = Pattern.compile("<form name=\"Form1\" method=\"post\" action=\"([^?]+)\\?([^\"]+)\"");
	private static final Pattern linkPinPattern     = Pattern.compile("[&;]txtPin=(\\d+)");
	private static final Pattern detailsLinkPattern = Pattern.compile("<A href=\"javascript:GoRptReports\\('([^']+)'\\);\">Property Report</A>");
	private static final Pattern dummyPattern       = Pattern.compile("&dummy=([A-Za-z0-9]+)&");
	
	protected String pinNote;
	protected String ZIP_SELECT;
	protected String LAND_USE_SELECT;
	protected String SCHOOL_SELECT;
	protected String EXEMPTION_SELECT;
	protected String TOWNSHIP_SELECT = null;
	
	public GenericISI(long searchId) {
		super(searchId);
	}

	public GenericISI(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		String county = getSearch().getSa().getCountyName().replaceAll("[\\s\\p{Punct}]+", "");
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		
		EXEMPTION_SELECT = 
			"<select tabindex=\"110\" id=\"ddlExemption\" name=\"ddlExemption\">" +
				"<option value=\"\"> No Selection </option>" +
				"<option value=\"G\">ENTERPRISE ZONE</option>" +
				"<option value=\"H\">HOME INPROVEMENT</option>" +
				"<option value=\"D\">HOMESTEAD</option>" +
				"<option value=\"A\">LIMITED</option>" +
				"<option value=\"F\">OPEN SOURCE</option>" +
				"<option value=\"C\">SENIOR</option>" +
				"<option value=\"I\">STF</option>" +
				"<option value=\"E\">TOTAL</option>" +
				"<option value=\"B\">VETERANS</option>" +
			"</select>";
		
		
		loadSpecialFields();
		
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
			
		if(tsServerInfoModule != null) {
			tsServerInfoModule.setData(10, ISIConn.DDL_COUNTY.get(county));
			tsServerInfoModule.setDefaultValue(10, ISIConn.DDL_COUNTY.get(county));
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if("PIN".equals(functionName)) {
					if (pinNote != null){
						htmlControl.setFieldNote(pinNote);
					}
				}
			}
		}
			
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ADDRESS_MODULE_IDX);
		if(tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(22), ZIP_SELECT);	            
	        setupSelectBox(tsServerInfoModule.getFunction(38), LAND_USE_SELECT);	            
	        setupSelectBox(tsServerInfoModule.getFunction(39), SCHOOL_SELECT);
	        setupSelectBox(tsServerInfoModule.getFunction(40), EXEMPTION_SELECT);
	        tsServerInfoModule.getFunction(35).setParamType(TSServerInfoFunction.idCheckBox);
	        tsServerInfoModule.getFunction(35).setHtmlformat(
	        	"<INPUT TYPE=\"Checkbox\" NAME=\"cbOutOfStateOwners\" CHECKED VALUE=\"P\">");
	        setupSelectBox(tsServerInfoModule.getFunction(50), MONTH_SELECT.replace("@@NAME@@", "ddlRecMonthFrom"));
	        setupSelectBox(tsServerInfoModule.getFunction(51), MONTH_SELECT.replace("@@NAME@@", "ddlRecMonthTo"));
	        setupSelectBox(tsServerInfoModule.getFunction(56), MONTH_SELECT.replace("@@NAME@@", "ddlSaleDateFrom"));
	        setupSelectBox(tsServerInfoModule.getFunction(57), MONTH_SELECT.replace("@@NAME@@", "ddlSaleDateTo"));
	        setupSelectBox(tsServerInfoModule.getFunction(67), MORT_TYPE_SELECT.replace("@@NAME@@", "ddlMortType"));
	        setupSelectBox(tsServerInfoModule.getFunction(68), MONTH_SELECT.replace("@@NAME@@", "ddlMortMonFrom"));
	        setupSelectBox(tsServerInfoModule.getFunction(69), MONTH_SELECT.replace("@@NAME@@", "ddlMortMonTo"));
	        setupSelectBox(tsServerInfoModule.getFunction(79), MORT_TYPE_SELECT.replace("@@NAME@@", "ddlMortType2"));
	        setupSelectBox(tsServerInfoModule.getFunction(80), MONTH_SELECT.replace("@@NAME@@", "ddlMortMonFrom2"));
	        setupSelectBox(tsServerInfoModule.getFunction(81), MONTH_SELECT.replace("@@NAME@@", "ddlMortMonTo2"));
	        if(TOWNSHIP_SELECT != null) {
	        	setupSelectBox(tsServerInfoModule.getFunction(99), TOWNSHIP_SELECT);
	        } else {
	        	tsServerInfoModule.getFunction(99).setHiden(true);
	        	tsServerInfoModule.getFunction(99).setDefaultValue("");
	        	tsServerInfoModule.getFunction(99).setParamType(TSServerInfoFunction.idTEXT);	
	        	tsServerInfoModule.getFunction(99).setLoggable(false);
	        	
	        		
	        }
	        tsServerInfoModule.setData(101, ISIConn.DDL_COUNTY.get(county));
	        tsServerInfoModule.setDefaultValue(101, ISIConn.DDL_COUNTY.get(county));
	        
	        
	        PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if(functionName != null) {
					if(functionName.contains("PIN")) {
						htmlControl.setFieldNote(pinNote);
					} else if(TOWNSHIP_SELECT == null && functionName.contains("Township")) {
						htmlControl.setHiddenParam(true);
					}
						
				}
					
				
			}
	
		}
		
		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
	

	protected void loadSpecialFields() {
	}


	protected static final String MONTH_SELECT = 
		"<select name=\"@@NAME@@\">" +
		"<option value=\"\"> No Selection </option>" +
		"<option value=\"01\">January</option>" +
		"<option value=\"02\">February</option>" +
		"<option value=\"03\">March</option>" +
		"<option value=\"04\">April</option>" +
		"<option value=\"05\">May</option>" +
		"<option value=\"06\">June</option>" +
		"<option value=\"07\">July</option>" +
		"<option value=\"08\">August</option>" +
		"<option value=\"09\">September</option>" +
		"<option value=\"10\">October</option>" +
		"<option value=\"11\">November</option>" +
		"<option value=\"12\">December</option>" +
		"</select>";
	
	protected static final String MORT_TYPE_SELECT = 
		"<select name=\"@@NAME@@\" size=\"1\" style=\"width:150px;\">" +
		"<option value=\"\"> No Selection </option>" +
		"<option value=\"TGCO\">CONST(TGCO)</option>" +
		"<option value=\"TGCA\">CONV ADJ(TGCA)</option>" +
		"<option value=\"MTGC\">CONV(MTGC)</option>" +
		"<option value=\"TGC\">CONV(TGC)</option>" +
		"<option value=\"MTGE\">EQUITY(MTGE)</option>" +
		"<option value=\"TGE\">EQUITY(TGE)</option>" +
		"<option value=\"TGFA\">FHA ADJ(TGFA)</option>" +
		"<option value=\"MTGF\">FHA(MTGF)</option>" +
		"<option value=\"TGF\">FHA(TGF)</option>" +
		"<option value=\"TGVA\">VA ADJ(TGVA)</option>" +
		"<option value=\"MTGV\">VA(MTGV)</option>" +
		"<option value=\"TGV\">VA(TGV)</option>" +
		"</select>";
	
	private FilterResponse getAddressFilter(){
		return AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d , true);
	}
	
	private FilterResponse getMultiPinFilter(){
		return new MultiplePinFilterResponse(searchId);
	}
	
	private FilterResponse getNameFilter(TSServerInfoModule module){
		return NameFilterFactory.getHybridNameFilter(module.getSaObjKey(), searchId, module);
	}
	
	private FilterResponse getOwnerCondoFilter(TSServerInfoModule module){
		FilterResponse filter = getNameFilter(module);
		filter.setMinRowsToActivate(10);
		return filter;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
	
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		
		if (Search.AUTOMATIC_SEARCH == searchType){
			// P0 - search by multiple PINs
			Collection<String> pins = getSearchAttributes().getPins(-1);
			if(pins.size() > 1){			
				for(String pin: pins){
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
					module.clearSaKeys();
					module.getFunction(7).forceValue(pin);
					modules.add(module);	
				}			
				// set list for automatic search 
				serverInfo.setModulesForAutoSearch(modules);
				resultType = MULTIPLE_RESULT_TYPE;
				return;
			}
		}
			
		// load relevant attributes
		String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);		
		String stNo = getSearchAttribute(SearchAttributes.P_STREETNO);
		String stName = getSearchAttribute(SearchAttributes.P_STREETNAME);	
		String stDir = getSearchAttribute(SearchAttributes.P_STREETDIRECTION);					
		String ownLast = getSearchAttribute(SearchAttributes.OWNER_LNAME);
		String city = getSearchAttribute(SearchAttributes.P_CITY);
		
		// make decisions
		boolean hasAddress = !StringUtils.isEmpty(stNo) && !StringUtils.isEmpty(stName);
		boolean hasPin  = !StringUtils.isEmpty(pin);		
		boolean hasOwner = !StringUtils.isEmpty(ownLast);

		// count criteria
		int critCount = 0;
		if(hasAddress){ critCount++; }
		if(hasPin){ critCount++; }
		if(hasOwner){ critCount++; }
				
		// P1 - search by everything we've got, no filtering
		if(critCount > 1){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.addFilter(getMultiPinFilter());			
			modules.add(module);			
		}
		
		// P2 - search by PIN
		if(hasPin){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(7).setSaKey(SearchAttributes.LD_PARCELNO);  
			modules.add(module);		
		}
		
		// P3 - search by address
        if(hasAddress){
        	
        	Collection<String> cities = new LinkedHashSet<String>();
        	cities.add(city); // try first with city
        	cities.add("");   // then without city
        	
        	Collection<String> directions = new LinkedHashSet<String>();
        	directions.add(stDir); // try first with direction
        	if(stDir.length() > 0){
        		directions.add(stDir.substring(0,1));
        	}
        	directions.add("");    // then without direction
        	
        	for(String cit: cities){
        		for(String dir: directions){
        			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
        			module.clearSaKeys();		
        			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
        			module.getFunction(0).setSaKey(SearchAttributes.P_STREETNO);
        			module.getFunction(1).setSaKey(SearchAttributes.P_STREETNO);       			
        			module.getFunction(3).setSaKey(SearchAttributes.P_STREETNAME);	        
        			module.getFunction(6).setSaKey(SearchAttributes.P_STREETUNIT);
        			module.getFunction(2).forceValue(dir);
        			module.getFunction(21).forceValue(cit);		
        			module.addFilter(getAddressFilter());
        			module.addFilter(getOwnerCondoFilter(module));
        			module.addFilter(getMultiPinFilter());	
        			modules.add(module);        			
        		}
        	}
        }

		// P4 - search by Owner
		if(hasOwner && hasAddress ){
			
			
			
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			
			FilterResponse nameFilterHybridDoNotSkipUnique = getNameFilter(module);
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
			
			
			module.clearSaKeys();
			//module.getFunction(8).setSaKey(SearchAttributes.OWNER_LFM_NAME);
			module.addFilter(getAddressFilter());
			
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addFilter(getMultiPinFilter());	
			module.setIteratorType(8,FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
			.getConfigurableNameIterator(module, searchId, new String[] {"L F M;;"});
			module.addIterator(nameIterator);
			
			modules.add(module);	
		}
		
		// set list for automatic search 
		serverInfo.setModulesForAutoSearch(modules);		
	}	
	
	/**
	 * Split the results rows
	 */
	@SuppressWarnings({ "rawtypes" })
	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action)
    throws ro.cst.tsearch.exceptions.ServerResponseException {
		// perform split        
		p.splitResultRows(pr, htmlString, pageId, "<tr", "</tr>", linkStart, action);
		
        // remove table header
        Vector rows = pr.getResultRows();        
        if (rows.size()>0){ 
            ParsedResponse firstRow = (ParsedResponse)rows.remove(0); 
            pr.setResultRows(rows);
            pr.setHeader(pr.getHeader() + firstRow.getResponse()); 
        }
    }
	
    /**
     * get file name from link
     */
	@Override
	protected String getFileNameFromLink(String link){
		Matcher detailsLinkPinMatcher = linkPinPattern.matcher(link);
		if(detailsLinkPinMatcher.find()){
			return  detailsLinkPinMatcher.group(1) + ".html";
		}
		Matcher dummyMatcher = dummyPattern.matcher(link);
		if(dummyMatcher.find()){
			return dummyMatcher.group(1) + ".html";
		}
        return "none.html";
    }
	
	private static int seq = 0;	
	protected synchronized static int getSeq(){
		return seq++;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String initialResponse = Response.getResult();
		String response = initialResponse;
		String ssiteID = TSServer.getCrtTSServerName(miServerID);
		
		String linkStart = CreatePartialLink(TSConnectionURL.idPOST);		
		String contents;
		
		String link = getCrtServerLink();
		switch(viParseID){
		case ID_SEARCH_BY_NAME:
			
			// check if password expired!
			if(response.contains("<title>ISI - Login</title>")){
				String userName = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), getClass().getSimpleName(), "user");
				Response.getParsedResponse().setError("<font color=\"red\">Redirected to login page! Probably the password for account <b>" + userName + "</b> needs to be updated!</font>");
				return;	
			}
			
			// get and save form action
			Matcher formActionMatcher = formActionPattern.matcher(response);
			if(!formActionMatcher.find()){
				return;
			}
			String formLink = link + ".com/ISI/search/" + formActionMatcher.group(1) + "?" + formActionMatcher.group(2);			

			// get and save view state
			String viewState = StringUtils.getTextBetweenDelimiters("\"__VIEWSTATE\" value=\"" , "\"", initialResponse);
			if("".equals(viewState)){
				return;
			}

			int lnk = getSeq();
			Map<String, String> params = ro.cst.tsearch.connection.http2.ISIConn.isolateParams(response, "Form1");
			params.put("nextLink", formLink);
			mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + lnk, params);
			
			// determine contents
			contents = getIntermediateContents(response);
			if(contents == null){
				return;
			}
			
			
			// rewrite all interm links as special links
			contents = contents.replaceAll("<a target='rptHead' href='../reports/rptHead.aspx\\?([^']+)'>([^<]+)</a>",
					"<a href=\"" + linkStart + "/ISI/reports/rptHead.aspx&$1\">$2</a>");
			
			// determine count info
			String countInfo = "";
			Matcher countInfoMatcher = countInfoPattern.matcher(response);
			if(countInfoMatcher.find()){
				countInfo = countInfoMatcher.group(1);
			}
			
			// determine prev/next page
			String [] links = extractPrevNextLinks(response, linkStart, lnk);
			String prevLink = links[0];
			String nextLink = links[1];
									
			// create addInfo
			String addInfo = "";
			if(nextLink != null && prevLink != null){
				addInfo = "<br>" + addInfo + prevLink + "&nbsp;&nbsp;" + nextLink + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"; 	
			} else{
				addInfo = (nextLink != null)?("<br>" + addInfo + nextLink + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"):addInfo;
				addInfo = (prevLink != null)?("<br>" + addInfo + prevLink + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"):addInfo;
			} 
			
			Matcher crtPageMatcher = crtPagePattern.matcher(response);
			if(crtPageMatcher.find()){{
				addInfo += "Page " + crtPageMatcher.group(1) + "&nbsp;&nbsp;&nbsp;";
			}
				
			}
			addInfo += countInfo;			
			
			// call the parser
        	parser.Parse(Response.getParsedResponse(), contents, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idPOST), TSServer.REQUEST_SAVE_TO_TSD);

			// append the additional info
        	Response.getParsedResponse().setFooter(Response.getParsedResponse().getFooter() + addInfo);
        	
			// set next link
			Response.getParsedResponse().setNextLink(nextLink);
			
			break;
			
		case ID_DETAILS:
			
			// get the real details page if necessary
			if(!response.contains(">Property Report</th></tr></table>")){ 
				
				// get the real details link page
				Matcher detailsLinkMatcher = detailsLinkPattern.matcher(initialResponse);
				if(!detailsLinkMatcher.find()){
					Response.getParsedResponse().setError("Selected document has no details!");
					return;				
				}
				String detailsLink = link + ".com" + detailsLinkMatcher.group(1);
				
				// get the details page
				response = null;
				HTTPResponse resp = null;
				for(int i=0; i<2; i++){
					try{
						HTTPRequest req = new HTTPRequest(detailsLink);
						HttpSite site = HttpManager.getSite(ssiteID, searchId);
						try
						{
							resp = site.process(req);
						} finally 
						{
							HttpManager.releaseSite(site);
						}	
						break;
					}catch(RuntimeException e){
						logger.error(e);
					}
				}
				
				response = resp.getResponseAsString();
				
				if(response == null){
					Response.getParsedResponse().setError("Error retrieving document details!");
					return;				
				}
			}

			// isolate the PIN
			Matcher detailsLinkPinMatcher = linkPinPattern.matcher(response);
			if(!detailsLinkPinMatcher.find()){
				Response.getParsedResponse().setError("Error retrieving document details!");
				return;				
			}
			String pin = detailsLinkPinMatcher.group(1);
			
			// create filename
			String fileName = pin + "_ISI_.html";
			//String fileNameM = pin + "_apMISCELLANEOUS.html" ;
			
			// get the detail contents
			contents = getDetailContents(response);
			if(contents == null){
				Response.getParsedResponse().setError("Selected document has no details!");
				return;				
			}
			
			if ((!downloadingForSave)) {				
				
				String originalLink = sAction + "&dummy=" + pin + "&" + Response.getQuerry();
                String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
                
                HashMap<String, String> data = new HashMap<String, String>();
				data.put("type","ASSESSOR");
				data.put("dataSource", "IS");
				
                //if (FileAlreadyExist(fileName) || FileAlreadyExist(fileNameM)) {
				if(isInstrumentSaved(pin, null, data)){
                	contents += CreateFileAlreadyInTSD();
                } else {
                	contents = addSaveToTsdButton(contents, sSave2TSDLink, viParseID);
                    mSearch.addInMemoryDoc(sSave2TSDLink, response);
                }

                Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
                Response.getParsedResponse().setResponse(contents);
                
			} else {
				
                msSaveToTSDFileName = fileName;
                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
                msSaveToTSDResponce = contents + CreateFileAlreadyInTSD();                
                parser.Parse(Response.getParsedResponse(), contents, Parser.PAGE_DETAILS);
                
			}
			
			break;
			
		case ID_GET_LINK:
			
			if(sAction.contains("/ISI/reports/rptHead.aspx")){
				ParseResponse(sAction, Response, ID_DETAILS);
			} else if(sAction.contains("next:dgResults:_ctl")){
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			}			
			break;
			
		case ID_SAVE_TO_TSD:			
			
			downloadingForSave = true;
			ParseResponse(sAction, Response, ID_DETAILS);
			downloadingForSave = false;
			
			break;
		}
	}
	
	@Override
    public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException
    {
		// eliminate dashes from pins
		int startIdx = 2, endIdx = 1;
        if(module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX){
        	startIdx = 7;
        	endIdx = 7;
        } else if(module.getModuleIdx() == TSServerInfo.ADDRESS_MODULE_IDX){
        	startIdx = 23;
        	endIdx = 28;
        }
        for(int i=startIdx; i<=endIdx; i++){
        	TSServerInfoFunction function = module.getFunction(i);
        	function.setParamValue(function.getParamValue().replaceAll("\\p{Punct}", ""));
        }
        return super.SearchBy(module, sd);
    }
	
	private String getIntermediateContents(String response){
		
		if(response.contains("No records found")){
			return null;
		}
		
		int istart = response.indexOf("<table class=\"searchResultTable\"");
		if(istart == -1){
			return null;
		}
		int iend = response.indexOf("</table>", istart);
		if(iend == -1){			
			return null;
		}
		iend += "</table>".length();
		
	    String contents = response.substring(istart, iend);
		
	    // eliminate the pages links rows
	    contents = StringUtils.removeSimpleTag(contents, contents.indexOf("<td colspan=\"12\">"), "tr");
	    contents = StringUtils.removeSimpleTag(contents, contents.indexOf("<td colspan=\"12\">"), "tr");
	    
	    // elliminate the checkbox row
	    contents = StringUtils.removeSimpleFirstColumn(contents);
	    
	    // cleanup
	    contents = contents.replaceFirst("(?i)<table[^>]+>","<table border='1' cellspacing='0' cellpadding='0' width='100%'>");
	    contents = contents.replaceAll("(?i)</?font[^>]*>","");
	    contents = contents.replaceAll("(?i)<tr[^>]*>","<tr>");
	    contents = contents.replaceAll("(?i)<th[^>]*>","<th align='center'>");	    
	    contents = contents.replaceFirst("<tr[^>]*>","<tr bgcolor='#cccccc'>");
	    contents = contents.replaceAll("(?i)<td[^>]*><a href=\"javascript:__doPostBack\\('dgResults\\$_ctl\\d+\\$_ctl\\d+',''\\)\"[^>]*>(PIN|Address|City|Owner|Recorded Date|Sale Date|Sale Price|Land Use)</a></td>","<th align=\"center\">$1</th>");
	    
		return contents;
	}
	
	private String getDetailContents(String response){
		
		int istart, iend;
		istart = response.indexOf("Property Report</th></tr></table>");
		iend = response.indexOf("</form>");
		if(istart == -1 || iend == -1){
			return null;
		}
		istart += "Property Report</th></tr></table>".length() + 1;
		String contents = response.substring(istart, iend);
	    contents = contents.replaceAll("(?i)</?font[^>]*>","");
	    
	    contents = contents.replaceAll("class=\"clslabelcell\"", "bgcolor=\"#cccccc\"");
	    contents = contents.replaceAll("(?i)<span (?:id=\"[^\"]+\" )*class=\"headerDivTital\">([^<]+)</span>","<span><b>$1</b></span>");
	    contents = contents.replaceAll("(?i) (?:class|id)=\"[^\"]*\"", "");
	    contents = contents.replaceAll("(?i) (?:colSpan|rowSpan)=\"1\"","");
	    
	    contents = "<table align=\"center\" border=\"1\" cellpadding=\"0\" cellspacing=\"0\"><tr><td>" + contents + "</td></tr></table>";
	    
		return contents;
		
	}
	
	private String getCrtServerLink(){
		String link = getDataSite().getLink();
		int idx = link.indexOf(".com");
		if(idx == -1){
			throw new RuntimeException("County " + getDataSite().getName() + " not supported by this class!");
		}
		return link.substring(0, idx);
	}

	private String [] extractPrevNextLinks(String response, String linkStart, int lnk){

		// isolate links row
		int istart = response.indexOf("<td colspan=\"12\">");
		int iend = response.indexOf("</td>", istart);
		if(istart == -1 || iend == -1){
			return new String [] {null, null};
		}
		String row = response.substring(istart, iend);
		
		// determine crt page
		Matcher crtPageMatcher = crtPagePattern.matcher(row);
		if(!crtPageMatcher.find()){
			return new String [] {null, null};
		}
		int crtPage = Integer.valueOf(crtPageMatcher.group(1));
		
		// determine prevPage
		String prevLink = null;
		if(crtPage != 1){
			Pattern pattern = Pattern.compile("<a href=\"javascript:__doPostBack\\('dgResults\\$_ctl(\\d+)\\$_ctl(\\d+)',''\\)\">" + (crtPage-1) + "</a>");
			Matcher matcher = pattern.matcher(row);
			if(matcher.find()){
				prevLink  = "<a href='" + linkStart + "next:dgResults:_ctl" + matcher.group(1) + ":_ctl" +  matcher.group(2) + "&lnk=" + lnk +"'>Previous</a>";
			}
		}
		
		// determine nextPage
		String nextLink = null;
		Pattern pattern = Pattern.compile("<a href=\"javascript:__doPostBack\\('dgResults\\$_ctl(\\d+)\\$_ctl(\\d+)',''\\)\">" + (crtPage+1) + "</a>");
		Matcher matcher = pattern.matcher(row);
		if(matcher.find()){
			nextLink  = "<a href='" + linkStart + "next:dgResults:_ctl" + matcher.group(1) + ":_ctl" +  matcher.group(2) + "&lnk=" + lnk +"'>Next</a>";
		}

		
		// return result
		return new String [] {prevLink, nextLink};
	}
	
	@Override
	protected int getResultType(){
		/*
		 * we have multiple results after a multiple PIN filtering
		 * or if we have multiple PINs in the search page
		 * in the latter case we will only search by PIN on ISI anyway
		 */ 
		if(mSearch.getAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN) == Boolean.TRUE ||
				(mSearch.getSa().getPins(-1).size() > 1 && (Search.AUTOMATIC_SEARCH == mSearch.getSearchType()))){
			return MULTIPLE_RESULT_TYPE; 
		} else {
			return UNIQUE_RESULT_TYPE;
		}
	}
	
	@Override
    public boolean anotherSearchForThisServer(ServerResponse sr) {
		/*
		 * We will iterate through all PINs from the search page
		 * but we will not issue the rest of the searches after a multiple PIN hit 
		 */
		return  mSearch.getSa().getPins(-1).size() > 1 &&
			    mSearch.getAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN) != Boolean.TRUE;
	}  
	
}
