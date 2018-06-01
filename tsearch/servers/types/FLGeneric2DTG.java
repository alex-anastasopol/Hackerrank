package ro.cst.tsearch.servers.types;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author mihaib
 * 
 */

public class FLGeneric2DTG extends FLGenericDTG {

	private static final long serialVersionUID = -6964284342671945857L;

	public FLGeneric2DTG(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid)
			throws FileNotFoundException, IOException {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public FLGeneric2DTG(long searchId) {
		super(searchId);
	}
}
