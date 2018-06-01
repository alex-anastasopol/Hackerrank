package ro.cst.tsearch.servers.types;
import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.xml.ResultMap;

public class TNMontgomeryTR extends TNGenericEgovTR
{
	private static final long serialVersionUID = 6724534718806487449L;

	public TNMontgomeryTR(String rsRequestSolverName, String rsSitePath, String rsServerID, 
			String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,	miServerID);
	}
	
	@Override
	protected ResultMap parseSpecificIntermediaryRow(TableRow row) {
		return ro.cst.tsearch.servers.functions.TNMontgomeryTR.parseIntermediaryRow(row);
	}
	
	@Override
	protected void parseSpecificDetails(ResultMap resultMap) {
		try {
			ro.cst.tsearch.servers.functions.TNMontgomeryTR.parseAddress(resultMap);
			ro.cst.tsearch.servers.functions.TNMontgomeryTR.parseNames(resultMap);
		} catch (Exception e) {
			logger.error("Error while parsing details");
		}
	}
		
}