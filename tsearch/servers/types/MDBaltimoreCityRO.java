package ro.cst.tsearch.servers.types;

import java.util.List;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFromDocumentFilterForNext;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;

/**
 *Baltimore City
 */

public class MDBaltimoreCityRO extends MDGenericRO {
	
	private static final long serialVersionUID = 2237912902609719752L;

	public MDBaltimoreCityRO(long searchId) {
		super(searchId);
	}

	public MDBaltimoreCityRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	@Override
	protected void addAddressSearch(
			List<TSServerInfoModule> modules, 
			TSServerInfo serverInfo, 
			FilterResponse[] filters,
			DocsValidator[] docsValidators,
			DocsValidator[] docsValidatorsCrossref) {
		
		TSServerInfoModule module = null;
		AddressFromDocumentFilterForNext addressFilterForNext = new AddressFromDocumentFilterForNext(
    		getSearch().getSa().getAtribute(SearchAttributes.P_STREET_NO_NAME),searchId);
		
		// search with address
		if (hasStreet() && hasStreetNo()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_DEFAULT_NOT_EMPTY);
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_DEFAULT_NOT_EMPTY);
			module.addFilterForNext(addressFilterForNext);
			
			if(filters != null) {
				for (FilterResponse filterResponse : filters) {
					module.addFilter(filterResponse);
				}
			}
			addFilterForUpdate(module, true);
			if(docsValidators != null) {
				for (DocsValidator docsValidator : docsValidators) {
					module.addValidator(docsValidator);
				}
			}
			if(docsValidatorsCrossref != null) {
				for (DocsValidator docsValidator : docsValidatorsCrossref) {
					module.addCrossRefValidator(docsValidator);
				}
			}
			module.addCrossRefValidator(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module).getValidator());
			
			modules.add(module);
		}
		
	}

}
