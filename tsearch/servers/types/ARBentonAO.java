package ro.cst.tsearch.servers.types;

import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.functions.ForwardingParseClass;
import ro.cst.tsearch.servers.info.TSServerInfo;

public class ARBentonAO extends ARGenericCountyDataAOTR {

	public ARBentonAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@Override
	protected ForwardingParseClass getParserInstance() {
		return super.getParserInstance();
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		super.setModulesForAutoSearch(serverInfo);
	}
	
	@Override
	protected int getIteratorType() {
		return FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE;
	}

	@Override
	public int getSiteType() {
		return GWTDataSite.AO_TYPE;
	}

}
