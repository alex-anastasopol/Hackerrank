package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.ResultBodyUtils;

public class COWeldAO extends TemplatedServer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3950102660453512237L;

	public COWeldAO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);

		int[] intermediary_cases = {};
		setIntermediaryCases(intermediary_cases);
		int[] details_cases = { ID_SEARCH_BY_PARCEL };
		setDetailsCases(details_cases);
		// setINTERMEDIARY_MESSAGE("property records were found");
		setDetailsMessage("property records were found");
	}

	protected void setMessages() {
		getErrorMessages().addNoResultsMessages(
				"Your search returned 0 records");
	}

	protected String getAccountNumber(String serverResult) {
		String accountNumber = RegExUtils.getFirstMatch(
				"(?<=Parcel:</b>)\\s*\\d+(?=\\s<br>)", serverResult, 0);
		return accountNumber;
	}

	protected String cleanDetails(String response) {
		response = response
				.replaceAll(
						"(?is)<!DOCTYPE html PUBLIC.*Search Again</a></font><BR>\\s*</table>",
						"");
		response = response.replaceAll("(?is)<HR>.*</html>", "");
		return response;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		return intermediaryResponse;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap map) {
		HtmlParser3 parser = new HtmlParser3(detailsHtml);
		NodeList tag = HtmlParser3.getTag(parser.getNodeList(), new TableTag(),
				false);

		List<HashMap<String, String>> sourceSet = new LinkedList<HashMap<String, String>>();
		map.put("PropertyIdentificationSet.ParcelID", RegExUtils.getFirstMatch(
				"(?<=Account:</b>)\\s*\\w+(?=\\s?<br>)", detailsHtml, 0));
		map.put("PropertyIdentificationSet.ParcelID2",
				getAccountNumber(detailsHtml));

		if (tag != null && tag.size() == 1) {
			TableTag table = (TableTag) tag.elementAt(0);
			TableRow[] rows = table.getRows();
			for (TableRow tableRow : rows) {
				HashMap<String, String> tempMap = new HashMap<String, String>();
				String html = tableRow.toHtml();
				/*String account = RegExUtils.getFirstMatch(
						"(?<=Account:</b>)\\s*\\w+(?=\\s?<br>)", html, 0)
						.trim();
				String parcel = RegExUtils.getFirstMatch(
						"(?<=Parcel:</b>)\\s*\\d+(?=\\s<br>)", html, 0).trim();*/
				String reception = RegExUtils.getFirstMatch(
						"(?<=Reception:</b>)\\s*\\w+(?=\\s?<br>)", html, 0)
						.trim();
				String salePrice = RegExUtils
						.getFirstMatch("(?<=Sale Price:</b>)\\s*.*", html, 0)
						.trim().replaceAll("(,|\\$)", "");
				String grantor = RegExUtils.getFirstMatch(
						"(?is)(?<=Grantor:</b> <br>)\\s*.*?(?=\\s?<br>)", html,
						0).trim();
				String grantee = RegExUtils.getFirstMatch(
						"(?is)(?<=Grantee:</b> <br>)\\s*.*?(?=\\s?</td>)",
						html, 0).trim();
				String salesDate = RegExUtils
						.getFirstMatch(
								"(?is)(?<=Sales Date:</b>)\\s*.*?(?=\\s?<br>)",
								html, 0).trim();
				String docDate = RegExUtils.getFirstMatch(
						"(?is)(?<=Doc Date:</b>)\\s*.*?(?=\\s?<br>)", html, 0)
						.trim();
				String deedType = RegExUtils
						.getFirstMatch(
								"(?is)(?<=Deed Type:</b>)\\s*.*?(?=\\s?</td>)",
								html, 0).trim();

				tempMap.put("Grantor", grantor);
				tempMap.put("Grantee", grantee);
				// tempMap.put("DocumentNumber", reception);
				tempMap.put("InstrumentNumber", reception);
				tempMap.put("RecordedDate", docDate);
				tempMap.put("InstrumentDate", salesDate);
				tempMap.put("SalesPrice", salePrice);
				tempMap.put("DocumentType", deedType);

				sourceSet.add(tempMap);
			}

			String[] header = { "Grantor", "Grantee", "DocumentNumber",
					"InstrumentNumber", "RecordedDate", "InstrumentDate",
					"SalesPrice", "DocumentType" };
			ResultBodyUtils.buildInfSet(map, sourceSet, header,
					SaleDataSet.class);

			// make grantor set
			try {
				@SuppressWarnings("rawtypes")
				ArrayList<List> body = new ArrayList<List>();

				for (HashMap<String, String> m : sourceSet) {

					String name = m.get("Grantor");

					String[] names = StringFormats.parseNameNashville(name,
							true);
					String[] type = GenericFunctions.extractAllNamesType(names);
					String[] otherType = GenericFunctions
							.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions
							.extractNameSuffixes(names);
					GenericFunctions.addOwnerNames(names, suffixes[0],
							suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), body);

					// GenericFunctions.addOwnerNames(name, names, suffixes[0],
					// suffixes[1], type, otherType,
					// NameUtils.isCompany(names[2]),
					// NameUtils.isCompany(names[5]), body);
				}

				GenericFunctions.storeOwnerInPartyNames(map, body, true);
				// map.put("GrantorSet",
				// GenericFunctions.storeOwnerInSet(body));

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		ArrayList<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		String pin = getSearch().getSa().getAtribute(
				SearchAttributes.LD_PARCELNO);

		if (hasPin()) {
			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(pin);
			modules.add(module);
		}
		if (hasPin()) {
			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(1).forceValue(pin.replaceAll("-", ""));
			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);
	}

}
