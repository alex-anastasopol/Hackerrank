package ro.cst.tsearch.servers.types;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.HTTPSiteInterface;
import ro.cst.tsearch.connection.http.HTTPSiteManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadyPresentFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.CourtDocumentIdentificationSet.CourtDocumentIdentificationSetKey;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.NameSet;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.RawResponseWrapper;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParams;

import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;


@SuppressWarnings("deprecation")
public class OHFranklinPR extends TSServer {
	
	private static final long serialVersionUID = 543654765874746L;
		
	private boolean downloadingForSave;
	
	private static final String FAKE_DETAILS = "fakeDetails.html";
	private static final String FAKE_DETAILS_MARRIAGE_M = "fakeDetailsM.html";
	private static final String FAKE_DETAILS_MARRIAGE_F = "fakeDetailsF.html";
	
	private static final String ROW_PARAM = "row";
	private static final String HEADER_PARAM = "header";
		
	/* the address of the server is taken as an ip from the main page */
	/* for the moment just set the address at current IP, but remember to put some code that 
	 * interrogates the server and parses the current ip */				
	private String getCrtServerIP(){
		return "198.30.81.162";	
	}
	
	/**
	 * constructor
	 */
	public OHFranklinPR(
			String rsRequestSolverName,
			String rsSitePath,
			String rsServerID,
			String rsPrmNameLink, long searchId, int mid) {			
			super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);		
			//docsValidatorType = DocsValidator.TYPE_REGISTER;
			resultType = MULTIPLE_RESULT_TYPE;			
	}
			
	/**
	 * setup modules for automatic search
	 */
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {    
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		String [] keys = new String[] {
			SearchAttributes.OWNER_OBJECT, 
			SearchAttributes.BUYER_OBJECT
		};
		
		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		
		FilterResponse rejectAlreadyPresentFilter = new RejectAlreadyPresentFilterResponse(searchId);
		FilterResponse lastTransferFilter = new LastTransferDateFilter(searchId);
		
		for(String key: keys) {
			if(!(SearchAttributes.BUYER_OBJECT.equals(key)&&search.isProductType(SearchAttributes.SEARCH_PROD_REFINANCE))){
				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.CASE_NAME_MODULE_IDX));
				module.setSaObjKey(key);
				module.clearSaKeys();
				module.clearIteratorTypes();
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
				ConfigurableNameIterator iterator = new ConfigurableNameIterator(searchId, new String[] {"L, F;;"});
				iterator.setInitAgain(true);
				module.addIterator(iterator);
			    GenericNameFilter nameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(key, searchId, module);
			    nameFilter.setIgnoreMiddleOnEmpty(true);
			    module.addFilter(nameFilter);
			    module.addFilter(lastTransferFilter);
			    module.addFilter(rejectAlreadyPresentFilter);
			    module.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT_OH_FRANKLIN_PR);
			    addBetweenDateTest(module, true, true, false);
			    modules.add(module);
			}
		}
		TSServerInfoModule module = null;
		ConfigurableNameIterator iterator = null;
		GenericNameFilter nameFilter = null;
		
		//if the current owner is a man, we need to search Marriage Index -> Male name
		{  
			for(String key: keys) {
				if(!(SearchAttributes.BUYER_OBJECT.equals(key)&&search.isProductType(SearchAttributes.SEARCH_PROD_REFINANCE))) {
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX38));
					//module.setSaObjKey(AdditionalInfoKeys.ADDITIONAL_NAMES_LIST);
					module.setSaObjKey(key);
					module.clearSaKeys();
					module.clearIteratorTypes();
					module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
					iterator = new ConfigurableNameIterator(searchId, new String[] {"L;F;"});
					iterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.MALE_NAME);
					iterator.clearSearchedNames();
					iterator.setInitAgain(true);
					module.addIterator(iterator);
					nameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(key, searchId, module);
			        nameFilter.setIgnoreMiddleOnEmpty(true);
			        module.addFilter(nameFilter);
			        module.addFilter(rejectAlreadyPresentFilter);
			        module.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT_OH_FRANKLIN_PR);
			        addBetweenDateTest(module, true, true, false);
			        modules.add(module);
				}
			}
		}		
		
