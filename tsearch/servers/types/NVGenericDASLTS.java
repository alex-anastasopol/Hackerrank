package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.List;

import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.info.TSServerInfoModule;

import com.stewart.ats.base.name.NameI;



public class NVGenericDASLTS extends COGenericDASLTS {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NVGenericDASLTS(long searchId) {
		super(searchId);
	}

	public NVGenericDASLTS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	public ConfigurableNameIterator getConfiguredNameIterator(
			ArrayList<NameI> searchedNames, TSServerInfoModule module) {
		
		ConfigurableNameIterator nameIterator = new ConfigurableNameIterator( searchId , new String[] { "L;F;" });
		nameIterator.setAllowMcnPersons( true );
		nameIterator.setEnableCompanyForceFirstName(true);
		List<String> companyForceFirstNameList = new ArrayList<String>();
		companyForceFirstNameList.add("%");
		nameIterator.setCompanyForceFirstNameList(companyForceFirstNameList);
		nameIterator.init(module);
		
		if ( searchedNames!=null ) {
			nameIterator.setInitAgain(true);
			nameIterator.setSearchedNames( searchedNames );
		}
		return nameIterator;
	}
	
}
