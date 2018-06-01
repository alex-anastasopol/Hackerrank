package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.htmlparser.Node;
import org.htmlparser.tags.TableColumn;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.StringUtils;

public class COMesaTR extends COGenericTylerTechTR {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public COMesaTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	public String getDetailsPrefix() {
		return "/Treasurer/treasurerweb/";
	}

	protected String getSaleInfoTable(String accountId) {
		// make a request to get Appraissal info for SaleDataSet
		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		String assessorPage = "";
		try {
			if (StringUtils.isNotEmpty(accountId)) {
				assessorPage = ((ro.cst.tsearch.connection.http2.COMesaTR) site).getAssessorPage(accountId);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager.releaseSite(site);
		}
		HtmlParser3 parser = new HtmlParser3(assessorPage);
		Node saleDataTable = parser.getNodeById("gdv_SalesDeeds");
		String html = "";
		if (saleDataTable != null){
			html = saleDataTable.toHtml();
			// clean it
			html = html.replaceAll("<u>\\(Click for Recorded Document\\)", "");
			html = html.replaceAll("(?is)<a .*?>(.*?)</a>", "$1");
		}
		
		return html;
	}

	protected String clean(String result) {
		result = result.replaceAll("\\bFor current year values visit the\\s*<a href=\"http://replaceme/\">Mesa County Assessor's site.</a>", "");
		return result;
	}
	
	public void parseLegal(String contents, ResultMap resultMap) throws Exception {
		ro.cst.tsearch.servers.functions.COMesaTR.parseLegal(contents, resultMap);
	}

	public void parseName(Set<String> hashSet, ResultMap resultMap) throws Exception {
		ro.cst.tsearch.servers.functions.COMesaTR.parseName(hashSet, resultMap);
	}

	public void parseAddress(String address, ResultMap resultMap) throws Exception {
		ro.cst.tsearch.servers.functions.COMesaTR.parseAddress(resultMap, address);
	}

	public void parseIntermediaryRow(COGenericTylerTechTR server, ResultMap resultMap, TableColumn[] cols) throws Exception {
		ro.cst.tsearch.servers.functions.COWeldTR.parseIntermediaryRow(server, resultMap, cols);
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule m;

		addPinModule(serverInfo, l);

		if (hasStreet()) {
			
			FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.7d);
			FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
			//DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
			FilterResponse rejectNonRealEstateFilter = new RejectNonRealEstate(SearchAttributes.OWNER_OBJECT, searchId);
			rejectNonRealEstateFilter.setThreshold(new BigDecimal("0.65"));
			
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.setSaKey(3, SearchAttributes.P_STREETNO);
			m.setSaKey(5, SearchAttributes.P_STREETNAME);
			m.addFilter(nameFilterHybrid);
			m.addFilter(rejectNonRealEstateFilter);
			m.addFilter(addressFilter);
			l.add(m);
		}

		serverInfo.setModulesForAutoSearch(l);
	}

	protected void addPinModule(TSServerInfo serverInfo, List<TSServerInfoModule> l) {
		String accountIDPattern = "(?i)[A-Z]\\w{5,9}";
		String parcelNumberPattern = "\\d{10,14}";
		String pid2 = org.apache.commons.lang.StringUtils
				.defaultString(getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO2_ALTERNATE)).trim();
		if (hasPin()) {
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			if (pid2.matches(accountIDPattern)) {
				module.setData(0, pid2);
			} else {
				String pid1 = org.apache.commons.lang.StringUtils
						.defaultString(getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO_GENERIC_TR)).trim();
				if (pid1.matches(parcelNumberPattern)) {
					module.setData(1, pid1);
				} else if (pid1.matches(accountIDPattern)) {
					module.setData(0, pid1);
				}
			}
			l.add(module);
		}
	}
}
