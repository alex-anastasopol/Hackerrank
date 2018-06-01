package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.MultiplePinFilterResponse;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * 
 * @author Oprina George
 * 
 *         Jun 8, 2011
 */

public class ILDuPageTR extends TemplatedServer {

	public static final long serialVersionUID = 10000000L;

	public ILDuPageTR(long searchId) {
		super(searchId);
	}

	public ILDuPageTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
		setSuper();
	}

	public void setSuper() {
		int[] intermediary_cases = new int[] { ID_SEARCH_BY_ADDRESS, ID_SEARCH_BY_PARCEL };
		super.setIntermediaryCases(intermediary_cases);
		super.setDetailsMessage("Tax Payment Information");
	}

	@Override
	protected void setMessages() {
		getErrorMessages().addNoResultsMessages("No Records Returned");
		// getErrorMessages().addServerErrorMessage("No Records Returned");
	}

	@Override
	protected HashMap<String, String> putAdditionalData(String serverResult) {
		HashMap<String, String> data = new HashMap<String, String>();
		data.put("type", "CNTYTAX");
		return data;
	}

	@Override
	protected String getAccountNumber(String serverResult) {
		// 07-19-419-017

		try {
			NodeList nodeList = org.htmlparser.Parser.createParser(
					serverResult, null).parse(null);

			nodeList = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("div"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("class", "tabdiv9"), true);

			String account = getParcelId(nodeList);

			return account;
		} catch (ParserException e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * @param nodeList
	 * @return
	 */
	public String getParcelId(NodeList nodeList) {
		if (nodeList != null) {
			String account = nodeList.elementAt(0).getChildren()
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "tabdiv11"))
					.elementAt(0).toPlainTextString();

			account = account.replaceAll("[^\\d^-]", "").trim();
			return account;
		}
		
		return "";
	}

	@Override
	protected String clean(String response) {
		return response;
	}

	@Override
	protected String cleanDetails(String response) {
		String cleanResp = response;

		try {
			NodeList nodeList = org.htmlparser.Parser.createParser(response, null).parse(null);

			Node secondTable = HtmlParser3.getNodeByID("tabid02", nodeList, true);  // Tax Distribution
			Node thirdTable = HtmlParser3.getNodeByID("tabid03", nodeList, true);  // Tax Distribution
			Node fourthTable = HtmlParser3.getNodeByID("tabid04", nodeList, true);  //Tax History Table
			
			nodeList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_pageContent_ctl00_pnlResults"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "tabid01"), true);
			String linkToBill = "";
			if (nodeList != null && nodeList.size() > 0) {
				cleanResp = nodeList.elementAt(0).toHtml();
				String[] matches = RegExUtils.parseByRegEx(cleanResp, 
						"(?is)<a id=\"(ctl00_pageContent_ctl00_lnkPrint)\"\\s*href=\"(/Treasurer/TaxBill.cfm\\?parcel=.*?)\"\\s*target=\"_blank\">(.*?)</a>",
						new int[] { 0, 1, 2, 3 });

				if (matches.length == 4) {
					linkToBill = String.format("<input type='hidden' id='%s' name='%s' value='%s' >", matches[1], matches[2], matches[3]);
				}
			}
			
			if (secondTable != null && secondTable.getChildren() != null && secondTable.getChildren().size() > 0 && !cleanResp.contains("id=\"tabid02\"")) {
				cleanResp += "<br/> <table border=\"1\" width=\"50%\"> <tr> <td>" + secondTable.toHtml() + "</td> </tr> </table>";
			}
			if (thirdTable != null && thirdTable.getChildren() != null && thirdTable.getChildren().size() > 0 && !cleanResp.contains("id=\"tabod03\"")) {
				cleanResp += "<br/> <table border=\"1\" width=\"50%\"> <tr> <td>" + thirdTable.toHtml() + "</td> </tr> </table>";
			}
			if (fourthTable != null && fourthTable.getChildren() != null && fourthTable.getChildren().size() > 0 && !cleanResp.contains("id=\"tabid04\"")) {
				cleanResp += "<br/> <table border=\"1\" width=\"50%\"> <tr> <td>" + fourthTable.toHtml() + "</td> </tr> </table> <br/> <br/>";
			}
			
			cleanResp = cleanResp.replaceAll("(?is)<h2>([A-Z\\s\\d]+)</h2>", "<h4> $1 </h4>");
			cleanResp = cleanResp.replaceAll("(?is)<div style\\s*=\\s*\\\"float:left[^\\>]+>", "<div>");
			cleanResp = cleanResp.replaceAll("(?is)<div style\\s*=\\s*\\\"clear\\s*\">\\s*</div>", "");
			cleanResp = cleanResp.replaceAll("(?is)<applet.*</applet>", "");
			cleanResp = cleanResp.replaceFirst("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1");

			cleanResp = cleanResp.replace("Click here to search for your next parcel",
					"<p align=\"center\"><font style=\"font-size:xx-large;\"><b>Property Information <br> County of DuPage</b></font></p>" + "<br>");

			cleanResp = cleanResp.replace("Parcel Number", "<b>Parcel Number</b>");
			cleanResp = cleanResp.replace("Parcel Address", "<br/><b>Parcel Address</b>");
			cleanResp = cleanResp.replace("Billing Address", "<br/><b>Billing Address</b>");
			cleanResp = cleanResp.replaceFirst("(?is)\\s*Print duplicate tax bill\\s*", "");
			cleanResp = cleanResp.replaceFirst("(?is)\\s*View Parcel on Interactive Map\\s*", "");
			cleanResp = cleanResp.replaceAll("(?is)<a [^>]*>([^<]*)<[^>]*>", "$1");
			cleanResp = cleanResp.replaceAll("(?ism)<input [^>]*>", "");
			cleanResp = cleanResp.replaceAll("(?is)(?:background-)?color\\s*:\\s*[#A-Z\\d]+\\s*[;\\\"]", "");
			
			cleanResp = cleanResp.replaceAll("(?is)<div style\\s*=\\s*\\\"float:right\\\"[^>]*>\\s*<br\\s*/?>\\s*<br\\s*/?>\\s*\\s*</div>", "");
			cleanResp = cleanResp.replaceAll("(?is)\\s+class\\s*=\\s*\\\"tabdiv ui-tabs-panel ui-widget-content ui-corner-bottom ui-tabs-hide\\\"", "style=\"border:1px solid black\"");
			
			cleanResp += linkToBill;
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		return cleanResp;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {
			// save params
			Form form = new SimpleHtmlParser(table).getForm("Form1");
			int seq = -1;
			if (form != null) {
				Map<String, String> params = form.getParams();
				seq = getSeq();
				mSearch.setAdditionalInfo(getCurrentServerName() + ":params:"
						+ seq, params);
			}

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTableList = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("table"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id",
							"ctl00_pageContent_ctl00_gvList"), true);

			if (mainTableList.size() == 0) {
				return intermediaryResponse;
			}

			TableTag tableTag = (TableTag) mainTableList.elementAt(0);

			TableRow[] rows = tableTag.getRows();

			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				if (row.getColumnCount() == 7) {
					TableColumn[] cols = row.getColumns();
					NodeList aList = cols[0].getChildren()
							.extractAllNodesThatMatch(new TagNameFilter("a"),
									true);
					String link = CreatePartialLink(TSConnectionURL.idPOST)
							+ "http://www.dupageco.org"
							+ ((LinkTag) aList.elementAt(0)).extractLink();

					LinkTag l = (LinkTag) (aList.elementAt(0));
					l.setLink(link);

					String rowHtml = row.toHtml();
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(
							ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setPageLink(new LinkInPage(link, link,
							TSServer.REQUEST_SAVE_TO_TSD));

					ResultMap resultMap = ro.cst.tsearch.servers.functions.ILDuPageTR
							.parseIntermediaryRow(row, searchId);
					Bridge bridge = new Bridge(currentResponse, resultMap,
							searchId);
					resultMap.removeTempDef();

					DocumentI document = (TaxDocumentI) bridge.importData();
					currentResponse.setDocument(document);
					intermediaryResponse.add(currentResponse);
				}
			}

			// set results
			response.getParsedResponse()
					.setHeader(
							"<table id=\"ctl00_pageContent_ctl00_gvList\" width=\"60%\" cellspacing=\"1\" border=\"1\" >"
									+ "<tr>"
									+ "<th>PIN</th>"
									+ "<th>Street Number</th>"
									+ "<th>Direction</th>"
									+ "<th >Street</th>"
									+ "<th>Unit</th>"
									+ "<th>City</th>"
									+ "<th>Zip</th>" + "</tr>");

			response.getParsedResponse().setFooter("</table>");
			outputTable.append(table);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return intermediaryResponse;
	}

	@Override
	protected void ParseResponse(String action, ServerResponse response, int viParseID) throws ServerResponseException {
		if (isError(response)) {
			response.setError("No data found for this criteria!");
			response.setResult("");
			return;
		}
		if (response.getResult().contains("Click on a PIN listed below to view property information.") || viParseID == ID_SAVE_TO_TSD)
			super.ParseResponse(action, response, viParseID);
		else if (response.getResult().contains("Click here to search for your next parcel"))
			super.ParseResponse(action, response, ID_DETAILS);

	}

	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(cleanDetails(detailsHtml), null);
			NodeList nodeList = htmlParser.parse(null);

			// get parcel no
			NodeList parcel = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("div"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("class", "tabdiv9"), true);
			String cleanPin = "[^\\d^-]";
			if (parcel.size() > 0) {
				String parcelID = getParcelId(parcel);
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID.replaceAll(cleanPin, ""));
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), parcelID.replaceAll("[^\\d]", ""));
			}

			// get name
			NodeList name = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("div"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("class", "tabdiv11"), true);

			if (name.size() > 0) {
				String tmpName = name.elementAt(7).toHtml();
				tmpName = tmpName.replaceAll("(?is)</?div.*?>", "").replaceAll("(?is)<br\\s*/?>", "   ");

				resultMap.put("tmpName", tmpName);

				ro.cst.tsearch.servers.functions.ILDuPageTR.nameILDuPageTR(resultMap, searchId);

				String billing_address = name.elementAt(1).toHtml();

				// get address
				// split it
				String[] parts = billing_address.split("(?ism)<br[^>]*>");
				//String pin = (String) resultMap.get(PropertyIdentificationSetKey.PARCEL_ID.getKeyName());
				if (parts.length == 3) {
					String legal = org.apache.commons.lang.StringUtils.defaultIfEmpty(parts[2].replaceAll("<[^>]*>", ""), "").trim();
					ro.cst.tsearch.servers.functions.ILDuPageTR.parseLegalDescription(resultMap, legal);

					String unit = (String) resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName());
					String address = org.apache.commons.lang.StringUtils.defaultIfEmpty(parts[1].replaceAll("<[^>]*>", ""), "").trim();

					if (StringUtils.isNotEmpty(unit)) {
						String substring = org.apache.commons.lang.StringUtils.substring(legal, 0, legal.indexOf(unit));
						address += " " + substring + " " + unit;
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), "");
					}

					ro.cst.tsearch.servers.functions.ILDuPageTR.parseAddress(resultMap, address);
				}
			}

			// parse taxes
			ro.cst.tsearch.servers.functions.ILDuPageTR.parseTaxes(resultMap, nodeList);

			resultMap.removeTempDef();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);

		pin = pin.replaceAll("[^\\d]", "");

		FilterResponse multiplePINFilter = new MultiplePinFilterResponse(searchId);
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d , true);
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		
		if (Search.AUTOMATIC_SEARCH == searchType){
			Collection<String> pins = getSearchAttributes().getPins(-1);
			if(pins.size() > 1){			
				for(String pid: pins){
					pid = pid.replaceAll("[^\\d]", "");
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
					module.clearSaKeys();
					module.getFunction(0).forceValue(pid);
					modules.add(module);	
				}			
				// set list for automatic search 
				serverInfo.setModulesForAutoSearch(modules);
				resultType = MULTIPLE_RESULT_TYPE;
				return;
			}
		}
		
		if (hasPin()) {// Search by PINs (Parcel Numbers)
			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.setData(0, pin);
			modules.add(module);
		}

		
		if (hasStreet() && hasStreetNo()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			String city = getSearchAttribute(SearchAttributes.P_CITY);
			module.clearSaKeys();
			module.setData(1, getSearchAttribute(SearchAttributes.P_STREETNO));
			module.setData(3, getSearchAttribute(SearchAttributes.P_STREETNAME));
			if (StringUtils.isNotEmpty(city)) {
				module.setData(5, city);
			}
			module.addFilter(addressFilter);
			module.addFilter(multiplePINFilter);
			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
	
	@Override
	protected int getResultType(){
		if(mSearch.getAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN) == Boolean.TRUE ||
				(mSearch.getSa().getPins(-1).size() > 1 && (Search.AUTOMATIC_SEARCH == mSearch.getSearchType()))){
			return MULTIPLE_RESULT_TYPE; 
		} else {
			return UNIQUE_RESULT_TYPE;
		}
	}
	
	@Override
    public boolean anotherSearchForThisServer(ServerResponse sr) {
		boolean result = mSearch.getSa().getPins(-1).size() > 1 &&
			    		 mSearch.getAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN) != Boolean.TRUE;
		return result?true:super.anotherSearchForThisServer(sr);
	}
}