package ro.cst.tsearch.servers.types;

import java.util.List;

import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ServerResponse;

public class TXWichitaTS extends TXDaslTS {
	
	private static final long serialVersionUID = 1L;

	public TXWichitaTS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		
		if (module.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_MODULE_IDX
				|| module.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX
				|| module.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX 
				|| module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX) {
			List<TSServerInfoModule> modules = getMultipleModules(module, sd);
			if (!modules.isEmpty()) {
				return super.searchByMultipleInstrument(modules, sd, null);
			}
		}
		
		return super.SearchBy(module, sd);
	}	
	
	@Override
	public LegalDescriptionIterator getLegalDescriptionIterator(long searchId,
			boolean lookUpWasWithNames, boolean legalFromLastTransferOnly) {
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId, lookUpWasWithNames, legalFromLastTransferOnly, getDataSite());
		it.setEnableTownshipLegal(false);
		it.setUseAddictionInsteadOfSubdivision(true);
		return it;
	}
	

}
