package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.servers.functions.ForwardingParseClass;
import ro.cst.tsearch.servers.functions.ParseInterface;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.RegExUtils;

public class UTDavisTR extends TemplatedServer {

	public UTDavisTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		int[] intermediary_cases = { ID_INTERMEDIARY, ID_SEARCH_BY_ADDRESS, ID_SEARCH_BY_MODULE30 };
		setIntermediaryCases(intermediary_cases);

		int[] details_cases = { TSServer.ID_DETAILS, TSServer.ID_SEARCH_BY_PARCEL };
		setDetailsCases(details_cases);

		int[] save_cases = { TSServer.ID_SAVE_TO_TSD };
		setSAVE_CASES(save_cases);
		setDetailsMessage("Tax Information");
		setIntermediaryMessage("Results ");
	}

	@Override
	protected void setMessages() {
		getErrorMessages().addNoResultsMessages("No Records Found for this search criteria");
	}

	@Override
	protected String getAccountNumber(String serverResult) {
		int offSetColumn = 1;
		int offSetRow = 0;
		HtmlParser3 parser = new HtmlParser3(serverResult);
		String parcelID = ro.cst.tsearch.servers.functions.UTDavisTR.getInstance().getParcelID(offSetColumn, offSetRow, parser);
		if (StringUtils.isNotEmpty(parcelID)){
			parcelID = parcelID.trim();
		}
		return parcelID;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		ForwardingParseClass instance = getParserInstance();
		instance.parseDetails(detailsHtml, searchId, map);
		return null;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		ForwardingParseClass instance = getParserInstance();
		MessageFormat format = createPartialLinkFormat();
		Vector<ParsedResponse> parseIntermediary = instance.parseIntermediary(response, table, searchId, format);
		return parseIntermediary;
	}

	private ForwardingParseClass getParserInstance() {
		ParseInterface instance = ro.cst.tsearch.servers.functions.UTDavisTR.getInstance();
		ForwardingParseClass forwardingParseClass = new ForwardingParseClass(instance);
		return forwardingParseClass;
	}

	@Override
	protected String clean(String response) {
		response = response.replaceAll("(?is)<span onClick=\" show_popup_box_.*Departments</span>", "");
		response = response.replaceAll("(?is)<script>.*?</script>", "");

		HtmlParser3 parser3 = new HtmlParser3(response);

		response = response.replaceAll(
				"(?is)<span onClick=\" show_popup_box_.*?more.*?<td style=\"text-align:left;\">(.*?)</td>.*?</div>.*?</div>", "$1</td>");

		response = RegExUtils.getFirstMatch("(?is)<table style=\"width:100%;\">.*?</table>", response, 0);
		return response;
	}

	@Override
	protected String cleanDetails(String response) {
		String firstMatch = RegExUtils.getFirstMatch(
				"(?is)<table class=\"border-light_bgcolor-transparent\" style=\"width:100%;\">.*Property Information.*</table>", response,
				0);
		firstMatch = firstMatch.replaceAll("(?is)<script>.*?</script>", "");
		firstMatch = firstMatch.replaceAll("(?is)<input type='button.*?>", "");

		// remove clarifications row
		firstMatch = firstMatch
				.replaceAll(
						"(?is)<tr>\\s*<td class=\"border-light-all_bgcolor\" colspan=\"5\">\\s*<p style=\"font-weight:bold;\">Important Clarifications.*?</tr>",
						"");

		// remove terms of use
		firstMatch = firstMatch.replaceAll("(?is)<table style=\"margin-bottom:5px;\">.*?</table>", "");

		// remove copyright link
		firstMatch = firstMatch.replaceAll("(?is)<tr>\\s*<th class=\"border-dark_bgcolor-medium-light\" .*?</tr>", "");

		firstMatch = firstMatch.replaceAll("(?is)<p><A href=\"#top\">Top</A></p>", "");

		response = firstMatch;
		return response;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		String streetNo = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNO);
		String streetName = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNAME);

		String pin = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO);
		PinFilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
		GenericAddressFilter addressFilter = AddressFilterFactory.getGenericAddressHighPassFilter(searchId, 0.5d);
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
		((GenericNameFilter) defaultNameFilter).setIgnoreMiddleOnEmpty(true);

		FilterResponse rejectNonRealEstateFilter = new RejectNonRealEstate(SearchAttributes.PROPERTY_TYPE, searchId);
		rejectNonRealEstateFilter.setThreshold(new BigDecimal("0.99"));
		if (hasPin()) {
			// Search by PIN
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			
			String pinRegEx = "(\\d{2,2})(\\d{3,3})(\\d{3,4})";
			if (RegExUtils.matches(pinRegEx, pin)) {
				pin = pin.replaceAll(pinRegEx, "$1-$2-$3");
			}
			
			module.getFunction(0).forceValue(pin);
			Set<String> parcelNumber = new HashSet<String>();
			parcelNumber.add(pin);
			pinFilter.setParcelNumber(parcelNumber);

			module.addFilter(pinFilter);
			module.addFilter(addressFilter);

			// module.addFilter(rejectNonRealEstateFilter);
			// module.addFilter(new TaxYearFilterResponse(searchId));
			modules.add(module);
		} else if (hasStreet() || hasStreetNo()) {

			// Search by Property Address
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(streetNo);
			module.getFunction(1).forceValue(streetName);
			// module.addFilter(new TaxYearFilterResponse(searchId));
			// module.addFilter(rejectNonRealEstateFilter);
			// module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			module.addFilter(pinFilter);
			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);

	}
	
	@Override
	protected HashMap<String, String> putAdditionalData(String serverResult) {
		HashMap<String, String> data = new HashMap<String, String>();
		data.put("type", "CNTYTAX");
		
		int offSetColumn =1;
		int offSetRow = 0;
		HtmlParser3 p = new HtmlParser3(serverResult);
		String year = ro.cst.tsearch.servers.functions.UTDavisTR.getInstance().getYear(offSetColumn, offSetRow, p); 
		data.put("year", year);
		return data;
	}
}
