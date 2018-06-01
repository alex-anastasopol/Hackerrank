package ro.cst.tsearch.servers.types;

/**
 * used for counties which have 'Property Type' select list
 */

public class TXGenericTaxNet1pAO extends TXGenericTaxNet1AO {
	
	private static final long	serialVersionUID	= -4619782661366899762L;

	public TXGenericTaxNet1pAO(long searchId) {
		super(searchId);
	}

	public TXGenericTaxNet1pAO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}

}
