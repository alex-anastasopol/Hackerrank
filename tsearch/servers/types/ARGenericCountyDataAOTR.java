package ro.cst.tsearch.servers.types;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.propertyInformation.Family;
import ro.cst.tsearch.propertyInformation.Person;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.functions.ForwardingParseClass;
import ro.cst.tsearch.servers.functions.ParseClassWithExceptionTreatment;
import ro.cst.tsearch.servers.functions.ParseInterface;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.RegExUtils;

public abstract class ARGenericCountyDataAOTR extends TemplatedServer {

	public static final String TMP_SITE_TYPE = "tmpSiteType";
	public static final long serialVersionUID = 10000000L;
	private static final Pattern dummyPattern = Pattern.compile("&dummy=([0-9]+)&");


	/**
	 * @param selectFile
	 * @return
	 */
	public static String getHtmlSelectFromFile(String selectFile) {
		String selects = ""; 
		String folderPath = ServerConfig.getModuleDescriptionFolder(BaseServlet.REAL_PATH + "WEB-INF/classes/resource/module/comments/");
		File folder = new File(folderPath);
		if(!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath + "] does not exist. Module Information not loaded!");
		}
		try {
			selects = FileUtils.readFileToString(new File(folderPath + File.separator + selectFile));
		} catch (Exception e) {
			e.printStackTrace();	
		}
		return selects;
	}

	public ARGenericCountyDataAOTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		initTemplatedServerData();
	}

	@Override
	protected String clean(String response) {
		HtmlParser3 parser3 = new HtmlParser3(response);
		Node node = parser3.getNodeByTypeAndAttribute("table", "bgcolor", "#aaaaaa", true);
		String cleanedResponse = response;
		if (node != null) {
			cleanedResponse = node.toHtml();
			cleanedResponse = cleanedResponse.replaceAll(
					"onmouseover=\"GetParcel\\(.*?'\\)\\;\" onmouseout=\"GetParcel\\('','PULTAX'\\);\"", "");
			cleanedResponse = cleanedResponse.replaceAll("onmouseout=\".*?\"","");
			cleanedResponse = cleanedResponse.replaceAll("onclick=\".*?\"","");
			cleanedResponse = cleanedResponse.replaceAll("onmouseover=\".*?\"","");
			
			// get the links to other pages
			cleanedResponse += "<br/>" + RegExUtils.getFirstMatch("(?is)(?=You are on page:).*?(<table.*?</table>)", response, 1);

			cleanedResponse = cleanedResponse.replaceAll("(?is)<input type=\"checkbox\".*?>", "");
		}
		return cleanedResponse;
	}

	public String getSiteLink(long searchId) {
		String siteLink = getDataSite().getLink();
		
		String[] split = siteLink.split("\\?");
		if (split.length == 2) {
			siteLink = split[0];
		}
		return siteLink;
	}

	@Override
	protected String cleanDetails(String response) {
		HtmlParser3 parser = new HtmlParser3(response);
		
		Node node = parser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("border", "0"))
			.extractAllNodesThatMatch(new HasAttributeFilter("cellspacing", "1"))
			.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "2"))
			.extractAllNodesThatMatch(new HasAttributeFilter("bgcolor", "#ffffff"))
			.elementAt(0);
		
		String html = "";
		if(node != null) {
			html = node.toHtml();
			html = html.replaceAll(Pattern.quote("class=\"headertext\""), "style=\"color:#FFFFFF;font-weight:bold;\"");
			html = html.replaceAll("<img.*?>", "");
			html = html.replaceAll("<a.*?</a>", "");
			html = html.replaceAll("(?is)onclick=\\\"[^\\\"]+\\\"\\s*", "");
			html = html.replaceAll("(?is)onmouseout=\\\"[^\\\"]+\\\"\\s*", "");
			html = html.replaceAll("(?is)onmouseover=\\\"[^\\\"]+\\\"\\s*", "");
		}
		
		return html;
	}

	@Override
	protected String getAccountNumber(String serverResult) {
		HtmlParser3 parser3 = new HtmlParser3(serverResult);
		String cell = parser3.getValueFromAbsoluteCell(1, 0, "Parcel Number:");
		return cell;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		String pid = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO);
		String streetNO = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNO);
		String streetName = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNAME);

		GenericAddressFilter addressFilter = AddressFilterFactory.getGenericAddressHighPassFilter(searchId, 0.8d);
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
		((GenericNameFilter) defaultNameFilter).setIgnoreMiddleOnEmpty(true);
		if (hasPin()) {
			// Search by Parcel/Schedule Number
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			pid = getCorrectPIN(pid);

			module.setData(1, pid);
			// addExtreValuesToModule(module);
			modules.add(module);
		}

		if (hasStreet()) {
			// Search by Property Address
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.forceValue(6, streetNO);
			module.forceValue(9, streetName);
			// addExtreValuesToModule(module);
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			modules.add(module);
		}

		if (hasOwner()) {
			// Search by owner
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			// addExtreValuesToModule(module);
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			module.setIteratorType(4, getIteratorType());
			ConfigurableNameIterator nameIterator = getConfigurableNameIterator(module);
			module.addIterator(nameIterator);
			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);
	}

	/**
	 * @param pid
	 * @return
	 */
	public String getCorrectPIN(String pid) {
		String pidRegEx = "(\\d{2,2}\\w)(\\d{3,3})(\\d{2,2})(\\d{3,3})(\\d{2,2})";
		if (RegExUtils.matches(pidRegEx, pid)) {
			pid = pid.replaceAll(pidRegEx, "$1-$2.$3-$4.$5");
		}
		return pid;
	}

	/**
	 * @return
	 */
	protected int getIteratorType() {
		return FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE;
	}

	/**
	 * @param module
	 * @return
	 */
	protected ConfigurableNameIterator getConfigurableNameIterator(TSServerInfoModule module) {
		return (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, new String[] { "L;f;" });
	}

	/**
	 * @param module
	 * @param streetName
	 */
	public void addExtreValuesToModule(TSServerInfoModule module) {
		String value = getSearch().getSa().getAtribute(SearchAttributes.LD_LOTNO);
		module.forceValue(21, value);

		value = getSearch().getSa().getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
		module.forceValue(22, value);

		value = getSearch().getSa().getAtribute(SearchAttributes.LD_SUBDIV_SEC);
		module.forceValue(23, value);

		value = getSearch().getSa().getAtribute(SearchAttributes.LD_SUBDIV_TWN);
		module.forceValue(24, value);

		value = getSearch().getSa().getAtribute(SearchAttributes.LD_SUBDIV_RNG);
		module.forceValue(25, value);

	}

	@Override
	protected void setMessages() {
		getErrorMessages().addNoResultsMessages("Nothing Matching Your Search Criteria Was Found");
		// getErrorMessages().addNoResultsMessages("The first 500 results are displayed.");
	}

	protected ForwardingParseClass getParserInstance() {
		ParseInterface instance = ro.cst.tsearch.servers.functions.ARGenericCountyDataAO.getInstance();
		((ro.cst.tsearch.servers.functions.ARGenericCountyDataAO) instance).setSiteLink(getSiteLink(searchId));
		ForwardingParseClass forwardingParseClass = new ParseClassWithExceptionTreatment(instance);
		return forwardingParseClass;
	}

	@Override
	protected HashMap<String, String> putAdditionalData(String serverResult) {
		HashMap<String, String> data = new HashMap<String, String>();
		data.put("type", "ASSESSOR");
		data.put("dataSource", "AO");
		return data;
	}

	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		MessageFormat format = createPartialLinkFormat();
		response.getParsedResponse().setAttribute(TMP_SITE_TYPE, getSiteType());
		Vector<ParsedResponse> intermediary = getParserInstance().parseIntermediary(response, table, searchId, format);
		outputTable.append(table);
		return intermediary;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		map.put(TMP_SITE_TYPE, getSiteType());
		getParserInstance().parseDetails(detailsHtml, searchId, map);
		return null;
	}

	/**
	 * get file name from link
	 */
	@Override
	protected String getFileNameFromLink(String link) {
		String fileName = "File.html";
		Matcher dummyMatcher = dummyPattern.matcher(link);
		if (dummyMatcher.find()) {
			fileName = dummyMatcher.group(1);
			fileName = fileName + ".html";
		}
		return fileName;
	}

	/**
	 * 
	 */
	public void initTemplatedServerData() {
		int[] intermediary_cases = { ID_INTERMEDIARY, ID_SEARCH_BY_NAME };
		setIntermediaryCases(intermediary_cases);

		int[] details_cases = { TSServer.ID_DETAILS };
		setDetailsCases(details_cases);

		int[] save_cases = { TSServer.ID_SAVE_TO_TSD };
		setSAVE_CASES(save_cases);
		setDetailsMessage("Parcel Details");
		setIntermediaryMessage("Results ");

	}

	/**
	 * returns the type of the current site implementation. The type should be
	 * one of the values defined in @see {@link GWTDataSite} e.g.
	 * {@link GWTDataSite.AO_TYPE}
	 * 
	 * @return
	 */
	public abstract int getSiteType();

}
