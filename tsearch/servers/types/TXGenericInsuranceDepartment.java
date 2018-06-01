package ro.cst.tsearch.servers.types;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.htmlparser.Node;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

public class TXGenericInsuranceDepartment extends TemplatedServer {

	public TXGenericInsuranceDepartment(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink,
			long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		int[] intermediary_cases = { ID_INTERMEDIARY, ID_SEARCH_BY_NAME };
		setIntermediaryCases(intermediary_cases);

		int[] details_cases = { TSServer.ID_DETAILS };
		setDetailsCases(details_cases);

		int[] save_cases = { TSServer.ID_SAVE_TO_TSD };
		setSAVE_CASES(save_cases);

		setIntermediaryMessage("Company Name beginning with");
		setDetailsMessage("General Information");

	}
	
	@Override
	protected String clean(String response) {
		HtmlParser3 parser = new HtmlParser3(response);
		NodeList nodeList = parser.getNodeList();
		Node nodeByTypeAndAttribute = HtmlParser3.getNodeByTypeAndAttribute(nodeList, "table", "border", "0", true);
		return nodeByTypeAndAttribute.toHtml();
	}
	
	@Override
	protected String cleanDetails(String response) {
		//remove all images
		response = response.replaceAll("<img.*?>", "");
		//remove the table show explanation of terms
		response = response.replaceAll("(?is)<table align=\"center\".*?</table>", "");
		response = response.replaceAll("(?is)<table bgcolor=\"#ddeeee\".*?</table>", "");
		response = response.replaceAll("(?is)<a.*?</a>", "");
		response = response.replaceAll("(?is)<!DOCTYPE HTML.*?(?=<body)", "");
		response = response.replaceAll("(?is)</body>|</html>", "");
		response = response.replaceAll("(?is)<input.*?/>", "");
		return response;
	}
	
	@Override
	protected String addInfoToDetailsPage(ServerResponse response, String serverResult, int viParseID) {
		if (isInArray(viParseID, getDETAILS_CASES()) != Integer.MIN_VALUE){
			URI lastURI = response.getLastURI();
			try {
				String pathQuery = lastURI.getPathQuery();
				String tdiNum = RegExUtils.getFirstMatch("tdiNum=(.*?)&", pathQuery, 1);
				String companyName = RegExUtils.getFirstMatch("companyName=(.*?)&", pathQuery, 1);
				String sysTypeCode = RegExUtils.getFirstMatch("sysTypeCode=(.*)", pathQuery, 1);
				
				String format = "<input type=\"hidden\" name='%s' value='%s'/>";
				serverResult = 
					serverResult + String.format(format, "tdiNum", tdiNum);
				serverResult = 
					serverResult + String.format(format, "companyName", companyName);
				serverResult = 
					serverResult + String.format(format, "sysTypeCode", sysTypeCode);
				
			} catch (URIException e) {
				e.printStackTrace();
			}
		}
		return serverResult;
	}
	
	@Override
	protected String getAccountNumber(String serverResult) {
		String match = RegExUtils.getFirstMatch("(?ism)TDI Company Number:.*?<td[^>]*>(.*?)</td>\\s*</tr>", serverResult, 1);
		String accountNumber = StringUtils.cleanHtml(match);
		if (StringUtils.isEmpty(accountNumber)){
			accountNumber = RegExUtils.getFirstMatch("name='tdiNum' value='(.*?)'/>", serverResult, 1);
		}
		return accountNumber;
	}
	
	
	@Override
	protected void setMessages() {
	}
	
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		MessageFormat format = createPartialLinkFormat();
		return getInstance().parseIntermediary(response, table, searchId, format) ;
	} 
	
	public ro.cst.tsearch.servers.functions.TXGenericInsuranceDepartment getInstance(){
		return ro.cst.tsearch.servers.functions.TXGenericInsuranceDepartment.getInstance();
	}
	
	@Override
	protected HashMap<String, String> putAdditionalData(String serverResult) {
		HashMap<String,String> dataMap =null;
		if (dataMap ==null){
			dataMap = new HashMap<String,String>();
		}
		dataMap.put("type", "CORPORATION");
		return dataMap;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		getInstance().parseDetails(detailsHtml, searchId, map);
		return null;
	}
}
