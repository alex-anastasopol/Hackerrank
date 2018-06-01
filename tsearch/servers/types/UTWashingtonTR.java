package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.functions.ForwardingParseClass;
import ro.cst.tsearch.servers.functions.ParseInterface;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.RegExUtils;

public class UTWashingtonTR extends TemplatedServer {

	private static final long serialVersionUID = 2783569494203752074L;

	public UTWashingtonTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		int[] intermediaryCases = new int[] { };
		setIntermediaryCases(intermediaryCases);
		int[] link_cases = new int[] { ID_GET_LINK };
		setLINK_CASES(link_cases);
		int[] save_cases = new int[] { ID_SAVE_TO_TSD };
		setSAVE_CASES(save_cases);
		int[] details_cases = new int[] { ID_DETAILS, ID_SEARCH_BY_INSTRUMENT_NO, ID_SEARCH_BY_SERIAL_ID};
		setDetailsCases(details_cases);

		setDetailsMessage("Primary Owner");

	}

	@Override
	protected String cleanDetails(String response) {
		StringBuilder r = new StringBuilder("");
		r.append(RegExUtils.getFirstMatch("(?is)<table width=\"600\">.*.*</table>", response, 0));
		String firstMatch = RegExUtils.getFirstMatch("(?is)</table>(.*)<p>", response, 1);
		firstMatch = firstMatch.replaceAll("(?is)<a hre.*</a>", "");
		r.append(firstMatch);
		return r.toString();
	}
	
	@Override
	protected String getAccountNumber(String serverResult) {
		String label = "Account Number:";
		String extractValueForLabel = ro.cst.tsearch.servers.functions.UTWashingtonTR.getInstance().extractValueForLabel(serverResult, label);
		return  extractValueForLabel;
	}
	
	@Override
	protected HashMap<String, String> putAdditionalData(String serverResult) {
		HashMap<String, String> data = new HashMap<String, String>();
		data.put("type", "CNTYTAX");
//		String year = instance.getCurrentYear(serverResult);
//		data.put("year", year);
		return data;
	}
	
	@Override
	protected void setMessages() {
		getErrorMessages().addNoResultsMessages("There is no entry for the number you searched for");
	}
		
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		getParserInstance().parseDetails(detailsHtml, searchId, map);
		return null;
	}
	
	private ForwardingParseClass getParserInstance() {
		ParseInterface instance = ro.cst.tsearch.servers.functions.UTWashingtonTR.getInstance();
		ForwardingParseClass forwardingParseClass = new ForwardingParseClass(instance);
		return forwardingParseClass;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		TSServerInfoModule module = null;
		String pin = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO);
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		if (hasPin()) {
			// Search by PIN
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(pin);
			Set<String> parcelNumber = new HashSet<String>();
			parcelNumber.add(pin);
			modules.add(module);
		}
		serverInfo.setModulesForAutoSearch(modules);
	}

}
