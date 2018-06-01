package ro.cst.tsearch.servers.types;

/**
 * counties which don't have Search by Parcel ID
 * AZ Navajo, CO Cheyenne, CO Saguache, CO San Miguel
 */
public class GenericCountyRecorder1ROImage extends GenericCountyRecorderROImage {
	
	private static final long serialVersionUID = 1798193762290705254L;

	public GenericCountyRecorder1ROImage(long searchId) {
		super(searchId);
	}

	public GenericCountyRecorder1ROImage(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
			
}