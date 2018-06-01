package ro.cst.tsearch.servers.types;


import ro.cst.tsearch.servers.info.TSServerInfoModule;
/**
 * @author Cristian Stochina
 */
public class FLWakullaDT extends FLSubdividedBasedDASLDT{

	private static final long serialVersionUID = 3477834L;

	public FLWakullaDT(long searchId) {
		super(searchId);
	}
	
	public FLWakullaDT(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	public void addAssesorMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}

	@Override
	public void addPlatMapSearch(TSServerInfoModule module,PersonalDataStruct str) {
		//addDefaultPlatMapSearch(module, str, "ASSESSOR_MAP");
	}
	
	@Override
	public void addParcelMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}
	
	@Override
	public void addSubdivisionMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}
	
	@Override
	public void addCondoMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}
}
