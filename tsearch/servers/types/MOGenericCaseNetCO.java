package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadyPresentFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.GranteeSet;
import ro.cst.tsearch.servers.response.GrantorSet;
import ro.cst.tsearch.servers.response.InfSet;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.name.NameI;

public class MOGenericCaseNetCO extends TSServer {


	private static final long serialVersionUID = 2416753544170917958L;
	private static final int ID_DETAILS = 23423415;
	private boolean downloadingForSave = false;

	/**
	 * Constructor
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 * @param searchId
	 * @param mid
	 */
	public MOGenericCaseNetCO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	


	/**
	 * Get table of intermediate results
	 * @param response
	 * @return
	 */
	protected String getIntermediateTable(String response){
		
		// extract contents table
		int istart = response.indexOf("<td class=\"header\">Party&nbsp;Name</td>");
		if(istart == -1){
			return "";
		}		
		istart = response.lastIndexOf("<table", istart);
		if(istart == -1){
			return "";
		}		
		String contents = StringUtils.extractTagContents(istart, response, "table");
		
		// format table
		contents = contents.replaceFirst("<table[^>]*>", "<table border=1 cellspacing=0 cellpadding=0>");
		contents = contents.replaceFirst("<tr", "<Tr bgcolor='#DADADA' ");
		contents = contents.replaceFirst("<tr", "<Tr bgcolor='#DADADA' ");
	    contents = contents.replaceFirst("<td>&nbsp;</td>","<th><div>" + SELECT_ALL_CHECKBOXES + "</div></th><td>&nbsp;</td>");
		contents = contents.replaceAll("<td>&nbsp;</td>","<td>&nbsp;</td><td>&nbsp;</td>");
		contents = contents.replaceAll("<td class=\"header\"([^>]*)>([^>]*)</td>", "<th$1>$2</th>");
		
		// add checkboxes	
		String linkPrefix = CreatePartialLink(TSConnectionURL.idPOST);
		int idx1 = contents.indexOf("<tr align=\"left\">");
		int idx2 = contents.indexOf("</tr>", idx1);		
		if(idx1 != -1 && idx2 != -1){
			StringBuilder sb = new StringBuilder();
			sb.append(contents.substring(0,idx1));
			int lastPos = idx1;
			while(idx1 != -1 && idx2 != -1){
				String row = contents.substring(idx1, idx2);
				Pattern linkPattern = Pattern.compile("href=\"javascript:goToThisCase\\('([^']+)', '([^']+)'\\);\"");
				Matcher linkMatcher = linkPattern.matcher(row);
				String clr = row.contains("class=\"td2\"") ? " bgcolor=\"#EEFEEE\"" : "";
				if(linkMatcher.find()){
					String link = linkPrefix + "/casenet/cases/header.do&inputVO.caseNumber=" + linkMatcher.group(1) + "&inputVO.courtId=" + linkMatcher.group(2);
					link = "<input type=\"checkbox\" name=\"docLink\" value=\""	+ link + "\"/>";
					row = row.replaceFirst("<td","<td" + clr + ">" + link + "</td><td");
				} else {
					row = row.replaceFirst("<td","<td" + clr + ">&nbsp;</td><td");
				}
				sb.append(row);				
				lastPos = idx2;
				idx1 = contents.indexOf("<tr align=\"left\">", idx2);
				idx2 = contents.indexOf("</tr>", idx1);
				
			}
			sb.append(contents.substring(lastPos));
			contents = sb.toString();
		}		
		
		contents = contents.replaceAll("class=\"td2\"", "bgcolor=\"#EEFEEE\"");
		// rewrite details links
		contents = contents.replaceAll(
			"href=\"javascript:goToThisCase\\('([^']+)', '([^']+)'\\);\"",
			"href=\"" + linkPrefix + "/casenet/cases/header.do&inputVO.caseNumber=$1&inputVO.courtId=$2\"");
		
		
	
		return contents;
	}

