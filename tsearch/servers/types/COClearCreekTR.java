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

public class COClearCreekTR extends COGenericTylerTechTR {

	private static final long serialVersionUID = 1L;

	public COClearCreekTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
	    
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
	    TSServerInfoModule m;
	                         	
		FilterResponse addressFilter 	= AddressFilterFactory.getAddressHybridFilter( searchId , 0.7d );
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , null );
		FilterResponse defaultLegalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);
		FilterResponse rejectNonRealEstateFilter = new RejectNonRealEstate(SearchAttributes.OWNER_OBJECT, searchId);
		rejectNonRealEstateFilter.setThreshold(new BigDecimal("0.65"));
		
		String parcelID = getSearchAttribute(SearchAttributes.LD_PARCELNO);
		if (parcelID.contains("-")) {
			parcelID = parcelID.replaceAll("-", "");
		}
		
		if(hasPin()){			
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.getFunction(3).setSaKey(SearchAttributes.LD_PARCELNO2);
			l.add(m);
		}
		
		if(hasPin()){			
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.forceValue(3, parcelID);
			l.add(m);
		}
		
		if(hasPin()){			
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.forceValue(0, parcelID);
			l.add(m);
		}
		
		if(hasStreet()){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.getFunction(5).setSaKey( SearchAttributes.P_STREETNO );
			m.getFunction(7).setSaKey( SearchAttributes.P_STREETNAME );
			m.addFilter(rejectNonRealEstateFilter);
			m.addFilter(nameFilterHybrid);
			m.addFilter(addressFilter);
			m.addFilter(defaultLegalFilter);
			l.add(m);
		}
		
		if( hasOwner() )
	    {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);			
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			m.addFilter(NameFilterFactory.getDefaultNameFilter( SearchAttributes.OWNER_OBJECT, searchId, m));
			m.addFilter(rejectNonRealEstateFilter);
			m.addFilter(addressFilter);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[] {"L;F;M","L;F;"});
			m.addIterator(nameIterator);
			l.add(m);
	    }
	
		serverInfo.setModulesForAutoSearch(l);
	
	  }
	
}
