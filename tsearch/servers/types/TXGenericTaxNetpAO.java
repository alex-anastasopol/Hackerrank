package ro.cst.tsearch.servers.types;

/**
 * used for counties which have 'Property Type' select list
 */

public class TXGenericTaxNetpAO extends TXGenericTaxNetAO {
	
	private static final long	serialVersionUID	= -3572667573353914210L;

	public TXGenericTaxNetpAO(long searchId) {
		super(searchId);
	}

	public TXGenericTaxNetpAO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}

}