	/**
	 * Bring and append garnishments to the details page
	 * @param site
	 * @param page
	 * @param sb
	 */
	protected void addGarnishments(HttpSite site, String page, StringBuilder sb){
		
		// remove links
		sb.append(page.replaceAll("(?i)</?a[^>]*>", ""));

		// bring each garnishment
		Pattern garnPattern = Pattern.compile("<a href=\"(/casenet/cases/garnishments\\.do[^\"]+)\">");
		Matcher garnMatcher = garnPattern.matcher(page);
		while(garnMatcher.find()){
			String link2 = "https://www.courts.mo.gov" + garnMatcher.group(1);
			String page2 = site.process(new HTTPRequest(link2)).getResponseAsString();
			int idxa = page2.indexOf("<table width=\"100%\" style=\"vertical-align: top;\">");							
			if(idxa != -1){
				String pa = StringUtils.extractTagContents(idxa, page2, "table");
				if(!isEmpty(pa)){
					sb.append("<br/>");
					sb.append(pa);									
				}
				int idxb = page2.indexOf("<table width=\"100%\" style=\"vertical-align: top;\">", idxa + 1);
				if(idxb!=-1){
					String pb = StringUtils.extractTagContents(idxb, page2, "table");
					if(!isEmpty(pb)){
						sb.append("<br/>");
						sb.append(pb);
					}
				}
			}
		}		
	}
	
	/**
	 * Create details page
	 * @param response
	 * @return
	 */
	protected String getDetailsPage(String response) {

		StringBuilder sb = new StringBuilder();
		
		// get the title 
		int idx1 = response.indexOf("<table style=\"width: 100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");
		if(idx1 == -1){
			return "";
		}
		String p1 = StringUtils.extractTagContents(idx1, response, "table");
		if(isEmpty(p1)){
			return sb.toString();
		}
		sb.append(p1);
		
		// get the details
		int idx2 = response.indexOf("<table class=\"detailRecordTable\">");
		if(idx2 == -1){
			return "";
		}
		String p2 = StringUtils.extractTagContents(idx2, response, "table");
		if(isEmpty(p2)){
			return sb.toString();
		}
		sb.append("<br/><b>Case Header</b><hr/>");
		sb.append(p2);
		
		// parse the params
		SimpleHtmlParser prsr = new SimpleHtmlParser(response);
		SimpleHtmlParser.Form form = prsr.getForm("casePalletteForm");
		if(form == null){
			return sb.toString();
		}
		
		String[][] items = new String[][] { 
				{"parties", "Parties &amp; Attorneys"},
				{"searchDockets", "Docket Entries"},
				{"charges", "Charges, Judgments &amp; Sentences"},
				//{"service", "Service Information"},
				//{"filings", "Fillings Due"},
				//{"events", "Scheduled Hearings &amp; Trials"},
				{"judgements","Civil Judgments"}
				//{"garnishment", "Garnishments/Execution"} 
				};

		// add all details
		String siteName = TSServer.getCrtTSServerName((int)miServerID);
		HttpSite site = HttpManager.getSite(siteName, searchId);
		try {
			for (String[] item : items) {
				String link = "https://www.courts.mo.gov/casenet/cases/" + item[0] + ".do";
				String title = item[1];
				Map<String, String> params = form.getParams();
				try {
					// bring the details
					HTTPRequest request = new HTTPRequest(link, HTTPRequest.POST);
					request.addPostParameters(params);
					String page = site.process(request).getResponseAsString();
					page = page.replaceFirst("<table class=\"detailRecordTable\"\\s*>", "<table class=\"detailRecordTable\">");
					page = page.replaceFirst("<table width=\"100%\"  style=\"vertical-align: top;\"\\s*>", 
							"<table width=\"100%\"  style=\"vertical-align: top;\">");
					int idx3 = -1;
					int idx4 = -1;
					if ("judgements".equals(item[0])) {
						idx3 = page.indexOf("<table width=\"100%\"  style=\"vertical-align: top;\">");
						idx4 = page.indexOf("<table class=\"detailRecordTable\">",idx3);
					}
					else {	
						idx3 = idx4 = page.indexOf("<table class=\"detailRecordTable\">");
					}
					if(idx4 == -1){ 						
						idx3 = page.indexOf("<table cellpadding=\"5\" cellspacing=\"0\" border=\"0\" width=\"100%\">");
						if(idx3 == -1){
							continue;
						}
					}
					String add = StringUtils.extractTagContents(idx3, page, "table");
					if(isEmpty(add)){
						continue;
					}
					sb.append("<br/><b>" + title + "</b><hr/>");					
					if ("judgements".equals(item[0]))
						add = add.replaceFirst("(?is)<tr>.*?" +
								"This information is provided as a service and is not considered an official court record\\." +
								".*?</tr>", "");
					if("garnishment".equals(item[0])){
						addGarnishments(site, add, sb);						
					} else {						
						sb.append(add.replaceAll("background: #f5f5c0", ""));						
					}
				}catch(Exception e){
					logger.error("Could not get link:" + link);
				}
			}
		} finally {
			HttpManager.releaseSite(site);
		}
		
		return sb.toString().replaceAll("(?is)<a[^>]+>([A-Z\\s</>]+)</a>", "$1");
	}
	
