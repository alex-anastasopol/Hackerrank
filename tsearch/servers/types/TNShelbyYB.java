package ro.cst.tsearch.servers.types;



public class TNShelbyYB extends TNGenericMSServiceCT {

	public static final long serialVersionUID = 10583724400L;
	
	
	public TNShelbyYB(long searchId) {
		super(searchId);
	}

	public TNShelbyYB(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
}