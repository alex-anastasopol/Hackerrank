package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.math.NumberUtils;
import org.htmlparser.Node;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

public class FLGenericQPublicAO extends TemplatedServer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public FLGenericQPublicAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);

		setIntermediaryMessage("Search Criteria:");
		setDetailsMessage("County Record Search");

		int[] intermediaryCases = new int[] { ID_SEARCH_BY_PARCEL, ID_SEARCH_BY_NAME, ID_SEARCH_BY_ADDRESS, ID_SEARCH_BY_SALES, ID_INTERMEDIARY };
		setIntermediaryCases(intermediaryCases);

		int[] link_cases = new int[] { ID_GET_LINK };
		setLINK_CASES(link_cases);

		int[] save_cases = new int[] { ID_SAVE_TO_TSD };
		setSAVE_CASES(save_cases);

		int[] details_cases = new int[] { ID_DETAILS };
		setDetailsCases(details_cases);

	}

	protected String getAccountNumber(String serverResult) {
		HtmlParser3 parser = new HtmlParser3(serverResult);
		String nodeValue = getParserInstance().getNodeValue(parser, "PARCEL NUMBER");
		String parcelId = StringUtils.cleanHtml(nodeValue);
		return parcelId;
	}

	protected void setMessages() {
		getErrorMessages().addNoResultsMessages("No data posted for search criteria.");
	}

	@Override
	protected String clean(String response) {
		response = response.replaceAll("<td.*<a href=.*?Map It</a></td>", "");
		return response;
	}

	@Override
	protected HashMap<String, String> putAdditionalData(String serverResult) {
		HashMap<String, String> data = new HashMap<String, String>();
		data.put("type", "ASSESSOR");
		// data.put("Year", "" + currentYear);
		return data;
	}

	@Override
	protected String cleanDetails(String response) {
		
		//clean the center tags that will mess up further cleaning 
		response = response.replaceAll("(?is)<center>(<A HREF.*</A>)</center>", "$1");
		response = response.replaceFirst("(?is)<CENTER>\\s*<table[^>]*>\\s*<tr[^>]*>\\s*<th[^>]*>\\s*<img[^>]*>\\s*.*?</CENTER>", "");
		
		// clean the header
		response = RegExUtils.getFirstMatch("(?is)<CENTER>(.*?)</?CENTER>", response, 1);
		String headerToBeRemoved = RegExUtils.getFirstMatch("(?is)<TABLE BORDER COLS=1 WIDTH=\"100%\">.*?\"(header_link)\".*?</TABLE>",
				response, 0);
		headerToBeRemoved = headerToBeRemoved.replaceAll("(\\?|\\-)", "\\\\$1");

		response = response.replaceAll("(?is)" + headerToBeRemoved, "");

		// remove the disclaimer
		response = response.replaceAll("(?is)<TABLE BORDER COLS=1 WIDTH=\"100%\">\\s*<TR><TD class=\"(disclaimer)\".*?</TABLE>", "");

		// check to see if it has link for legal is in initial response
		boolean containsLongLegalLink = false;
		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		try{
		String legalLink = ((ro.cst.tsearch.connection.http2.FLGenericQPublicAO) site).getLegalLink();
		if (StringUtils.isNotEmpty(legalLink) && response.contains(legalLink)) {
			containsLongLegalLink = true;
		}
		}catch (Exception e){
			e.printStackTrace();
		}finally{
			HttpManager.releaseSite(site);
		}

		// remove all the links
		response = response.replaceAll("(?is)<a\\b[^>]*>\\s*Clerks?\\s+Documents\\s*</a>", "");
		response = response.replaceAll("(?is)<a\\b[^>]*>(.*?)</a>", "$1");

		// remove the images
		response = response.replaceAll("(?is)<img.*?>", "");

		// remove all the buttons
		response = response.replaceAll("(?is)<button[^>]*>.*?</button>", "");

		if (containsLongLegalLink){
			// get the complete legal before cleaning images and links
			response = getCompleteLegal(response, containsLongLegalLink);
		}

		response = "<table id=\"details\" align=\"center\" border=\"1\"><tr><td>" + response + "</td></tr></table>";
		return response;
	}

	protected String getCompleteLegal(String serverResult, boolean containsLonlLegalLink) {
		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		String completeLegalPage = "";
		String accountId = getAccountNumber(serverResult);

		String html = serverResult;
		if (containsLonlLegalLink) {
			try {
				if (StringUtils.isNotEmpty(accountId)) {
					HtmlParser3 parser = new HtmlParser3(serverResult);
					Node legalTD = HtmlParser3.getNodeByTypeAndAttribute(parser.getNodeList(), "TD", "class", "legal_value", true);
					// get the legal page
					completeLegalPage = ((ro.cst.tsearch.connection.http2.FLGenericQPublicAO) site).getCompleteLegalPage(accountId);
					if (StringUtils.isNotEmpty(completeLegalPage)){
						// clean and parse the new legal page
						completeLegalPage = RegExUtils.getFirstMatch("(?s)<table.*</table>", completeLegalPage, 0);
						completeLegalPage = RegExUtils.getFirstMatch("(?s)<td><font.*>(.*?)</font></b></td>", completeLegalPage, 0);

						// replace the short legal with the long legal
						serverResult = serverResult.replace(legalTD.toHtml(), completeLegalPage);
						html = serverResult;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				HttpManager.releaseSite(site);
			}
			
		}
		return html;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		MessageFormat format = createPartialLinkFormat();

		Vector<ParsedResponse> parseIntermediary = getParserInstance().parseIntermediary(response, table, searchId, format);

		String footer = response.getParsedResponse().getFooter();
		String[] links = processLinks(response);
		footer += "<br><center>" + links[2] + "  " + links[0] + " " + "</center>";
		response.getParsedResponse().setFooter(footer);
		response.getParsedResponse().setNextLink(links[1]);

		return parseIntermediary;
	}

	private String[] processLinks(ServerResponse response) {
		String baseLink = CreatePartialLink(TSConnectionURL.idGET);
		String linkNextRegEx = "(?i)<a\\shref=\\'(.*)\\'.*Search Next 50 Parcels.*</a>";
		List<String> linkNextComponents = RegExUtils.getFirstMatch(linkNextRegEx, response.getResult(), 0, 1);
		String aHrefTag = "";
		String hrefValue = "";
		String newHrefValue = "";
		String previousLink = "";

		if (linkNextComponents.size() == 2) {
			aHrefTag = linkNextComponents.get(0);
			hrefValue = linkNextComponents.get(1);
			newHrefValue = baseLink + hrefValue;

			String startIndexRegEx = "BEGIN=(\\d+)";
			String beginPageIndex = RegExUtils.getFirstMatch(startIndexRegEx, hrefValue, 1);
			int resultsPerPage = 50;
			if (!"50".equals(beginPageIndex.trim())) {
				if (NumberUtils.isNumber(beginPageIndex)) {
					int previousStart = Integer.parseInt(beginPageIndex) - resultsPerPage;
					String prevHrefValue = newHrefValue.replaceAll(startIndexRegEx, "BEGIN=" + previousStart);
					previousLink = aHrefTag.replace(hrefValue, prevHrefValue).replace(" Next ", " Previous ");
				}
			}

			aHrefTag = aHrefTag.replace(hrefValue, newHrefValue);

		}
		return new String[] { aHrefTag, newHrefValue, previousLink };
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		getParserInstance().parseDetails(detailsHtml, searchId, map);
		return null;
	}

	public ro.cst.tsearch.servers.functions.FLGenericQPublicAO getParserInstance() {
		return ro.cst.tsearch.servers.functions.FLGenericQPublicAO
				.getInstance(ro.cst.tsearch.servers.functions.FLGenericQPublicAO.FlGenericQPublicAOParseType.DefaultGenericQPublicAOParse);
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		PinFilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.7d);
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
		TaxYearFilterResponse fr = new TaxYearFilterResponse(searchId);
		fr.setThreshold(new BigDecimal("0.95"));

		TSServerInfoModule module = null;
		if (hasPin()) {
			l.add(new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX)));
		}

		if (hasStreet() || hasStreetNo()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(0, SearchAttributes.P_STREETNO);
			module.setSaKey(1, SearchAttributes.P_STREETNAME);

			module.addFilter(pinFilter);
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			l.add(module);
		}

		if (hasOwner()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);

			module.addFilter(pinFilter);
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);

			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);

			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(
					module, searchId, new String[] { "L;F;", "L; F M;", "L; f;", "L; m;" });
			module.addIterator(nameIterator);
			l.add(module);
		}

		serverInfo.setModulesForAutoSearch(l);
	}

	@Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX);
		if (tsServerInfoModule != null) {
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					htmlControl.setFieldNote(getParcelIdFieldNode());
				}
			}
		}
		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}

	protected String getParcelIdFieldNode() {
		return "Parcel Number with Dashes: 04589-000R<br>Parcel Number without Dashes: 04589000R";
	}

}
