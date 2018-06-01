package ro.cst.tsearch.servers.types;

/**
 * used for counties which have 'Data to Search' select list
 */

public class TXGenericTaxNet1dAO extends TXGenericTaxNet1AO {
	
	private static final long	serialVersionUID	= 7810889296517723410L;

	public TXGenericTaxNet1dAO(long searchId) {
		super(searchId);
	}

	public TXGenericTaxNet1dAO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}

}
