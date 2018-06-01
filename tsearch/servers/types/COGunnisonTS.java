package ro.cst.tsearch.servers.types;

public class COGunnisonTS extends COHuerfanoTS {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public COGunnisonTS(long searchId) {
		super(searchId);
	}

	public COGunnisonTS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
}
