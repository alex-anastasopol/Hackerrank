package ro.cst.tsearch.search;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.exceptions.NotImplFeatureException;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.TSInterface;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringCleaner;

import com.stewart.ats.base.document.DocumentI.SearchType;
/**
 * @author elmarie
 */
public class ServerSearchesIterator implements StatesIterator, Serializable
{
	static final long serialVersionUID = 10000000;
	protected static final Category logger =
		Logger.getLogger(ServerSearchesIterator.class);
	protected ResponseManager rm;
	protected FilterResponse filter;
	private List<TSServerInfoModule> modulesList;
	private int errorCount = 0;
	private LinkedHashSet<String> errorListOfSearchTypes;
	private int crtIdx = -1;
	private ModuleStatesIterator currentIterator = null;
	private TSInterface intrfServer;
	private DocsValidator defaultServerDocsValidator;
	
	private DocsValidator defaultCrossRefServerDocsValidator = null;
	
	private List<TSServerInfoModule> defaultFilterModules;
	public static final int AUTO_MODULES = 1;
	public static final int GO_BACK_ONE_LEVEL_MODULES = 2;
	
	private ServerSearchesIterator(TSInterface intrfServer, int modulesType,long searchId) {
		setResponseManager(ResponseManager.getInstance());
		this.intrfServer = intrfServer;
		this.defaultServerDocsValidator = intrfServer.getDocsValidator();
		this.defaultCrossRefServerDocsValidator = intrfServer.getCrossRefDocsValidator();
		TSServerInfo serverInfo = intrfServer.getCurrentClassServerInfo();
		
		filter = FilterResponse.getInstance(serverInfo.getFilterType(), null, StringCleaner.NO_CLEAN,searchId);
		if (modulesType == GO_BACK_ONE_LEVEL_MODULES) {
			modulesList = serverInfo.getModulesForGoBackOneLevelSearch();
		} else {
			modulesList = serverInfo.getModulesForAutoSearch();
		}
		defaultFilterModules = serverInfo.getModules(TSServerInfoModule.idFilterModule);
		
		reset(searchId);
	}

	protected void setResponseManager(ResponseManager manager) {
		this.rm = manager;
	}

	public ServerResponse getLastGoodResponse() {
		return rm.getLastGoodResponse();
	}
	
	public void manageResponse(ServerResponse sr, TSServerInfoModule searchedModule, long searchId)
		throws ServerResponseException
	{
		if (currentIterator != null)
		{
			currentIterator.filterResponse(sr, intrfServer);
		}
		filter.filterResponse(sr);
		rm.manageResponse(sr);
		if(sr.isError() && getErrorCount() < modulesList.size() && sr.getErrorCode() != ServerResponse.NOT_PERFECT_MATCH_WARNING_FIRST ) {	//if we had an error we increment the counter 
				//but not more than the max errors allowed (defensive)  
			addError(searchedModule);
			StringBuilder errorMessage = new StringBuilder("<div>");
			errorMessage.append("WARNING: Error appeared when searching with module <span class='searchName'>")
				.append(searchedModule.getLabel())
				.append("</span>");
			Object info = searchedModule.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION);
	        if(info!=null){
	        	errorMessage.append(" - " + info + "<br>");
	        }
	        errorMessage.append("</div><hr>");
			SearchLogger.info(errorMessage.toString(), searchId);
		} else if(sr.isError()) {
			//SearchLogger.info("Entered Manage Response But the errot is not counted ", searchId);
		}
		
