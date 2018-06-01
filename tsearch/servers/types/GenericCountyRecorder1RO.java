package ro.cst.tsearch.servers.types;

/**
 * counties which don't have Search by Parcel ID
 * CO Baca, CO Dolores, CO Kiowa, CO Lincoln, CO San Juan, CO Sedgwick, CO Teller, CO Washington
 */
public class GenericCountyRecorder1RO extends GenericCountyRecorderRO {
	
	private static final long serialVersionUID = 1473029151972791900L;

	public GenericCountyRecorder1RO(long searchId) {
		super(searchId);
	}

	public GenericCountyRecorder1RO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
		
}

