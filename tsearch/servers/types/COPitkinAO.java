package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;


public class COPitkinAO extends COGenericGoodTurnsTR {
	
	private static final long serialVersionUID = 1L;
	
	public COPitkinAO(long searchId) 
	{
		super(searchId);
	}
	
	public COPitkinAO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) 
	{
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		
		// search by PIN
		String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
		
		if(!isEmpty(pin)){
			if (pin.matches("[A-Z]\\d+")){
				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PROP_NO_IDX));			
				module.clearSaKeys();
				module.getFunction(0).forceValue(pin);  
				modules.add(module);
			} else {
				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));			
				module.clearSaKeys();
				module.getFunction(0).forceValue(pin);
				
				modules.add(module);
			}
		}
		
		// search by Address
		String strNo = getSearchAttribute(SearchAttributes.P_STREETNO);
		String strName = getSearchAttribute(SearchAttributes.P_STREETNAME);
		String city = getSearchAttribute(SearchAttributes.P_CITY);
		boolean hasAddress = !isEmpty(strNo) && !isEmpty(strName);
		if(hasAddress){
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(strName);  
			module.getFunction(1).forceValue(strNo);
			if(!isEmpty(city)) {
				module.getFunction(2).forceValue(city);
			}
			module.getFunction(3).forceValue("500");
			module.addFilter(addressFilter);
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module));
			
			modules.add(module);			
		}
		
		// search by name - filter by address
		if(hasOwner()) {
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.getFunction(1).forceValue("500");
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module));
			module.addFilter(addressFilter);
			
			module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, new String[] {"L F M;;","L F;;","L f;;"});
			module.addIterator(nameIterator);
			
			modules.add(module);			
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
	
	@Override
	protected void putData(HashMap<String, String> data) {
		data.put("type", "ASSESSOR");
		data.put("dataSource", "AO");
	}
	
}
