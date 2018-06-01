package ro.cst.tsearch.servers.types;

import java.util.List;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;

public class TXBrazoriaAO extends TXGenericTaxNetAO {
	
	private static final long serialVersionUID = -6272366911919183212L;

	public TXBrazoriaAO(long searchId) {
		super(searchId);
	}

	public TXBrazoriaAO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}
	
	@Override
	protected void addPinModules(TSServerInfo serverInfo, List<TSServerInfoModule> modules) {
		
		TSServerInfoModule module = null;
		
		if (hasPin()) {
			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(1, SearchAttributes.LD_PARCELNO_GENERIC_AO);	//search with GEO Account
			modules.add(module);
			
			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(2, SearchAttributes.LD_PARCELNO_GENERIC_AO);	//backup search with PIDN
			modules.add(module);
		}
	}

}
