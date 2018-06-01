package ro.cst.tsearch.servers.types;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.SimpleNodeIterator;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.datatrace.Utils;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parentsitedescribe.ParentSiteEditorUtils;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.RegExUtils;

public class TXWiseTR extends TemplatedServer {

	public TXWiseTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		int[] intermediary_cases = { ID_INTERMEDIARY, ID_SEARCH_BY_NAME };
		setIntermediaryCases(intermediary_cases);

		int[] details_cases = { TSServer.ID_DETAILS };
		setDetailsCases(details_cases);

		int[] save_cases = { TSServer.ID_SAVE_TO_TSD };
		setSAVE_CASES(save_cases);

		setIntermediaryMessage("Click a row in the result table"); //to view record details.
		setDetailsMessage("Address");
	}

	@Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.GENERIC_MODULE_IDX);
		if (tsServerInfoModule != null && tsServerInfoModule.getFunctionList() != null && tsServerInfoModule.getFunctionList().size() > 7) {
			int currentYear = Calendar.getInstance().get(Calendar.YEAR);
			String defaultOption = "<option value=\"{0}\" selected=\"selected\">{1}</option>";
			String generateYearSelectOptions = ParentSiteEditorUtils.generateYearSelectOptions("form1:rollYear", 1940, currentYear - 1,
					MessageFormat.format(defaultOption, "all", "All Years"));
			Utils.setupSelectBox(tsServerInfoModule.getFunction(0), generateYearSelectOptions);
		}
		return super.getDefaultServerInfo();
	}

	@Override
	protected void setMessages() {
		getErrorMessages().addNoResultsMessages("No results found");
	}

	@Override
	protected String getAccountNumber(String serverResult) {
		HtmlParser3 parser3 = new HtmlParser3(serverResult);
		Node nodeByID = HtmlParser3.getNodeByID("form1:textGdsa_geo_number1", parser3.getNodeList(), true);
		return nodeByID.toPlainTextString();
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		getParserInstance().parseDetails(detailsHtml, searchId, map);
		return null;
	}

	@Override
	protected String addInfoToDetailsPage(ServerResponse response, String serverResult, int viParseID) {
		if (isInArray(viParseID, getDETAILS_CASES()) != Integer.MIN_VALUE){
			String taxHistoryTablesForAccountId = getTaxHistoryTablesForAccountId(getAccountNumber(serverResult));
			taxHistoryTablesForAccountId = taxHistoryTablesForAccountId.replaceAll("(?is)<tfoot.*</tfoot>", "");
			taxHistoryTablesForAccountId = taxHistoryTablesForAccountId.replaceAll("(?is)<script.*</script>", "");
			serverResult = serverResult + "<br/>" + taxHistoryTablesForAccountId;
			response.setResult(serverResult);
		}
		
		return serverResult;
	}

	@Override
	protected String clean(String response) {
		HtmlParser3 htmlParser3 = new HtmlParser3(response);
		NodeList localNodeList = htmlParser3.getNodeList();
		String formAction = RegExUtils.getFirstMatch("action=\"(.*?)\"", response, 1);
		Node nodeByID = HtmlParser3.getNodeByID("resultForm:tableEx1", localNodeList, true);
		if (nodeByID == null ){
			nodeByID = HtmlParser3.getNodeByID("resultForm:tableEx2", localNodeList, true);
		}
		String result = nodeByID.toHtml();
		result = result.replaceAll("(?is)<script.*</script>", "");
		result = result.replaceAll("(?is)(?is)<img.*?</img>", "");
		result = result.replaceAll("(?is)<td class=.*?</td>", "<td><td/>");
		NodeList list = HtmlParser3.getNodeListByTypeAndAttribute(localNodeList, "script", "type", "text/javascript", true);
		if (list.size() == 5) {
			result += list.elementAt(4).toHtml();
		}
		result = result + String.format("<input type=\"hidden\" name=\"actionForm\" value=\"%s\">", formAction);
		// result = result.replaceAll("(?is)<script.*</script>", "");
		return result;
	}

	@Override
	protected String cleanDetails(String response) {
		HtmlParser3 parser3 = new HtmlParser3(response);
		NodeList list = HtmlParser3.getNodeListByType(parser3.getNodeList(), "table", true);
		if (list != null) {
			list.remove(list.size() - 1);
		}
		NodeFilter filter = new TagNameFilter("input");
		NodeList buttonsToRemove = list.extractAllNodesThatMatch(filter, true);
		SimpleNodeIterator nodeIterator = buttonsToRemove.elements();
		String result = list.toHtml();
		while (nodeIterator.hasMoreNodes()) {
			result = result.replace(nodeIterator.nextNode().toHtml(), "");
		}

		return result;
	}

	/**
	 * For an accessed line in intermediary results goes makes a new search for
	 * the account id with all years selected. If there are multiple rows get
	 * the tax history for the existing rows.
	 */
	public String getTaxHistoryTablesForAccountId(String accountId) {
		String taxTables = "";

		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		if (site instanceof ro.cst.tsearch.connection.http2.TXWiseTR) {
			taxTables = ro.cst.tsearch.connection.http2.TXWiseTR.getIntermediaryForAccount(accountId,
					(ro.cst.tsearch.connection.http2.TXWiseTR) site);
		}
		taxTables = clean(taxTables);
		return taxTables;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		MessageFormat format = createPartialLinkFormat();
		return getParserInstance().parseIntermediary(response, table, searchId, format);
	}

	public ro.cst.tsearch.servers.functions.TXWiseTR getParserInstance() {
		ro.cst.tsearch.servers.functions.TXWiseTR instance = ro.cst.tsearch.servers.functions.TXWiseTR.getInstance();
		return instance;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);

		String streetNo = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNO);
		String streetName = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNAME);

		PinFilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
		FilterResponse taxYearFilter = new TaxYearFilterResponse(searchId);

		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
		((GenericNameFilter) defaultNameFilter).setIgnoreMiddleOnEmpty(true);

		if (hasPin()) {
			String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO_GENERIC_TR);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(3).forceValue(pin);
			module.addFilter(taxYearFilter);
			modules.add(module);
		}

		if (hasStreetNo() || hasStreet()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(4).forceValue(streetNo);
			module.getFunction(5).forceValue(streetName);
			module.addFilter(taxYearFilter);
			module.addFilter(nameFilterHybrid);
			modules.add(module);
		}

		if (hasOwner()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);

			module.addFilter(pinFilter);
			if (!hasPin()) {
				module.addFilter(nameFilterHybrid);
				module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);

				ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(
						module, searchId, new String[] { "L;F;", "L; F M;", "L; f;", "L; m;" });
				module.addIterator(nameIterator);

			}
			module.addFilter(taxYearFilter);
			modules.add(module);
		}
		serverInfo.setModulesForAutoSearch(modules);
	}

	protected HashMap<String, String> putAdditionalData(String serverResult) {
		HashMap<String, String> data = new HashMap<String, String>();
		data.put("type", "CNTYTAX");
		return data;
	}
}
