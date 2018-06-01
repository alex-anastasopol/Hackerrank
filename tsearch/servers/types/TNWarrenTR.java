package ro.cst.tsearch.servers.types;
import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.xml.ResultMap;

public class TNWarrenTR extends TNGenericEgovTR
{
	private static final long serialVersionUID = 4312021854156507300L;

	public TNWarrenTR(String rsRequestSolverName, String rsSitePath, String rsServerID, 
			String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,	miServerID);
	}
	
	@Override
	protected ResultMap parseSpecificIntermediaryRow(TableRow row) {
		return ro.cst.tsearch.servers.functions.TNWarrenTR.parseIntermediaryRow(row, searchId);
	}
	
	@Override
	protected void parseSpecificDetails(ResultMap resultMap) {
		try {
			ro.cst.tsearch.servers.functions.TNWarrenTR.parseAddress(resultMap);
			ro.cst.tsearch.servers.functions.TNWarrenTR.parseNames(resultMap, searchId);
		} catch (Exception e) {
			logger.error("Error while parsing details");
		}
	}
		
}