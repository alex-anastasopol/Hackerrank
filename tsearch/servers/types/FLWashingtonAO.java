package ro.cst.tsearch.servers.types;

import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

public class FLWashingtonAO extends FLGenericQPublicAO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public FLWashingtonAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		setDetailsMessage("Washington County Property Appraiser");
	}

	@Override
	protected String cleanDetails(String response) {
		// clean the header
		
		// clean the center tags that will mess up further cleaning
		response = response.replaceFirst("(?is)<CENTER>\\s*<table[^>]*>\\s*<tr[^>]*>\\s*<th[^>]*>\\s*<img[^>]*>\\s*.*?</CENTER>", "");

		String result = RegExUtils.getFirstMatch("(?is)<CENTER>(.*?)<CENTER>", response, 1);
		if (StringUtils.isNotEmpty(result)){
			response = result;
		}
		
		String headerToBeRemoved = RegExUtils.getFirstMatch(
				"(?is)<TABLE BORDER COLS=1 WIDTH=\"100%\">.*?\"(Arial Unicode MS)?\".*?</TABLE>", response, 0);
		headerToBeRemoved = headerToBeRemoved.replaceAll("(\\?|\\-)", "\\\\$1");

		response = response.replaceAll("(?is)" + headerToBeRemoved, "");

		// remove the disclaimer
		response = response.replaceAll("(?is)<TABLE BORDER COLS=1 WIDTH=\"100%\">\\s*<TR><TD class=\"(disclaimer)\".*?</TABLE>", "");

		// remove all the links
		response = response.replaceAll("(?is)<a\\b[^>]*>\\s*Clerks?\\s+Documents\\s*</a>", "");
		response = response.replaceAll("(?is)<a href.*?>(.*?)</a>", "$1");
		response = response.replaceAll("(?is)<a href.*?>", "");
		
		// remove the images
		response = response.replaceAll("(?is)<img.*?>", "");
		
		//remove the forms 
		response = response.replaceAll("<form method(.*?)<button(.*?)>(.*?)</button></form>", "$3");
		
		response = response.replaceAll("(?is)<input type=\"button\" .*?Show Parcel Map.*?>", "");
		
		response = "<table id=\"details\" align=\"center\" border=\"1\"><tr><td>" + response + "</td></tr></table>";
		
		return response;
	}

	public ro.cst.tsearch.servers.functions.FLGenericQPublicAO getParserInstance() {
		return ro.cst.tsearch.servers.functions.FLGenericQPublicAO
				.getInstance(ro.cst.tsearch.servers.functions.FLGenericQPublicAO.FlGenericQPublicAOParseType.FLWashingtonAO);
	}
	
	protected String getParcelIdFieldNode() {
		return "Parcel Number with Dashes: 00000000-00-1651-0000";
	}
	
}
