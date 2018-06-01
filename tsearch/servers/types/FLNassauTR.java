package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;

public class FLNassauTR extends FLGenericGovernmaxTR {
	
	private static final long serialVersionUID = 6525099447955957961L;

	private static final CheckTangible CHECK_TANGIBLE = new CheckTangible() {
		public boolean isTangible(String row){			
			String linkText = getLinkText(row);
			return linkText.matches("\\d{8}\\s.+");
		}
	};
	
	public FLNassauTR(String rsRequestSolverName, String rsSitePath, String rsServerID,
			String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		checkTangible = CHECK_TANGIBLE;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
		
		FilterResponse realEstateFilter = new RejectNonRealEstate(SearchAttributes.OWNER_OBJECT, searchId);
		realEstateFilter.setThreshold(new BigDecimal("0.65"));
		
		FilterResponse addressFilter 	= AddressFilterFactory.getAddressHighPassFilter( searchId , 0.8d );
		
		FilterResponse legalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);
		
		// P1 : search by PIN	
		if(hasPin()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO);  
			module.addFilter(realEstateFilter);
			modules.add(module);		
		}
		
		// P2 : search by Address				
		if(hasAddress()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).setSaKey(SearchAttributes.P_STREET_FULL_NAME_EX);  
			module.addFilter(realEstateFilter);
			module.addFilter(addressFilter);
			if(hasLegal()){
				module.addFilter(legalFilter);
			}
			modules.add(module);		
		}
		
		// P3 : search by Owner Name				
		if(hasName()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
						
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(realEstateFilter);
			
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(
					SearchAttributes.OWNER_OBJECT, searchId, module));
			
			if(hasLegal()){
				module.addFilter(LegalFilterFactory.getDefaultLegalFilter(searchId));
			}
			module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
			.getConfigurableNameIterator(module, searchId, new String[] {"L;F;M","L;F;"});
			
			module.addIterator(nameIterator);
			
			modules.add(module);		
		}
		
		serverInfo.setModulesForAutoSearch(modules);
	}
}
