package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ServerResponse;

public class FLSarasotaTR extends FLGenericGovernmaxTR {
	
	private static final long serialVersionUID = 6525099447955957961L;

	private static final CheckTangible CHECK_TANGIBLE = new CheckTangible() {
		public boolean isTangible(String row){
			String linkText = getLinkText(row);
			return !linkText.matches("^\\d{10} .+");
		}
	};
	
	public FLSarasotaTR(String rsRequestSolverName, String rsSitePath, String rsServerID,
			String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		checkTangible = CHECK_TANGIBLE;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		TSServerInfoModule module;
		
		FilterResponse rnre = new RejectNonRealEstate(searchId);
		rnre.setThreshold(new BigDecimal("0.65"));
		GenericAddressFilter addressFilter 	= AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.8d );
		
		// P1 : search by PIN	
		if(hasPin()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO_GENERIC_TR);  
			module.addFilter(rnre);
			modules.add(module);		
		}
		
		// P1bis : search by old PIN	
		if(hasPin()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO);
			module.addFilter(rnre);
			modules.add(module);		
		}
		
		// P2 : search by Address	
		if(hasAddress()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).setSaKey(SearchAttributes.P_STREET_FULL_NAME_EX);  
			module.addFilter(rnre);	
			module.addFilter(addressFilter);
			if(hasLegal()){
				module.addFilter(LegalFilterFactory.getDefaultLegalFilter(searchId));
			}
			modules.add(module);		
		}
		
		// P3 : search by Owner Name	
		if(hasName()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			
			FilterResponse fr = new RejectNonRealEstate(SearchAttributes.OWNER_OBJECT, searchId);
			fr.setThreshold(new BigDecimal("0.65"));
			
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(fr);
			
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
	
	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
	
		// add dashes to pin if necessary 
        if(module.getModuleIdx() == TSServerInfo.ADV_SEARCH_MODULE_IDX){
        	// turn 043721000 into 04-3721-000        	
        	String pin = module.getFunction(0).getParamValue();
        	pin = pin.replaceFirst("\\s*(\\d{4})-?(\\d{2})-?(\\d{4})\\s*", "$1-$2-$3");
           	module.getFunction(0).setParamValue(pin);
          
        }
        return super.SearchBy(module, sd);
	}
	
	@Override
	protected String getKeyNumber(String page){
		String info = super.getKeyNumber(page);
		info = info.replace("-", "");
		
		return info;
	}

	@Override
	protected String getIntermResults(String response, String linkStart) {
		String intermediaryHtml = super.getIntermResults(response, linkStart);
		
		intermediaryHtml = intermediaryHtml.replaceAll("(?is)<td\\s+colspan=\"?4\"?(.*?</td>)\\s*<td.*?</td>", "<td colspan=\"5\"$1");
		
		return intermediaryHtml;
	}

	@Override
	protected String cleanup(String page) {
		String result = super.cleanup(page);
		
		result = result.replaceAll("(?is)<a\\s+.*?tab_collect_mvp_registerEbills.*?</a>", "");
		
		return result;
	}
	
	
}
