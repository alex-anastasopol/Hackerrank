package ro.cst.tsearch.servers.types;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.HasChildFilter;
import org.htmlparser.filters.NotFilter;
import org.htmlparser.filters.StringFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.SelectTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

public class XXGenericFDIC extends TemplatedServer {
	
	private static String bhcPeriod;
	private static String bhcSortBy;
	private static String instPeriod;
	

	private static final long serialVersionUID = 1L;

	public XXGenericFDIC(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		int[] details_cases = { ID_DETAILS };
		setDetailsCases(details_cases);

		int[] intermediary_cases = { ID_INTERMEDIARY, ID_SEARCH_BY_MODULE19, ID_SEARCH_BY_MODULE20, ID_SEARCH_BY_MODULE21 };
		setIntermediaryCases(intermediary_cases);

		String[] details_message = new String[] { "-- Detail", "Offices and Branches of", "Ez Summary" };
		setDetailsMessages(details_message);
		CharSequence[] intermediary_message = new String[] { "Institutions found", "Total Offices found within criteria" };
		setIntermermediaryMessages(intermediary_message);
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		if(bhcPeriod == null) {
			String bhcPage = getLinkContents("http://www2.fdic.gov/idasp/frm_bhc.asp?sCurrentPg=Find%20Bank%20Holding%20Companies");
			if(bhcPage != null) {
				HtmlParser3 bhcParser = new HtmlParser3(bhcPage);
				NodeList selectPeriodList = bhcParser.getNodeList()
						.extractAllNodesThatMatch(new TagNameFilter("select"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("name", "Period1"));
				
				if(selectPeriodList != null && selectPeriodList.size() > 0) {
					SelectTag selectPeriod = (SelectTag) selectPeriodList.elementAt(0);
					selectPeriod.removeAttribute("onchange");					
					bhcPeriod = selectPeriod.toHtml();
				}
				
				selectPeriodList = bhcParser.getNodeList()
						.extractAllNodesThatMatch(new TagNameFilter("select"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("name", "inSort"));
				
				if(selectPeriodList != null && selectPeriodList.size() > 0) {
					SelectTag selectPeriod = (SelectTag) selectPeriodList.elementAt(0);
					selectPeriod.removeAttribute("onchange");					
					bhcSortBy = selectPeriod.toHtml();
				}
			}
		}
		
		
		if(instPeriod == null) {
			String instPage = getLinkContents("http://www2.fdic.gov/idasp/frm_inst.asp");
			if(instPage != null) {
				HtmlParser3 bhcParser = new HtmlParser3(Tidy.tidyParse(instPage, null));
				NodeList selectPeriodList = bhcParser.getNodeList()
						.extractAllNodesThatMatch(new TagNameFilter("select"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("name", "Period1"));
				
				if(selectPeriodList != null && selectPeriodList.size() > 0) {
					SelectTag selectPeriod = (SelectTag) selectPeriodList.elementAt(0);
					selectPeriod.removeAttribute("onchange");					
					instPeriod = selectPeriod.toHtml();
				}
			}
		}
		
		TSServerInfo serverInfo = super.getDefaultServerInfo();
	
		TSServerInfoModule tsServerInfoModule = serverInfo.getModule(TSServerInfo.CONDOMIN_MODULE_IDX);
		
		if(tsServerInfoModule != null && bhcPeriod != null) {
			tsServerInfoModule.getFunction(4).setupSelectBox(bhcPeriod);
			tsServerInfoModule.getFunction(9).setupSelectBox(bhcSortBy);
		}
		
		tsServerInfoModule = serverInfo.getModule(TSServerInfo.SURVEYS_MODULE_IDX);
		
		if(tsServerInfoModule != null && instPeriod != null) {
			tsServerInfoModule.getFunction(2).setupSelectBox(instPeriod);
		}
		
		return serverInfo;
	}
	

	@Override
	protected String clean(String response) {
		
		response = response.replaceAll("<img.*?>", "");
		response = response.replaceAll("target=\".*?\"", "");
		response = response.replaceAll("(?is)<script.*?</script>", "");
		return response;
	}
	
	@Override
	public boolean isError(ServerResponse response) {
		Pattern p = Pattern.compile("(?is)<script[^>]*>\\s*Download\\(\"[\\d,\\s]*\",\".*?institution[\\w\\s]*\",\"[\\w\\s,]*\"\\)\\s*;\\s*</script>");
		Matcher m = p.matcher(response.getResult());
		if (m.find())
		{
			response.setResult(response.getResult() + "refine Search by Entity Name.");
		}
		boolean superError = super.isError(response);
		if(!superError) {
			String html = response.getResult();
			if(html.contains("offices were found matching your selection criteria")) {
				Pattern downloadCallPattern = Pattern.compile("download\\(\\s*\\\"([^\\\"]+)\\\"\\s*,\\s*\\\"([^\\\"]+)\\\"\\s*,\\s*\\\"([^\\\"]+)\\\"\\s*\\)\\s*");
				Matcher matcher = downloadCallPattern.matcher(html);
				if(matcher.find()) {
					response.getParsedResponse().setError(
							"<font color=\"red\">" + matcher.group(1) + 
							" offices were found matching your selection criteria " + 
							matcher.group(3) + ". Please change your search parameters.</font>");
					superError = true;
				}
			}
		}
		return superError;
	}

	@Override
	protected String cleanDetails(String response) {
		String body = RegExUtils.getFirstMatch("(?is)<BODY.*?>(.*?)</body>", response, 1);
		body = body.replaceAll("(?is)<div id=\"divFDICFooter.*</div>", "");
		body = body.replaceAll("<img.*?>", "");
		body = body.replaceAll("(?is)<a.*?>(.*?)</a>", "$1");
		if( response.contains("<title>FDIC: Bank Holding Company Detail</title>") ) {
			HtmlParser3 parser = new HtmlParser3(body);
			
			NodeList width990Tables = parser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("style", "width:990px;border:0px;"));
			width990Tables.add(parser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "990"))
					// .extractAllNodesThatMatch(new NotFilter(new HasAttributeFilter("cellpadding")))
					.extractAllNodesThatMatch(new NotFilter(new HasChildFilter(new StringFilter("Privacy Policy"), true))));
			
			if(width990Tables.size() > 0) {
				body = "";
				for (int i = 0; i < width990Tables.size(); i++) {
					body += width990Tables.elementAt(i).toHtml();
				}
			}
			
		} else if (!response.contains("<FORM action=\"rpt_Offices.asp\"")) {
			body = RegExUtils.getFirstMatch("(?is)<form[^>]+>(.*?)</form>", body, 1);
			body = body.replaceAll("(?is)<script.*?</script>", "");
			body = body.replaceAll("<input name=.*?>", "");
			body = body.replaceAll("(?is)<a.*?>(.*?</font>)", "$1");
		} else {
			body = body.replaceAll("(?is)<select.*?</select>", "");
			body = body.replaceAll("(?is)<font color=\"#919191\".*?</font>", "");
			body = body.replaceAll("(?is)Page .*?</a>", "");
			body = body.replaceAll("<input name=.*?>", "");
			body = body.replaceAll("Questions,.*Requests", "");
			body = body.replaceAll("<FORM action=\"rpt_Offices.asp\" name=\"mainReport\" method=get>",
					"<input type='hidden' value='rpt_Offices.asp'/>");
			body = body.replaceAll("</FORM>", "");
			body = body.replaceAll("(?is)<script.*?</script>", "");
		}
		
		int indexOfSomething = -1;
		
		while((indexOfSomething = body.indexOf("Next >>")) > 0) {
			if(indexOfSomething > 0) {
				int indexOfStartTr = body.substring(0, indexOfSomething).lastIndexOf("<tr>");
				int indexOfEndTr = body.substring(indexOfSomething).indexOf("</tr>") + indexOfSomething;
				if(indexOfStartTr > 0 && indexOfEndTr > indexOfStartTr) {
					body = body.substring(0, indexOfStartTr) + body.substring(indexOfEndTr + 5);
				}
			}
		}
		
		//body = body.replaceAll("\\[Next \\>\\>\\]", "");
		
		
		body = body.replaceAll("<b>(&nbsp;)*Du", "");
		
		return body;
	}

