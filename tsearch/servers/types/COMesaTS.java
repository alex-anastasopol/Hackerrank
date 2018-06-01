package ro.cst.tsearch.servers.types;

public class COMesaTS extends COHuerfanoTS {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public COMesaTS(long searchId) {
		super(searchId);
	}

	public COMesaTS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
}
