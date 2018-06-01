package ro.cst.tsearch.servers.types;

import java.util.List;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.filter.fornext.ParcelNumberFilterResponseForNext;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;

/**
 *Montgomery
 */

public class MDMontgomeryRO extends MDGenericRO {
	
	private static final long serialVersionUID = -4203704587423331982L;

	public MDMontgomeryRO(long searchId) {
		super(searchId);
	}

	public MDMontgomeryRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	@Override
	protected void addTaxAccountNoSearch(List<TSServerInfoModule> modules, TSServerInfo serverInfo) {
		TSServerInfoModule module = null;
		if(hasPin()) {
			String taxAccountNo = getSearchAttribute(SearchAttributes.LD_PARCELNO_GENERIC_TR);
			taxAccountNo = taxAccountNo.replaceFirst("^0+", "");
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.TAX_BILL_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(taxAccountNo);
			module.addFilterForNext(new ParcelNumberFilterResponseForNext(taxAccountNo, searchId));
			modules.add(module);
		}
	}
	
	@Override
	protected int getNameType() {
		return 1;	//has individual and corporation name type (INDIVIDUAL_NAME_TYPE_MAP and CORPORATION_NAME_TYPE_MAP2)
	}
	
}
