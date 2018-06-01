package ro.cst.tsearch.servers.types;

public class FLGilchristAO extends FLGenericQPublicAO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public FLGilchristAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		setDetailsMessage("Gilchrist County Property Appraiser");
	}
	
	protected String getParcelIdFieldNode() {
		return "Parcel Number with Dashes: 16-10-15-0048-0027-0010<br>Parcel Number without Dashes: 161015004800270010";
	}
	
	public ro.cst.tsearch.servers.functions.FLGenericQPublicAO getParserInstance() {
		return ro.cst.tsearch.servers.functions.FLGenericQPublicAO
				.getInstance(ro.cst.tsearch.servers.functions.FLGenericQPublicAO.FlGenericQPublicAOParseType.FLGilchristAO);
	}

}
