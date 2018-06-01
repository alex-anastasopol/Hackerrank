package ro.cst.tsearch.servers.types;

/**
 * counties which have Search by Parcel ID
 * AZ Graham, AZ Greenlee
 */
public class GenericCountyRecorder2ROImage extends GenericCountyRecorderROImage {
	
	private static final long serialVersionUID = 2367356466296237045L;

	public GenericCountyRecorder2ROImage(long searchId) {
		super(searchId);
	}

	public GenericCountyRecorder2ROImage(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
			
}