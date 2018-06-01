package ro.cst.tsearch.servers.types;

/**
 * used for counties which have 'Data to Search' and 'Property Type' select lists
 */

public class TXGenericTaxNetdpAO extends TXGenericTaxNetAO {
	
	private static final long	serialVersionUID	= -2443790756865524172L;

	public TXGenericTaxNetdpAO(long searchId) {
		super(searchId);
	}

	public TXGenericTaxNetdpAO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}

}
