package ro.cst.tsearch.servers.types;


/**
 * @author mihaib
  */

public class ILMcHenryTR extends ILGenericTR {

	public static final long serialVersionUID = 10000000L;
	

	public ILMcHenryTR(long searchId) {
		super(searchId);
	}

	public ILMcHenryTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	

}