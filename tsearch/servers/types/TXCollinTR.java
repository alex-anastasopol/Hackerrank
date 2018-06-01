package ro.cst.tsearch.servers.types;

import java.util.List;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.StringUtils;

public class TXCollinTR extends TXGenericSMediaTR{
	
	/**
	 * @author mihaib
	 */
	private static final long serialVersionUID = 3922610441378189743L;

	public TXCollinTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	protected String cleanBaseLink(){
		String baseLink = getBaseLink();
		if (StringUtils.isEmpty(baseLink)){
			return "";
		} else {
			int idx = baseLink.lastIndexOf(".gov");
			if(idx == -1){
				throw new RuntimeException("Cannot clean the base link");
			}
			return baseLink.substring(0, idx + 4);//http://taxpublic.collincountytx.gov/webcollincounty/accountsearch.htm
		}
	}

	@Override
	protected void addPinModule(TSServerInfo serverInfo, List<TSServerInfoModule> modules) {
		TSServerInfoModule module;
		String pid = "";
		FilterResponse pinFilter = PINFilterFactory.getPinFilter(searchId, SearchAttributes.LD_PARCELNO_GENERIC_TR, false, false);
		pid = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO_GENERIC_TR).trim();

		if (hasPin()) {
			if (pid.matches("(?i)[A-Z]\\w{9,13}")) {// if pin matches Account No. format (e.g. R-1595-002-0100-1) then search by Account No.
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.clearSaKeys();
				module.setData(0, pid);
				modules.add(module);
			} else if (pid.matches("(?i)\\d{5,9}")) {// if pin matches PIDN format (e.g. 1687650) then search by PIDN
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.setData(0, pid);
				module.addFilter(pinFilter);
				modules.add(module);
			}
		}
	}
	
}
