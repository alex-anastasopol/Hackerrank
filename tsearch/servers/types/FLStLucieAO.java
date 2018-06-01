package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.tags.BodyTag;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.ResultBodyUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;

@SuppressWarnings("deprecation")
public class FLStLucieAO extends TemplatedServer {

	private static final long serialVersionUID = -1386991926286019554L;
	
	private static String SUBDIVISION_LIST = "";
	private static String LANDUSE_LIST = "";
	private static String LANDUSE_SALES_LIST = "";
	
	static {
		String folderPath = ServerConfig.getModuleDescriptionFolder(BaseServlet.REAL_PATH + "WEB-INF/classes/resource/module/comments/");
		File folder = new File(folderPath);
		if (!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath + "] does not exist. Module Information not loaded!");
		}
		try {
			SUBDIVISION_LIST = FileUtils.readFileToString(new File(folderPath + File.separator + "FLStLucieAOSubdivisionList.xml"));
			LANDUSE_LIST = FileUtils.readFileToString(new File(folderPath + File.separator + "FLStLucieAOLandUseList.xml"));
			LANDUSE_SALES_LIST = FileUtils.readFileToString(new File(folderPath + File.separator + "FLStLucieAOLandUseSalesList.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public TSServerInfo getDefaultServerInfo() {

		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX);
		if (tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(0), SUBDIVISION_LIST);
		}

		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.MODULE_IDX38);
		if (tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(0), LANDUSE_LIST);
		}

		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.SALES_MODULE_IDX);
		if (tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(4), LANDUSE_SALES_LIST);
			setupSelectBox(tsServerInfoModule.getFunction(5), SUBDIVISION_LIST);
		}
		
		setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
		
		return msiServerInfoDefault;
	}

	public FLStLucieAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		int[] intermediaryCases = new int[] { ID_SEARCH_BY_NAME, ID_SEARCH_BY_ADDRESS, ID_SEARCH_BY_PARCEL, ID_SEARCH_BY_SALES,
				ID_SEARCH_BY_SUBDIVISION_NAME, ID_SEARCH_BY_MODULE38, ID_INTERMEDIARY };
		setIntermediaryCases(intermediaryCases);
		int[] link_cases = new int[] { ID_GET_LINK };
		setLINK_CASES(link_cases);
		int[] details_cases = new int[] { ID_DETAILS };
		setDetailsCases(details_cases);
		setDetailsMessage("PROPERTY RECORD CARD");
		setIntermediaryMessage("Records Selected");

	}

	protected void setMessages() {
		getErrorMessages().addServerErrorMessage("Response Buffer Limit Exceeded");
		getErrorMessages().addServerErrorMessage("Microsoft OLE DB Provider for SQL Server");
		getErrorMessages().addServerErrorMessage("error '80040e31'");
		
	}

	@Override
	protected void ParseResponse(String action, ServerResponse response, int viParseID) throws ServerResponseException {
		super.ParseResponse(action, response, viParseID);
	}

	private static int seq = 0;

	protected synchronized static int getSeq() {
		return seq++;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		String serverResult = response.getResult();
		HtmlParser3 parser = new HtmlParser3(serverResult);
		
		NodeList divs = HtmlParser3.getNodeListByType(parser.getNodeList(), "div", true);
		
		for (int i=0;i<divs.size();i++) {
			Div div = (Div)divs.elementAt(i);
			String id = div.getAttribute("id");
			if ("divq1".equalsIgnoreCase(id) || "divq2".equalsIgnoreCase(id)) {
				
				ParsedResponse currentResponse = new ParsedResponse();
				String rowHtml = div.toHtml();
				outputTable.append(rowHtml);
				
				String parcelId = "";
				String name = "";
				String address = "";
				String split[] = rowHtml.replaceAll("(?is)</?div[^>]*>", "").split("(?is)<br>");
				if (split.length>2) {
					parcelId = ro.cst.tsearch.utils.StringUtils.cleanHtml(split[0]);
					name = ro.cst.tsearch.utils.StringUtils.cleanHtml(split[1]);
					if (name.matches("^Grantor:.*")) {	//Search by Sales, the grantor is not the owner
						name = "";
					}
					address = ro.cst.tsearch.utils.StringUtils.cleanHtml(split[2]);
					address = address.trim().replaceFirst("(?is)^TBD$", "");
				}
				
				String atsLink = CreatePartialLink(TSConnectionURL.idGET) + "/paslc/prc.asp?prclid=" + parcelId.replaceAll("-", "");

				Map<String, String> tempLinkAttrMap = new HashMap<String, String>();
				int seq2 = getSeq();
				tempLinkAttrMap.put("&ats_link_attributes_key", "" + seq2);
				atsLink = ro.cst.tsearch.utils.StringUtils.addParametersToUrl(atsLink, tempLinkAttrMap, true);
				
				String key1 = getCurrentServerName() + ":name:" + seq2;
				mSearch.setAdditionalInfo(key1, name);
				
				rowHtml = rowHtml.replaceFirst(parcelId, "<a href=\"" + atsLink + "\">" + parcelId + "</a>");
				rowHtml = rowHtml.replaceAll("(?is)<br>\\s*<a\\s+href='javascript[^']+'.+?Show\\s+Map.*?(<br/?>)", "$1");
				
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);

				currentResponse.setOnlyResponse(rowHtml);

				currentResponse.setPageLink(new LinkInPage(atsLink, atsLink, TSServer.REQUEST_SAVE_TO_TSD));

				ResultMap resultMap = new ResultMap();
				resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "AO");
				resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name);
				resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelId);

				ro.cst.tsearch.servers.functions.FLStLucieAO.parseAndFillResultMap(resultMap);

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
		}
		
		response.getParsedResponse().setHeader("<table><tr><td>");
		response.getParsedResponse().setFooter("</td></tr></table>");
		
		return intermediaryResponse;
				
	}
	
	protected String cleanDetails(String response) {
		
		response = Tidy.tidyParse(response, null);
		
		HtmlParser3 parser3 = new HtmlParser3(response);
		NodeList tableList1 = HtmlParser3.getNodeListByType(parser3.getNodeList(), "table", true);
		List<TableTag> tableList2 = new ArrayList<TableTag>();
		
		String label1 = "";
		String label2 = "";
		String newHeader = "<table width=\"98.5%\" border=\"0\" align=\"CENTER\" cellspacing=\"0\" cellpadding=\"1\">";
		
		for (int i=0;i<tableList1.size();i++) {
			Node node = tableList1.elementAt(i);
			if (node instanceof TableTag) {
				TableTag table = (TableTag)node;
				Node parent = table.getParent();
				if (parent instanceof BodyTag) {
					if ("98.5%".equalsIgnoreCase(table.getAttribute("width")) && "center".equalsIgnoreCase(table.getAttribute("align"))) {
						tableList2.add(table);
					} else {
						if (table.toPlainTextString().trim().startsWith("Ownership and Mailing")) {
							label1 = table.toHtml().replaceFirst("(?is)<table>", newHeader);
						} else if (table.toPlainTextString().trim().startsWith("Sales Information")) {
							label2 = table.toHtml().replaceFirst("(?is)<table>", newHeader);
						} 
					}
				}
			}
		}
		
		StringBuilder result = new StringBuilder();
		boolean hasMoreLink = false;
		
		int index = 0;
		for (int i=0;i<tableList2.size();i++) {
			String toPlainTextString = tableList2.get(i).toPlainTextString().trim();
			String toHtml = tableList2.get(i).toHtml();
			
			if (toPlainTextString.equalsIgnoreCase("PROPERTY RECORD CARD")) {
				continue;
			}
			if (toHtml.toLowerCase().contains("bgcolor=\"firebrick\"")) {
				continue;
			}
			if (toPlainTextString.equalsIgnoreCase("THIS INFORMATION IS BELIEVED TO BE CORRECT AT THIS TIME BUT IT IS SUBJECT TO CHANGE AND IS NOT WARRANTED.")) {
				continue;
			}
			if (toHtml.toLowerCase().contains("href=\"basearea.asp?sketch=")) {
				toHtml = label1.replaceFirst("(?is)<td[^>]+bgcolor=\"SteelBlue\".*?</td>", "");
				toHtml = toHtml.replaceFirst("(?is)(<td[^>]+bgcolor=\"SteelBlue\".*?)Legal Description(.*?</td>)", "$1Exterior Features$2");
			}
			
			if (index==2) {
				result.append(label1);
				String oldtoHtml = toHtml;
				toHtml = toHtml.replaceFirst("<a[^>]+>.*?More\\.\\.\\..*?</a>", "...");
				if (!oldtoHtml.equals(toHtml)) {
					hasMoreLink = true;
				}
			} else if (index==3) {
				result.append(label2);
			}
			result.append(toHtml);
			index++;
			if (!toHtml.toLowerCase().contains("bgcolor=\"steelblue\"")) {
				result.append("<br>");
			}
		}
		
		StringBuilder fullLegalDescription = new StringBuilder();
		if (hasMoreLink){
			HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			String legalDescriptionPage = "";
			try {
				ro.cst.tsearch.connection.http2.FLStLucieAO connSite = (ro.cst.tsearch.connection.http2.FLStLucieAO)site;
				String link = site.getSiteLink() + "paslc/legal.asp";
				if (StringUtils.isNotEmpty(link )) {
					legalDescriptionPage = connSite.getLegalDescription(link);
					HtmlParser3 parser32 = new HtmlParser3(legalDescriptionPage);
					NodeList nodeList = parser32.getNodeList();
					if (nodeList!=null && nodeList.size()>=4){
						fullLegalDescription.append(nodeList.elementAt(3).toHtml());
						fullLegalDescription.append(nodeList.elementAt(4).toHtml());
					}
					
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				HttpManager.releaseSite(site);
			}
			
		}
		
		result.append(fullLegalDescription);
		
		String resultString = result.toString();
		resultString = resultString.replaceAll("(?is)<td[^>]*>\\s*<img[^>]*>\\s*</td>", "");

		return resultString;
	}

	@Override
	protected String addInfoToDetailsPage(ServerResponse response, String serverResult, int viParseID) {
		StringBuilder result = new StringBuilder( serverResult );
		if (viParseID == isInArray(viParseID, getDETAILS_CASES()) ){
			Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			String querry = response.getQuerry();
			String syncKey = querry.replaceAll("ats_link_attributes_key=(\\d+)", "$1");
			String intermediaryName = (String) search.getAdditionalInfo(getCurrentServerName() + ":" + "name" +":"+ syncKey);
			result.append("<input type=\"hidden\" name=\"intermediary_name\" value=\""+ intermediaryName + "\" />");
		}
		
		return result.toString();
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		// Property Identification
		
		NodeList list = new HtmlParser3(detailsHtml).getNodeList();
		//String[] labelsToGet = new String[] {};
		Map<String, String> labelsToAtsSets = new HashMap<String, String>();
		//1st table
		labelsToAtsSets.put("PropertyIdentificationSet.AddressOnServer", "Site Address:");
		labelsToAtsSets.put("PropertyIdentificationSet.ParcelID", "ParcelID:");
		labelsToAtsSets.put("PropertyIdentificationSet.SubdivisionSection", "Sec/Town/Range:");
		labelsToAtsSets.put("PropertyIdentificationSet.SubdivisionTownship", "Sec/Town/Range:");
		labelsToAtsSets.put("PropertyIdentificationSet.SubdivisionRange", "Sec/Town/Range:");
		labelsToAtsSets.put("PropertyIdentificationSet.City", "City/Cnty:");
		
		//2nd table
//		labelsToAtsSets.put("PropertyAppraisalSet.LandAppraisal", "Land Value:");
		labelsToAtsSets.put("PropertyAppraisalSet.TotalAssessment", "Assessed:");
		labelsToAtsSets.put("PropertyIdentificationSet.NameOnServer", "Owner:");
		
		
		Set<Entry<String, String>> entrySet = labelsToAtsSets.entrySet();

		for (Entry<String, String> entry : entrySet) {
			String key = entry.getKey(); 
			String textFromNextCell = getTextFromNextCell(entry.getValue(), list);
			if (key.equals(PropertyIdentificationSetKey.CITY.getKeyName())) {
				if (textFromNextCell.endsWith("County")) {
					textFromNextCell = "";
				} 
			} else if (key.equals(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName())) {
				textFromNextCell = textFromNextCell.trim().replaceFirst("(?is)^TBD$", "");
			}  
			map.put(key, textFromNextCell);
		}
		String parcelID = (( String) map.get("PropertyIdentificationSet.ParcelID")).replaceAll("-", "");
		map.put("PropertyIdentificationSet.ParcelID", parcelID);
		
		String legalDescriptionOnServer = "";
		//int inputIndex = 10;
		if (!detailsHtml.contains("Full Legal Description")){
			NodeList tables = HtmlParser3.getNodeListByTypeAndAttribute(list, "table", "width", "98.5%", true);
			for (int i=1;i<tables.size();i++) {
				if (tables.elementAt(i).toPlainTextString().contains("Legal Description")) {
					i++;
					if (i<tables.size()) {
						TableTag table = (TableTag)tables.elementAt(i);
						if (table.getRowCount()>0) {
							TableRow row = table.getRow(0);
							if (row.getColumnCount()>1) {
								legalDescriptionOnServer = row.getColumns()[1].toPlainTextString().trim();
							}
						}
					}
					break;
				}
			}
		}else{
			legalDescriptionOnServer = HtmlParser3.traverseNodelist(list, new int[] {21,0,0}).toPlainTextString();
			//inputIndex = 12;
		}
		map.put("PropertyIdentificationSet.LegalDescriptionOnServer", legalDescriptionOnServer);
		
		LinkInPage lip = response.getParsedResponse().getPageLink();
		if (lip!=null) {
			String link = lip.getLink();
			String seq = RegExUtils.getFirstMatch("ats_link_attributes_key=(\\d+)&", link + "&", 1);
			String key = getCurrentServerName() + ":name:" + seq;
			String name = (String)mSearch.getAdditionalInfo(key);
			if (!StringUtils.isEmpty(name)) {
				map.put("tmpIntermediaryName",  name);
			}
		}
		
		Node saleDataSetTable = HtmlParser3.traverseNodelist(list, new int[] {7,1,1,1});
		if (saleDataSetTable != null && saleDataSetTable instanceof TableTag){
			List<HashMap<String,String>> saleDataSet = HtmlParser3.getTableAsListMap((TableTag) saleDataSetTable);
			for (HashMap<String, String> hashMap : saleDataSet) {
				String bookPage = hashMap.get("Book/Page");
				String[] split = bookPage.split("/");
				if (split!=null && split.length==2){
					hashMap.put("Book", ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes( split[0].trim()));
					hashMap.put("Page", ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(split[1].trim()));
				}
			}
			
			HashMap<String, String> expectedValuesToActualValues = new HashMap<String, String>();
			expectedValuesToActualValues.put( "InstrumentDate", "Date");
			expectedValuesToActualValues.put( "SalesPrice", "Price");
			expectedValuesToActualValues.put( "DocumentType", "Deed");
			ResultBodyUtils.buildSaleDataSet(map, saleDataSet,expectedValuesToActualValues );
		}
		
		ro.cst.tsearch.servers.functions.FLStLucieAO.parseAndFillResultMap(map);
		
		// test
		/*String pin = parcelID;
		String text = pin + "\r\n" + map.get("PropertyIdentificationSet.AddressOnServer") +
		 "\r\n\r\n\r\n";
     	String path = "D:\\work\\FlStLucieAO\\";
		ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "address.txt", text);
				
		 text = pin + "\r\n" +
		legalDescriptionOnServer + "\r\n\r\n\r\n";
		 ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "legal_description.txt",
		 text);
		 
		 text = pin + "\r\n" +
			map.get("PropertyIdentificationSet.NameOnServer") + "\r\n\r\n\r\n";
			 ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "name.txt",
			 text);
			*/ 
		// end test
		
		return null;
	}

	@Override
	protected String getAccountNumber(String serverResult) {
		HtmlParser3 parser3 = new HtmlParser3(serverResult);
		String labelText = "ParcelID:";
		NodeList nodeList = parser3.getNodeList();
		String string = getTextFromNextCell(labelText, nodeList).replaceAll("-", "");

		// parser3.getValueFromNextCell(new TextNode("ParcelID:"),
		// "\\d+-\\d+-\\d+-\\d+-\\d+", false);
		return string;
	}

	private String getTextFromNextCell(String labelText, NodeList nodeList) {
		return HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodeList, labelText), "", true).replaceAll("</?font[^>]*>", "").trim();
	}
	
	protected HashMap<String, String> putAdditionalData(String serverResult) {
		HashMap<String, String> data = new HashMap<String, String>();
		data.put("type", "ASSESSOR");
//		data.put("Year", "" + currentYear);
		return data;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		ArrayList<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.7d);
		
		String ownerName = getSearch().getSa().getAtribute(SearchAttributes.OWNER_FML_NAME);
		
		String streetNo = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNO);
		String streetName = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNAME);
		
		String pin = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO);

		PinFilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
		
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
		
		if (hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(pin);
			module.addFilter(pinFilter);
			module.addFilter(nameFilterHybrid);
			modules.add(module);
		}

		if (hasStreet()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue((streetNo + " " + streetName).trim());
			module.addFilter(pinFilter);
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			module.addValidator(addressFilter.getValidator());
			modules.add(module);
		}

		if (hasOwner()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(ownerName);
			module.addFilter(nameFilterHybrid);
			modules.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(modules);

	}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public ServerResponse removeUnnecessaryResults(ServerResponse sr) {
			HashMap<String, ParsedResponse> newResult = new HashMap<String,ParsedResponse>();
			for (int i = 0; i < sr.getParsedResponse().getResultsCount(); i++) {
				ParsedResponse pr = (ParsedResponse) (sr.getParsedResponse()
						.getResultRows()).elementAt(i);
				newResult.put( pr.getInstrumentNumber(),  pr);
				
			}	
			Vector newResultsRows = new Vector();
			newResultsRows.addAll( newResult.values());
			sr.getParsedResponse().setResultRows( newResultsRows );
			return sr;
		}
		
		@Override
		protected ServerResponse performRequest(String page, int methodType, String action, int parserId, String imagePath, String vbRequest, Map<String, Object> extraParams)
				throws ServerResponseException {
			ServerResponse serverResponse = super.performRequest(page, methodType, action, parserId, imagePath, vbRequest, extraParams);
			String result = serverResponse.getResult();
			if (result.contains("Execution of the ASP page caused the Response Buffer to exceed its configured limit.")){
				serverResponse.setResult("");
				String text = "Internal server error received from source site!!!";
				serverResponse.getParsedResponse().setError("<font color=\"red\">" + text + "</font>");
			}
			return serverResponse;
		}
}
