package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.database.rowmapper.RequestCount;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.GenericMultipleAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.address.AddressI;
import com.stewart.ats.base.document.TransferI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;

/**
* @author Cristian Stochina
*/
@Deprecated
public class FLApnBasedDASLDT extends FLGenericDASLDT{

	private static final long serialVersionUID = 5763983322173597146L;

	public FLApnBasedDASLDT(long searchId) {
		super(searchId);
	}

	public FLApnBasedDASLDT(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		if(module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX){
			List<TSServerInfoModule> modules = getMultipleModules(module, sd);
			ServerResponse ret = null;
			
			if(!modules.isEmpty()) {
				ret = super.searchByMultipleInstrument(modules,sd, null);
			}else{
				ret = super.SearchBy(module, sd);
			} 
			return ret;
			
		} 
		
		//search plats 
		if ( TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX == module.getModuleIdx()  ) {
			CurrentInstance ci 	= InstanceManager.getManager().getCurrentInstance(mSearch.getID());
			Search search 		= ci.getCrtSearchContext();
			
			String platBook = getSearchAttribute(SearchAttributes.LD_BOOKNO_1);
			String platPage = getSearchAttribute(SearchAttributes.LD_PAGENO_1);
			
			if(isParentSite()){
				platBook = module.getFunction(0).getParamValue();
				platPage = module.getFunction(1).getParamValue();
			}
			else if(StringUtils.isEmpty(platBook)||StringUtils.isEmpty(platPage)){
				//take them from last transfer
				
				//TODO - take if from last transfer as document if ever needed
//				ParsedResponse lastTransferParsedResponse = search.getLastTransferAsParsedResponse(true, false);
//				if(lastTransferParsedResponse != null) {
//					PropertyIdentificationSet elem = lastTransferParsedResponse.getPropertyIdentificationSet(0);
//					platBook = elem.getAtribute("PlatBook");
//					platPage = elem.getAtribute("PlatNo");
//				}
			}
			
			if(StringUtils.isEmpty(platBook)||StringUtils.isEmpty(platPage)){
				return new ServerResponse();
			}
			platBook = platBook.replaceFirst("(?i)[a-z]", "");
			String county = ci.getCurrentCounty().getName();

			TSInterface server = TSServersFactory.GetServerInstance((int) TSServersFactory.getSiteId("FL", county, "RV"), "", "", searchId);
			TSServerInfoModule mod = server.getCurrentClassServerInfo().getModuleForSearch(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX, new SearchDataWrapper());
			mod.forceValue( 0, platBook );
			mod.forceValue( 1, platPage );
			mod.forceValue( 2, "PLAT");
			server.setServerForTsd(mSearch, msSiteRealPath);
			return server.SearchBy(mod, sd);
		}
		
		if( TSServerInfo.BOOK_AND_PAGE_MODULE_IDX == module.getModuleIdx()) {
			
			List<TSServerInfoModule> modules = getMultipleModules(module, sd);
			if(!modules.isEmpty()) {
				return super.searchByMultipleInstrument(modules,sd, null);
			}
		
		}
		
		if( TSServerInfo.INSTR_NO_MODULE_IDX == module.getModuleIdx()) {
			 
			List<TSServerInfoModule> modules = getMultipleModules(module, sd);
			if(!modules.isEmpty()) {
				return super.searchByMultipleInstrument(modules,sd, null);
			}
			
		}
		
		return super.SearchBy(module, sd);
	}	
	
/* Pretty prints a link that was already followed when creating TSRIndex
 * (non-Javadoc)
 * @see ro.cst.tsearch.servers.types.TSServer#getPrettyFollowedLink(java.lang.String)
 */	
    public String getPrettyFollowedLink (String initialFollowedLnk)
    {
    	if (initialFollowedLnk.matches("(?i).*DL[_]{2,}([0-9]+)[_]{1,}([0-9]+)[^a-z]*([a-z]+).*"))
    	{
/*"Book 13676 Page 1504 which is a Court doc type has already been saved from a
previous search in the log file."*/
    		String retStr = initialFollowedLnk.replaceFirst(
    				"(?i).*DL[_]{2,}([0-9]+)[_]{1,}([0-9]+)[^a-z]*([a-z]*)[^a-z]*.*", 
    				"Book " + "$1" + " Page " + "$2" + " " + "$3" + 
    				" has already been processed from a previous search in the log file. ");
    		retStr =  "<br/><span class='followed'>"+retStr+"</span><br/>";
    		
    		return retStr;
    	}
    	
    	return "<br/><span class='followed'>Link already followed</span>:" + initialFollowedLnk + "<br/>";
    }
	
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
	    super.setModulesForGoBackOneLevelSearch(serverInfo);
		
	}

	private  String prepareAPN( String county){
		String apn = getSearchAttribute( SearchAttributes.LD_PARCELNONDB ).replaceAll("[.-]", "");
		if(StringUtils.isEmpty(apn)){
			apn = getSearchAttribute( SearchAttributes.LD_PARCELNO ).replaceAll("[.-]", "");
		}
		if("broward".equalsIgnoreCase(county.toLowerCase())){ //6-2-4
			apn = getSearchAttribute( SearchAttributes.LD_PARCELNO ).replaceAll("[.-]", "");
			if(apn.length()>=12){
				apn = apn.substring(0,5)+"-"+apn.substring(5,7)+"-"+apn.substring(7);
			}
		}
		return apn;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		String apn = prepareAPN( InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName());
		boolean isUpdateOrDateDown = (isUpdate()) || global.getSa().isDateDown();
		
		GenericNameFilter nameFilterOwner 	= (GenericNameFilter)NameFilterFactory.getDefaultNameFilter( SearchAttributes.OWNER_OBJECT, global.getID(), null );
		nameFilterOwner.setInitAgain(true);
		FilterResponse nameFilterBuyer 	= NameFilterFactory.getDefaultNameFilter( SearchAttributes.BUYER_OBJECT, global.getID(), null );
		((GenericNameFilter)nameFilterBuyer).setIgnoreMiddleOnEmpty(true);
		((GenericNameFilter)nameFilterBuyer).setIgnoreEmptyMiddleOnCandidat(false);
		FilterResponse legalFilter 		= LegalFilterFactory.getDefaultLegalFilter( searchId );
		//FilterResponse addressFilter 	= AddressFilterFactory.getAddressHighPassFilter( searchId, 0.8d );
		
		GenericMultipleAddressFilter addressFilter 	= new GenericMultipleAddressFilter(searchId);
		
		for (AddressI address : getSearchAttributes().getForUpdateSearchAddressesNotNull(getServerID())) {
			if(StringUtils.isNotEmpty(address.getStreetName())) {
				addressFilter.addNewFilterFromAddress(address);
			}
		}
		
		if ( !StringUtils.isEmpty(apn) ) {// APN search
			TSServerInfoModule module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX) );
			module.clearSaKeys();
			module.setData( 0, apn );
			if (isUpdateOrDateDown) {
				module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId));
			}
			modules.add(module);
		}
		
		
		//search by owner
		nameFilterOwner.setIgnoreMiddleOnEmpty(true);
		FilterResponse[] filtersO 	= { nameFilterOwner, legalFilter, addressFilter,  new LastTransferDateFilter( searchId ) };
		ArrayList<NameI> searchedNames = addNameSearch(  modules, serverInfo, SearchAttributes.OWNER_OBJECT, null, Arrays.asList(filtersO)  );
		
		//OCR searcher and boostraper
		addOCRSearch( modules, serverInfo, legalFilter );
		
		//plat search
		TSServerInfoModule module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
		module.clearSaKeys();
		module.setData(0,"B");
		module.setData(1,"P");
		module.setData(2,"PLAT");
		if(!isUpdateOrDateDown) {
			modules.add(module);
		}		
		
		//name search with the new names gathered
		addNameSearch( modules, serverInfo,SearchAttributes.OWNER_OBJECT, searchedNames==null?new ArrayList<NameI>():searchedNames, Arrays.asList(filtersO) );
		
		//FilterResponse[] filtersB 	= {nameFilterBuyer, DoctypeFilterFactory.getDoctypeBuyerFilter( searchId )};
		
		//search by buyer
		//addNameSearch(modules, serverInfo, SearchAttributes.BUYER_OBJECT, searchedNames, Arrays.asList(filtersB) );
		
		serverInfo.setModulesForAutoSearch( modules );
	}

	@Override
	protected Object getRequestCountType(int moduleIDX) {
		switch (moduleIDX) {
		case TSServerInfo.PARCEL_ID_MODULE_IDX:
			return RequestCount.TYPE_PIN_COUNT;
		case TSServerInfo.SUBDIVISION_MODULE_IDX:
		case TSServerInfo.SECTION_LAND_MODULE_IDX:
		case TSServerInfo.ARB_MODULE_IDX:
			return RequestCount.TYPE_LEGAL_COUNT;
		case TSServerInfo.ADDRESS_MODULE_IDX:
			return RequestCount.TYPE_ADDR_COUNT;
		case TSServerInfo.GENERIC_MODULE_IDX:
		case TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX:
		case TSServerInfo.RELATED_MODULE_IDX:
			return RequestCount.TYPE_MISC_COUNT;
		case TSServerInfo.NAME_MODULE_IDX:
			return RequestCount.TYPE_NAME_COUNT;
		case TSServerInfo.IMG_MODULE_IDX:
			return RequestCount.TYPE_IMAGE_COUNT;
		case TSServerInfo.BOOK_AND_PAGE_MODULE_IDX:
		case TSServerInfo.INSTR_NO_MODULE_IDX:
			return RequestCount.TYPE_INSTRUMENT_COUNT;
		}
		
		try{
			throw new Exception("Bad module Id for counting request on " + getDataSite().getSTCounty());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
}
