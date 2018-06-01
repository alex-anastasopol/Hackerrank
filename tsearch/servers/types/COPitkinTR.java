package ro.cst.tsearch.servers.types;

import java.util.HashSet;
import java.util.Set;

import org.htmlparser.tags.TableColumn;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.StringUtils;

public class COPitkinTR extends COGenericTylerTechTR {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6964698492873019843L;

	public COPitkinTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	public COPitkinTR(long searchId) {
		super(searchId);
	}

	
	public String getDetailsPrefix() {
		return "/treasurer/treasurerweb/";
	}

	protected String clean(String result) {
		result = result.replaceAll("<br>For current year values visit the <a[^>]+>Pitkin County Assessor's site.</a>", "");
		return result;
	}
	
	public void parseLegal(String contents, ResultMap resultMap) throws Exception {
		ro.cst.tsearch.servers.functions.COPitkinTR.parseLegal(resultMap, searchId);
	}

	public void parseName(Set<String> hashSet, ResultMap resultMap) throws Exception {
		if (hashSet == null || hashSet.isEmpty()){
			String nameOnServer = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
			nameOnServer = nameOnServer.replaceAll("(?is)\\d+%", "").trim();
			if (!nameOnServer.matches("^\\w*(, \\w*)*( \\w* ?\\w?\\.?)?$") || 
					!nameOnServer.matches("^\\w* \\w* \\w & \\w* \\w?, \\w*(\\s\\w)? (& \\w* \\w)*$")){
				if (StringUtils.isNotEmpty(nameOnServer)){
					nameOnServer = nameOnServer.replaceAll("(?is)&amp;", "&");
					String[] split = nameOnServer.split(",");
					hashSet = new HashSet<String>(); 
					for (String string : split) {
						hashSet.add(string);
					}
					ro.cst.tsearch.servers.functions.COPitkinTR.parseName(hashSet, resultMap);
				}
			}else{
				hashSet.add(nameOnServer);
			}
		}else{
			ro.cst.tsearch.servers.functions.COWeldTR.parseName(hashSet, resultMap);
		}
	}

	public void parseAddress(String address, ResultMap resultMap) throws Exception {
		String addressOnServer = (String) resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		if (StringUtils.isNotEmpty(addressOnServer)){
			ro.cst.tsearch.servers.functions.COMesaTR.parseAddress(resultMap, addressOnServer);
		}else{
			ro.cst.tsearch.servers.functions.COMesaTR.parseAddress(resultMap, address);
		}
	}

	public void parseIntermediaryRow(COGenericTylerTechTR server, ResultMap resultMap, TableColumn[] cols) throws Exception {
		ro.cst.tsearch.servers.functions.COPitkinTR.parseIntermediaryRow(server, resultMap, cols);
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		super.parseAndFillResultMap(response, detailsHtml, map);
		HtmlParser3 htmlParser3 = new HtmlParser3(detailsHtml);
		NodeList nodeList = htmlParser3.getNodeList();
		
		//property appraisal set
		NodeList landImprovementsTable = HtmlParser3.getNodeByTypeAttributeDescription(nodeList, "table", "class",
				"account stripe",
				new String[] { "Property Code", "Value Type", "Actual", "Assessed" },
				true).getChildren();
		if (landImprovementsTable.size() > 0) {
			String land = HtmlParser3.getValueFromAbsoluteCell(0, 3, HtmlParser3.findNode(landImprovementsTable, "LAND"), "", true)
					.replaceAll("[ $,-]", "")
					.trim();
			if (!land.isEmpty()) {
				map.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), land);
			}
			String improvement = HtmlParser3.getValueFromAbsoluteCell(0, 3, HtmlParser3.findNode(landImprovementsTable, "IMPROVEMENT"), "", true)
					.replaceAll("[ $,-]", "")
					.trim();
			if (improvement.isEmpty()) {
				improvement = HtmlParser3.getValueFromAbsoluteCell(0, 3, HtmlParser3.findNode(landImprovementsTable, "CONDOS-IMPROVMENTS"), "", true)
						.replaceAll("[ $,-]", "")
						.trim();
			}
			if (!improvement.isEmpty()) {
				map.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), improvement);
			}
		}
		return null;
	}
	
	@Override
	protected String getDetails(String response, String originalLink, String accountNo) {
		String result = super.getDetails(response, originalLink, accountNo);
		return result.replaceAll("(?i)<a.*?>(.*?)</a>", "$1");
	}
}
