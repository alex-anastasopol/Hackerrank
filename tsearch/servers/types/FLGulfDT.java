package ro.cst.tsearch.servers.types;

import ro.cst.tsearch.servers.info.TSServerInfoModule;

/**
 * @author cristian stochina
 */
public class FLGulfDT extends FLSubdividedBasedDASLDT{
	
	private static final long serialVersionUID = -5163620029713689847L;

	public FLGulfDT(long searchId) {
		super(searchId);
	}
	
	public FLGulfDT(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
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
