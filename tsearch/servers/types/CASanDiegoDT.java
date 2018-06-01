package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.name.NameI;

/**
 * @author Cristian Stochina
 */
public class CASanDiegoDT extends CAGenericDASLDT {

	private static final long serialVersionUID = 5596627821129730349L;

	
	public CASanDiegoDT(long searchId) {
		super(searchId);
	}

	public CASanDiegoDT(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	public TSServerInfo getDefaultServerInfo() {
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
		
		if ( !StringUtils.isEmpty(pin) ) {
			int pinSize = pin.length();
			if ( pinSize >= 10 && !pin.contains("-") ) {
				
				String pin1 = pin.substring(0, 3); // split in 3 - 3 - 2 - 2  format
				String pin2 = pin.substring(3, 6);
				String pin3 = pin.substring(6, 8);
				String pin4 = pin.substring(8, 10);

				String newpin = pin1+"-"+pin2+"-"+pin3+"-"+pin4;
				if( pinSize >= 11 ){
					newpin += ("-" + pin.substring( 10, pinSize ));
				}
				pin = newpin;
			}
		}
		sa.setAtribute(SearchAttributes.LD_PARCELNO2, pin);
		return  super.getDefaultServerInfo();
	}
	
	
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		SearchAttributes sa = global.getSa() ;
		String fromDateStr = sa.getFromDateString("MM/dd/yyyy");
		String asseorBook  =  null;
		String assesorPage =  null;
	
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		// P1 : search by PID search
		String pin = getSearchAttribute( SearchAttributes.LD_PARCELNO2 );
		if ( !StringUtils.isEmpty(pin) ) {
				module = new TSServerInfoModule( serverInfo.getModule( TSServerInfo.PARCEL_ID_MODULE_IDX ) );
				module.clearSaKeys();
				module.setData( 0, pin );
				if(isUpdate()){
					module.setData( 3, fromDateStr );
				}
				else module.forceValue( 3, "" );
				modules.add(module);
		}
		
		ArrayList<NameI> searchedNames = addOwnerNameSearch( modules, serverInfo, null, false );
			
		pin = getSearchAttribute( SearchAttributes.LD_PARCELNONDB );
		if(StringUtils.isEmpty(pin)){
			pin = getSearchAttribute( SearchAttributes.LD_PARCELNO2 );
		}
		if( pin.length()>=5 ){
			asseorBook = pin.substring( 0, 3 );
			assesorPage = pin.substring( 3, 4 );
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

