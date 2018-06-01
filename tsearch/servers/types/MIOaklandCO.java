package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.HTTPSiteManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadyPresentFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

public class MIOaklandCO extends TSServer implements TSServerROLikeI {

	private static final long serialVersionUID = 687728441406262412L;
	
	protected static final Category logger = Logger.getLogger(MIOaklandCO.class);
	
	public static final Pattern viewActionPattern = Pattern
			.compile("<a href=.*?ShowDockets\\('/crts0004/cs/docket.jsp\\?([^']*)'\\);\">View Register of Actions</a>");

	private boolean downloadingForSave;

	public MIOaklandCO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
		ConfigurableNameIterator nameIterator = null;
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();	
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
	    TSServerInfoModule module;	
	    GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);
		DocsValidator alreadyPresentValidator = new RejectAlreadyPresentFilterResponse(searchId).getValidator();
		GenericNameFilter nameFilter =null;
		for (String id : gbm.getGbTransfers()) {
			  		   	    	 
	         module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
	         module.setIndexInGB(id);
	         module.setTypeSearchGB("grantor");
	         module.clearSaKeys();
		     module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
		     module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			 module.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			 nameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
			 nameFilter.setIgnoreMiddleOnEmpty(true);
			 module.addFilter(nameFilter);
			 module.addValidator(alreadyPresentValidator);
		     nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;", "L;M;"} );
		 	 module.addIterator(nameIterator);
			 modules.add(module);
		    
		     
		     if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
			     module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			     module.setIndexInGB(id);
			     module.setTypeSearchGB("grantee");
			     module.clearSaKeys();
				 module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				 module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				 module.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				 nameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
				 nameFilter.setIgnoreMiddleOnEmpty(true);
				 module.addFilter(nameFilter);
				 module.addValidator(alreadyPresentValidator);
				 nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;", "L;M;"} );
				 module.addIterator(nameIterator);
			 
			
	
			 modules.add(module);
			 
		     }

	    }	 
  serverInfo.setModulesForGoBackOneLevelSearch(modules);	     
		     		     
	}
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		DocsValidator alreadyPresentValidator = new RejectAlreadyPresentFilterResponse(searchId).getValidator();
		
		
		for(String key: new String[]{SearchAttributes.OWNER_OBJECT, SearchAttributes.BUYER_OBJECT}){
			
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));			
			DocsValidator intervalValidator = new BetweenDatesFilterResponse(searchId, module).getValidator();
			module.setName("Automatic Name Search");
			module.clearSaKeys();
			module.clearIteratorTypes();
			module.setSaObjKey(key);
			module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			GenericNameFilter nameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(key, searchId, module);
	        nameFilter.setIgnoreMiddleOnEmpty(true);
	        module.addFilter(nameFilter);
	        module.addValidator(alreadyPresentValidator);
	        module.addValidator(intervalValidator);	        
			ConfigurableNameIterator iterator = new ConfigurableNameIterator(searchId, new String[] {"L;F;", "L;M;"});
			iterator.setInitAgain(true);
			module.addIterator(iterator);						
	        
			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);
	}

	private String getInstrumentNumber(String response) {
		String inum = StringUtils.extractParameter(response,
				"(?i)<td class='(?:tableShadow|tableBand)' width=\"130\" align=\"left\">([^<]+)</td>");
		
		if(!StringUtils.isEmpty(inum)){
			return inum;
		}
		
		try {
			int istart = response.indexOf("Case&nbsp;Number");

			istart = response.indexOf("<td", istart + 1);
			istart = response.indexOf(">", istart + 1);
			int iend = response.indexOf("</", istart + 1);

			inum = response.substring(istart + 1, iend);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return inum;
	}

	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		int istart, iend;

		String rsResponse = Response.getResult();
		String initialResponse = rsResponse;

		switch (viParseID) {
		
		case ID_SEARCH_BY_NAME:

			int formStart = rsResponse.indexOf("<form name=\"SearchResult\"");
			int formEnd = rsResponse.indexOf("</form", formStart);

			String linkNext = null;
			String linkPrevious = null;

			if (rsResponse.indexOf("Next 500 records") >= 0) {
				try {
					String nextForm = rsResponse.substring(formStart, formEnd);
					HashMap<String, String> formParams = HttpUtils.getFormParams(nextForm);

					linkNext = "";

					Iterator<String> keyIterator = formParams.keySet().iterator();
					while (keyIterator.hasNext()) {
						String postParam = keyIterator.next();

						linkNext += postParam + "=" + formParams.get(postParam) + "&";
					}

					if (linkNext.length() > 0) {
						linkNext = linkNext.substring(0, linkNext.length() - 1);
					}

					linkNext = CreatePartialLink(TSConnectionURL.idPOST) + "/crts0004/main&" + linkNext;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (rsResponse.indexOf("Previous 500 records") >= 0) {
				linkPrevious = "history.back(-1);";
			}

			rsResponse = getIntermResults(rsResponse);

			parser.Parse(Response.getParsedResponse(), rsResponse, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idPOST),
					TSServer.REQUEST_SAVE_TO_TSD);

			String footerNextLink = "";
			String footerPrevLink = "";

			if (linkNext != null) {
				footerNextLink = "<a href=\"" + linkNext + "\">Next</a>&nbsp;&nbsp;";
				Response.getParsedResponse().setNextLink(linkNext);
			}

			if (linkPrevious != null) {
				footerPrevLink = "<a name=\"here\"><a href=\"#here\" onClick=\"javascript: " + linkPrevious + "\">Previous</a>&nbsp;&nbsp;";
			}

			String navLinks = footerPrevLink + footerNextLink;
			if(!StringUtils.isEmpty(navLinks)){
				navLinks = "<br/>" + navLinks + "</br></br>";
			} else {
				navLinks = "<br/>";
			}
			
			if (Response.getParsedResponse().getResultsCount() != 0) {
				Response.getParsedResponse()
						.setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST") + Response.getParsedResponse().getHeader());
				Response.getParsedResponse().setFooter(
						Response.getParsedResponse().getFooter() + navLinks + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1));
			}
			break;
			
		case ID_DETAILS:

			boolean archivedCase = rsResponse.contains("<strong>Archived Case:</strong>");
			
			if(!archivedCase){
				
				Matcher showActionMatcher = viewActionPattern.matcher(rsResponse);
				String actions = "";
				if (showActionMatcher.find()) {
					// retrieve action dockets
					try {
						HTTPRequest req = new HTTPRequest("http://www.oakgov.com/crts0004/cs/docket.jsp?" + showActionMatcher.group(1));
						HTTPResponse res = HTTPSiteManager.pairHTTPSiteForTSServer("MIOaklandCO", searchId, miServerID).process(req);
	
						actions = res.getResponseAsString();
	
						istart = actions.indexOf("<form name=\"dockets\"");
						iend = actions.indexOf("</form");
	
						if (istart >= 0 && iend >= 0) {
							actions = actions.substring(istart, iend);
						}
						
						actions = actions.replace("Order Document", "");						

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
	
				istart = rsResponse.indexOf("<form name=\"caseSummary\"");
				iend = rsResponse.indexOf("</form");
	
				if (istart < 0 || iend < 0) {
					return;
				}
	
				rsResponse = rsResponse.substring(istart, iend);
				rsResponse += actions;
				
				rsResponse = rsResponse.replaceAll("class=[\"'](tableShadow)[\"']", "bgcolor=#F4F4F4");
				rsResponse = rsResponse.replaceAll("class=[\"'](medTextbold)[\"']", "style=\"font-weight:bold\"");
				rsResponse = rsResponse.replaceAll(">(View Register of Actions|New Search|View Parties)<", "><");
				
				rsResponse = rsResponse.replaceAll("<div[^>]*>", "<div>");
				rsResponse = rsResponse.replace("width=\"762\"", "width=\"100%\"");
				rsResponse = rsResponse.replace("<table cellspacing=\"0\" cellpadding=\"0\">",
						"<table cellspacing=\"0\" cellpadding=\"0\" width=\"100%\">");
				rsResponse = "<table align=\"center\" border=\"1\" cellpadding=\"0\" cellspacing=\"0\" width=\"762\">" + "<tr><td>" + rsResponse
						+ "</td></tr></table>";
				
			} 

			String instrNum = getInstrumentNumber(rsResponse);
			
			rsResponse = rsResponse.replaceAll("(?i)<form.*?>", "");
			rsResponse = rsResponse.replaceAll("(?i)<input.*?>", "");
			rsResponse = rsResponse.replaceAll("(?is)<div[^>]*>", "");
			rsResponse = rsResponse.replaceAll("(?is)</div>", "");
			rsResponse = rsResponse.replaceAll("(?i)<a .*?>([^<]*)</a>", "$1");

			if ((!downloadingForSave)) {
				
				String qry = Response.getQuerry();
				qry = "dummy=" + instrNum + "&" + qry;
				String originalLink = sAction + "&" + qry;
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
				if (FileAlreadyExist(instrNum + ".html") ) {
					rsResponse += CreateFileAlreadyInTSD();
				} else {
					rsResponse = addSaveToTsdButton(rsResponse, sSave2TSDLink,viParseID);
					mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));

				rsResponse = Tidy.tidyParse(rsResponse, null);		
				parser.Parse(Response.getParsedResponse(), rsResponse, Parser.NO_PARSE);
				
			} else {
				
				msSaveToTSDFileName = instrNum + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);

				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD(true);

				ParsedResponse pr = Response.getParsedResponse();

				rsResponse = Tidy.tidyParse(rsResponse, null);
				if(!archivedCase){
					parser.Parse(pr, rsResponse, Parser.PAGE_DETAILS, getLinkPrefix(TSConnectionURL.idPOST), TSServer.REQUEST_SAVE_TO_TSD);
				} else {
					parser.Parse(pr, rsResponse, Parser.PAGE_DETAILS, getLinkPrefix(TSConnectionURL.idPOST), TSServer.REQUEST_SAVE_TO_TSD, "-archived", "");
				}

				Response.getParsedResponse().setOnlyResponse(rsResponse);
			}

			break;

		case ID_GET_LINK:

			if (sAction.indexOf("caseSummary.jsp") >= 0) {
				ParseResponse(sAction, Response, ID_DETAILS);
			} else {
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

	private String getIntermResults(String rsResponse){		
		
		int istart = rsResponse.indexOf("<table cellspacing=\"0\" cellpadding=\"0\">");
		int iend = rsResponse.indexOf("</table>", istart + 1);

		if (istart < 0 || iend < 0) {
			return "";
		}

		istart += "<table cellspacing=\"0\" cellpadding=\"0\">".length();
		iend += "</table>".length();
		
		rsResponse = 
			"<table cellspacing=\"0\" cellpadding=\"0\" border=\"1\">" +				
		  	"<tr bgcolor='#ACACAC'>" + 
		  	"<th><div class=\"submitLinkBlue\" onClick=\"var elems=document.getElementsByName('docLink'); for(var i=0; i<elems.length;i++) {elems[i].checked = !elems[i].checked;}\">Sel</div></th>" +
		  	"<th align='left'>Party&nbsp;Name</th>" +
		  	"<th align='left'>Party&nbsp;Type</th>" +
		  	"<th align='left'>Opposition&nbsp;Party</th>" +
			"<th align='left'>Case&nbsp;Number</th>" +
		  	"<th align='left'>Judge</th>" +
			"<th align='left'>Archived</th>"+
			"<th align='left'></th></tr>" +
			rsResponse.substring(istart, iend);

		String docHeader =  
			"<table cellspacing=\"0\" cellpadding=\"0\" border=\"1\">" +				
		  	"<tr bgcolor='#ACACAC'>" + 
		  	"<th></th>" +
		  	"<th align='left'>Party&nbsp;Name</th>" +
		  	"<th align='left'>Party&nbsp;Type</th>" +
		  	"<th align='left'>Opposition&nbsp;Party</th>" +
			"<th align='left'>Case&nbsp;Number</th>" +
		  	"<th align='left'>Judge</th>" +
			"<th align='left'>Archived</th>"+
			"<th align='left'></th>";

		String sTmp = CreatePartialLink(TSConnectionURL.idPOST);
		String sTmp1 = getLinkPrefix(TSConnectionURL.idPOST);
		
		Pattern pat = Pattern.compile("(?i)<td class='(?:tableShadow|tableBand)' width=\"225\"[^>]*>(<a href[^>]*>)?([^><]*)(</a>)?</td>");
		
		int lastStart = rsResponse.indexOf("<tr>");
		if(lastStart == -1){
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(rsResponse.substring(0, lastStart));
		
		while(true){
			
			int start = rsResponse.indexOf("<tr>", lastStart);
			int end = rsResponse.indexOf("</tr>", start);
			if(start == -1 || end == -1){
				break;
			}
			end += "</tr>".length();
			
			// append the text between the previous and last matches
			sb.append(rsResponse.substring(lastStart, start));
			lastStart = end;
			
			// isolate the row and identify first TD
			String row = rsResponse.substring(start, end);
			Matcher mat = pat.matcher(row);
			if(!mat.find()){
				continue;
			}
			
			// rewrite the td
			String linkBlock = mat.group(1);
			String name = mat.group(2);
			String link = "";
			if(linkBlock != null){			
				link = StringUtils.extractParameter(linkBlock, "ShowCaseSummary\\('([^']+)'\\)");
			}
		
			boolean fakeLink = StringUtils.isEmpty(link);
			
			if(fakeLink){
				String caseNo = StringUtils.extractParameter(row,
						"(?i)<td class='(?:tableShadow|tableBand)' width=\"130\" align=\"left\">([^<]+)</td>");
				link = "MIOAKLANDCO___" + caseNo;
			}
			
			String checkBox = "<input type=\"checkbox\" name=\"docLink\" value=\"" + sTmp + link + "\">";
			String selfLink = "<a href=\"" + sTmp + link + "\">";
			String td;
			if(fakeLink){
				td = "<td>" + checkBox + "</td><td>"+ selfLink + "</a>" + name + "</td>";
			} else {
				td = "<td>" + checkBox + "</td><td>"+ selfLink + name + "</a></td>";
			}				
			String modifRow = row.substring(0, mat.start()) + td + row.substring(mat.end());
				
			if(fakeLink){			
				String doc = "<strong>Archived Case:</strong><p/>" + 
					docHeader + modifRow + "</table>";				
				mSearch.addInMemoryDoc(sTmp + link, doc);				
				mSearch.addInMemoryDoc(sTmp1 + link, doc);
			}				
			
			// append the modified row
			sb.append(modifRow);						
		}

		// append suffix and return 
		sb.append(rsResponse.substring(lastStart));		
		return sb.toString();
	}
	
	@SuppressWarnings("deprecation")
	protected String getFileNameFromLink(String link) {
		String parcelId = StringUtils.extractParameter(link, "MIOAKLANDCO___([^&]+)");
		if(StringUtils.isEmpty(parcelId)){
			parcelId = org.apache.commons.lang.StringUtils.getNestedString(link, "dummy=", "&");
		}
		return parcelId + ".html";
	}

	@SuppressWarnings("unchecked")
	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action)
			throws ro.cst.tsearch.exceptions.ServerResponseException {
		
		// split rows
		p.splitResultRows(pr, htmlString, pageId, "<tr", "</table>", linkStart, action);
		
		// remove table header
		Vector<ParsedResponse> rows = (Vector<ParsedResponse>)pr.getResultRows();
		if (rows.size() > 0) {
			ParsedResponse firstRow = rows.remove(0);
			pr.setResultRows(rows);
			pr.setHeader(pr.getHeader() + firstRow.getResponse());
		}
	
	}
	
	@Override
    protected ServerResponse SearchBy(boolean bResetQuery, TSServerInfoModule module, Object sd) throws ServerResponseException {
	
		// if we have only last name, move it in the company box,
		// but only for automatic searches
		if(module.getName().toLowerCase().contains("automatic")){
			String last = module.getParamValue(0);
			String first = module.getParamValue(1);
			String company = module.getParamValue(2);		
			if(StringUtils.isEmpty(first) && StringUtils.isEmpty(company) && !StringUtils.isEmpty(last)){
				module.forceValue(2, last);
				module.forceValue(0, "");
			}
		}
		
    	return super.SearchBy(bResetQuery, module, sd);
    }
    
}
