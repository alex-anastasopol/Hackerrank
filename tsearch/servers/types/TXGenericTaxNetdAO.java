package ro.cst.tsearch.servers.types;

/**
 * used for counties which have 'Data to Search' select list
 */

public class TXGenericTaxNetdAO extends TXGenericTaxNetAO {
	
	private static final long	serialVersionUID	= 6731114655891326362L;

	public TXGenericTaxNetdAO(long searchId) {
		super(searchId);
	}

	public TXGenericTaxNetdAO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}

}
