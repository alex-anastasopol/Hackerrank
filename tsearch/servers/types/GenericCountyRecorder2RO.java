package ro.cst.tsearch.servers.types;

/**
 * counties which have Search by Parcel ID
 * AZ Apache, AZ Cochise, AZ La Paz, AZSanta Cruz
 */
public class GenericCountyRecorder2RO extends GenericCountyRecorderRO {
	
	private static final long serialVersionUID = -1420501317539338402L;

	public GenericCountyRecorder2RO(long searchId) {
		super(searchId);
	}

	public GenericCountyRecorder2RO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
		
}

