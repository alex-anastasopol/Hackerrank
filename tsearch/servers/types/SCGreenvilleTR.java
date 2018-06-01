package ro.cst.tsearch.servers.types;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.datatrace.Utils;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parentsitedescribe.ParentSiteEditorUtils;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
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
import ro.cst.tsearch.utils.StringUtils;

public class SCGreenvilleTR extends TemplatedServer {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 764007772945499933L;

	public SCGreenvilleTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		int[] details_cases = { ID_DETAILS };
		setDetailsCases(details_cases);

		String details_message = "Real Property Detail Information";
		setDetailsMessage(details_message);

		int[] intermediary_cases = { ID_INTERMEDIARY, ID_SEARCH_BY_NAME };
		setIntermediaryCases(intermediary_cases);

		String[] intermediary_message = new String[0];
		setIntermermediaryMessages(intermediary_message);

	}

	@Override
	protected String clean(String response) {
		String firstMatch = RegExUtils.getFirstMatch(
				"(?is)(<table border='1' width='100%'>.*?</table>)\\s*<table border='1' width='100%'>", response, 1);
		firstMatch = firstMatch.replaceAll("</TR>", "</tr>");
		return firstMatch;
	}

	@Override
	protected void ParseResponse(String action, ServerResponse response, int viParseID) throws ServerResponseException {
		if (isInArray(viParseID, getINTERMEDIARY_CASES()) != Integer.MIN_VALUE
				&& checkIfResultContainsMessage(response.getResult(), getDetailsMessages())) {
			super.ParseResponse(action, response, ID_DETAILS);
		} else {
			super.ParseResponse(action, response, viParseID);
		}
	}

	/**
	 * It also gets two other pages that contain sales data and appraissal data.
	 * 
	 */
	@Override
	protected String cleanDetails(String response) {
		String returnVal = RegExUtils.getFirstMatch("(?is)tr>\\s*(<table border=\"1\" width=\"100%\">.*?</table>)\\s*<p", response, 1);

		StringBuffer result = new StringBuffer();
		
		result.append("<table id=details width=95% align=center>");
		result.append("<tr><td><p id=\"fakeHeader\" align=\"center\" width=100%><font style=\"font-size:xx-large;\"><b>SC Greenville County</b></font></p>"
					+ "<br></td></tr><tr><td>");
		
		if (StringUtils.isNotEmpty(returnVal)) {

			returnVal = returnVal.replaceAll("(?is)<td style=\"width:100%\">For Property Tax Information <a href=\"../voTaxQry/\">Click Here</a>\\.\\s*</td>", "");

			String mapNum = getAccountNumber(response);
			String cleanedMapNum = mapNum;

			// make a request to get Appraissal info for SaleDataSet
			HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			String appraiserPage = "";
			String taxHistoryPage = "";
			
			try {
				String link = "http://www.gcgis.org/webmappub/zoomToParcel.aspx?pin=" + cleanedMapNum;
				appraiserPage = ((ro.cst.tsearch.connection.http2.SCGreenvilleTR) site).getAppraiserPage(link, HTTPRequest.GET);
				
				String taxHistoryLink = "http://www.greenvillecounty.org/voTaxQry/wcMain.ASP?WCI=Process&WCE=submit&MapNum=" + mapNum;
				taxHistoryPage = ((ro.cst.tsearch.connection.http2.SCGreenvilleTR) site).getAppraiserPage(taxHistoryLink, HTTPRequest.POST);
				
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				HttpManager.releaseSite(site);
			}

			if(StringUtils.isNotEmpty(taxHistoryPage)){
				// get table from page
				String replaceString = "<table border=\"1\"";
				String firstMatch = RegExUtils.getFirstMatch("(?is)" + replaceString + " width=\"100%\">.*?</table>", taxHistoryPage, 0);
				taxHistoryPage = firstMatch.replaceAll(replaceString, replaceString + "id=\"taxHistoryId\"");
			}

			String parcelHistoryPage = "";
			String assessmentHistoryPage = "";
			
			if (!appraiserPage.contains("No parcels found for PIN")) {
				//get table from page
				appraiserPage = RegExUtils.getFirstMatch("(?is)<table class=\"idTbl\".*?</table>", appraiserPage, 0);
				appraiserPage = appraiserPage.replaceAll("(?is)<img.*?>", "");

				String objectId = RegExUtils.getFirstMatch("(?is)ObjectID=(.*?)\"", appraiserPage, 1);

				if (StringUtils.isNotEmpty(objectId)) {
					
					site = HttpManager.getSite(getCurrentServerName(), searchId);
					
					
					
					try {
						String parcelHistoryURL = "http://www.gcgis.org/webmappub/" + "parcelHistory.aspx?ObjectID=" + objectId;
						parcelHistoryPage = ((ro.cst.tsearch.connection.http2.SCGreenvilleTR) site).getAppraiserPage(parcelHistoryURL, HTTPRequest.GET);
						
						String assessmentHistoryURL = "http://www.gcgis.org/webmappub/" + "assessmentHistory.aspx?ObjectID=" + objectId;
						assessmentHistoryPage = ((ro.cst.tsearch.connection.http2.SCGreenvilleTR) site).getAppraiserPage(assessmentHistoryURL, HTTPRequest.GET);
						
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						HttpManager.releaseSite(site);
					}
					
					if(StringUtils.isNotEmpty(parcelHistoryPage)){
						//get table from page
						parcelHistoryPage = RegExUtils.getFirstMatch("(?is)<table rules=\"groups\".*?</table>", parcelHistoryPage, 0);
					}
					
					if(StringUtils.isNotEmpty(assessmentHistoryPage)){
						//get table from page
						assessmentHistoryPage = RegExUtils.getFirstMatch("(?is)<table rules=\"groups\".*?</table>", assessmentHistoryPage, 0);
					}
				}
			}
			
			result.append("<table id=retVal width=95% align=center><tr><td>" + returnVal.replaceAll("(?ism)<tr>[\\n\\r\\t\\s]*</tr>","<tr><td></td></tr>") + "</td></tr></table>");
			if (StringUtils.isNotEmpty(appraiserPage))
				result.append("<table id=appraiserPage width=95% align=center><tr><td>" + appraiserPage + "</td></tr></table>");
			if (StringUtils.isNotEmpty(taxHistoryPage))
				result.append("<table id=taxHistoryPage width=95% align=center><tr><td>" + taxHistoryPage + "</td></tr></table>");
			if (StringUtils.isNotEmpty(parcelHistoryPage))
				result.append("<table id=parcelHistoryPage width=95% align=center><tr><td>" + parcelHistoryPage + "</td></tr></table>");
			if (StringUtils.isNotEmpty(assessmentHistoryPage))
				result.append("<table id=assessmentHistoryPage width=95% align=center><tr><td>"+ assessmentHistoryPage + "</td></tr></table>");
			
			
		} else {
			result.append(response);
		}
		
		result.append("</td></tr></table>");
		
		return result.toString().replaceAll("(?is)<a.*?>(.*?)</a>", "$1").replaceAll("(?is)<input[^>]*>", "");
	}

	protected String addInfoToDetailsPage(ServerResponse response, String serverResult, int viParseID) {

		return serverResult;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();

		PinFilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);

		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.7d);
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);

		int currentYear = Calendar.getInstance().get(Calendar.YEAR);
		if (hasPin()) {
			TSServerInfoModule tsServerInfoModule = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			tsServerInfoModule.clearSaKeys();

			tsServerInfoModule.forceValue(10, "" + currentYear);
			String id = getSearchAttribute(SearchAttributes.LD_PARCELNO_GENERIC_TR);
			tsServerInfoModule.forceValue(3, "" + id);
			l.add(tsServerInfoModule);
		}
		if (hasStreet()) {
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.forceValue(10, "" + currentYear);
			module.forceValue(2, "" + getSearchAttribute(SearchAttributes.P_STREETNAME));
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			l.add(module);
		}

		if (hasOwner()) {
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));

			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.forceValue(10, "" + currentYear);
			module.addFilter(pinFilter);
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, new String[] {"L;F;"});
			module.addIterator(nameIterator);
			
			l.add(module);
		}

		serverInfo.setModulesForAutoSearch(l);

	}

	@Override
	protected String getAccountNumber(String serverResult) {
		String firstMatch = RegExUtils.getFirstMatch("(?is)Map #.*?(\\w+)</td>", serverResult, 1);
		return firstMatch;
	}

	@Override
	protected void setMessages() {
		getErrorMessages().addNoResultsMessages("No Records Found");
		getErrorMessages().addServerErrorMessage("Session Has Expired");
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		getParserInstance().parseDetails(detailsHtml, searchId, map);
		return null;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		MessageFormat format = createPartialLinkFormat();
		return getParserInstance().parseIntermediary(response, table, searchId, format);
	}

	@Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
		if (tsServerInfoModule != null && tsServerInfoModule.getFunctionList() != null && tsServerInfoModule.getFunctionList().size() > 7) {
			int currentYear = Calendar.getInstance().get(Calendar.YEAR);
			String defaultOption = "<option value=\"{0}\" selected=\"selected\">{1}</option>";
			String generateYearSelectOptions = ParentSiteEditorUtils.generateYearSelectOptions("SelectYear", 1995, currentYear + 3,
					MessageFormat.format(defaultOption, "" + currentYear, "" + currentYear));

			Utils.setupSelectBox(tsServerInfoModule.getFunction(10), generateYearSelectOptions);

			generateYearSelectOptions = ParentSiteEditorUtils.generateYearSelectOptions("SelectSalesYear", 1995, currentYear + 3,
					MessageFormat.format(defaultOption, "All", "All"));

			Utils.setupSelectBox(tsServerInfoModule.getFunction(8), generateYearSelectOptions);
			tsServerInfoModule.getFunction(10).setDefaultValue("" + currentYear);

		}
		return super.getDefaultServerInfo();
	}

	public ro.cst.tsearch.servers.functions.SCGreenvilleTR getParserInstance() {
		ro.cst.tsearch.servers.functions.SCGreenvilleTR instance = ro.cst.tsearch.servers.functions.SCGreenvilleTR.getInstance();
		return instance;
	}

	@Override
	protected HashMap<String, String> putAdditionalData(String serverResult) {
		HashMap<String, String> data = new HashMap<String, String>();
		data.put("type", "CNTYTAX");
		return data;
	}

}
