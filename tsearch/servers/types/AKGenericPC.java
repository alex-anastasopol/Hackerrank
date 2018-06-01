package ro.cst.tsearch.servers.types;

import java.util.List;

import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;

public class AKGenericPC extends GenericPC {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AKGenericPC(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
        resultType = MULTIPLE_RESULT_TYPE;
	}
	
	@Override
	public void addAutomaticModule(TSServerInfo serverInfo, List<TSServerInfoModule> modules, int moduleIndex, List<String> regions) {
		//Bug 6182 - (Ticket#1288469) EN AK - When the Pacer search runs in automatic, they only want it to run the "Bankruptcy" search
		if(moduleIndex == MODULE_IDX_BANKRUPTCY) {	
			super.addAutomaticModule(serverInfo, modules, moduleIndex, regions);
		}
	}

}
