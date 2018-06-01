package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;

import com.stewart.ats.base.name.NameI;

/**
 * 
 * @author Oprina George
 * 
 *         Sep 1, 2011
 */

public class TXGenericDaslAcs extends GenericDaslAcs {

	/**
	 * 
	 */
	private static final long serialVersionUID = 733891983178971731L;

	public TXGenericDaslAcs(long searchId) {
		super(searchId);
	}

	public TXGenericDaslAcs(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink,
			long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	protected ServerResponse searchBy(TSServerInfoModule module, Object sd, String fakeResponse) throws ServerResponseException {

		int moduleIdx = module.getModuleIdx();

		if (moduleIdx == TSServerInfo.NAME_MODULE_IDX) {
			List<TSServerInfoModule> modules = getMultipleModules(module, sd);
			if (!modules.isEmpty()) {
				return super.searchByMultipleInstrument(modules, sd, null);
			}
		}

		return super.searchBy(module, sd, fakeResponse);
	}
	

	protected ArrayList<NameI> addNameSearch(List<TSServerInfoModule> modules, TSServerInfo serverInfo, String key,
			ArrayList<NameI> searchedNames, FilterResponse... filters) {

		CommunityAttributes ca = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity();
		Integer years = null;
		if(SearchAttributes.OWNER_OBJECT.equals(key)) {
			years = ServerConfig.getOwnerNameSearchFilterAllow(ca);
		} else if(SearchAttributes.BUYER_OBJECT.equals(key)) {
			years = ServerConfig.getBuyerNameSearchFilterAllow(ca);
		}
		
		if(years != null) {
			Date toDate = getSearch().getSa().getEndDate();
			Calendar cal = Calendar.getInstance();
			cal.setTime(toDate);
			cal.add(Calendar.YEAR, - years);
			FilterResponse[] newfilters = { new BetweenDatesFilterResponse(searchId, cal.getTime()) };
			newfilters = (FilterResponse[]) ArrayUtils.addAll(newfilters, filters);

			return super.addNameSearch(modules, serverInfo, key, searchedNames, newfilters);
		} else {
			return super.addNameSearch(modules, serverInfo, key, searchedNames, filters);
		}
	}
}