	@Override
	protected void ParseResponse(String action, ServerResponse response, int viParseID) throws ServerResponseException {
		if (checkIfResultContainsMessage(response.getResult(), getDetailsMessages()) && viParseID!= ID_SAVE_TO_TSD &&  ( viParseID == ID_SEARCH_BY_MODULE19)){
			viParseID = ID_DETAILS;
		}else if (!checkIfResultContainsMessage(response.getResult(), getIntermediaryMessages()) && viParseID== ID_SEARCH_BY_MODULE20 && viParseID!= ID_SAVE_TO_TSD ){
			viParseID = ID_DETAILS;
		} else if (!checkIfResultContainsMessage(response.getResult(), getIntermediaryMessages()) && viParseID== ID_SEARCH_BY_MODULE21 && viParseID!= ID_SAVE_TO_TSD ){
			viParseID = ID_DETAILS;
		}
		super.ParseResponse(action, response, viParseID);
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		try{
			getParserInstance().parseDetails(detailsHtml, searchId, map);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return null;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		MessageFormat format = createPartialLinkFormat();
		return getParserInstance().parseIntermediary(response, table, searchId, format);
	}

	@Override
	protected void setMessages() {
		getErrorMessages().addNoResultsMessages("Your search found 0 institutions");
		getErrorMessages().addServerErrorMessage("An error occurred processing the page you requested");
	}

	@Override
	protected String getAccountNumber(String serverResult) {
		String accountNumber = RegExUtils.getFirstMatch("BHC ID #(.*?)<br>", serverResult, 1);
		//if (getParserInstance().isTheOtherDetail(serverResult)) {
		if(StringUtils.isEmpty(accountNumber)){
			accountNumber = org.apache.commons.lang.StringUtils.defaultIfEmpty(
					RegExUtils.getFirstMatch("Total Offices for(.*?):", serverResult, 1), "").trim();
		}
		return accountNumber;
	}

	public ro.cst.tsearch.servers.functions.XXGenericFDIC getParserInstance() {
		ro.cst.tsearch.servers.functions.XXGenericFDIC instance = ro.cst.tsearch.servers.functions.XXGenericFDIC.getInstance();
		return instance;
	}

	HashMap<String, String> dataMap = null;

	@Override
	protected HashMap<String, String> putAdditionalData(String serverResult) {
		if (dataMap == null) {
			dataMap = new HashMap<String, String>();
		}
		dataMap.put("type", "CORPORATION");

		return dataMap;
	}

}
