package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.List;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsForUpdateFilter;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.name.NameI;

public class COHuerfanoTS extends COGenericDASLTS {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public COHuerfanoTS(long searchId) {
		super(searchId);
	}

	public COHuerfanoTS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	protected ArrayList<NameI>  addNameSearch( List<TSServerInfoModule> modules, TSServerInfo serverInfo,String key, ArrayList<NameI> searchedNames, FilterResponse ...filters ) {
		Search global 		= getSearch();
		SearchAttributes sa = global.getSa();
		String fromDateStr 	= sa.getFromDateString("MM/dd/yyyy");
		ConfigurableNameIterator nameIterator = null;
		
		TSServerInfoModule module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX) );
		module.clearSaKeys();
		module.setSaObjKey(key);

		for (int i = 0; i < filters.length; i++) {
			module.addFilter(filters[i]);
		}

		if ( !StringUtils.isEmpty( fromDateStr ) ) {
			module.setData( 0, fromDateStr );
		}
		module.setIteratorType( 2,  FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
		module.setIteratorType( 4,  FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
		nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;", "L F;;" });
		nameIterator.setAllowMcnPersons( true );
		
		if ( searchedNames!=null ) {
			nameIterator.setInitAgain( true );
			nameIterator.setSearchedNames( searchedNames );
		}
		
		searchedNames = nameIterator.getSearchedNames() ;
		module.addIterator( nameIterator );
		if(isUpdate()){
			module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId));
			module.addFilter(new RejectAlreadySavedDocumentsForUpdateFilter(searchId));
		}
		modules.add( module );
		return searchedNames;
	}
	
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
		
		ConfigurableNameIterator nameIterator = null;
		Search search = getSearch();
		int searchType = search.getSearchType();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		if(searchType == Search.GO_BACK_ONE_LEVEL_SEARCH) {
			SearchAttributes sa = search.getSa();	
			
		    TSServerInfoModule module;	
		    GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);
	
		    boolean useNameForLookUp = false;
			if(!addAoLookUpSearches(serverInfo, new ArrayList<TSServerInfoModule>(), getAllAoReferences(search), searchId, true)
					&& !(StringUtils.isEmpty(sa.getAtribute(SearchAttributes.LD_LOTNO)) && StringUtils.isEmpty(sa.getAtribute(SearchAttributes.LD_SUBDIV_BLOCK))) ){
				useNameForLookUp = true;
			}
			LegalDescriptionIterator it = new LegalDescriptionIterator(searchId, useNameForLookUp, false, getDataSite());
			it.setEnableTownshipLegal(false);
			it.createDerrivations();	//i do this to load "TS_LOOK_UP_DATA" for legal filtering.
			
			FilterResponse doctypeFilter = DoctypeFilterFactory.getDoctypeBuyerFilter(searchId);
	
		    for (String id : gbm.getGbTransfers()) {
				  		   	    	 
		    	module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX) );
		    	module.setIndexInGB(id);
		    	module.setTypeSearchGB("grantor");
		    	module.clearSaKeys();
		    	module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
		    	String date=gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
		    	if (date!=null) 
		    		module.getFunction(0).forceValue(date);
			    module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				module.addFilter( NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
				module.addFilter( doctypeFilter );
		    	module.addValidator( DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator() );
				module.setIteratorType( 2,  FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
				module.setIteratorType( 4,  FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
		    	nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;", "L F;;" } );
			 	module.addIterator(nameIterator);
			 	modules.add(module);
			    
			     
			 	if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
			 		module =new TSServerInfoModule( serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX) );
			 		module.setIndexInGB(id);
			 		module.setTypeSearchGB("grantee");
			 		module.clearSaKeys();
			 		module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
			 		date=gbm.getDateForSearchBrokenChain(id, "MM/dd/yyyy", searchId);
			 		if (date!=null) 
			 			module.getFunction(0).forceValue(date);
			 		module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
			 		module.addFilter( NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
			 		module.addFilter( doctypeFilter );
			 		module.addValidator( DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator() );
			 		module.setIteratorType( 2,  FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
			 		module.setIteratorType( 4,  FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
			 		nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;", "L F;;" } );
			 		module.addIterator(nameIterator);			
			 		modules.add(module);
			 	}
		    }	 
		}
	    serverInfo.setModulesForGoBackOneLevelSearch(modules);
		
	}
}
