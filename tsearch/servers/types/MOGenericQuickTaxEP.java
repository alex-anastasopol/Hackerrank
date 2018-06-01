package ro.cst.tsearch.servers.types;


public class MOGenericQuickTaxEP extends TSServer {

	private static final long serialVersionUID = -2754273316620692457L;

	/**
	 * Constructor
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 * @param searchId
	 * @param miServerID
	 */
	public MOGenericQuickTaxEP(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
}
