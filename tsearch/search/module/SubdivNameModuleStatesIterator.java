package ro.cst.tsearch.search.module;

import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Category;

import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.Decision;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.TSInterface;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.utils.StringUtils;
/**
 * SubdivNameModuleStatesIterator
 *
 */
public class SubdivNameModuleStatesIterator extends ModuleStatesIterator {

	protected static final Category logger= Category.getInstance(SubdivNameModuleStatesIterator.class.getName());
	private int nCount = 0;
	
	public SubdivNameModuleStatesIterator(long searchId){
		super(searchId);
	}
	
	public boolean timeToStop(Decision d){
		if (logger.isDebugEnabled())
			logger.debug("SubdivNameModuleStatesIterator: timeToStop" );
		
		if ((d == null)||(d.getServerResponse()==null)){ 
			return false;
		}

		ServerResponse sr = d.getServerResponse();
		ParsedResponse pr = sr.getParsedResponse();
		TSInterface intrfServer = d.getIntrfServer(); 

		if(pr.isUnique()) {
			if (logger.isDebugEnabled())
				logger.debug("obtained unique, filtering page 2 of intermediary results");
			try {
				Matcher m = Pattern.compile("<a\\s+href=['\\\"]?([^>'\\\"]*)['\\\">]", 
					Pattern.CASE_INSENSITIVE).matcher(
					(String)sr.getParsedResponse().getResultRowsAsStrings().get(0));
				if(m.find()) {
					ServerResponse stmpResponse= intrfServer.GetLink(m.group(1), true);
					appendNextResults (stmpResponse, intrfServer);
					sr.setParsedResponse(stmpResponse.getParsedResponse());
				}
			}catch(ServerResponseException sre) {
				return true;
			}
			return true;	 
		}else if (pr.getResultsCount() == ParsedResponse.UNKNOW_RESULTS_COUNT) {
			if (logger.isDebugEnabled())
				logger.debug("obtained unknown  =>stop");
			return true;
		}else if (pr.isMultiple()){
			if (logger.isDebugEnabled())
				logger.debug("obtained multiple =>stop");
			return true;
		}else{
			if (logger.isDebugEnabled())
				logger.debug("obtained none");
			return false;
		}
	}

	public boolean appendNextResults (ServerResponse Response, TSInterface intrfServer) throws ServerResponseException {

		 if ( !(((TSServer)intrfServer).continueSeach() || 
	               Response.isParentSiteSearch()) ||
	             !((TSServer)intrfServer).continueSeachOnThisServer() ||
	             ((TSServer)intrfServer).skipCurrentSite() || ((TSServer)intrfServer).isStopAutomaticSearch())
		        return false;
		 
		long originalRowsNo = Response.getParsedResponse().getResultsCount();
		if (logger.isDebugEnabled())
			logger.debug(" originalRowsNo = [" + originalRowsNo + "]"); 

		for (Iterator iter = filtersForNext.iterator(); iter.hasNext();) {
			FilterResponse filter = (FilterResponse) iter.next();
			filter.filterResponse(Response);
		}

		String alllinkNext = Response.getParsedResponse().getNextLink();

		if (StringUtils.isStringBlank(alllinkNext)){
			return false;
		}

		Response.getParsedResponse().setNextLink("");
		String footer = Response.getParsedResponse().getFooter();
		//logger.debug(" footer = " + footer);
		//logger.debug(" linkNext = " + linkNext);
		footer = StringUtils.replaceFirstSubstring(footer, alllinkNext, "");
		Response.getParsedResponse().setFooter(footer);

		TSServerInfoModule crtModule = (TSServerInfoModule) current();
		if (!crtModule.isGoOnNextLink()){
			return false;
		}

		long afterFilteringRowsNo = Response.getParsedResponse().getResultsCount();
		if (logger.isDebugEnabled())
			logger.debug(" afterFilteringRowsNo = [" + afterFilteringRowsNo + "]");
		if (originalRowsNo-afterFilteringRowsNo > 5){
			if(nCount < 5 && afterFilteringRowsNo == 0) {
				nCount++; 
			}
			else {
				return false;				
			}
		}


		if (logger.isDebugEnabled())
			logger.debug(" bring results from next " + alllinkNext);
		
		Matcher m = Pattern.compile("<a\\s+href=['\\\"]?([^>'\\\"]*)['\\\">]", Pattern.CASE_INSENSITIVE).matcher(alllinkNext);
		if( m.find() ){
			String linkNext = m.group(1);
			if (logger.isDebugEnabled())
				logger.debug(" actualLink= " + linkNext);
			
			if (linkNext.endsWith(Response.getQuerry())){
				if (logger.isDebugEnabled())
					logger.debug(" next-link points to the same page!!  don't go there" );
				return false;
			} 

			ServerResponse  stmpResponse= intrfServer.GetLink(linkNext, true);
			appendNextResults (stmpResponse, intrfServer);

			
			String nextPageText = stmpResponse.getParsedResponse().getResponse();
			Vector nextPageRows = stmpResponse.getParsedResponse().getResultRows();
	
			String crtPageText = Response.getParsedResponse().getResponse();
			crtPageText += "<br> "+nextPageText;
			Response.getParsedResponse().setOnlyResponse(crtPageText);
		
		
			Vector parsedRows = Response.getParsedResponse().getResultRows();
			parsedRows.addAll(nextPageRows); 
			Response.getParsedResponse().setResultRows(parsedRows);
			if (logger.isDebugEnabled())
				logger.debug(" found rows = " + parsedRows.size());
	
			return true;
		}else{
			return false;
		}
	}


}
