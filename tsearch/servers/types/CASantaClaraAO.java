package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;

public class CASantaClaraAO extends TSServer {
		
	private static final long serialVersionUID = -3604163568073224983L;
	
	public CASantaClaraAO(long searchId) {
		super(searchId);
	}
	
	public CASantaClaraAO(String rsRequestSolverName, String rsSitePath, String rsServerID, 
			String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, 
				rsPrmNameLink, searchId, miServerID);
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) 
			throws ServerResponseException {
		
		String rsResponse = StringEscapeUtils.unescapeHtml(Response.getResult())
				.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "");
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		String apnValue = extractValue(Response.getQuerry(), "apnValue");
		if (!StringUtils.isEmpty(apnValue)) {
			if (!apnValue.matches("\\d{8}") && !apnValue.matches("\\d{3}-\\d{2}-\\d{3}")) {
				Response.getParsedResponse().setError("Wrong format of APN.");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
		}
		
		String hnumber = extractValue(Response.getQuerry(), "hnumber");
		if (!StringUtils.isEmpty(hnumber)) {
			if (!hnumber.matches("\\d+")) {
				Response.getParsedResponse().setError("Street Number can contain only numbers.");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
		}
		
		String unumber = extractValue(Response.getQuerry(), "unumber");
		if (!StringUtils.isEmpty(unumber)) {
			if (!unumber.matches("\\d+")) {
				Response.getParsedResponse().setError("Unit can contain only numbers.");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
		}
				
		if (rsResponse.indexOf("No records meet your search criteria") > -1) {
			Response.getParsedResponse().setError("No data found.");
			Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
			return;
		}
		
		if (viParseID==ID_SEARCH_BY_ADDRESS && rsResponse.indexOf("Situs Address(es) :")>-1) {
			viParseID = ID_DETAILS;			//if searching by address and only one result is found, the details are displayed
		}
		
		switch (viParseID) {
								
		case ID_SEARCH_BY_ADDRESS:				
			
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
			
			if(smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
            }
			
			break;	
				
		case ID_SEARCH_BY_PARCEL:
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:
			
			StringBuilder serialNumber = new StringBuilder();
			String details = getDetails(rsResponse, serialNumber);
			String filename = serialNumber + ".html";
			
			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type","ASSESSOR");
				data.put("dataSource","AO");
				if (isInstrumentSaved(serialNumber.toString(),null,data)){
					details += CreateFileAlreadyInTSD();
				}
				else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse( details );
				
			} else {
				smartParseDetails(Response,details);
				
				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName( getServerTypeDirectory() + msSaveToTSDFileName);	
				Response.getParsedResponse().setResponse( details );
				
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				
			}
			break;
		
		case ID_GET_LINK :
			ParseResponse(sAction, Response, ID_SEARCH_BY_ADDRESS);
			break;	
			
		default:
			break;
		}
	}	
		
	private String extractValue(String url, String param) {
		String res = "";
		Matcher matcher = Pattern.compile(param+"=(.*?)&").matcher(url + "&");
		if (matcher.find()) {
			res = matcher.group(1);
		}
		return res;
	}
	
	protected String getDetails(String rsResponse, StringBuilder parcelNumber) {
		try {
			
			StringBuilder details = new StringBuilder();
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
									
			/* If from memory - use it as is */
			if(!rsResponse.contains("<html")){
				NodeList apnList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)	
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "top"));
				if (apnList.size()>0) {
					parcelNumber.append(RegExUtils.getFirstMatch("(?i)APN:([\\d-]+)", apnList.elementAt(0).toPlainTextString(), 1));
				}
				return rsResponse;
			}
			
			NodeList apnList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "top"));
			if (apnList.size()>0) {
				parcelNumber.append(RegExUtils.getFirstMatch("(?i)APN:([\\d-]+)", apnList.elementAt(0).toPlainTextString(), 1));
			}
			
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "divAPN"));
			
			if (tableList.size()>0) {
				
				NodeList itemHeaderList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)	
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "itemHeader"));
				if (itemHeaderList.size()>0) {
					details.append(itemHeaderList.elementAt(0).toHtml());
				}
				NodeList itemBodyList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)	
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "itemBody"));
				if (itemBodyList.size()>0) {
					details.append(itemBodyList.elementAt(0).toHtml());
				}
				
				NodeList ulList = nodeList.extractAllNodesThatMatch(new TagNameFilter("ul"), true)	
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "gkTabs top"));
				if (ulList.size()>0) {
					NodeList liList = ulList.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("li"), true);
					List<String> labels = new ArrayList<String>();
					for (int i=0;i<liList.size();i++) {
						labels.add(liList.elementAt(i).toPlainTextString().trim());
					}
					if (labels.size()>0) {
						NodeList tabList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)	
							.extractAllNodesThatMatch(new HasAttributeFilter("class", "gkTabItem active"));
						NodeList otherTabsList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)	
							.extractAllNodesThatMatch(new HasAttributeFilter("class", "gkTabItem"));
						tabList.add(otherTabsList);
						for (int i=0;i<labels.size()&&i<tabList.size();i++) {
							if (labels.get(i).equalsIgnoreCase("Street View")) {
								break;
							}
							details.append("<br><b>" + labels.get(i) + "</b>");
							details.append(tabList.elementAt(i).toHtml());
						}
					}
				}
			}
			
			String stringDetails = details.toString();
			
			stringDetails = stringDetails.replaceAll("(?is)<img[^>]+>", "");
			stringDetails = stringDetails.replaceAll("(?is)<div\\s+class=\"itemToolbar\">.*?</div>", "");
			stringDetails = stringDetails.replaceAll("(?is)<a[^>]+>Search\\s+Again</a>", "");
			stringDetails = stringDetails.replaceAll("(?is)<a[^>]+>\\s*View\\s+Google\\s+Map</a>", "");
			stringDetails = stringDetails.replaceAll("(?is)<a[^>]+>\\s*View\\s+Assessor's\\s+Map\\s+Book</a>", "");
			stringDetails = stringDetails.replaceAll("(?is)</?a\\b[^>]*>", "");
			stringDetails = stringDetails.replaceFirst("(?is)<div[^>]*\\bid=\"divToolbar\"[^>]*>.*?</div>","");
			stringDetails = stringDetails.replaceFirst("(?is)>[^<]*Print Assessor[^<]*<","><");
			
			//display all addresses
			stringDetails = stringDetails.replaceAll("(?is)<div[^<]+id=\"oneAddress\"[^<]*>", "<div id=\"oneAddress\" style=\"display: none\">");
			stringDetails = stringDetails.replaceAll("(?is)<div[^<]+id=\"moreAddresses\"[^<]*>", "<div id=\"moreAddresses\" style=\"display: block\">");
			//remove link "Show all x addresses"
			stringDetails = stringDetails.replaceAll("(?is)<a\\s+href=\"#\".+?</a>", "");
						
			return stringDetails;
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
		
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		HashSet<String> apns = new HashSet<String>();
		
		try {
			
			StringBuilder newtable = new StringBuilder();
			
			JSONObject jsonObject = new JSONObject(table);
			JSONArray jsonArray = jsonObject.getJSONArray("rows");
			int len = jsonArray.length();
			for (int i=0;i<len;i++) {
				JSONObject elem = (JSONObject)jsonArray.get(i);
				String apn = (String)elem.get("id");
				if (!apns.contains(apn)) {
					apns.add(apn);
					JSONArray data = (JSONArray)elem.get("data");
					String linkAndaddress = "";
					String link = "";
					String address = "";
					if (data.length()>1) {
						linkAndaddress = (String)data.get(1);
						Matcher matcher = Pattern.compile("(?is)<a\\s+href='([^']+)'\\s*>([^<]+)</a>").matcher(linkAndaddress);
						if (matcher.find()) {
							link = "apps/" + matcher.group(1);
							address = matcher.group(2);
						}
					}
					link = CreatePartialLink(TSConnectionURL.idGET) + link;
					String htmlRow = "<tr><td><a href=\"" + link + "\">" + apn + "</a></td><td>" + address + "</td></tr>";
					newtable.append(htmlRow);
					
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
					
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,htmlRow);
					currentResponse.setOnlyResponse(htmlRow);
					
					ResultMap m = new ResultMap();
					m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "AO");
					m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), apn);
					m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
					ro.cst.tsearch.servers.functions.CASantaClaraAO.parseAddress(m);
					Bridge bridge = new Bridge(currentResponse, m, searchId);
					
					DocumentI document = (AssessorDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				}
			}
						
			response.getParsedResponse().setHeader("<table align=\"center\" border=\"1\"><tr><th width=\"102px\">APN</th><th width=\"598px\">Address</th></tr>");
				
			response.getParsedResponse().setFooter("</table>");
												
			outputTable.append(newtable);
			
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap) {
				
		try {
					
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			String parcelID = "";
			NodeList apnList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "top"));
			if (apnList.size()>0) {
				parcelID = RegExUtils.getFirstMatch("(?i)\\(?\\s*APN\\s*\\)?\\s*:\\s*([\\d-]+)", apnList.elementAt(0).toPlainTextString(), 1);
			}
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
						
			StringBuilder address = new StringBuilder();
			List<String> addresses =  RegExUtils.getMatches("(?is)<span\\s+id=\"ContentMasterPage_rptAddress_lblAddress_[^\"]+\">(.+?)</span>", detailsHtml, 1);
			for (int i=0;i<addresses.size();i++) {
				address.append(addresses.get(i).trim()).append(" / ");
			}
			String stringAddress = address.toString();
			stringAddress = stringAddress.replaceFirst(" / $", "");
			if (!StringUtils.isEmpty(stringAddress)) {
				resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), stringAddress);
			}
			
			NodeList mytableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "mytable"));
			if (mytableList.size() > 0) {
				String tableAsString = mytableList.elementAt(0).toHtml();
				String documentNo = RegExUtils.getFirstMatch("(?is)Document No:([^<]+)", tableAsString, 1).trim();
				String documentType = RegExUtils.getFirstMatch("(?is)Document Type:([^<]+)", tableAsString, 1).trim();
				String transferDate = RegExUtils.getFirstMatch("(?is)Transfer Date:([^<]+)", tableAsString, 1).trim();
				
				if (!StringUtils.isEmpty(documentNo)) {
					ResultTable transactionHistory = new ResultTable();
					@SuppressWarnings("rawtypes")
					List<List> tablebody = new ArrayList<List>();
					List<String> list = new ArrayList<String>();
					if (org.apache.commons.lang.StringUtils.isNotEmpty(transferDate)){
						if (transferDate.matches("(?is)\\d{1,2}/\\d{1,2}/\\d{4}")){
							String year = transferDate.substring(transferDate.lastIndexOf("/") + 1);
							documentNo = year + "-" + documentNo;
						}
					}
					list.add(documentNo);
					list.add(transferDate);
					list.add(documentType);
					tablebody.add(list);
					String[] header = {SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(), 
									   SaleDataSetKey.RECORDED_DATE.getShortKeyName(), 
									   SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName()};
					transactionHistory = GenericFunctions2.createResultTable(tablebody, header);
					if (transactionHistory != null){
						resultMap.put("SaleDataSet", transactionHistory);
					}
				}
				
			}
			
			if (mytableList.size() > 1) {
				TableTag mytable = (TableTag)mytableList.elementAt(1);
				int rowsNo = mytable.getRowCount();
				if (rowsNo>0) {
					TableRow lastRow = mytable.getRow(rowsNo-1);
					int colsNo = lastRow.getColumnCount();
					if (colsNo>0) {
						TableColumn lastCol = lastRow.getColumns()[colsNo-1];
						String totalAssessed = lastCol.toPlainTextString().trim().replaceAll("[,$]", "");
						resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), totalAssessed);
					}
				}
			}
			
			ro.cst.tsearch.servers.functions.CASantaClaraAO.parseAddress(resultMap);
		
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
		
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;

		if (hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(0, SearchAttributes.LD_PARCELNO);
			moduleList.add(module);
		}
		
		String city = getSearchAttribute(SearchAttributes.P_CITY).toUpperCase();
		if (hasStreet() && StringUtils.isNotEmpty(city)) {
			FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(0, SearchAttributes.P_STREETNO);
			module.setSaKey(2, SearchAttributes.P_STREETNAME);
			module.forceValue(5, city);
			module.addFilter(addressFilter);
			moduleList.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(moduleList);
	}
}
