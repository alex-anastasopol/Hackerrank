package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.newfilters.pin.PINRejectALL;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.name.NameI;

/**
 * @author Cristian Stochina
 **/
public class CAOrangeDT extends CAGenericDASLDT {
	
	private static final long serialVersionUID = -2033410343191757464L;

	public CAOrangeDT(long searchId) {
		super(searchId);
	}

	public CAOrangeDT(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	public TSServerInfo getDefaultServerInfo() {
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
		
		if ( !StringUtils.isEmpty(pin) ) {
			int pinSize = pin.length();
			if ( pinSize >= 8 && !pin.contains("-") ) {
				pin = pin.substring(0,8);
				
				String pin1 = "";
				String pin2 = "";
				String pin3 = "";	

				if(pin.startsWith("93")){
					pin1 = pin.substring(0, 3); // split in 3 - 2 - 3 format
					pin2 = pin.substring(3, 5);
					pin3 = pin.substring(5, 8);	
					
				}
				else{
					pin1 = pin.substring(0, 3); // split in 3 - 3 - 2 format
					pin2 = pin.substring(3, 6);
					pin3 = pin.substring(6, 8);	
				}					
				sa.setAtribute(SearchAttributes.LD_PARCELNO2, pin1 + "-" + pin2+"-" + pin3 );
			}
		}
		return  super.getDefaultServerInfo();
		
	}
	
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
		Search search = currentInstance.getCrtSearchContext();
		if(!this.isParentSite() && module.getModuleIdx()==TSServerInfo.PARCEL_ID_MODULE_IDX
			/* locate property search can be address or name search*/){
			DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(
					currentInstance.getCurrentCommunity().getID().intValue(), 
					miServerID);
			String siteName =  dat.getName();
			Boolean makeAddressSearch = (Boolean)search.getAdditionalInfo(search.getID() + siteName + miServerID + ":" + "MAKE_OTHER_PIN_SEARCH");
			if(makeAddressSearch == null) {
				makeAddressSearch = true;
			}
			if(!makeAddressSearch){
				return new ServerResponse();
			}
		}
		return super.SearchBy(module, sd);
	}
	
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		SearchAttributes sa = global.getSa() ;
		String fromDateStr = sa.getFromDateString("MM/dd/yyyy");
		String asseorBook  =  null;
		String assesorPage =  null;
		
		PINRejectALL pinFilter = new PINRejectALL(SearchAttributes.LD_PARCELNO2, searchId, miServerID);
		pinFilter.setIgNoreZeroes(true);
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		// P1 : search by PID search
		String pin = getSearchAttribute( SearchAttributes.LD_PARCELNO2 );
		if ( !StringUtils.isEmpty(pin) ) {
			/*	module = new TSServerInfoModule( serverInfo.getModule( TSServerInfo.PARCEL_ID_MODULE_IDX ) );
				module.clearSaKeys();
				module.setData( 0, pin );
				if(isUpdate){
					module.setData( 3, fromDateStr );
				}
				else module.forceValue( 3, "" );
				module.addFilter( pinFilter );
				modules.add(module);
				*/
				pin = pin.replaceAll("-", "");
				if ( pin.length() >= 8 && !pin.contains("-") ) {
					module = new TSServerInfoModule( serverInfo.getModule( TSServerInfo.PARCEL_ID_MODULE_IDX ) );
					module.clearSaKeys();
					String pin1 = "";
					String pin2 = "";
					String pin3 = "";	

					if(pin.startsWith("93")){
						pin1 = pin.substring(0, 3); // split in 3 - 2 - 3 format
						pin2 = pin.substring(3, 5);
						pin3 = pin.substring(5, 8);	
						
					}
					else{
						pin1 = pin.substring(0, 3); // split in 3 - 3 - 2 format
						pin2 = pin.substring(3, 6);
						pin3 = pin.substring(6, 8);	
					}					
					module.setData( 0, pin1+"-"+pin2+"-"+pin3 );
					if(isUpdate()){
						module.setData( 3, fromDateStr );
					}
					else module.forceValue( 3, "" );
					module.addFilter( pinFilter );
					modules.add(module);
					
					
					module = new TSServerInfoModule( serverInfo.getModule( TSServerInfo.PARCEL_ID_MODULE_IDX ) );
					module.clearSaKeys();
					if(!pin.startsWith("93")){
						pin1 = pin.substring(0, 3); // split in 3 - 2 - 3 format
						pin2 = pin.substring(3, 5);
						pin3 = pin.substring(5, 8);	
					}		
					else{
						pin1 = pin.substring(0, 3); // split in 3 - 3 - 2 format
						pin2 = pin.substring(3, 6);
						pin3 = pin.substring(6, 8);	
					}					

					module.setData( 0, pin1+"-"+pin2+"-"+pin3 );
					if(isUpdate()){
						module.setData( 3, fromDateStr );
					}
					else module.forceValue( 3, "" );
					module.addFilter( pinFilter );
					modules.add(module);
				}
		}
		
		ArrayList<NameI> searchedNames = addOwnerNameSearch( modules, serverInfo, null, false );
			
		pin = getSearchAttribute( SearchAttributes.LD_PARCELNONDB );
		if(StringUtils.isEmpty(pin)){
			pin = getSearchAttribute( SearchAttributes.LD_PARCELNO2 );
		}
		if( pin.length()>=5 ){
			pin=pin.replaceAll("[^0-9a-zA-Z]+", "");
			asseorBook = pin.substring( 0, 3 );
			assesorPage = pin.substring( 3, 5 );
			//search for plat
			module = new TSServerInfoModule( serverInfo.getModule( TSServerInfo.BOOK_AND_PAGE_MODULE_IDX ) );
			module.clearSaKeys();
			module.setData(0 , asseorBook );
			module.setData(1 , assesorPage );
			module.setData(2 , "ASSESORMAP");
			module.forceValue(5, Calendar.getInstance().get( Calendar.YEAR )+"");
			if(!sa.isUpdate()) {
				modules.add(module);
			}
		}
		
		 // OCR last transfer - do not perform search just extract DATAs from OCR
	    module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX) );
	    module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
	    module.clearSaKeys();
		modules.add( module );
		
		searchedNames.addAll( addOwnerNameSearch( modules, serverInfo, searchedNames, false ) ) ;
		addBuyerNameSearch ( modules, serverInfo, searchedNames, false );
		
		serverInfo.setModulesForAutoSearch(modules);

	}
	
	
}
