package ro.cst.tsearch.servers.types;

import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.utils.StringUtils;

public class FLWakullaAO extends FLGenericQPublicAO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public FLWakullaAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		setDetailsMessage("Wakulla County Property Appraiser");
	}
	
	public ro.cst.tsearch.servers.functions.FLGenericQPublicAO getParserInstance() {
		return ro.cst.tsearch.servers.functions.FLGenericQPublicAO
				.getInstance(ro.cst.tsearch.servers.functions.FLGenericQPublicAO.FlGenericQPublicAOParseType.FLWakullaAO);
	}

	protected String getParcelIdFieldNode() {
		return "Parcel Number with Dashes: 00-00-077-021-10675-000<br>Parcel Number without Dashes: 000007702110675000";
	}
	
	@Override
	protected String getAccountNumber(String serverResult) {
		HtmlParser3 parser = new HtmlParser3(serverResult);
		String nodeValue = parser.getValueFromNextCell(parser.getNodeList(), "Parcel Number", "", true);
		String parcelId = StringUtils.cleanHtml(nodeValue);
		return parcelId;
	}

}
