package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.InstanceManager;

import com.stewart.ats.base.name.NameI;

public class TXGenericConcatNameDASLTS extends COGenericDASLTS {

	private static final long serialVersionUID = 1L;

	public TXGenericConcatNameDASLTS(String rsRequestSolverName,
			String rsSitePath, String rsServerID, String rsPrmNameLink,
			long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}

	@Override
	public String[] getNameSearchDerivations() {
		return new String[] { "L;F;", "L F;;" };
	}

	@Override
	public LegalDescriptionIterator getLegalDescriptionIterator(long searchId,
			boolean lookUpWasWithNames, boolean legalFromLastTransferOnly) {
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId,
				lookUpWasWithNames, legalFromLastTransferOnly, getDataSite());
		it.setEnableTownshipLegal(false);
		it.setUseAddictionInsteadOfSubdivision(true);
		return it;
	}

	@Override
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