		// if it was never filtered, it means we need to display it
		if(!sr.isFiltered()){
			if(sr.getParsedResponse().getResultRows().size() != 0 ){
				FilterResponse.logRows(sr.getParsedResponse().getResultRows(), searchId);
			}
		}
	}

	public void addError(TSServerInfoModule searchedModule) {
		errorCount++;
		if(searchedModule != null) {
			SearchType searchType = searchedModule.getSearchTypeCompleteInAutomatic();
			if(searchType != null) {
				//please remember to change the searchType also in 
				//com.stewart.ats.base.document.Document.getSimpleChapterWithoutReferences()
				getErrorListOfSearchTypes().add(searchType.toString().toLowerCase());
			}
		}
	}
	public void resetPreviousResponse()
	{
		rm.resetPreviousResponse();
	}
	public static ServerSearchesIterator getInstance(TSInterface intrfServer, int moduleTypes,long searchId)
		throws NotImplFeatureException
	{
		int serverId = intrfServer.getServerID();
		
		
		try {
			DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID( 
	    			InstanceManager.getManager().getCommunityId(searchId), 
	    			serverId );
			if(dataSite != null) {
				return new ServerSearchesIterator(intrfServer, moduleTypes,searchId);
			} else {
				throw new NotImplFeatureException(
						"Automatic search is not implemented for this server."
							+ " Please go to Parent Site to perform search on this server.");
			}
		
		} catch (Exception e) {
			logger.error("Error while getting instance for ServerSearchesIterator", e);
			throw new NotImplFeatureException(
					"Automatic search is not implemented for this server."
						+ " Please go to Parent Site to perform search on this server.");
		}
		

		
	}
	public boolean mustStopToShowPartialResults(long searchId)
	{
		if (currentIterator == null)
		{
			return true;
		}
		if (!currentIterator.hasNext(searchId)
			&& (currentIterator.getInitial().isStopAfterModule()))
		{
			//logger.debug("mi="+mi.toString());
			return true;
		}
		else
		{
			return false;
		}
	}
	public void reset(long searchId)
	{
		if (modulesList.size() > 0)
		{
			advanceToIterator(0,searchId);
		}
		else
		{
			advanceToIterator(-1,searchId);
		}
	}
	public boolean hasNext( long searchId)
	{
		if ( ((TSServer)intrfServer).skipCurrentSite() || ((TSServer)intrfServer).isStopAutomaticSearch() )
            return false;
		
		//logger.debug( "Has Next ( ) for " + this );
		if ((crtIdx < 0)
			|| (crtIdx >= modulesList.size())
			|| (currentIterator == null))
		{
			return false;
		}
		if (currentIterator.hasNext(searchId))
		{
			return true;
		}
		else
		{
			advanceToIterator(crtIdx + 1, searchId);
			return hasNext(searchId);
		}
	}
	private void advanceToIterator(int idx,long searchId)
	
	{   Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		if(search !=null){
			search .clearRecursiveAnalisedLinks();
			search .clearVisitedLinks();
		}
		TSServerInfo serverInfo = intrfServer.getCurrentClassServerInfo();
		serverInfo.replaceModulesFromList(defaultFilterModules);
		crtIdx = idx;
		if ((idx < 0) || idx >= modulesList.size())
		{
			currentIterator = null;
			return;
		}
		else
		{
			TSServerInfoModule moduleForAutoSearch =
				(TSServerInfoModule)modulesList.get(crtIdx);
			TSServerInfoModule module = moduleForAutoSearch;
			module.setTSInterface( intrfServer );
			if (module.getModuleStatesItList()!=null&&module.getModuleStatesItList().size()>0){
			   currentIterator=module.getModuleStatesItList().get(0);
			   if(currentIterator.needInitAgain()){
				   currentIterator.init(moduleForAutoSearch);
				   currentIterator.setInitAgain(false);
			   }
			}else{
				currentIterator = ModuleStatesIterator.getInstance(module,searchId);
			}
	
			//------------
			intrfServer.clearDocsValidators();
			for(DocsValidator val:module.getValidatorList()){
				intrfServer.addDocsValidator(val,defaultServerDocsValidator);
			}
			
			//------------
			intrfServer.clearCrossRefDocsValidators();
			for(DocsValidator val:module.getCrossRefValidatorList()){
				intrfServer.addCrossRefDocsValidator(val,defaultServerDocsValidator); 
			}
			intrfServer.setIteratorType( module.getIteratorType() );
			
		}
	}
	public void goToNext()
	{
		if (currentIterator != null)
		{
			currentIterator.goToNext();
		}
	}
	public Object current()
	{
		if (currentIterator != null)
		{
			return currentIterator.current();
		}
		else
		{
			return null;
		}
	}
	/**
	 * Returns the initial module which generates all iterations<br>
	 * Please be careful because no
	 * @return
	 */
	public TSServerInfoModule getInitial()
	{
		if (currentIterator != null)
		{
			return currentIterator.getInitial();
		}
		else
		{
			return null;
		}
	}
	public boolean timeToStop(Decision d,long searchId)
	{
		if ((crtIdx < 0)
			|| (crtIdx >= modulesList.size())
			|| (currentIterator == null))
		{
			return true;
		}
		ModuleStatesIterator msi = (ModuleStatesIterator)currentIterator;
		if (msi.timeToStop(d))
		{
			return true;
		}
		else
		{
			if (msi.hasNext(searchId))
			{
				return false;
			}
			else
			{
				if (getLastGoodResponse().getParsedResponse().isNone())
				{
					return false;
				}
				else
				{
					return true;
				}
			}
		}
	}

	public TSInterface getIntrfServer() {
		return intrfServer;
	}

	public int getErrorCount() {
		return errorCount;
	}

	public void setErrorCount(int errorCount) {
		if (errorCount > modulesList.size())
			errorCount = modulesList.size();
		this.errorCount = errorCount;
	}

	public void setMaxErrorCount() {
		errorCount = modulesList.size();
	}

	public String getStatus() {
		if (modulesList.isEmpty() || getErrorCount() == 0)
			return "done";
		if (modulesList.size() == getErrorCount())
			return "not responding";
		StringBuilder sb = new StringBuilder();
		for (String searchType : getErrorListOfSearchTypes()) {
			if(sb.length() == 0) {
				sb.append(searchType);
			} else {
				sb.append(", ").append(searchType);
			}
		}
		
		if(sb.length() == 0) {
			sb.append("site warning");
		} else {
			sb.insert(0, "site warning [").append("]");
		}
		
		return sb.toString();
	}

	public LinkedHashSet<String> getErrorListOfSearchTypes() {
		if(errorListOfSearchTypes == null) {
			errorListOfSearchTypes = new LinkedHashSet<String>();
		}
		return errorListOfSearchTypes;
	}

	public void setErrorListOfSearchTypes(LinkedHashSet<String> errorListOfSearchTypes) {
		this.errorListOfSearchTypes = errorListOfSearchTypes;
	}
	
	@Override
	public Object peekAtNext() {
		return null;
	}
	
}
