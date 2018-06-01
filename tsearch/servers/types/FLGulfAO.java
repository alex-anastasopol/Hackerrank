package ro.cst.tsearch.servers.types;

public class FLGulfAO extends FLGenericQPublicAO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public FLGulfAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		setDetailsMessage("Gulf County Property Appraiser");
	}

	public ro.cst.tsearch.servers.functions.FLGenericQPublicAO getParserInstance() {
		return ro.cst.tsearch.servers.functions.FLGenericQPublicAO
				.getInstance(ro.cst.tsearch.servers.functions.FLGenericQPublicAO.FlGenericQPublicAOParseType.FLGulfAO);
	}

	protected String getParcelIdFieldNode() {
		return "Parcel Number with Dashes: 04589-000R<br>Parcel Number without Dashes: 04589000R";
	}

}
