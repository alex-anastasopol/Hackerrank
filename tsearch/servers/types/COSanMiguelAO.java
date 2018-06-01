/**
 * 
 */
package ro.cst.tsearch.servers.types;

import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.xml.ResultMap;

/**
 * @author vladb
 */
public class COSanMiguelAO extends COGenericValuewestAO {

	private static final long serialVersionUID = 1L;

	public COSanMiguelAO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
	}

	@Override
	protected ResultMap parseSpecificIntermediaryRow(TableRow row) {
		
		return ro.cst.tsearch.servers.functions.COSanMiguelAO.parseIntermediaryRow(row, searchId);
	}
	
	@Override
	protected void parseSpecificDetails(ResultMap resultMap) {
		
		ro.cst.tsearch.servers.functions.COSanMiguelAO.parseNames(resultMap, null);
		ro.cst.tsearch.servers.functions.COSanMiguelAO.parseAddress(resultMap);
		ro.cst.tsearch.servers.functions.COSanMiguelAO.parseLegalSummary(resultMap);
	}
}