	/**
	 * Get one link to certain page no from paginated results
	 * @param pageNo
	 * @param params
	 * @return
	 */
	protected String getNavLink(int pageNo, Map<String,String> params){		
		if(pageNo == -1){
			return null;
		}
		String startingRecord = String.valueOf((pageNo-1) * 8 + 1);
		params.put("startingRecord", startingRecord);
		params.put("inputVO.startingRecord", startingRecord);
		String link = CreatePartialLink(TSConnectionURL.idPOST) + "/casenet/cases/nameSearch.do";;
		for(Map.Entry<String, String> entry : params.entrySet()){
			link += "&" + entry.getKey() + "=" + entry.getValue();				
		}
		return link;		
	}
	
	/**
	 * Get prev and next links
	 * @param response
	 * @return
	 */
	protected String[] getNavigationLinks(String response){

		// check to see if we have a paged response 
		Pattern crtPagePattern = Pattern.compile("<span class=\"selectedPage\">(\\d+)</span>");
		Matcher crtPageMatcher = crtPagePattern.matcher(response);
		if(!crtPageMatcher.find()){
			return new String[]{null,null};
		}
		
		// identify prev and next page numbers
		int crtPageNo = Integer.valueOf(crtPageMatcher.group(1));		
		int prevPageNo = -1;
		int nextPageNo = -1;
		Pattern lnkPagePattern = Pattern.compile("href=\"javascript:goToThisPage\\((\\d+)\\);\"");
		Matcher lnkPageMatcher = lnkPagePattern.matcher(response);
		while(lnkPageMatcher.find()){
			int pageNo = Integer.valueOf(lnkPageMatcher.group(1));
			if(pageNo == (crtPageNo -1)){
				prevPageNo = crtPageNo - 1;
			} else if(pageNo == (crtPageNo + 1)){
				nextPageNo = crtPageNo + 1;  
			}
			if(prevPageNo != -1 && nextPageNo != -1){
				break;
			}
		}
		
		// create the links		
		Map<String,String> params = new SimpleHtmlParser(response).getForm("nameSearchForm").getParams();
		String [] links = new String[2];
		links[0] = getNavLink(prevPageNo, params);
		links[1] = getNavLink(nextPageNo, params);		
		return links;
	}
	
	/**
	 * Get summary from intermediate results
	 * @param response
	 * @return
	 */
	protected String getSummary(String response){
		// Displaying <b> 1 </b> thru <b>5</b> of <b>5</b>
		Pattern recsPattern = Pattern.compile("Displaying[\\s\\n\\r]*<b>[\\s\\n\r]*(\\d+)[\\s\\n\\r]*</b>[\\s\\n\\r]*thru[\\s\\n\\r]*<b>(\\d+)</b>[\\s\\n\\r]*of[\\s\\n\\r]*<b>(\\d+)");
		Matcher recsMatcher = recsPattern.matcher(response);
		if(recsMatcher.find()){
			String summary = recsMatcher.group(0).replaceAll("[\\n\\r\\s]+", " ").trim();
			return summary;
		} else {		
			return "";
		}
	}
	
