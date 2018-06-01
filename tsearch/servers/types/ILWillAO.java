package ro.cst.tsearch.servers.types;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableTag;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.pin.MultiplePinFilterResponse;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.ResultBodyUtils;

import com.stewart.ats.base.document.DocumentI;

public class ILWillAO extends TemplatedServer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4718746957047841358L;

	public ILWillAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		int[] intermediaryCases = new int[] { ID_SEARCH_BY_ADDRESS, ID_SEARCH_BY_SALES, ID_SEARCH_BY_SECTION_LAND, ID_INTERMEDIARY };
		setIntermediaryCases(intermediaryCases);
		int[] link_cases = new int[] { ID_GET_LINK };
		setLINK_CASES(link_cases);
		int[] save_cases = new int[] { ID_SAVE_TO_TSD };
		setSAVE_CASES(save_cases);
		int[] details_cases = new int[] { ID_DETAILS, ID_SEARCH_BY_PARCEL };
		setDetailsCases(details_cases);

		setDetailsMessage("PIN #:");
		setIntermediaryMessage("Displaying");

	}

	@Override
	protected void setMessages() {
		getErrorMessages().addServerErrorMessage("We're sorry");
		getErrorMessages().addNoResultsMessages("There are no records that match your criteria");
	}

	@Override
	public TSServerInfo getDefaultServerInfo() {
		return super.getDefaultServerInfo();// msiServerInfoDefault;
	}

	@Override
	protected void ParseResponse(String action, ServerResponse response, int viParseID) throws ServerResponseException {

		if (response.getQuerry().contains("ctl00$BC$cbNeighborhood=on")) {
			super.ParseResponse(action, response, ID_INTERMEDIARY);
		} else {
			if (response.getResult().contains(getDetailMessage()) && viParseID != ID_SAVE_TO_TSD) {
				super.ParseResponse(action, response, ID_DETAILS);
			} else {
				super.ParseResponse(action, response, viParseID);
			}

		}
	}

	@Override
	protected HashMap<String, String> putAdditionalData(String serverResult) {
		HashMap<String, String> data = new HashMap<String, String>();
		data.put("type", "ASSESSOR");
		// data.put("Year", "" + currentYear);
		return data;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		String[] links = processLinks(response);
		HtmlParser3 parser = new HtmlParser3(response.getResult());

		// equivalences
		TableTag resultTable = (TableTag) HtmlParser3.getNodeByID("ctl00_BC_gvParcels", parser.getNodeList(), true);
		outputTable.append(resultTable.toHtml());
		outputTable.append(resultTable);
		Node tableHeader = resultTable.getChild(1);
		Node tableFooter = resultTable.getChild(resultTable.getChildCount() - 2);

		resultTable.removeChild(0);
		resultTable.removeChild(0);
		resultTable.removeChild(resultTable.getChildCount() - 1);
		resultTable.removeChild(resultTable.getChildCount() - 1);

		List<HashMap<String, String>> tableAsListMap = HtmlParser3.getTableAsListMap(resultTable);
		String linkStart = CreatePartialLink(TSConnectionURL.idGET);

		for (int i = 0; i < tableAsListMap.size(); i++) {

			ParsedResponse currentResponse = new ParsedResponse();
			Node currentHtmlRow = resultTable.getChild(i + 1);
			LinkTag linkTag = HtmlParser3.getFirstTag(currentHtmlRow.getChildren(), LinkTag.class, true);
			String newLink = linkStart + linkTag.extractLink();
			linkTag.setLink(newLink);

			String rowHtml = currentHtmlRow.toHtml();
			currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
			currentResponse.setOnlyResponse(rowHtml);
			currentResponse.setPageLink(new LinkInPage(newLink, newLink, TSServer.REQUEST_SAVE_TO_TSD));

			ResultMap resultMap = new ResultMap();
			ro.cst.tsearch.servers.functions.ILWillAO.parseAndFillResultMap(resultMap, tableAsListMap.get(i));
			//String pin = convertPinToDashesFormat((String) resultMap.get("PropertyIdentificationSet.ParcelID"));

			//Map<String, String> parseLegalFromPIN = ro.cst.tsearch.servers.functions.ILWillAO.parseLegalFromPIN(pin);
			//String[] header = { "SubdivisionLotNumber", "SubdivisionBlock", "SubdivisionSection", "SubdivisionTownship" };
			//List<HashMap<String, String>> sourceSet = new ArrayList<HashMap<String, String>>();
			//sourceSet.add((HashMap<String, String>) parseLegalFromPIN);
			//ResultBodyUtils.buildInfSet(resultMap, sourceSet, header, PropertyIdentificationSet.class);

			Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
			DocumentI document = null;
			try {
				document = bridge.importData();
			} catch (Exception e) {
				e.printStackTrace();
			}
			currentResponse.setDocument(document);
			intermediaryResponse.add(currentResponse);
		}

		response.getParsedResponse().setHeader("<table>" + tableHeader.toHtml());
		response.getParsedResponse().setFooter(tableFooter.toHtml() + "</table>");
		response.getParsedResponse().setNextLink(links[0]);
		return intermediaryResponse;
	}

	private String[] processLinks(ServerResponse response) {
		String baseLink = CreatePartialLink(TSConnectionURL.idPOST);
		String serverAction = "/results.aspx";
		String result = response.getResult();

		// get available post parameters
		HtmlParser3 parser = new HtmlParser3(result);

		FormTag formTag = (FormTag) HtmlParser3.getTag(parser.getNodeList(), new FormTag(), true).elementAt(0);
		Map<String, String> postParams = HttpUtils.getInputsFromFormTag(formTag);
		String value = "";
		for (Entry<String, String> entry : postParams.entrySet()) {
			try {
				value = entry.getValue();
				entry.setValue(URLDecoder.decode(value, "UTF-8"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		int lnk = getSeq();
		String key1 = getCurrentServerName() + ":post_params:" + lnk;
		// String key2 = getCurrentServerName() + ":request_querry:" + lnk;
		mSearch.setAdditionalInfo(key1, postParams);

		// build links; add __Eventtarget as url parameter
		String link = "" + baseLink + serverAction + "?__EVENTTARGET=$2" + "&__EVENTARGUMENT=$3" + "&"
				+ ro.cst.tsearch.connection.http2.ILWillAO.ATS_LINK_ATTRIBUTES_KEY + "=" + lnk;
		result = result.replaceAll("javascript:__doPostBack\\((&#39;(.*?)&#39;,&#39;(.*?))&#39;\\)", link);// javascript:__doPostBack\\(('(.*?)','(.*?)')\\)
		response.setResult(result);

		// put paramaters as attributes
		// System.out.println(result);
		parser = new HtmlParser3(result);
		LinkTag nextLink = (LinkTag) parser.getNodeById("ctl00_BC_gvParcels_ctl01_lbNext");
		String nextURI = "";
		if (nextLink != null) {
			nextURI = nextLink.getLink();
		}
		String[] links = new String[] { nextURI };
		return links;
	}

	private static int seq = 0;

	protected synchronized static int getSeq() {
		return seq++;
	}

	@Override
	protected String getAccountNumber(String serverResult) {
		String accountNumber = RegExUtils.parseValuesForRegEx(serverResult, "\\d{2,2}-\\d{2,2}-\\d{2,2}-\\d{2,3}-\\d{2,3}-\\d{4,4}");
		return accountNumber.trim();
	}

	@Override
	protected String cleanDetails(String response) {
		// ctl00_BC_pnParcel

		// remove GIS Dept /Recorder/ Treas Tax info
		response = response.replaceAll("(?is)<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\".*?GIS.*?</table>", "");
		// remove the map image ctl00_BC_ajaxUP_Photo
		response = response.replaceAll("(?is)<div id=\"ctl00_BC_ajaxUP_Photo\".*?</div>", "");

		// replace the header images with text
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("/images/earth_view.gif", "");

		headers.put("/images/presale.gif", "<b>PREVIOUS SALE INFORMATION</b>");
		headers.put("/images/currate.gif", "<b>MOST CURRENT RATE</b>");
		headers.put("/images/asmtinfo.gif", "<b>ASSESSMENT INFORMATION</b>");
		headers.put("/images/bldginfo.gif", "<b>BUILDING INFORMATION</b>");
		headers.put("/images/legal.gif", "<b>LEGAL DESCRIPTION</b>");
		headers.put("ctl00_BC_imgPropertyPic", "");
		headers.put("/images/newsearch.gif", "");
		headers.put("/images/back.gif", "");
		headers.put("/images/home.gif", "");
		headers.put("/images/prc.gif", "");
		headers.put("/images/pinsearch.gif", "");
		headers.put("/images/addresssearch.gif", "");
		headers.put("/images/salessearch.gif", "");
		headers.put("/images/neighborhoodsearch.gif", "");

		String imgPattern = "<img.*%s.*?/>";

		for (String key : headers.keySet()) {
			response = response.replaceAll(String.format(imgPattern, key), headers.get(key));
		}

		response = response.replaceAll("(?is)<table id=\"Table4\".*?</table>", "");
		response = response.replaceAll("(?is)<script.*?</script>", "");
		response = response.replaceAll("(?is)<a.*?</a>", "");
		response = response.replaceAll("(?is)<table id=\"Table1\".*?</table>", "");
		response = response.replaceAll("ctl00_BC_tblResidential", "Table5");
		response = response.replaceAll("ctl00_BC_tblNonParticpate", "Table55");
		
		List<String> matches = RegExUtils.getMatches("(?is)<table id=\"Table.*?</table>", response, 0);
		String result = StringUtils.join(matches.toArray(new String[] {}));

		return result;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		String serverResult = response.getResult();
		String pin = getAccountNumber(serverResult);

		String pisFormat = "PropertyIdentificationSet.%s";
		resultMap.put("PropertyIdentificationSet.ParcelID", pin);
		String sdsFormat = "SaleDataSet.%s";
		HtmlParser3 parser = new HtmlParser3(serverResult);
		// address
		Node addressNode = parser.getNodeById("ctl00_BC_lbAddress");
		String rawAddress = addressNode.toPlainTextString();
		resultMap.put("PropertyIdentificationSet.AddressOnServer", rawAddress);
		ro.cst.tsearch.servers.functions.ILWillAO.parseAddress(resultMap);
		Node cityNode = parser.getNodeById("ctl00_BC_lbCity");
		String city = cityNode.toPlainTextString();
		resultMap.put(String.format(pisFormat, "City"), city);
		Node zipNode = parser.getNodeById("ctl00_BC_lbZip");
		String zip = zipNode.toPlainTextString();
		resultMap.put(String.format(pisFormat, "Zip"), zip);

		// previous sale info
		Node saleDateNode = parser.getNodeById("ctl00_BC_lbSaleDate");
		resultMap.put(String.format(sdsFormat, "RecordedDate"), saleDateNode.toPlainTextString());
		Node saleAmtNode = parser.getNodeById("ctl00_BC_lbSaleDate");
		resultMap.put(String.format(sdsFormat, "SalesPrice"), saleAmtNode.toPlainTextString());

		// assessment info
		Node totalAssessmentNode = parser.getNodeById("ctl00_BC_lbASTotal");
		resultMap.put("PropertyAppraisalSet.TotalAssessment", totalAssessmentNode.toPlainTextString());

		// legal description with proper attributes
		Node lotNode = parser.getNodeById("ctl00_BC_lbLot");
		Node blockNode = parser.getNodeById("ctl00_BC_lbBlock");
		Node unitNode = parser.getNodeById("ctl00_BC_lbUnit");
		Node buildingNode = parser.getNodeById("ctl00_BC_lbBuilding");
		Node areaNode = parser.getNodeById("ctl00_BC_lbArea");

		List<HashMap<String, String>> pisList = new ArrayList<HashMap<String, String>>();

		//HashMap<String, String> pisMap1 = (HashMap<String, String>) ro.cst.tsearch.servers.functions.ILWillAO.parseLegalFromPIN(pin);

		String lot = lotNode.toPlainTextString();
		String block = blockNode.toPlainTextString();
		
		HashMap<String, String> pisMap2 = new HashMap<String, String>();
		
		/*if ("".equals(blockNode.toPlainTextString())){
			lot += " " + pisMap1.get("SubdivisionLotNumber") ;
			block += " " + pisMap1.get("SubdivisionBlock");
		} else {
			pisList.add(pisMap1);
		}*/
		
		pisMap2.put("SubdivisionLotNumber", lot);
		pisMap2.put("SubdivisionBlock", block);
		pisMap2.put("SubdivisionUnit", unitNode.toPlainTextString());
		
		// legal description text
		Node legalDescriptionNode = parser.getNodeById("ctl00_BC_lbLegalDesc");
		Map<String, String> pisMap3 = ro.cst.tsearch.servers.functions.ILWillAO.parseAndFillResultMap(resultMap,
				legalDescriptionNode.toPlainTextString(), pisMap2);
		
		pisMap3.put("SubdivisionBldg", buildingNode.toPlainTextString());
		pisMap3.put("Area", areaNode.toPlainTextString());
		
		pisList.add((HashMap<String, String>) pisMap3);
		String[] header = { "SubdivisionLotNumber", "SubdivisionBlock", "SubdivisionUnit", "SubdivisionBldg", "Area", "SubdivisionPhase",
				"SubdivisionName", "SubdivisionSection", "SubdivisionTownship", "SubdivisionRange" };

		ResultBodyUtils.buildInfSet(resultMap, pisList, header, PropertyIdentificationSet.class);
		// test
		// String text = pin + "\r\n" + addressNode.toPlainTextString().trim() +
		// "\r\n\r\n\r\n";
		// FileUtils.appendToTextFile("D:\\work\\ILWillAO\\address.txt", text);
		//
		// text = pin + "\r\n" +
		// legalDescriptionNode.toPlainTextString().replaceAll(
		// "&nbsp;", " ").trim() + "\r\n\r\n\r\n";
		// FileUtils.appendToTextFile("D:\\work\\ILWillAO\\legal_description.txt",
		// text);
		// end test

		return null;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		ArrayList<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		String streetNo = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNO);
		String streetName = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNAME);
		String city = getSearch().getSa().getAtribute(SearchAttributes.P_CITY);
		String zip = getSearch().getSa().getAtribute(SearchAttributes.P_ZIP);

		String pid = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO);

		// GenericAddressFilter addressHighPassFilter =
		// AddressFilterFactory.getGenericAddressHighPassFilter(searchId, 0.8d);
		//PinFilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
		FilterResponse multiplePINFilter = new MultiplePinFilterResponse(searchId);

		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		
		if (Search.AUTOMATIC_SEARCH == searchType){
			if (hasPin()) {
				Collection<String> pins = getSearchAttributes().getPins(-1);
				if(pins.size() > 1){			
					for(String pin: pins){
						module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
						pin = convertPinToDashesFormat(pin);
						String[] pinNumbers = StringUtils.split(pin, "-");
						if (pinNumbers.length == 6) {
							module.clearSaKeys();
							for (int i = 0; i < pinNumbers.length; i++) {
								module.getFunction(i).forceValue(pinNumbers[i].trim());
							}
							modules.add(module);
						}
					}
					serverInfo.setModulesForAutoSearch(modules);
					resultType = MULTIPLE_RESULT_TYPE;
					return;
				}
			}
		}

		if (hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			pid = convertPinToDashesFormat(pid);
			String[] pinNumbers = StringUtils.split(pid, "-");
			if (pinNumbers.length == 6) {
				module.clearSaKeys();
				for (int i = 0; i < pinNumbers.length; i++) {
					module.getFunction(i).forceValue(pinNumbers[i].trim());
				}
				modules.add(module);
			}
		}
		
		if (hasStreet()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(streetNo);
			module.getFunction(1).forceValue(streetNo);
			module.getFunction(3).forceValue(streetName);
			module.getFunction(5).forceValue(city);
			module.getFunction(6).forceValue(zip);
			module.addFilter(multiplePINFilter);
			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);

	}

	private String convertPinToDashesFormat(String pin) {
		return pin.replaceAll("(\\d{2,2})(\\d{2,2})(\\d{2,2})(\\d{2,3})(\\d{2,3})(\\d{4,4})", "$1-$2-$3-$4-$5-$6");
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
