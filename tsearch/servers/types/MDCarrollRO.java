package ro.cst.tsearch.servers.types;

/**
 *Carroll
 */

public class MDCarrollRO extends MDGenericRO {
	
	private static final long serialVersionUID = 1112623233484402255L;

	public MDCarrollRO(long searchId) {
		super(searchId);
	}

	public MDCarrollRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	@Override
	protected int getNameType() {
		return 1;	//has individual and corporation name type (INDIVIDUAL_NAME_TYPE_MAP and CORPORATION_NAME_TYPE_MAP2)
	}
	
}
