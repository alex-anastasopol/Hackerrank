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
 **/
public class CASanFranciscoDT extends CAGenericDASLDT {

	private static final long serialVersionUID = -7721629532980516161L;
	
	public TSServerInfo getDefaultServerInfo() {
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
		if ( !StringUtils.isEmpty(pin) ) {
			int pinSize = pin.length();
			if ( pinSize >= 5 && !pin.contains("-") ) {
				String pin1 = "";
				String pin2 = "";

				String pDetect =pin.substring(4,5);
				System.err.println("pozitia   "+pDetect);
				if(!pDetect.equals(pDetect.replaceAll("(?is)[a-zA-Z]+", ""))){
					pin1=pin.substring(0, 5);
					pin2=pin.substring(5, pinSize);
				}
				else{
					pin1=pin.substring(0, 4);
					pin2=pin.substring(4, pinSize);
					
				}
				pin = pin1+"-"+pin2;
			}
		}
		sa.setAtribute(SearchAttributes.LD_PARCELNO2, pin);
		return  super.getDefaultServerInfo();
	}
	
	public CASanFranciscoDT(long searchId) {
		super(searchId);
	}

	public CASanFranciscoDT(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		SearchAttributes sa = global.getSa() ;
		String fromDateStr = sa.getFromDateString("MM/dd/yyyy");
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		// P1 : search by PID search
		String pin = getSearchAttribute( SearchAttributes.LD_PARCELNO2 );
		if ( !StringUtils.isEmpty(pin) ) {
				module = new TSServerInfoModule( serverInfo.getModule( TSServerInfo.PARCEL_ID_MODULE_IDX ) );
				module.clearSaKeys();
				module.setData( 0, pin );
				module.forceValue(1, "TAXONLY");
				if(isUpdate()){
					module.setData( 3, fromDateStr );
				}
				else module.forceValue( 3, "" );
				modules.add(module);
				
				module = new TSServerInfoModule( serverInfo.getModule( TSServerInfo.PARCEL_ID_MODULE_IDX ) );
				module.clearSaKeys();
				module.setData( 0, pin );
				module.forceValue(1, "NO");
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
		if( pin.length()>=7 ){
			String asseorBook = pin.substring( 0, 4 );
			String assesorPage = pin.substring( 4, 5 );
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