	/**
	 * Legacy
	 * @param Response
	 */
	@SuppressWarnings("rawtypes")
	private void storeParty(ServerResponse Response) {
		
		String mainResponse = Response.getParsedResponse().getResponse();
		Vector resultRows = Response.getParsedResponse().getResultRows();
		
		for (int resultsNo = 0; resultsNo < Response.getParsedResponse().getResultRows().size(); resultsNo++) {
			
			ParsedResponse pr = (ParsedResponse) resultRows.elementAt(resultsNo);
			
			String partyName = pr.getCourtDocumentIdentificationSet(0).getAtribute("PartyName");
			String partyType = pr.getCourtDocumentIdentificationSet(0).getAtribute("PartyType");
			
			if(partyType.equals("Defendant") || partyType.equals("Respondent")){
				partyType = "d";
			} else {
				partyType = "p";
			}
			
			if (!isEmpty(partyName)) {
				
				String resultRow = pr.getResponse();
				try {
					partyName = URLEncoder.encode(partyName + "_" + partyType, "UTF-8");
					String hrefUrl = "";
					int istart = resultRow.indexOf("href=\"");
					int iend = -1;
					if (istart >= 0) {
						iend = resultRow.indexOf("\"", istart + 6);
					}
					if (istart >= 0 && iend >= 0) {
						hrefUrl = resultRow.substring(istart + 6, iend);
					} else {
						continue;
					}
					resultRow = resultRow.replace(hrefUrl, hrefUrl + "&PN=" + partyName);
					pr.setResponse(resultRow);
					LinkInPage pageLink = pr.getPageLink();
					pageLink.setOnlyOriginalLink(pageLink.getOriginalLink() + "&PN=" + partyName);
					pageLink.setOnlyLink(pageLink.getLink() + "&PN=" + partyName);
					mainResponse = mainResponse.replace(hrefUrl, hrefUrl + "&PN=" + partyName);
				} catch (Exception e) {
				}
			}
		}
		
		Response.getParsedResponse().setResponse(mainResponse);
		Response.getParsedResponse().setResultRows(resultRows);
	}
	
	/**
	 * Legacy 
	 * @param Response
	 */
	@SuppressWarnings("unchecked")
	private void retrieveParty(ServerResponse Response) {
		
		// check for special link
		ParsedResponse pr = Response.getParsedResponse();
		int istart = Response.getQuerry().indexOf("&PN=");
		if (istart == -1) {
			return;
		}
		
		// check party name not empty
		String partyName = Response.getQuerry().substring(istart + 4);
		if (partyName.indexOf("&") >= 0) {
			partyName = partyName.substring(0, partyName.indexOf("&"));
		}
		if (isEmpty(partyName)) {
			return;
		}
		
		// decode party name
		try {
			partyName = URLDecoder.decode(partyName, "UTF-8");
		} catch (Exception e) {
		}
		
		// extracty party type info
		boolean isDefendant = partyName.endsWith("_d");
		partyName = partyName.replaceFirst("_[dp]$", "");
		
		// add the new element to either grantors or grantees set
		InfSet g;
		if(isDefendant){
			g = new GranteeSet();
			Vector<GranteeSet> grantees = (Vector<GranteeSet>) pr.getGranteeNameSet();
			if (grantees == null) {
				grantees = new Vector<GranteeSet>();
				pr.setGranteeNameSet(grantees);
			}
			grantees.add((GranteeSet)g);
		} else {
			g = new GrantorSet();
			Vector<GrantorSet> grantors = (Vector<GrantorSet>) pr.getGrantorNameSet();
			if (grantors == null) {
				grantors = new Vector<GrantorSet>();
				pr.setGrantorNameSet(grantors);
			}
			grantors.add((GrantorSet)g);
		}
		
		// set the values of the new element
		String[] tokens = StringFormats.parseNameNashville(partyName);
		g.setAtribute("PropertyIdentificationSet.OwnerFirstName", tokens[0]);
		g.setAtribute("PropertyIdentificationSet.OwnerMiddleName", tokens[1]);
		g.setAtribute("PropertyIdentificationSet.OwnerLastName", tokens[2]);
		g.setAtribute("PropertyIdentificationSet.SpouseFirstName", tokens[3]);
		g.setAtribute("PropertyIdentificationSet.SpouseMiddleName", tokens[4]);
		g.setAtribute("PropertyIdentificationSet.SpouseLastName", tokens[5]);		
				
		// add it to sale data set
		SaleDataSet sds = pr.getSaleDataSet(0);
		String key = isDefendant ? "Grantee" : "Grantor";
		String prev = sds.getAtribute(key); 		
		if(StringUtils.isEmpty(prev)){
			sds.setAtribute(key, partyName);
		} else {
			String otherKey = isDefendant ? "Grantor" : "Grantee";
			String otherPrev = sds.getAtribute(otherKey);
			if(StringUtils.isEmpty(otherPrev)){
				sds.setAtribute(otherKey, partyName);
			} else {
				sds.setAtribute(key, prev + "/" + partyName);
			}
		}
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String response = Response.getResult();

		// check for no results
		if (response.contains("Your query returned no matches")) {
			Response.getParsedResponse().setError("<font color=\"red\">No results found.</font>");
			return;
		}
		
		// check for too many results
		if (response.contains("You have attempted to search on a 'common' Last Name")) {
			Response.getParsedResponse().setError("<font color=\"red\">Refine the search. Too many results.</font>");
			return;
		}

		// check for errors
		String error = StringUtils.extractParameter(response, "<td class=\"errorLine\">[\\n\\r\\s]*([^\\n\\r]+)[\\n\\r\\s]*</td>");
		if (!isEmpty(error)) {
			Response.getParsedResponse().setError(error);
			return;
		}

		switch (viParseID) {

		case ID_SEARCH_BY_NAME:
			
			// check for warnings
			String message = StringUtils.extractParameter(response, "(?is)<td class=\"messageLine\">[\\n\\s\\r]*([^\\n\\r]*)[\\n\\s\\r]*</td>");
			if (!isEmpty(message)) {
				Response.getParsedResponse().setWarning(message);
				return;
			}
			
			// get intermediary results table
			String contents = getIntermediateTable(response);			
			if(isEmpty(contents)){
				return;
			}
			
			// get navigation links
			String links [] = getNavigationLinks(response);
			String prevLink = links[0];
			String nextLink = links[1];
			
			// create footer
			String footer = "";
			if(!isEmpty(prevLink)){
				footer += "<a href='" + prevLink + "'>Previous</a>&nbsp;&nbsp;";
			}			
			if(!isEmpty(nextLink)){
				footer += "<a href='" + nextLink + "'>Next</a>&nbsp;&nbsp;";
			}
			footer += getSummary(response)+ "<br/><br/>";

			parser.Parse(Response.getParsedResponse(), contents + footer, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idPOST),	TSServer.REQUEST_SAVE_TO_TSD);
			
			contents = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST") + 
						contents + footer + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);

