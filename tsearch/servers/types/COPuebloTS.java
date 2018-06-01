package ro.cst.tsearch.servers.types;

public class COPuebloTS extends COHuerfanoTS {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public COPuebloTS(long searchId) {
		super(searchId);
	}

	public COPuebloTS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
}
