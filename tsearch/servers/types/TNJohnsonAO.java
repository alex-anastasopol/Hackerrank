package ro.cst.tsearch.servers.types;


public class TNJohnsonAO extends TNGenericCountyAO {
	static final long serialVersionUID = 10000000;
	
	public TNJohnsonAO(
			String rsRequestSolverName,
			String rsSitePath,
			String rsServerID,
			String rsPrmNameLink, long searchId, int mid) {
			super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
			setSpecificCounty("Johnson");
			
		}
}