			parser.Parse(Response.getParsedResponse(), contents, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idPOST),	TSServer.REQUEST_SAVE_TO_TSD);

			if (nextLink != null){
				Response.getParsedResponse().setNextLink("<a href=\"" + nextLink + "\">Next</a>");
			}
			
			storeParty(Response);
				
			break;

		case ID_DETAILS:
			
			String details = response;
			if(response.contains("<!DOCTYPE html PUBLIC")){
				details = getDetailsPage(response);
			}
			if(isEmpty(details)){
				return;
			}
			
			try {
				details = new String(details.getBytes(), "ISO-8859-1");
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			String instNo = StringUtils.extractParameter(response, "<td class=\"searchType\">([^\\s]+)");
			String type = "";
			try {
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(details, null);
				NodeList nodeList = htmlParser.parse(null);
				type = HtmlParser3.getValueFromNextCell(nodeList,"Case Type:", "", false).trim();
				
			}catch(Exception e) {
				e.printStackTrace();
			}

			if(!downloadingForSave){
				
				String qry = Response.getQuerry();
				qry = "dummy=" + instNo + "&" + qry;
				String originalLink = sAction + "&" + qry;
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
				
				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", type);
				data.put("dataSource","CO");
				
				if(isInstrumentSaved(instNo, null, data)){
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
					
				}
				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parser.Parse(Response.getParsedResponse(), details, Parser.NO_PARSE);
				
			} else {
				
				msSaveToTSDFileName = instNo + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD(true);
				ParsedResponse pr = Response.getParsedResponse();
				parser.Parse(pr, details.replace("\000"," ").replace("\0x0"," "), Parser.PAGE_DETAILS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
				Response.getParsedResponse().setOnlyResponse(details);				
				retrieveParty(Response);
			}	
			
			break;
			
		case ID_GET_LINK:

			if (sAction.indexOf("nameSearch.do") != -1) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			} else if (sAction.indexOf("header.do") != -1) {
				ParseResponse(sAction, Response, ID_DETAILS);
			}
			break;
			
		case ID_SAVE_TO_TSD:
			
			downloadingForSave = true;
			ParseResponse(sAction, Response, ID_DETAILS);
			downloadingForSave = false;
			
			break;
		}
	}
	
	/**
	 * Create Already Present filter
	 * @return
	 */
	protected FilterResponse getAlreadyPresentFilter(){
		RejectAlreadyPresentFilterResponse filter = new RejectAlreadyPresentFilterResponse(searchId);	
		filter.setUseInstr(true);
		filter.setThreshold(new BigDecimal("0.90"));
		return filter;
	}
	
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
    
		ConfigurableNameIterator nameIterator = null;
		
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();	
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
	    TSServerInfoModule module;	
	    GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);
        
		FilterResponse alreadyPresentFilter = getAlreadyPresentFilter();		
		FilterResponse intervalFilter = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId);
		FilterResponse acCourtsFilter = new AcCourtsFilter(searchId);
		for(String type: new String []{"Civil"} ){
			for (String id : gbm.getGbTransfers()) {
				  		   	    	 
		         module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
		         module.setIndexInGB(id);
		         module.setTypeSearchGB("grantor");
		         module.clearSaKeys();
			     module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
			     module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
			    
			 	 module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			 	 module.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			 	 module.getFunction(3).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE);
			 	 module.getFunction(4).forceValue(type);
			 	 module.addFilter(acCourtsFilter);
			 	 module.addFilter(alreadyPresentFilter);
			 	 module.addFilter(intervalFilter);
			 	 nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;f;"} );
				 module.addIterator(nameIterator);
			
				 modules.add(module);
			    
			     
			     if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
				     module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				     module.setIndexInGB(id);
				     module.setTypeSearchGB("grantee");
				     module.clearSaKeys();
					 module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
					 module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
					 
					 module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
					 module.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
					 module.getFunction(3).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE);
					 module.getFunction(4).forceValue(type);
					 module.addFilter(acCourtsFilter);
					 module.addFilter(alreadyPresentFilter);
					 module.addFilter(intervalFilter);
					 nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;f;"} );
					 module.addIterator(nameIterator);
					 
					 modules.add(module);
				 
			     }	
		    }
		}	
		serverInfo.setModulesForGoBackOneLevelSearch(modules);	    
    }
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		FilterResponse alreadyPresentFilter = getAlreadyPresentFilter();		
		FilterResponse acCourtsFilter = new AcCourtsFilter(searchId);
				

		NameFilter nameFilter = new NameFilter(SearchAttributes.OWNER_OBJECT, searchId);
		String type = "Civil";
		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));				
		module.clearSaKeys();
		/*don't trust the derivation here, look in ConfigurableNameIterator for more details for Case Net*/
		ConfigurableNameIterator iterator = new ConfigurableNameIterator(searchId, true, new String[]{"L;f;"});
		iterator.setInitAgain(true);
		module.addIterator(iterator);
		module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
		module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
		module.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
		module.getFunction(3).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE);
		module.getFunction(4).forceValue(type);
		nameFilter.setUseSynonymsForCandidates(true);
		module.addFilter(acCourtsFilter);
		module.addFilter(nameFilter);
		module.addFilter(alreadyPresentFilter);
		addBetweenDateTest(module, true, true, true);		
		modules.add(module);
		
		serverInfo.setModulesForAutoSearch(modules);
		
	}

	@Override
	protected String getFileNameFromLink(String url) {
		String rez = url.replaceAll(".*insno=(.*?)(?=&|$)", "$1");
		if (rez.trim().length() > 10)
			rez = rez.replaceAll("&parentSite=true", "");
		if (rez.trim().length() <= 15)
			return rez.trim() + ".html";
		else
			return msSaveToTSDFileName;
	}

	/**
	 * Split the intermediate results into rows
	 * @param p
	 * @param pr
	 * @param htmlString
	 * @param pageId
	 * @param linkStart
	 * @param action
	 * @throws ro.cst.tsearch.exceptions.ServerResponseException
	 */
	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action)
			throws ro.cst.tsearch.exceptions.ServerResponseException {

		Vector<ParsedResponse> parsedRows = new Vector<ParsedResponse>();
		int startIdx = 0, endIdx = 0;
		String vsRowSeparator = "<tr align=\"left\">", rowEndSeparator = "</tr>";

		startIdx = htmlString.indexOf(vsRowSeparator, 0);
		if (startIdx != -1) {

			pr.setHeader(htmlString.substring(0, startIdx));

			endIdx = htmlString.indexOf(vsRowSeparator, startIdx + vsRowSeparator.length());
			endIdx = htmlString.indexOf(vsRowSeparator, endIdx + vsRowSeparator.length());

			while (endIdx != -1) {

				String row = htmlString.substring(startIdx, endIdx);
				row = row.replaceAll("\n", " ").replaceAll("\r", " ").replaceAll("\n\r", " ");

				ParsedResponse pResponse = new ParsedResponse();
				p.Parse(pResponse, row, pageId, linkStart, action);
				parsedRows.add(pResponse);

				startIdx = endIdx;
				endIdx = htmlString.indexOf(vsRowSeparator, startIdx + vsRowSeparator.length());
				endIdx = htmlString.indexOf(vsRowSeparator, endIdx + vsRowSeparator.length());
			}

			endIdx = htmlString.indexOf(rowEndSeparator, startIdx);
			if (endIdx == -1)
				endIdx = htmlString.length();
			endIdx = htmlString.indexOf(rowEndSeparator, endIdx + vsRowSeparator.length());
			if (endIdx == -1)
				endIdx = htmlString.length();

			String row = htmlString.substring(startIdx, endIdx).replaceAll("\n", " ").replaceAll("\r", " ").replaceAll("\n\r", " ");

			ParsedResponse pResponse = new ParsedResponse();
			p.Parse(pResponse, row, pageId, linkStart, action);
			parsedRows.add(pResponse);

			pr.setFooter(htmlString.substring(endIdx, htmlString.length()));

		} else {
			endIdx = htmlString.indexOf(rowEndSeparator, 0);
			if (endIdx == -1)
				endIdx = htmlString.length();

			pr.setHeader(htmlString.substring(0, endIdx));
			pr.setFooter(htmlString.substring(endIdx, htmlString.length()));
		}
		pr.setResultRows(parsedRows);

	}
	
	private static class AcCourtsFilter extends FilterResponse{
		
		private static final long serialVersionUID = 2767178948899823368L;

		public AcCourtsFilter(long searchId) {
			super(searchId);
			setThreshold(new BigDecimal("0.95"));
		}

		@Override
		public BigDecimal getScoreOneRow(ParsedResponse pr){
			if(pr.getCourtDocumentIdentificationSetCount() == 0){
				return ATSDecimalNumberFormat.ONE;
			}			
			String caseType = pr.getCourtDocumentIdentificationSet(0).getAtribute("CaseType");
			return caseType.startsWith("AC ") ? ATSDecimalNumberFormat.ZERO : ATSDecimalNumberFormat.ONE;
		}

		@Override
		public String getFilterCriteria() {		
			return "AC";
		}

		@Override
		public String getFilterName() {
			return "Reject AC Cases";
		}
		
	}
	
	public static class NameFilter extends GenericNameFilter {

		private static final long serialVersionUID = 5921250493503886730L;
		
		public NameFilter(String key, long searchId) {
			super(key, searchId);
			setIgnoreMiddleOnEmpty(true);
			setUseArrangements(false);
		}

		@Override
		protected GenericNameFilter.Result calculateMatchForFirstOrMiddle(String refToken, String candToken) {
			
			// empty reference or candidate
			if(refToken.length() == 0 || candToken.length() == 0){
				return new GenericNameFilter.Result(1.0d);
			}
			
			// candidate starts with reference
			if(candToken.startsWith(refToken)){
				return new GenericNameFilter.Result(1.0d);			
			}
			
			// ref = 'Walter' cand = 'W', 'Walt', 'Walte' 			
			if((candToken.length() == 1 || candToken.length() >= 4)  && refToken.startsWith(candToken)) {
				return new GenericNameFilter.Result(1.0d);
			}
			
			return new GenericNameFilter.Result(0.0d);
		}	
		
//		@Override
//		public String getFilterCriteria() {	
//			Set<NameI> ref = getRefNames();
//			StringBuilder nameBuff= new StringBuilder();
//			for ( NameI element : ref ) {
//				nameBuff.append( element.getFullName() );
//				nameBuff.append(";");
//				nameBuff.append( StringUtils.capitalizeFirstLettersOnly(element.getLastName()) + ", " + element.getFirstInitial().toUpperCase() + " " +  StringUtils.capitalizeFirstLettersOnly(element.getMiddleName()));
//				nameBuff.append(";");
//				if (element.getFirstName().length()>4){
//					nameBuff.append(StringUtils.capitalizeFirstLettersOnly(element.getLastName()) + ", " + StringUtils.capitalizeFirstLettersOnly(element.getFirstName().substring(0, 4)) + " " +  StringUtils.capitalizeFirstLettersOnly(element.getMiddleName()));
//					nameBuff.append(";");
//				}
//			}
//			return "Names: " + nameBuff.toString();
//		}

		@Override
		public String getFilterName() {
			return "Courts Name Filter";
		}
	}

}