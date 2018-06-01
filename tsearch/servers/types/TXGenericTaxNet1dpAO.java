package ro.cst.tsearch.servers.types;

/**
 * used for counties which have 'Data to Search' and 'Property Type' select lists
 */

public class TXGenericTaxNet1dpAO extends TXGenericTaxNet1AO {
	
	private static final long	serialVersionUID	= -5810071041407782594L;

	public TXGenericTaxNet1dpAO(long searchId) {
		super(searchId);
	}

	public TXGenericTaxNet1dpAO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}
}
