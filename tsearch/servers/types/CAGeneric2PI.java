package ro.cst.tsearch.servers.types;


public class CAGeneric2PI extends CAGenericPI{

	private static final long serialVersionUID = 8863687363582028102L;
	

	public CAGeneric2PI(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	public CAGeneric2PI(long searchId) {
		super(searchId);
	}
	
}
