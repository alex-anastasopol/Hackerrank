package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ServerResponse;

/**
 * 
 * @author Oprina George
 * 
 *         Nov 7, 2012
 */

public class MOJacksonYB extends TSServer {

	private static final long	serialVersionUID	= 1L;

	public MOJacksonYB(long searchId) {
		super(searchId);
	}

	public MOJacksonYB(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();

		if (rsResponse.contains("There are no special assessments on record for parcel number")) {
			Response.getParsedResponse().setError("No Results Found!");
			Response.getParsedResponse().setResponse("");
			return;
		}

		switch (viParseID) {

		case ID_SEARCH_BY_PARCEL:
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			StringBuilder accountId = new StringBuilder();
			HashMap<String, String> data = new HashMap<String, String>();
			String details = getDetails(Response, rsResponse, accountId, data);
			String accountName = accountId.toString();

			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

				if (isInstrumentSaved(accountName, null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}
				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);

			} else {
				smartParseDetails(Response, details);

				msSaveToTSDFileName = accountName + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				Response.getParsedResponse().setResponse(details);

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}

			break;
		}
	}

	protected void loadDataHash(HashMap<String, String> data) {
		if (data != null) {
			data.put("type", "CITYTAX");
		}
	}

	protected String getDetails(ServerResponse response, String rsResponse, StringBuilder accountId, HashMap<String, String> data) {
		try {
			StringBuilder details = new StringBuilder();

			NodeList nodes = new HtmlParser3(rsResponse).getNodeList();

			NodeList auxNodes = nodes.extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "cphBody_cphBody_pnlRegAssessments"));

			String parcel = "";

			loadDataHash(data);

			if (auxNodes.size() > 0) {
				parcel = ro.cst.tsearch.servers.functions.MOJacksonYB.getValueFromHtml(auxNodes.elementAt(0).toHtml(), "Parcel Number:");
				details.append("<table id=details width=95%><tr><td>" + auxNodes.elementAt(0).toHtml() + "</td></tr></table>");
				accountId.append(parcel.replaceAll("\\s+", ""));
			}

			/* If from memory - use it as is */
			if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "<html")) {
				return rsResponse;
			}

			return details.toString().replaceAll("</?a[^>]*>", "").replace("335071", "989898");

		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		ro.cst.tsearch.servers.functions.MOJacksonYB.parseAndFillResultMap(response, detailsHtml, resultMap);
		
//		String date = (String) resultMap.get("tmpDate"); 
//		String amountDue = (String) resultMap.get(TaxHistorySetKey.TOTAL_DUE.getKeyName());
//		
//		if(StringUtils.isNotEmpty(date) && StringUtils.isNotEmpty(amountDue)){
//			Date d = Util.dateParser3(date);
//			
//			if(d!=null && d.before(getDataSite().getDueDate())){
//				resultMap.remove(TaxHistorySetKey.TOTAL_DUE.getKeyName());
//				resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), amountDue);
//			}
//		}
		
		return null;
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		String city = getSearchAttribute(SearchAttributes.P_CITY).toUpperCase();

		if (StringUtils.isEmpty(city))
			return;

		if (!StringUtils.isEmpty(city)) {
			if (!city.startsWith(getDataSite().getCityName().toUpperCase())) {
				return;
			}
		}

		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;

		// pin
		if (hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			moduleList.add(module);
		}

		serverInfo.setModulesForAutoSearch(moduleList);
	}

}
