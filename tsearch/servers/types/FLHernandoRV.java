package ro.cst.tsearch.servers.types;

import java.util.List;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.StringUtils;

public class FLHernandoRV extends GenericDASLRV {

	private static final long serialVersionUID = 1L;

	public FLHernandoRV(long searchId) {
		super(searchId);
	}

	public FLHernandoRV(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink,
			long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	public void addPinSearch(TSServerInfo serverInfo, List<TSServerInfoModule> modules) {
		SearchAttributes sa = getSearchAttributes();
		String pin = sa.getAtribute( SearchAttributes.LD_PARCELNO );
		if( StringUtils.isEmpty(pin) ){
			pin = sa.getAtribute(SearchAttributes.LD_PARCELNO2);
		}
		if(StringUtils.isNotEmpty(pin)) {
			pin = pin.replaceAll("[-\\s]+", "");
			addPinSearch( pin, modules, serverInfo, mSearch );
		}
	}

}