//		keys = new String[] {
//				SearchAttributes.OWNER_OBJECT, 
//				SearchAttributes.BUYER_OBJECT,
//				AdditionalInfoKeys.ADDITIONAL_NAMES_LIST
//		};
		//if the current owner is a single woman, we need to search Marriage Index -> Female name and see if she has been married before
		{
			for(String key: keys) {
				if(!(SearchAttributes.BUYER_OBJECT.equals(key)&&search.isProductType(SearchAttributes.SEARCH_PROD_REFINANCE))) {
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX39));
					module.setSaObjKey(key);
					module.clearSaKeys();
					module.clearIteratorTypes();
					module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
					iterator = new ConfigurableNameIterator(searchId, new String[] {"L;F;"});
					iterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.FEMALE_NAME);
					iterator.clearSearchedNames();
					iterator.setInitAgain(true);
					module.addIterator(iterator);
					nameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(key, searchId, module);
			        nameFilter.setIgnoreMiddleOnEmpty(true);
			        module.addFilter(nameFilter);
			        module.addFilter(rejectAlreadyPresentFilter);
			        module.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT_OH_FRANKLIN_PR);
			        addBetweenDateTest(module, true, true, false);
			        modules.add(module);
				}
			}
		}
		
		//for each document found searching with Marriage Index -> Female name, search with the first name of the woman and the last name of the man
		//(presumably her last name during the marriage)
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.CASE_NAME_MODULE_IDX));
		module.setSaObjKey(AdditionalInfoKeys.ADDITIONAL_NAMES_LIST);
		module.clearSaKeys();
		module.clearIteratorTypes();
		module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
		iterator = new ConfigurableNameIterator(searchId);
		iterator.setInitAgain(true);
		module.addIterator(iterator);
		nameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(AdditionalInfoKeys.ADDITIONAL_NAMES_LIST, searchId, module);
		nameFilter.setIgnoreMiddleOnEmpty(true);
		nameFilter.setInitAgain(true);
		module.addFilter(nameFilter);
		module.addFilter(lastTransferFilter);
		module.addFilter(rejectAlreadyPresentFilter);
		module.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT_OH_FRANKLIN_PR);
		addBetweenDateTest(module, true, true, false);
		modules.add(module);
	    
		serverInfo.setModulesForAutoSearch(modules);
	}
	
	/**
	 * split the results into rows 
	 */
    public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart,
            int action) throws ro.cst.tsearch.exceptions.ServerResponseException {
        
        p.splitResultRows(
                pr,
                htmlString,
                pageId,
                "<tr",
                "</tr>", linkStart, action);
    }
        
	/**
	 * used only by extractLink
	 */
	private static Pattern linkPat = Pattern.compile("href=[\"\']([^\'\"]+)[\'\"]");
	
	/**
	 * extract first link from a piece of html code
	 */
	private String extractLink(String row){
		Matcher m = linkPat.matcher(row);
		if(m.find()){
			return m.group(1);
		}
		return "";
	}
	
	/**
	 * get case number from a details page 
	 */
	private String getCaseNumber(String response){
		int istart, iend;
		istart = response.indexOf("Case Number / Suffix"); 
		if(istart == -1) { return "none"; }
		istart = response.indexOf("<td", istart);
		if(istart == -1) { return "none"; }
		istart = response.indexOf(">", istart);
		if(istart == -1) { return "none"; }
		istart += ">".length();
		//iend = response.indexOf("&nbsp;", istart);
		iend = response.indexOf("</td>", istart);
		if(iend == -1) { return "none"; }
		response = response.substring(istart, iend);
		response = response.replaceAll("&nbsp;","");
		//Pattern p = Pattern.compile("([0-9]+\\s*[A-Z]?)");
		Pattern p = Pattern.compile("([0-9]+[A-Z]?)");
		response = response.replaceAll("<[^>]+>","");
		Matcher m = p.matcher(response);
		if(m.find()){
			return m.group(1);
		}else{
			return "none";
		}
	}
	
	public String CreatePartialLink(int iActionType) {
        return msRequestSolverName + "?" + msServerID + "&"
                + RequestParams.SEARCH_ID + "=" + mSearch.getSearchID() + "&"
                + getLinkPrefix(iActionType);
    }
		
	/**
	 * parse response from document server
	 */
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		    
		// compute linkStart, linkSuffix, nextKey, prevKey
		boolean computeNextLinks = true;
		String linkStart = CreatePartialLink(TSConnectionURL.idGET);
    	String linkSuffix = "", nextKey = "", prevKey = "";
    	
    	if(sAction.indexOf("PBAttyCNumInx.ndm") != -1) {  	//set viParseID for Search by Attorney/Case
    		viParseID = ID_SEARCH_BY_MODULE37;
    	}
    	
    	switch (viParseID){
    	
	    	case ID_SEARCH_BY_NAME:
	    		linkSuffix = "/netdata/PBCNameInx.ndm/";	//Search By Case Name
	    		nextKey =  "Next Case Names >>";
	    		prevKey = "<< Prev Case Names";
	    		// intermType = "Case Name Search";
	    		break;
	    	case ID_SEARCH_BY_MODULE29: 					//Search By Case Open Date
	    		linkSuffix = "/netdata/PBODateInx.ndm/";
	    		nextKey =  "Next Cases >>";
	    		prevKey = "<< Prev Cases";
	    		// intermType = "Case Open Date Search";
	    		break;
	    	case ID_SEARCH_BY_MODULE30: 					//Search By Case Number/Suffix
	    		linkSuffix = "/netdata/PBCNumbInx.ndm/";
	    		nextKey =  "Next Case >>";
	    		prevKey = "<< Prev Case";
	    		// intermType = "Case Number Suffix Search";
	    		break;
	    	case ID_SEARCH_BY_MODULE31: 					//Search By Case Type/Subtype
	    		linkSuffix = "/netdata/PBCTypeInx.ndm/";
	    		nextKey =  "Next Case Types >>";
	    		prevKey = "<< Prev Case Types ";
	    		// intermType = "Case Type Subtype Search";
	    		break;
	    	case ID_SEARCH_BY_MODULE32: 					//Search By Attorney Name
	    		linkSuffix = "/netdata/PBAttyInx.ndm/";
	    		nextKey =  "Next Names >>";
	    		prevKey = "<< Prev Names ";
	    		// intermType = "Attorney Name Search";
	    		break;
	    	case ID_SEARCH_BY_MODULE33: 					//Search By Fiduciary Name
	    		linkSuffix = "/netdata/PBFidyInx.ndm/";
	    		nextKey =  "Next Fiduciary Cases >>";
	    		prevKey = "<< Prev Fiduciary Cases ";
	    		// intermType = "Fiduciary Search";
	    		break;
	    	case ID_SEARCH_BY_MODULE34:
	    		linkSuffix = "/netdata/PBMthrInx.ndm/";		//Search By Mother's Name
	    		nextKey =  "Next Mothers >>";
	    		prevKey = "<< Prev Mothers";
	    		// intermType = "Mother Name Search";
	    		break;
	    	case ID_SEARCH_BY_MODULE35:						//Search By Father's Name 
	    		linkSuffix = "/netdata/PBFthrInx.ndm/";
	    		nextKey =  "Next Fathers >>";
	    		prevKey = "<< Prev Fathers";
	    		// intermType = "Father Name Search";
	    		break;
	    	case ID_SEARCH_BY_MODULE36:						//Search By Chiropractic Register 
	    		linkSuffix = "/netdata/PBChiroInx.ndm/";
	    		nextKey =  "Next Chiropractic >>";
	    		prevKey = "<< Prev Chiropractic";
	    		// intermType = "Chiropractic Search";
	    		break;
	    	case ID_SEARCH_BY_MODULE37:						//Search By Attorney Name/Case 
	    		linkSuffix = "/netdata/PBAttyCNumInx.ndm/";
	    		nextKey =  "Next Cases >>";
	    		prevKey = "<< Prev Cases";
	    		// intermType = "Attorney Case Search";
	    		break;
	    		
	    	case ID_SEARCH_BY_MODULE38:						//Search By Male Name 
	    		linkSuffix = "/netdata/PBMLMNameInx.ndm/";
	    		nextKey =  "Next Male Names >>"; 
	    		prevKey = "<< Prev Male Names";
	    		// intermType = "Male Name Search";
	    		break;
	    		
	    	case ID_SEARCH_BY_MODULE39:						//Search By Female Name 
	    		linkSuffix = "/netdata/PBMLFNameInx.ndm/";
	    		nextKey =  "Next Female Names >>"; 
	    		prevKey = "<< Prev Female Names";
	    		// intermType = "Female Name Search";
	    		break;
	    		
	    	case ID_SEARCH_BY_MODULE40:						//Search By License Issued Date
	    		linkSuffix = "/netdata/PBMLIDateInx.ndm/";
	    		nextKey =  "Next Cases >>"; 
	    		prevKey = "<< Prev Cases";
	    		// intermType = "License Issued Date Search";
	    		break;	
		
	    	default: computeNextLinks = false;
    	}
    	
    	// temp variables used in pre-parsing
        int istart = -1, iend = -1;

        // get HTML response
        String rsResponse = Response.getResult();
                
        //add </OPTION> tags
        rsResponse = rsResponse.replaceAll("(?is)<OPTION\\s+value=;;>ALL\\s+Cases", "<OPTION value=;;>ALL Cases</OPTION>");
        rsResponse = rsResponse.replaceAll("(?is)<OPTION\\s+value=E;>E=Estate", "<OPTION value=E;>E=Estate</OPTION>");
        rsResponse = rsResponse.replaceAll("(?is)<OPTION\\s+value=C;>C=Civil", "<OPTION value=C;>C=Civil</OPTION>");
        rsResponse = rsResponse.replaceAll("(?is)<OPTION\\s+value=T;>T=Trust", "<OPTION value=T;>T=Trust</OPTION>");
        rsResponse = rsResponse.replaceAll("(?is)<OPTION\\s+value=GA>GA=Adult\\s+Guardianship", "<OPTION value=GA>GA=Adult Guardianship</OPTION>");
        rsResponse = rsResponse.replaceAll("(?is)<OPTION\\s+value=GM>GM=Minor\\s+Guardianship", "<OPTION value=GM>GM=Minor Guardianship</OPTION>");
        rsResponse = rsResponse.replaceAll("(?is)<OPTION\\s+value=M;>M=Miscellaneous", "<OPTION value=M;>M=Miscellaneous</OPTION>");
        
    	// compute previous and next links based on nextKey and prevKey
    	String nextLink = "";
    	String prevLink = "";
    	if(computeNextLinks){
        	// isolate next link
        	istart = rsResponse.indexOf(nextKey);
        	if(istart != -1){
        		istart = rsResponse.lastIndexOf("<a", istart);
        		if(istart != -1){
        			iend = rsResponse.indexOf(">", istart) + ">".length();
        			if(iend != -1){
        				nextLink = rsResponse.substring(istart, iend);
        				// we have added "&disableEncoding=true" in order to instruct the TSServer.GetRequestSettings() and TSConnectionURL.BuildQuery() 
        				// not to escape the parameters. this is because the query is custom-made by a javascript and any encoding makes it inutilizable
        				// by the document server
        				nextLink = nextLink.replaceFirst("href=\"input\\?([^\"]+)\">", "href='" + linkStart + linkSuffix + "input&" + "$1" + "&disableEncoding=true" + "'>");
        				nextLink = nextLink + "Next >></a>";
        			}        				
        		}
        	}
        	
        	// isolate previous link
        	istart = rsResponse.indexOf(prevKey);
        	if(istart != -1){
        		istart = rsResponse.lastIndexOf("<a", istart);
        		if(istart != -1){
        			iend = rsResponse.indexOf(">", istart);
        			if(iend != -1){
        				iend += ">".length();
        				prevLink = rsResponse.substring(istart, iend + 1);
        				// we have added "&disableEncoding=true" in order to instruct the TSServer.GetRequestSettings() and TSConnectionURL.BuildQuery() 
        				// not to escape the parameters. this is because the query is custom-made by a javascript and any encoding makes it inutilizable
        				// by the document server        				
        				prevLink = prevLink.replaceFirst("href=\"input\\?([^\"]+)\">", "href='" + linkStart + linkSuffix + "input&" + "$1" + "&disableEncoding=true" + "'>");
        				prevLink = prevLink + "<< Prev</a>";
        			}        				
        		}
        	}    		
    	}
    	
        switch (viParseID) {
        
        case ID_SEARCH_BY_NAME:				//Search By Case Name        	
        case ID_SEARCH_BY_MODULE29:			//Search By Case Open Date
        case ID_SEARCH_BY_MODULE30:			//Search By Case Number/Suffix
        case ID_SEARCH_BY_MODULE31:			//Search By Case Type/Subtype
        case ID_SEARCH_BY_MODULE32:			//Search By Attorney Name
        case ID_SEARCH_BY_MODULE33:			//Search By Fiduciary Name
        case ID_SEARCH_BY_MODULE34:			//Search By Mother's Name
        case ID_SEARCH_BY_MODULE35:			//Search By Father's Name
        case ID_SEARCH_BY_MODULE36:			//Search By Chiropractic Register
        case ID_SEARCH_BY_MODULE37:			//Search By Attorney Name/Case
        case ID_SEARCH_BY_MODULE38:			//Search By Male Name
        case ID_SEARCH_BY_MODULE39:			//Search By Female Name
        case ID_SEARCH_BY_MODULE40:			//Search By License Issued Date
        	
        	// no result
			if (rsResponse.indexOf("No Names Found") > -1) {
				Response.getParsedResponse().setError("No results found. No Names Found");
				return;
			}
			
			// no result
			if (rsResponse.indexOf("No Dates Found") > -1) {
				Response.getParsedResponse().setError("No results found. No Dates Found");
				return;
			}
        				
			// no result
			if (rsResponse.indexOf("CASE IS NOT FOUND") > -1) {
				Response.getParsedResponse().setError("No results found. CASE IS NOT FOUND");
				return;
			}
			
			// no result
			if (rsResponse.indexOf("No Attorney Names Found") > -1) {
				Response.getParsedResponse().setError("No results found. No Attorney Names Found");
				return;
			}
									
			// no result
			if (rsResponse.indexOf("No Fidys Found") > -1) {
				Response.getParsedResponse().setError("No results found. No Fidys Found");
				return;
			}
        	
			// no result
			if (rsResponse.indexOf("No Chiros Found") > -1) {
				Response.getParsedResponse().setError("No results found. No Chiros Found");
				return;
			}
			
			// no result
			if (rsResponse.indexOf("No Cases with this Criteria") > -1) {
				Response.getParsedResponse().setError("No results found. No Cases with this Criteria");
				return;
			}
			
			// remove attorney detail links
			rsResponse = rsResponse.replaceAll("<a class=\"white\" href=\"http://" + getCrtServerIP() + "/netdata/PBAttyForm.ndm/ATTY_FORM\\?string=(\\d)+\"</a>", "");
			
        	rsResponse = rsResponse.replaceAll("<a style= \"font-size:12px; color:#07528B;\" class=\"white\" href=\"http://" + getCrtServerIP() + "/netdata/PBAttyForm.ndm/ATTY_FORM\\?string=[^\"]+\"</a>", "");
        	
        	// remove "Status" table header link
        	rsResponse = rsResponse.replace("<a  class=\"white\" href=\"http://www.franklincountyohio.gov/probate/PDF/StatusCodes.pdf\">Status</a>","Status");
        	
        	// rewrite links
        	rsResponse = rsResponse.replaceAll("href=\"http://" + getCrtServerIP() + "([^?]+)\\?([^\"]+)\"" , "href='" + linkStart + "$1" + "&" + "$2" + "'");
        	
        	// isolate the table contents
        	istart = rsResponse.indexOf("Table headers");
        	if(istart == -1) { return; }
        	
        	if (viParseID == ID_SEARCH_BY_MODULE30)					//for Case Number/Suffix Search there are no 
        	{														//Navigation data and links (i.e. Previous and Next)
        		istart = rsResponse.indexOf("<table", istart);		//to remove
        		istart = rsResponse.indexOf("<tr", istart);
        	}
        	else
        	{
        		istart = rsResponse.indexOf("</tr>", istart);
            	if(istart == -1){ return; }
            	istart += "</tr>".length();
        	}
        	iend = rsResponse.indexOf("</table>", istart);
        	if(iend == -1){ return; }
        	iend = rsResponse.lastIndexOf("<tr", iend);
        	if(iend == -1){ return; }
        	iend -= 1;
        	
        	rsResponse = rsResponse.substring(istart, iend);
        	       	       	        	
        	// remove color from table header
        	rsResponse = rsResponse.replace("bgcolor=\"#0651A4\"", "");
        	        	
            istart = rsResponse.indexOf("<tr");
            iend = rsResponse.indexOf("</tr>", istart);            
            
            iend += "</tr>".length();
            String tableHeader = rsResponse.substring(istart, iend);     
            
            //for Attorney Name Search there is no check box to save the selected documents
            if (viParseID != ID_SEARCH_BY_MODULE32) {
            	tableHeader = tableHeader.replaceFirst("<th>", "<th>" + SELECT_ALL_CHECKBOXES + "</th><th>");
            } 
            tableHeader = tableHeader.replaceAll("</?font[^>]*>","");
            tableHeader = tableHeader.replaceAll("<td[^>]+>","<td>");
            tableHeader = tableHeader.replaceAll("<tr[^>]+>","<tr>");
            
            /*tableHeader = tableHeader.replaceFirst("</tr>","<th>Search Type</th></tr>");*/
            tableHeader = tableHeader.replaceAll("(?i)</?a[^>]*>", "");
            istart = rsResponse.indexOf("<tr", iend);
            iend = rsResponse.indexOf("</tr>", istart);
            String correctedTableHeader = tableHeader.replaceFirst("(?is)<th>\\s*<input[^>]*>\\s*</th>", ""); 
            
            Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
            
            try {
            	StringBuilder newResponce = new StringBuilder();
                while (istart > 0 && iend > 0) {
                   	iend += "</tr>".length();
                   	String row = rsResponse.substring(istart, iend);
                   	ResultMap m = new ResultMap();
                   	parseIntermediaryRow(m, row, viParseID); 
                   	String year = "-1";
                   	if (viParseID==ID_SEARCH_BY_MODULE38 || viParseID==ID_SEARCH_BY_MODULE39 || viParseID==ID_SEARCH_BY_MODULE40) {
                   		int seq = getSeq();
						Map<String, String> params = new HashMap<String, String>(); 
						params.put(ROW_PARAM, row.replaceFirst("(?is)(<tr[^>]+bgcolor=\"?)White(\"?[^>]*>)", "$1lightblue$2").replaceAll("(?is)</?font[^>]*>", ""));
						params.put(HEADER_PARAM, correctedTableHeader);
						mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
						String caseNumber = (String)m.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName());
						String recordedDate = (String)m.get(SaleDataSetKey.RECORDED_DATE.getKeyName());
						if (!StringUtils.isEmpty(recordedDate)) {
							year = recordedDate.replaceFirst("\\d{1,2}/\\d{1,2}/(\\d{2,4})", "$1");
						}
//						String link = linkStart + FAKE_DETAILS + "?caseno=" + caseNumber + "&seq=" + seq + "&year=" + year;
						String link = linkStart;
						if (viParseID == ID_SEARCH_BY_MODULE38) {
							link += FAKE_DETAILS_MARRIAGE_M;
						} else if (viParseID == ID_SEARCH_BY_MODULE39) {
							link += FAKE_DETAILS_MARRIAGE_F;
						} else {
							link += FAKE_DETAILS;
						}
						link += "?caseno=" + caseNumber + "&seq=" + seq + "&year=" + year;
						
                       	row = row.replaceFirst("(?is)(<tr[^>]*>\\s*<td[^>]*>\\s*(?:<font[^>]*>)?)([^<]+)((?:</font>)?\\s*</td>)", "$1<a href=\"" + link + "\">$2</a>$3");
    	            }
         	           
                   	String docLink = extractLink(row);
         	           
         	        String caseNo = "";
         	        if (viParseID==ID_SEARCH_BY_MODULE38 || viParseID==ID_SEARCH_BY_MODULE39 || viParseID==ID_SEARCH_BY_MODULE40) {
         	        	caseNo = ro.cst.tsearch.utils.StringUtils.extractParameter(docLink, "caseno=([^&?]*)");
         	        } else {
         	        	caseNo = StringUtils.substringBetween(docLink + ";", "caseno=", ";"); 
         	        }
         	           
        	        /*row = row.replaceFirst("</tr>", "<td>" + intermType + "</td></tr>");*/
        	            
         	        //for Attorney Name Search the results can't be saved (only cases can be saved)
         	        if (viParseID != ID_SEARCH_BY_MODULE32)
        	        {	
         	        	HashMap<String, String> data = new HashMap<String, String>();
         	        	if (viParseID==ID_SEARCH_BY_MODULE38 || viParseID==ID_SEARCH_BY_MODULE39 || viParseID==ID_SEARCH_BY_MODULE40) {
         	        		data.put("year", year);
    	        			data.put("type", "MARRIAGE Marriage Licenses");
         	        	} else {
         	        		loadDataHashIntermediate(data, row);
         	        	}
         	        	if (StringUtils.isNotEmpty(caseNo) && caseNo.matches("\\d+") && isInstrumentSaved(caseNo.toString(),null,data) ){
         	        		row = row.replaceFirst("<td","<td align=\"center\">saved</td><td");                    	
    	                } else {
    	                	row = row.replaceFirst("<td","<td align=\"center\"><input type=\"checkbox\" name=\"docLink\" value=\"" + docLink + "\"></td></td><td");
    	                }
        	        }
        	            
        	        newResponce.append(row);
        	        
        	        ParsedResponse currentResponse = new ParsedResponse();
    				currentResponse.setPageLink(new LinkInPage(docLink, docLink, TSServer.REQUEST_SAVE_TO_TSD));
    				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,row);
    				currentResponse.setOnlyResponse(row);
    				Bridge bridge = new Bridge(currentResponse, m, searchId);
    				RegisterDocumentI document = (RegisterDocument)bridge.importData();
    				currentResponse.setDocument(document);
    				intermediaryResponse.add(currentResponse);
        	            
                    istart = rsResponse.indexOf("<tr", iend);
                    iend = rsResponse.indexOf("</tr>", istart);            	
                }            	
                rsResponse = newResponce.toString();
                rsResponse = rsResponse.replaceFirst("color=\"White\"", "");
                
            } catch (Throwable t){
    			logger.error("Error while parsing intermediary data", t);
    		}
            
            if(intermediaryResponse.size() > 0) {
            	Response.getParsedResponse().setResultRows(new Vector<ParsedResponse>(intermediaryResponse));
            	Response.getParsedResponse().setOnlyResponse(rsResponse);
            	Response.getParsedResponse().setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rsResponse);
            }
             

            //script to search for attorney cases (they can be saved)
            String script = "<SCRIPT language=\"JavaScript\">\n" 
			   + "var first;\n" 
			   + "var second;\n" 
			   + "var third;\n" 
			   + "var fourth;\n"  
			   + "function newURL(obj_button){\n" 
			   + "var buttons = document.getElementsByName(\"button1\");\n" 
			   + "var types = document.getElementsByName(\"s_CaseType\");\n" 
			   + "var subtypes = document.getElementsByName(\"s_CaseSubType\");\n"
			   + "var statuses = document.getElementsByName(\"s_CaseStatus\");\n"
			   + "first = \"" + CreatePartialLink(TSConnectionURL.idGET) + "/netdata/\";\n"
			   + "second = \"PBAttyCNumInx.ndm/input\";\n" 
			   + "third = \"?string=\";\n" 
			   + "for (var i = 0; i <= 39; i++) \n" 
			   + "if (buttons[i].value == obj_button.value) {break;}\n"
			   + "var atty_number = buttons[i].value;\n" 
			   + "var onetext = types[i].value;\n"  
			   + "var twotext =  \";;\";\n" 
			   + "var threetext =  \";;\";\n" 
			   + "var String_SubType = new String();\n" 
			   + "String_SubType = subtypes[i].value;\n" 
			   + "var SubTypeLength = String_SubType.length;\n"  
			   + "if (SubTypeLength == \"2\")\n"  
			   + "{twotext = (String_SubType) }\n"  
			   + "if (SubTypeLength == \"1\")\n"  
			   + "{twotext = (String_SubType + \";\") }\n"  
			   + "var String_Status = new String();\n" 
			   + "String_Status = statuses[i].value;\n" 
			   + "var StatusLength = String_Status.length;\n"  
			   + "if (StatusLength == \"2\")\n"  
			   + "{threetext = (String_Status) }\n"  
			   + "if (StatusLength == \"1\")\n"  
			   + "{threetext = (String_Status + \";\") }\n"		  
			   + "Window = window.location=(first + second + third + atty_number + \"*=\" + onetext + twotext + threetext + \"&disableEncoding=true\");}\n" 
			   + "</SCRIPT>";
                
            //for Attorney Name Search the results can't be saved
            if (viParseID == ID_SEARCH_BY_MODULE32) {
            	Response.getParsedResponse().setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST") + "<table border=0>" + tableHeader + script);
            } else {
            	Response.getParsedResponse().setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST") + "<table border=0>" + tableHeader);
            }
               
            //for Attorney Name Search there are no save buttons 
            if (viParseID == ID_SEARCH_BY_MODULE32) {
            	Response.getParsedResponse().setFooter("</table>" + "<br>" + prevLink + "&nbsp;&nbsp;" + nextLink);
            } else {
            	Response.getParsedResponse().setFooter("</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1) + "<br>" + prevLink + "&nbsp;&nbsp;" + nextLink);
            }
                        
            Response.getParsedResponse().setNextLink(nextLink.replaceAll("class=\"blue\"", ""));
                		
     		break;
     		
        case ID_DETAILS:
        	
        	boolean isfakeDetail = false;
    		String query = Response.getQuerry();
    		if (!StringUtils.isEmpty(query)) {
    			if (query.contains(FAKE_DETAILS) || query.contains(FAKE_DETAILS_MARRIAGE_M) || query.contains(FAKE_DETAILS_MARRIAGE_F)) {
    				isfakeDetail = true;
    			}
    		}
    		
    		String htmlText = "<!DOCTYPE HTML PUBLIC";
    		if (isfakeDetail) {
    			htmlText = "<html>";
    		}
        	
        	// we might be called from final page on parent site-with page prepared
        	// but we might also be called from intermediate parent site
        	boolean addSaveButton = true;
        	if(downloadingForSave && rsResponse.indexOf(htmlText) != -1){
        		downloadingForSave = false;
        		addSaveButton = false;
        	}
        	
        	if(!downloadingForSave){
        		
        		String caseNumber = "";
        		if (isfakeDetail) {
        			if (!StringUtils.isEmpty(query)) {
        				caseNumber = ro.cst.tsearch.utils.StringUtils.extractParameter(query, "caseno=([^&?]*)");
        			}
        		} else {
        			caseNumber = getCaseNumber(rsResponse); 
        		}
        		
        		msSaveToTSDFileName = caseNumber + ".html";
        		
	        	String newPage = "";
	        	String docketPart = null;
	        	// extract docket and fidu links, content from main page
	        	ProcessedPage ppMain = null;
	        	if (isfakeDetail) {
	        		ppMain = processPage(rsResponse, PAGE_CASE_DETAIL_FAKE);
	        	} else {
	        		ppMain = processPage(rsResponse, PAGE_CASE_DETAIL);
	        	}
	        	if(ppMain == null){ return; }
	        	
	        	// append main table
	        	if (isfakeDetail) {
	        		newPage = ppMain.page;
	        	} else {	
	        		newPage += "<table><tr><td>CASE</td><td>" + ppMain.page + "</td></tr>";
	        	
		        	// get the docket page if it exists
		        	if(ppMain.docketLink != null){
		        		String docketPage = getPage(ppMain.docketLink[0], ppMain.docketLink[1]);
		        		if(docketPage != null && !docketPage.equals("")){
		        			// extract content from docket page
		                	ProcessedPage ppDocket = processPage(docketPage, PAGE_DOCKETS);
		                	if(ppDocket != null && ppDocket.page != null && !ppDocket.page.equals("")){
		                		docketPart = ppDocket.page;
		                	}        			
		        		}
		        	}
		        	// get the fidu detail pages
		        	if(ppMain.fidLinks != null){
		        		for(Iterator<String[]> it=ppMain.fidLinks.iterator(); it.hasNext();){
		        			String [] crt = it.next();
		        			// get crt intermed page
		        			String fiduIntermPage = getPage(crt[0], crt[1]);	
		        			if(fiduIntermPage != null && !fiduIntermPage.equals("")){
		        				// get links from intermediary page
		        				ProcessedPage ppFiduInterm = processPage(fiduIntermPage, PAGE_FID_INTERM);
		        				if(ppFiduInterm != null && ppFiduInterm.fidFinalLinks != null){
		        					// get and concatenate all fiduciaries detail pages
		        					for(int j=0; j<ppFiduInterm.fidFinalLinks.size(); j++){
		        						String[] crtFinalLink = ppFiduInterm.fidFinalLinks.get(j);       
		            					// get crt details page
		        						String fiduFinalPage = getPage(crtFinalLink[0], crtFinalLink[1]);        						
		        						// process 
		        						ProcessedPage ppFiduFinal = processPage(fiduFinalPage, PAGE_FID_DETAIL);
		        						if(ppFiduFinal != null && ppFiduFinal.page != null && !ppFiduFinal.page.equals("")){
		        							newPage += "<tr><td>FIDUCIARY</td><td>" + ppFiduFinal.page + "</td></tr>";
		        						}
		        					}
		        				}
		        			}
		        		}
		        	}
		        	
		        	if(docketPart != null){
		        		newPage += "<tr><td>DOCKET</td><td>" + docketPart + "</td></tr>";
		        	}
		        	newPage += "</table>";
		        	
		        	newPage = newPage.replaceAll("(?s)<(table|tr|td|th)[^>]+>","<$1>");
		        	newPage = newPage.replaceAll("</?font[^>]*>","");
		        	newPage = newPage.replace("<tr>","<tr align=\"left\">");
		        	newPage = newPage.replaceFirst("<table>","<table border=\"1\">");
		        	newPage = newPage.replaceAll("(?is)<illegible>", "");
	        	}
	        	
	        	rsResponse = newPage;
	        	rsResponse = rsResponse.replace("<!", "<!--");
	        	
	        	String originalLink = sAction + "&dummy=" + caseNumber + "&" + Response.getQuerry();
	        	String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
	        	
	        	rsResponse = rsResponse.replaceAll("(?i)</?a[^>]*>", "");
	        	
	        	if(addSaveButton){
	        		HashMap<String, String> data = new HashMap<String, String>();
	        		if (isfakeDetail) {
	        			String year = ro.cst.tsearch.utils.StringUtils.extractParameter(query, "year=([^&?]*)");
	        			data.put("year", year);
	        			data.put("type", "MARRIAGE Marriage Licenses");
	        		} else {
	        			loadDataHashDetails(data, rsResponse);
	        		}
	        		if (isInstrumentSaved(caseNumber.toString(),null,data)) {
		        		rsResponse += CreateFileAlreadyInTSD();
		        	}else{	        		
	                    mSearch.addInMemoryDoc(sSave2TSDLink, rsResponse);
	                    rsResponse = addSaveToTsdButton(rsResponse, sSave2TSDLink, viParseID);
		        	}
	        	}
	        	
	        	Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
	        	
	        	if (isfakeDetail) {
                	smartParseDetails(Response, rsResponse);
                } else {
                	if(!addSaveButton){
    	        		parser.Parse(Response.getParsedResponse(), rsResponse, Parser.PAGE_DETAILS);
    	        	}else{
    	        		parser.Parse(Response.getParsedResponse(), rsResponse, Parser.NO_PARSE);
    	        	}
                }
	        	
        	} else {  // downloading for save
        		
        		rsResponse = rsResponse.replaceAll("(?i)</?a[^>]*>", "");
        		
        		String caseNumber = "";
        		if (isfakeDetail) {
        			if (!StringUtils.isEmpty(query)) {
        				caseNumber = ro.cst.tsearch.utils.StringUtils.extractParameter(query, "caseno=([^&?]*)");
        			}
        		} else {
        			caseNumber = getCaseNumber(rsResponse); 
        		}
        		
        		msSaveToTSDFileName = caseNumber + ".html";
                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
                msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();             
                
                if (isfakeDetail) {
                	smartParseDetails(Response, rsResponse);
                } else {
                	parser.Parse(Response.getParsedResponse(), rsResponse, Parser.PAGE_DETAILS);
                }
                
                Response.getParsedResponse().setResponse(rsResponse);        		
        	}
        	
        	break;
        	
		case ID_GET_LINK :
			
			if(sAction.indexOf("PBCNameInx.ndm") != -1){ // case name next page
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			} else if(sAction.indexOf("PBODateInx.ndm") != -1){ // case open date next page
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE29);
				// case number/suffix has no next or previous pages
			} else if(sAction.indexOf("PBCTypeInx.ndm") != -1){ // case type/subtypre next page
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE31);
			} else if(sAction.indexOf("PBAttyInx.ndm") != -1){ // case attorney name next page
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE32);
			} else if(sAction.indexOf("PBFidyInx.ndm") != -1){ // case fiduciary name next page
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE33);
			} else if(sAction.indexOf("PBMthrInx.ndm") != -1){ // case mother's name next page
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE34);
			} else if(sAction.indexOf("PBFthrInx.ndm") != -1){ // case father's name next page
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE35);
			} else if(sAction.indexOf("PBChiroInx.ndm") != -1){ // case chiropractic register next page
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE36);
			} else if(sAction.indexOf("PBAttyCNumInx.ndm") != -1){ // attorney case next page
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE37);	
			} else if(sAction.indexOf("PBMLMNameInx.ndm") != -1){ // male name next page
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE38);	
			} else if(sAction.indexOf("PBMLFNameInx.ndm") != -1){ // female name next page
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE39);	
			} else if(sAction.indexOf("PBMLIDateInx.ndm") != -1){ // license issued date next page
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE40);	
			} else{
				ParseResponse(sAction, Response, ID_DETAILS);
			}
			break;
			
		case ID_SAVE_TO_TSD : 
						
			downloadingForSave = true;
			ParseResponse(sAction, Response, ID_DETAILS);
			downloadingForSave = false;
			
			break;
        }        
	}
	
	protected void loadDataHashDetails(HashMap<String, String> data, String html) {
		if(data != null) {
			String type = "";
			String subtype = "";
			Matcher ma1 = Pattern.compile("(?is)<tr.*?>\\s*<th>\\s*Case Type\\s*</th>\\s*<td>(.*?)</td>").matcher(html);
			if (ma1.find()) {
				type = ma1.group(1).trim();
			}
			Matcher ma2 = Pattern.compile("(?is)<tr.*?>\\s*<th>\\s*Case Subtype\\s*</th>\\s*<td>(.*?)</td>").matcher(html);
			if (ma2.find()) {
				subtype = ma2.group(1).trim();
			}
			data.put("type", type + " " + subtype);
		}
	}
	
	protected void loadDataHashIntermediate(HashMap<String, String> data, String html) {
		if(data != null) {
			String type = "";
			String subtype = "";
			String[] split = html.split("(?i)<td");
			String prefix = "";
			if (split.length==8)
				prefix = "<font size=\"-2\">.*?</font>.*?";
			Matcher ma = Pattern.compile("(?is)" + prefix + "<font size=\"-2\">(.*?)</font>.*?<font size=\"-2\">(.*?)</font>").matcher(html);
			if (ma.find()) {
				type = ma.group(1).trim();
				subtype = ma.group(2).trim();
			}
			data.put("type", type + " " + subtype);
		}
	}
	
	// types of pages processed by processPage
	private static final int PAGE_CASE_DETAIL = 0;
	private static final int PAGE_DOCKETS     = 1;
	private static final int PAGE_FID_INTERM  = 2;
	private static final int PAGE_FID_DETAIL  = 3;
	private static final int PAGE_CASE_DETAIL_FAKE = 4;
	
	/**
	 * Wrapper class for returning multiple values from processPage function
	 * @author radu bacrau
	 */
	private static class ProcessedPage{
		public String [] docketLink = null;
		public List<String[]> fidLinks = null;
		public List<String[]> fidFinalLinks = null;
		public String page = null;
	}
	
	/**
	 * stores the associated httpsite instance
	 */
	private HTTPSiteInterface httpSite = null;
	
	/**
	 * get a page from web
	 * @param link link of the page
	 * @param query querry for the page 
	 * @return the page
	 */
	private String getPage(String link, String query){
		if(httpSite == null){
			httpSite = (HTTPSiteInterface)HTTPSiteManager.pairHTTPSiteForTSServer("OHFranklinPR",searchId,miServerID);
		}
		HTTPRequest req = new HTTPRequest(link + "?" + query);
	    req.setMethod( HTTPRequest.GET );
	    req.noRedirects = false;        
	    HTTPResponse res = httpSite.process( req );
	    String strdata = res.getResponseAsString();
	    return strdata;
	}
	
	/**
	 * parse a web page and retrieve contents and relevant links
	 * @param page
	 * @param pageType one of PAGE_CASE_DETAIL, PAGE_DOCKETS, PAGE_FID_INTERM, PAGE_FID_DETAIL
	 * @return ProcessedPage instance
	 */
	private ProcessedPage processPage(String page, int pageType){		
		ProcessedPage pp =  new ProcessedPage();
		
		if (pageType==PAGE_CASE_DETAIL_FAKE) {
			int istart, iend;
        	istart = page.toLowerCase().indexOf("<table");
        	if(istart == -1){ return null; }
        	iend = page.toLowerCase().indexOf("</table>");
        	if(iend == -1){ return null; }
        	pp.page = page.substring(istart, iend) + "</table>";
        	return pp;
		}
		
		boolean searchDocket = false;
		boolean searchFidu = false;
		boolean searchFinalFidu = false;
		
		// prepare table prefix in case we need to get the table
		String tablePrefix = null;
		switch(pageType){
			case PAGE_CASE_DETAIL: 
				tablePrefix = "case detail table";
				searchDocket = true; 
				searchFidu = true;
				break;
			case PAGE_DOCKETS:
				tablePrefix = "table headers";
				break;
			case PAGE_FID_INTERM:
				searchFinalFidu = true;
				break;
			case PAGE_FID_DETAIL:
				tablePrefix = "case detail table";
				break;
		}
		
    	// isolate result table
		if(tablePrefix != null){
			int istart, iend;
        	istart = page.toLowerCase().indexOf(tablePrefix);
        	if(istart == -1){ return null; }
        	istart = page.indexOf("<table", istart);
        	if(istart == -1){ return null; }
        	iend = page.indexOf("value=\"Back\"", istart);
        	if(iend == -1){ return null; }
        	iend = page.lastIndexOf("</tr>", iend);
        	if(iend == -1){ return null; }
        	iend += "</tr>".length();
        	pp.page = page.substring(istart, iend) + "</table>";
		}
		
		if(searchDocket){
			// http://198.30.81.162/netdata/PBDocket.ndm/input?caseno=000005;;
			Pattern docketPat = Pattern.compile("\"(http://" + getCrtServerIP() + "/netdata/PBDocket\\.ndm/input)" + "\\?" + "([^\"]+)\"");
			Matcher docketMat = docketPat.matcher(page);
			if(docketMat.find()){
				pp.docketLink = new String[]{docketMat.group(1), docketMat.group(2)};
			}			
		}
		
		if(searchFidu){
			// http://198.30.81.162/netdata/PBFidy.ndm/input?caseno=466478;;
			Pattern fidPat = Pattern.compile("\"(http://" + getCrtServerIP() + "/netdata/PBFidy\\.ndm/input)" + "\\?" + "([^\"]+)\"");
			List<String[]> links = new ArrayList<String[]>();
			Matcher fidMat = fidPat.matcher(page);
			while(fidMat.find()){
				links.add(new String[]{fidMat.group(1), fidMat.group(2)});
			}
			
			if(links.size() != 0){
				pp.fidLinks = links;
			}					
		}
		
		if(searchFinalFidu){
			// http://198.30.81.162/netdata/PBFidDetail.ndm/FID_DETAIL?caseno=506710;;01
			Pattern fidPat = Pattern.compile("\"(http://" + getCrtServerIP() + "/netdata/PBFidDetail\\.ndm/FID_DETAIL)" + "\\?" + "([^\"]+)\"");
			List<String[]> links = new ArrayList<String[]>();
			Matcher fidMat = fidPat.matcher(page);
			while(fidMat.find()){
				links.add(new String[]{fidMat.group(1), fidMat.group(2)});
			}
			
			if(links.size() != 0){
				pp.fidFinalLinks = links;
			}								
		}
		
		return pp;
	}
	
	// ActionType=2&Link=/netdata/PBCaseTypeC.ndm/CIVIL_DETAIL&dummy=461101&caseno=461101A;&parentSite=true
    protected String getFileNameFromLink(String url) {
    	String retVal = "none";
    	Pattern p1 = Pattern.compile("caseno=([0-9A-Z]+)");
    	Matcher m1 = p1.matcher(url);
    	if(m1.find()){
    		retVal = m1.group(1);
    	}
        return retVal; 
    }
    
    @Override
	protected ServerResponse SearchBy(boolean bResetQuery,
            TSServerInfoModule module, Object sd)
            throws ServerResponseException {
		
    	ServerResponse serverResponseError = null;
    	
    	if (module.getParserID() == ID_SEARCH_BY_MODULE29 || module.getParserID() == ID_SEARCH_BY_MODULE40) {	//Search By Case Open Date, Search By License Issued Date 	
    		serverResponseError = validateDateForModule(module);
    	}
    	    	
    	if(serverResponseError != null) {
    		return serverResponseError;
    	}
    	return super.SearchBy(bResetQuery, module, sd);
	}
    
    //validates date, which must have Use MM/DD/YYYY Date Format
    //and month and date values less than 10 must have use 0 at the beginning  
    protected ServerResponse validateDateForModule(TSServerInfoModule module) {
    	
    	HashMap<String, Integer> namePositionMap = new HashMap<String, Integer>();
    	for (int i = 0; i < module.getFunctionCount(); i++) {
			namePositionMap.put(module.getFunction(i).getParamName(), i);
		}
    	 
    	if(namePositionMap.get("string") != null) {
	    	String field1 = module.getFunction(namePositionMap.get("string")).getParamValue().trim();
	    	
	    	ServerResponse serverResponse = new ServerResponse();
	    	ParsedResponse parsedResponse = serverResponse.getParsedResponse();
	    	
	    	SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
	    	sdf.setLenient(false);
	    	
	    	String errorMessage = "The date entered is not valid.  Please enter a valid date in the format MM/DD/YYYY. For month and date values less than 10, use 0 in front of the date you enter. Example: 01/01/2000 for January 1, 2000.";
	    		
	    	if(field1.isEmpty()) {
	    		parsedResponse.setError(errorMessage);
	    		return serverResponse;
	    	} else {
	    		try {
	    			sdf.parse(field1);
	    			if (field1.charAt(2)!='/' || field1.charAt(5)!='/')	//if there is no 0 before month and date values less than 10
	    			{
	    				parsedResponse.setError(errorMessage);
						return serverResponse;
	    			}
	    			
				} catch (ParseException e) {
					//logger.error("Error while parsing date", e);
					parsedResponse.setError(errorMessage);
					return serverResponse;
				} 
	    	}
    	}
    	
    	return null;
    }
    
    @SuppressWarnings("unchecked")
	public ServerResponse generateFakeResponse(String vbRequest, String sAction, int viParseID) throws ServerResponseException {
    	
    	if (vbRequest!=null) {
    		String link = ro.cst.tsearch.utils.StringUtils.extractParameter(vbRequest, "Link=(.*)");
    		
    		if (link.contains(FAKE_DETAILS) || link.contains(FAKE_DETAILS_MARRIAGE_M) || link.contains(FAKE_DETAILS_MARRIAGE_F)) {
    			String seq = ro.cst.tsearch.utils.StringUtils.extractParameter(vbRequest, "seq=([^&?]*)");
    			String row = "";
    			String header = "";
    			if (!StringUtils.isEmpty(seq))
    			{
    				Map<String, String> addParams = (Map<String, String>)mSearch.getAdditionalInfo(getCurrentServerName() + ":params:" + seq);
    				row = addParams.get(ROW_PARAM);
    				header = addParams.get(HEADER_PARAM);
    			}
    			String fakeResponse = "<html><body><table>" + header + row + "</table></body></html>";
    			        	
    			ServerResponse response = new ServerResponse();
    			response.setCheckForDocType(false);
    			String query = link;
    			if (query.indexOf("parentSite=true") >= 0 || isParentSite()) {
    				response.setParentSiteSearch(true);
    			 }
    			response.setQuerry(query);
    			ParsedResponse parsedResponse = response.getParsedResponse();
    			parsedResponse.setResponse(fakeResponse);
    			if((parsedResponse.getPageLink() == null || StringUtils.isNotEmpty(parsedResponse.getPageLink().getLink())) && StringUtils.isNotEmpty(vbRequest)) {
    				parsedResponse.setPageLink(new LinkInPage(vbRequest,vbRequest));
    			} 
    			solveResponse(sAction, viParseID, "GetLink", response, new RawResponseWrapper(fakeResponse), null);
    			return response;
    		}
    	}
    	
    	return null;
    }
    
    @Override
	public ServerResponse GetLink(String vsRequest, boolean vbEncoded) throws ServerResponseException {
		msLastLink = vsRequest;
		String sAction = GetRequestSettings(vbEncoded, vsRequest);
	
		ServerResponse response = null;
		
		response = generateFakeResponse(vsRequest, sAction, ID_DETAILS);
		
		if (response!=null) {
			return response;
		}
		
		return super.performRequest(sAction, miGetLinkActionType, "GetLink", ID_GET_LINK, null, vsRequest, null);
	 }
    
    @Override
    protected ServerResponse performRequest(String page, int methodType, String action, int parserId, String imagePath, String vbRequest , Map<String, Object> extraParams) throws ServerResponseException {
    	
    	ServerResponse response = null;
		
		response = generateFakeResponse(vbRequest, page, ID_SAVE_TO_TSD);
		
		if (response!=null) {
			return response;
		}
    	
		return super.performRequest(page, methodType, action, parserId, imagePath, vbRequest, extraParams);
    }
    
    public static ResultMap parseIntermediaryRow(ResultMap resultMap, String row, int viParseID) {
		
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "PR");
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(row, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList tdList = nodeList.extractAllNodesThatMatch(new TagNameFilter("td"), true);
			int len = tdList.size();
			
			String caseNumber = "";
			String grantor1 = "";
			String grantor2 = "";
			String type = "";
			String subType = "";
			String recordedDate = "";		//date opened
			String dispositionDate = "";	//date closed
			
			int caseNumberIndex = -1;
			int grantor1Index = -1;
			int grantor2Index = -1;
			int typeIndex = -1;
			int subTypeIndex = -1;
			int recordedDateIndex = -1;
			int dispositionDateIndex = -1;
			
			if (viParseID==ID_SEARCH_BY_NAME || viParseID==ID_SEARCH_BY_MODULE29 || viParseID==ID_SEARCH_BY_MODULE30 || viParseID==ID_SEARCH_BY_MODULE31) {
				caseNumberIndex = 0;
				grantor1Index = 1;
				typeIndex = 2;
				subTypeIndex = 3;
				recordedDateIndex = 5;
				dispositionDateIndex = 6;
			} else if (viParseID==ID_SEARCH_BY_MODULE33 || viParseID==ID_SEARCH_BY_MODULE34 || viParseID==ID_SEARCH_BY_MODULE35 || viParseID==ID_SEARCH_BY_MODULE36) {
				caseNumberIndex = 0;
				grantor1Index = 7;
				typeIndex = 2;
				subTypeIndex = 3;
				recordedDateIndex = 5;
				dispositionDateIndex = 6;
			} else if (viParseID==ID_SEARCH_BY_MODULE37) {
				caseNumberIndex = 0;
				typeIndex = 1;
				subTypeIndex = 2;
			} else if (viParseID==ID_SEARCH_BY_MODULE38 || viParseID==ID_SEARCH_BY_MODULE39) {
				caseNumberIndex = 0;
				grantor1Index = 1;
				grantor2Index = 2;
				recordedDateIndex = 4;
			} else if (viParseID==ID_SEARCH_BY_MODULE40) {
				caseNumberIndex = 0;
				grantor1Index = 2;
				grantor2Index = 3;
				recordedDateIndex = 1;
			} 
			
			if (caseNumberIndex!=-1 && caseNumberIndex<len) {
				caseNumber = tdList.elementAt(caseNumberIndex).toPlainTextString().trim();
			}
			if (grantor1Index!=-1 && grantor1Index<len) {
				grantor1 = tdList.elementAt(grantor1Index).toPlainTextString().trim();
			}
			if (grantor2Index!=-1 && grantor2Index<len) {
				grantor2 = tdList.elementAt(grantor2Index).toPlainTextString().trim();
			}
			if (typeIndex!=-1 && typeIndex<len) {
				type = tdList.elementAt(typeIndex).toPlainTextString().trim();
			}
			if (subTypeIndex!=-1 && subTypeIndex<len) {
				subType = tdList.elementAt(subTypeIndex).toPlainTextString().trim();
			}
			if (recordedDateIndex!=-1 && recordedDateIndex<len) {
				recordedDate = tdList.elementAt(recordedDateIndex).toPlainTextString().trim();
			}
			if (dispositionDateIndex!=-1 && dispositionDateIndex<len) {
				dispositionDate = tdList.elementAt(dispositionDateIndex).toPlainTextString().trim();
			}
			
			String grantor = grantor1 + " / " + grantor2;
			grantor = grantor.replaceFirst("^\\s*/", "");
			grantor = grantor.replaceFirst("/\\s*$", "");
			grantor = grantor.trim();
			
			String caseType = type + " " + subType;
			caseType = caseType.trim();
			
			if (!StringUtils.isEmpty(caseNumber)) {
				resultMap.put(CourtDocumentIdentificationSetKey.CASE_NUMBER.getKeyName(), caseNumber);
				resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), caseNumber);
			}
			
			if (!StringUtils.isEmpty(grantor)) {
				resultMap.put(CourtDocumentIdentificationSetKey.PARTY_NAME.getKeyName(), grantor);
				resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), grantor);
			}
			
			if (!StringUtils.isEmpty(caseType)) {
				resultMap.put(CourtDocumentIdentificationSetKey.CASE_TYPE.getKeyName(), caseType);
				resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), caseType);
			}
			
			if (!StringUtils.isEmpty(recordedDate)) {
				resultMap.put(CourtDocumentIdentificationSetKey.FILLING_DATE.getKeyName(), recordedDate);
				resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recordedDate);
			}
			
			if (!StringUtils.isEmpty(dispositionDate)) {
				resultMap.put(CourtDocumentIdentificationSetKey.DISPOSITION_DATE.getKeyName(), dispositionDate);
			}
			
			parseNames(resultMap);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return resultMap;
	}
    
    @SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap) {

		ArrayList<List> grantor = new ArrayList<List>();
		String partyGtor = (String)resultMap.get(SaleDataSetKey.GRANTOR.getKeyName());
		if (StringUtils.isNotEmpty(partyGtor)){
			parseName(partyGtor, grantor, resultMap);
			try {
				resultMap.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}
    
    @SuppressWarnings("rawtypes")
	public static void parseName(String rawNames, ArrayList<List> list, ResultMap resultMap) {
		
		String names[] = {"", "", "", "", "", ""};
		String[] suffixes, type, otherType;
		
		String split[] = rawNames.split(" / ");
		
		for (int i=0;i<split.length;i++) {
			
			String name = split[i];
				
			if (!StringUtils.isEmpty(name)) {
				names = StringFormats.parseNameNashville(name, true);
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);
				GenericFunctions.addOwnerNames(split[i], names, suffixes[0],
					suffixes[1], type, otherType, NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), list);
			}
			
		}
	}
    
    @Override
    protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		
    	int viParseID = -1;
    	String header = "";
    	String row = "";
    	Matcher ma1 = Pattern.compile("(?is)<table[^>]*>\\s*(<tr[^>]*>.+?</tr>)\\s*(<tr[^>]*>(.+?)</tr>)\\s*</table>").matcher(detailsHtml);
    	if (ma1.find()) {
    		header = ma1.group(1).trim();
    		row = ma1.group(2).trim();
    	}
    	
    	if (!StringUtils.isEmpty(header)) {
    		Matcher ma2 = Pattern.compile("(?is)<tr[^>]*>\\s*<th>.+?</th>\\s*<th>(.+?)</th>").matcher(header);
        	if (ma2.find()) {
        		String label = ma2.group(1).trim();
        		if (label.matches("Groom\\s+Name")) {
        			viParseID = ID_SEARCH_BY_MODULE38;
        		} else if (label.matches("Bride\\s+Name")) {
        			viParseID = ID_SEARCH_BY_MODULE39;
        		} else if (label.matches("License\\s+Issued")) {
        			viParseID = ID_SEARCH_BY_MODULE40;
        		}
        	}
    	}
    	
    	parseIntermediaryRow(map, row, viParseID);
    	
    	map.put(CourtDocumentIdentificationSetKey.CASE_TYPE.getKeyName(), "MARRIAGE Marriage Licenses");
		map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "MARRIAGE Marriage Licenses");
    	
		return null;
	}
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected TSServer.ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(ServerResponse response, String htmlContent, boolean forceOverritten) {
		TSServer.ADD_DOCUMENT_RESULT_TYPES status = super.addDocumentInATS(response, htmlContent, forceOverritten);
		boolean brideIsFirst = true;
		if (htmlContent.matches("(?is).*<th>\\s*Case Number\\s*</th>\\s*<th>\\s*Groom Name\\s*</th>.*")) {
			brideIsFirst = false;
		}
		
		if(mSearch.getSearchType() == Search.AUTOMATIC_SEARCH && status == ADD_DOCUMENT_RESULT_TYPES.ADDED) {
			String query = response.getQuerry();
    		if (!StringUtils.isEmpty(query)) {
//    			if (query.contains(FAKE_DETAILS)) {	//Marriage Index - Search By Female Name
    			if (query.contains(FAKE_DETAILS_MARRIAGE_F)) {	//Marriage Index - Search By Female Name
    				Vector grantors = response.getParsedResponse().getGrantorNameSet();
    				if (grantors.size()==2) {
    					
    					NameSet bride = null;
    					NameSet groom = null;
    					if (brideIsFirst) {
    						bride = (NameSet)grantors.get(0);
        					groom = (NameSet)grantors.get(1);
    					} else {
    						bride = (NameSet)grantors.get(1);
        					groom = (NameSet)grantors.get(0);
    					}
    					
    					NameI name = new Name();
    					name.setFirstName(bride.getAtribute(PropertyIdentificationSetKey.OWNER_FIRST_NAME.getShortKeyName()));
    					name.setMiddleName(bride.getAtribute(PropertyIdentificationSetKey.OWNER_MIDDLE_NAME.getShortKeyName()));
    					name.setLastName(groom.getAtribute(PropertyIdentificationSetKey.OWNER_LAST_NAME.getShortKeyName()));
    					
    					List<NameI> additionalNames = new ArrayList<NameI>();
    					Object additionalInfo = mSearch.getAdditionalInfo(AdditionalInfoKeys.ADDITIONAL_NAMES_LIST);
    					if(additionalInfo != null && additionalInfo instanceof List) {
    						additionalNames = (List<NameI>) additionalInfo;
    					}
    					additionalNames.add(name);
    					mSearch.setAdditionalInfo(AdditionalInfoKeys.ADDITIONAL_NAMES_LIST, additionalNames);
    				}
    			}
    		}
		}
		
		return status;
	}
    
}
