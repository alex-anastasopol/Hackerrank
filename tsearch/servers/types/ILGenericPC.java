package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.List;

import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.name.NameSourceType;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;

public class ILGenericPC extends GenericPC {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ILGenericPC(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
        resultType = MULTIPLE_RESULT_TYPE;
	}
	
	@Override
	public void addAutomaticModule(TSServerInfo serverInfo,
			List<TSServerInfoModule> modules,
			int moduleIndex, List<String> regions) {
		if(regions.size() > 0) {
			ArrayList<NameI> searchedNames = null;
		    for(String key: new String[] {/*SearchAttributes.OWNER_OBJECT,*/ SearchAttributes.BUYER_OBJECT}){
		        TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule( moduleIndex ));		
		        module.clearSaKeys();
		        module.setSaObjKey(key);
		        
		        
		        module.setSaKey(3, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		        module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
		        module.setSaKey(4, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
		        module.setIteratorType(7, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);

		        ConfigurableNameIterator iterator = new ConfigurableNameIterator(searchId, false, new String[]{"L;F;"});
		        iterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
		        
		        iterator.setInitAgain(true);
		        module.addIterator(iterator);
		        
		        if(searchedNames == null) {
		        	searchedNames = iterator.getSearchedNames();
		        } else {
		        	iterator.setSearchedNames(searchedNames);
		        }
		        
		        GenericNameFilter nameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(key, searchId, module);
		        nameFilter.setUseSynonymsForCandidates(true);
		        nameFilter.setIgnoreMiddleOnEmpty(true);
		        module.addFilter(nameFilter);
		        
		        if(moduleIndex == MODULE_IDX_BANKRUPTCY || moduleIndex == MODULE_IDX_CIVIL) {
		        	
		        	module.forceValue(0, regions.get(0));
		        	for (int i = 1; i < regions.size(); i++) {
						int newFctId = module.addFunction();
						TSServerInfoFunction newFct = module.getFunction(newFctId);
						newFct.setParamName(module.getFunction(0).getParamName());
						newFct.setDefaultValue(regions.get(i));
						newFct.setHiden(true);
					}
		        }
		        modules.add(module);
		    }
		}
	}
	
}
