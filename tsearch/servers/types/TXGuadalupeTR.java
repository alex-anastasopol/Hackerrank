package ro.cst.tsearch.servers.types;

import java.util.ArrayList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.CityFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ServerResponse;

public class TXGuadalupeTR extends TXGenericSite{

	private static final long serialVersionUID = 679493777119984078L;

	public TXGuadalupeTR(long searchId) {
		super(searchId);
	}

	public TXGuadalupeTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	} 
	

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		map.put("OtherInformationSet.SrcType","TR");
		return super.parseAndFillResultMap(response,detailsHtml,map);
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		TSServerInfoModule module = null;
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , module );
		FilterResponse cityFilter = CityFilterFactory.getCityFilterDefault(searchId);
				
		SearchAttributes sa = getSearch().getSa();
		ArrayList<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		FilterResponse addressFilter 	= AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d );
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilterNoSinonims( 
				SearchAttributes.OWNER_OBJECT , searchId , module );
		String pin = sa.getAtribute(SearchAttributes.LD_PARCELNO).replaceAll("-", "");
		String pin2 = sa.getAtribute(SearchAttributes.LD_PARCELNO2);
		boolean emptyPid = ro.cst.tsearch.utils.StringUtils.isEmpty(pin);
		boolean emptyPid2 = ro.cst.tsearch.utils.StringUtils.isEmpty(pin2);
		if (!emptyPid) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.clearSaKeys();
			module.forceValue(0, pin);
			modules.add(module);
			sa.setAtribute(SearchAttributes.LD_PARCELNO, pin);
		}
		if (!emptyPid2) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.forceValue(0, pin2);
			modules.add(module);
		}
		
		if (hasStreet()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.addFilter(defaultNameFilter);
			module.forceValue(1, sa.getAtribute(SearchAttributes.P_STREETNAME));
			module.forceValue(0, sa.getAtribute(SearchAttributes.P_STREETNO));
			module.addFilter( cityFilter );
			module.addFilter( nameFilterHybrid );
			modules.add(module);
		}
		
		if (hasOwner()){
			//Search by Owner
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			module.setIteratorType(1,FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, new String[] {"L;F;", "L;f;"});
			module.addIterator(nameIterator);
			modules.add(module);
		}
		
		NameFilterFactory.getHybridNameFilter( 
				SearchAttributes.OWNER_OBJECT , searchId , module );
		
		serverInfo.setModulesForAutoSearch(modules);
	}
	
}
