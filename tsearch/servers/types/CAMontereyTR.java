package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.List;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;

public class CAMontereyTR extends CAMegabyteCommonSiteTR {
	private static final long	serialVersionUID	= 445321998774294937L;

	public CAMontereyTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		super.setModulesForAutoSearch(serverInfo);
		
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;
		Search search = getSearch();
		int searchType = search.getSearchType();

		if (searchType == Search.AUTOMATIC_SEARCH) {
			if (hasPin()) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.setSaKey(0, SearchAttributes.LD_PARCELNO);
				module.forceValue(1, "c");// current tax year
				module.forceValue(2, "=");// search type - 'equal to'
				module.addValidator(rejectNonRealEstateValidator);
				moduleList.add(module);
			}
			serverInfo.setModulesForAutoSearch(moduleList);
		}
	}

	@Override
	protected boolean siteHasOwner() {
		return true;
	}
}