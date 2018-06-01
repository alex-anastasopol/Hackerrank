package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.StringUtils;

public class COGrandTR extends COGenericTylerTechTR {

	private static final long	serialVersionUID	= 1303740694600928990L;

	public COGrandTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	@Override
	protected String clean(String result) {
		result = result.replaceAll("(?is)<tr[^>]*>(\\s*<td>\\s*</td>)+\\s*</tr>", "");
		result = result.replaceAll("(?is)<p[^>]*>The amounts of taxes due on this page are based on <strong>last year's</strong> property value assesments\\..*?</p>", "");
		return result;
	}
	
	
	@Override
	public void parseName(Set<String> hashSet, ResultMap resultMap) throws Exception {
		ro.cst.tsearch.servers.functions.COGrandTR.parseNames(resultMap, searchId);
	}
	
	@Override
	public void parseAddress(String address, ResultMap resultMap) throws Exception {
		ro.cst.tsearch.servers.functions.COGrandTR.parseAddress(resultMap, searchId);
	}
	
	@Override
	public void parseLegal(String contents, ResultMap resultMap) throws Exception {
		ro.cst.tsearch.servers.functions.COGrandTR.parseLegalSummary(resultMap, searchId);
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();		
		
		FilterResponse rejectNonRealEstateFilter = new RejectNonRealEstate(SearchAttributes.OWNER_OBJECT, searchId);
		rejectNonRealEstateFilter.setThreshold(new BigDecimal("0.65"));
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		FilterResponse legalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , null );
				
		// search by PIN
		String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
		
		if(!isEmpty(pin)){
			if(pin.matches("\\d+")) {
				pin = "R" + pin;
			}
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.forceValue(0, pin);
			modules.add(module);
		}
		
		boolean hasPin2 = !StringUtils.isEmpty(getSearchAttribute(SearchAttributes.LD_PARCELNO2));
		if(hasPin2){			
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(3).setSaKey(SearchAttributes.LD_PARCELNO2);
			modules.add(module);
		}
		
		if(hasPin()){			
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(3).setSaKey(SearchAttributes.LD_PARCELNO);
			modules.add(module);
		}
		
		// search by Address
		if(hasStreet()){
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(5).setSaKey(SearchAttributes.P_STREET_NO_NAME);
			module.addFilter(rejectNonRealEstateFilter);
			module.addFilter(addressFilter);
			module.addFilter(legalFilter);
			module.addFilter(nameFilterHybrid);
			modules.add(module);
		}
		
		// search by name - filter by address
		if(hasOwner()) {
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module);
			((GenericNameFilter) defaultNameFilter).setIgnoreMiddleOnEmpty(true);
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(rejectNonRealEstateFilter);
			module.addFilter(defaultNameFilter);
			module.addFilter(addressFilter);
			module.addFilter(legalFilter);
					
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, new String[] {"L F M;;", "L, F M;;", "L F;;", "L, F;;"});
			module.addIterator(nameIterator);
		
			modules.add(module);			
		}
		
		serverInfo.setModulesForAutoSearch(modules);
	}

}
