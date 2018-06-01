package ro.cst.tsearch.servers.types;

import java.util.List;

import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ServerResponse;

public class TXLubbockTS extends TXDaslTS {
	
	private static final long serialVersionUID = 1L;

	public TXLubbockTS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		
		if (module.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX 
				|| module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX) {
			List<TSServerInfoModule> modules = getMultipleModules(module, sd);
			if (!modules.isEmpty()) {
				return super.searchByMultipleInstrument(modules, sd, null);
			}
		}
		
		ServerResponse response = super.searchByMultipleInstrument(module, sd);
		if (response!=null) {
			return response;
		}
		
		return super.SearchBy(module, sd);
	}	
	

}
