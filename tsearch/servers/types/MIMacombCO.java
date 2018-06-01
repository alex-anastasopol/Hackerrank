package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.propertyInformation.Family;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadyPresentFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;

import ro.cst.tsearch.titledocument.abstracts.Chapter;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

public class MIMacombCO extends TSServer implements TSServerROLikeI {

	private static final long serialVersionUID = 6538526237924319266L;
	protected static final Category logger = Logger.getLogger(MIMacombCO.class);
	private boolean downloadingForSave;
	public static final Pattern caseNoPattern = Pattern.compile("\\d{4}-\\d{6}-\\w{2}");

	public MIMacombCO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	public TSServerInfo getDefaultServerInfo()
    {
		TSServerInfo msiServerInfoDefault=super.getDefaultServerInfo();
		setModulesForGoBackOneLevelSearch( msiServerInfoDefault);
		return msiServerInfoDefault;
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
			 module.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE);
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
				 module.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE);
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
			module.clearSaKeys();
			module.clearIteratorTypes();
			module.setSaObjKey(key);
			
			module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			module.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE);
			
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
    
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		String sTmp;
		int istart, iend;
		String keyNumber = "";

		String rsResponse = Response.getResult();
		String initialResponse = rsResponse;
		String next = null, prev = null;
		String imgLink = null;

		sTmp = CreatePartialLink(TSConnectionURL.idGET);
		next = StringUtils.getTextBetweenDelimiters("<a id=\"pagerNextPage\" href=\"", "\"", rsResponse).replace("&amp;", "&").replace("?","&");
		prev = StringUtils.getTextBetweenDelimiters("<a id=\"pagerPreviousPage\" href=\"", "\"", rsResponse).replace("&amp;", "&").replace("?", "&");

		switch (viParseID) {
		
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_INSTRUMENT_NO:
			
			istart = rsResponse.indexOf("<tr class=\"line\">");
			iend = rsResponse.indexOf("</table>", istart + 1);

			if (istart < 0 || iend < 0) {
				return;
			}

			rsResponse = "<table>" + rsResponse.substring(istart, iend) + "</table>";
			rsResponse = rsResponse.replaceAll("<a href=.*?CRTVSearchResults.jsp.*?</a>", "");
			rsResponse = rsResponse.replaceAll("<a.*?href=\"(.*?)CRTVCaseSummary.jsp\\?(.*?)\".*?>", "<a href=\"" + sTmp
					+ "$1CRTVCaseSummary.jsp&$2\"");

			rsResponse = rsResponse.replace("&amp;", "&");

			if (prev != null && !"".equals(prev)) {
				rsResponse = rsResponse + "<a href=\"" + sTmp + prev + "\">Previous</a>";
			}

			if (next != null && !"".equals(next)) {
				rsResponse = rsResponse + "&nbsp;&nbsp;&nbsp;<a href=\"" + sTmp + next + "\">Next</a>";
			}

			parser.Parse(Response.getParsedResponse(), rsResponse, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);

			if (!"".equals(next)) {
				Response.getParsedResponse().setNextLink("<a href=\"" + sTmp + next + "\">Next</a>");
			}

			break;
			
		case ID_DETAILS:
			
			istart = rsResponse.indexOf("<td class=\"casetitle\"");
			istart = rsResponse.lastIndexOf("<table", istart);

			iend = rsResponse.lastIndexOf("</table");
			iend = rsResponse.lastIndexOf("</table");
			iend = rsResponse.lastIndexOf("</table");

			if (istart < 0 || iend < 0) {
				return;
			}

			rsResponse = rsResponse.substring(istart, iend) + "</table>";
			Matcher keyNumberMatcher = caseNoPattern.matcher(rsResponse);
			if (keyNumberMatcher.find()) {
				//keyNumber = keyNumberMatcher.group( 0 ).replaceAll( "-" , "");
				iend = rsResponse.indexOf("<", keyNumberMatcher.start(0));
				keyNumber = rsResponse.substring(keyNumberMatcher.start(0), iend);
				keyNumber = keyNumber.replaceAll("[^A-Za-z0-9]", "");
			}

			if (!downloadingForSave) {
				
				//not saving to TSR
				String qry = Response.getQuerry();
				qry = "dummy=" + keyNumber + "&" + qry;
				Response.setQuerry(qry);
				String originalLink = sAction + "&" + qry;
				String sSave2TSDLink = CreatePartialLink(TSConnectionURL.idGET) + originalLink;

				if (FileAlreadyExist(keyNumber + ".html") ) {
					rsResponse += CreateFileAlreadyInTSD();
				} else {
					rsResponse = addSaveToTsdButton(rsResponse, sSave2TSDLink, viParseID);
					mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parser.Parse(Response.getParsedResponse(), rsResponse, Parser.NO_PARSE);
				
			} else {
				
				//saving
				msSaveToTSDFileName = keyNumber + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();
				if (imgLink != null) {
					Response.getParsedResponse().addImageLink(new ImageLinkInPage(imgLink, keyNumber + ".tiff"));
				}

				// HTML correction for docs with condominium
				rsResponse = rsResponse
						.replaceAll(
								"(?s)(<table width=100% cellspacing=0 cellpadding=0 border=1>.?<tr><td  colspan=6><b>Condominium Description Sequence #([2-9]|1\\d))",
								"</table>$1");
				rsResponse = Tidy.tidyParse(rsResponse, null);

				parser.Parse(Response.getParsedResponse(), rsResponse, Parser.PAGE_DETAILS, getLinkPrefix(TSConnectionURL.idGET),
						TSServer.REQUEST_SAVE_TO_TSD);
			}
			break;
			
		case ID_GET_LINK:		
			String query = Response.getQuerry();
			if (query.indexOf("last_name") >= 0 || query.indexOf("company_name") >= 0 || query.indexOf("case_nbr") >= 0) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			} else if (sAction.indexOf("CaseSummary") >= 0) {
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

	@SuppressWarnings("deprecation")
	protected String getFileNameFromLink(String link) {
		String parcelId = org.apache.commons.lang.StringUtils.getNestedString(link, "dummy=", "&");
		return parcelId + ".html";
	}

	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action)
			throws ro.cst.tsearch.exceptions.ServerResponseException {
		p.splitResultRows(pr, htmlString, pageId, "<tr>", "</table>", linkStart, action);
	}
}
