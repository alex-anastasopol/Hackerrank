package ro.cst.tsearch.servers.types;

import ro.cst.tsearch.servers.info.TSServerInfoModule;

public class FLAlachuaDT extends FLSubdividedBasedDASLDT{
	
	private static final long serialVersionUID = 5903343950886088901L;

	public FLAlachuaDT(long searchId) {
		super(searchId);
	}
	
	public FLAlachuaDT(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@Override
	public void addAssesorMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}

	@Override
	public void addPlatMapSearch(TSServerInfoModule module,PersonalDataStruct str) {}
	
	@Override
	public void addParcelMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}
	
	@Override
	public void addSubdivisionMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}
	
	@Override
	public void addCondoMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}

}
