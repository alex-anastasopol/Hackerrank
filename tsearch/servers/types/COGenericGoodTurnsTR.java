package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

public class COGenericGoodTurnsTR extends TSServer {
	
	private static final long serialVersionUID = 1L;
	private boolean downloadingForSave; 
	
	public COGenericGoodTurnsTR(long searchId) 
	{
		super(searchId);
	}
	
	public COGenericGoodTurnsTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) 
	{
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		StringBuilder accountId;
		String details;
		
		switch (viParseID) 
		{
			case ID_SEARCH_BY_NAME:
			case ID_SEARCH_BY_SUBDIVISION_NAME:
			case ID_SEARCH_BY_ADDRESS:
			case ID_SEARCH_BY_SECTION_LAND:
				// no result
				if (rsResponse.indexOf("No Records Selected") > -1) {
					Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
					return;
				}
				
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				
				if(smartParsedResponses.size() == 0) {
					return;
				}
				
				Response.getParsedResponse().setHeader("<table width=\"100%\" border=\"1\">\n" +
						"<tr>" +
							"<th align=\"center\">Account Number</th>" +
							"<th align=\"center\">Parcel Number</th><th width=\"50%\" align=\"center\">Owner Name</th>" +
						"</tr>" +
						"<tr>" +
							"<th width=\"50%\" align=\"center\" colspan=\"2\">Physical Address</th>" +
							"<th width=\"50%\" align=\"center\">Legal Description</th>" +
						"</tr>");
				Response.getParsedResponse().setFooter("</table>");
				
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				
				break;
				
			case ID_SEARCH_BY_PROP_NO:
			case ID_SEARCH_BY_PARCEL:
			case ID_DETAILS:
				

				if(rsResponse.indexOf("Parcel Record Not Found") > -1) {
					Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
					return;
				}
				
				accountId = new StringBuilder();
				String requestLink = "";
				
				HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
				requestLink = site.getSiteLink().replaceAll("/assessor.*", "") + (sAction + "&" + Response.getRawQuerry());
				HttpManager.releaseSite(site);
				
				details = getDetails(rsResponse, accountId, requestLink);
				if(!downloadingForSave) {
								
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
					
					HashMap<String, String> data = new HashMap<String, String>();
					putData(data);
					
					if (isInstrumentSaved(accountId.toString().trim(),null,data)){
						details += CreateFileAlreadyInTSD();
					}
					else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
					}
	
					Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
					Response.getParsedResponse().setResponse(details);
				} else {
					
					//details = getDetails(rsResponse, accountId, null);
					
					smartParseDetails(Response, details);
					
					msSaveToTSDFileName = accountId.toString().trim() + ".html";
					Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
					Response.getParsedResponse().setResponse(details);
					
					msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				}
				
				break;
			
			case ID_SAVE_TO_TSD :
				downloadingForSave = true;
				ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
			break;
				
			case ID_GET_LINK:
				if (rsResponse.indexOf("Parcel Detail Information") > -1) {
					ParseResponse(sAction, Response, ID_DETAILS);
				} 
				
				break;
			
//			case ID_SEARCH_BY_SECTION_LAND: //search doesn't work on official site
//				// no result
//				if (rsResponse.indexOf("No Records Selected") > -1) {
//					Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
//					return;
//				}
		}
	}

	/**
	 * @param data
	 */
	protected void putData(HashMap<String, String> data) {
		data.put("type","CNTYTAX");
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList parcelTables = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "parcel_table"), true);
//			NodeList valueTables = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "value_table"), true);
			NodeList salesTables = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "sales_table"), true);
//			NodeList landTables = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "land_table"), true);
			
			String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
			if (crtCounty.equalsIgnoreCase("Pitkin")){
				resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "AO");
			} else {
				resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
			}
			
			String subdivisionName = "";
			
			for(int i = 0; i < parcelTables.size(); i++) {
				TableTag table = (TableTag)parcelTables.elementAt(i);
				
				if(i == 0) {
					if (StringUtils.isNotEmpty(table.getRows()[1].getColumns()[1].toPlainTextString().trim())){
						resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), table.getRows()[1].getColumns()[1].toPlainTextString().trim());
					} 
					
					if (StringUtils.isNotEmpty(table.getRows()[1].getColumns()[2].toPlainTextString().trim())){
						resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), table.getRows()[1].getColumns()[2].toPlainTextString().trim());
					}
					
				} else if(i == 1) {
					resultMap.put("PropertyIdentificationSet.NameOnServer", table.getRows()[0].getColumns()[0].toPlainTextString().trim());
					resultMap.put("tmpPossibleName", table.getRows()[1].getColumns()[0].toPlainTextString().trim());//273512454003
				} else if(i == 2) {
					String legalDescription = "";
					for(int j = 0; j < table.getRowCount(); j++) {
						legalDescription += table.getRows()[j].getColumns()[0].toPlainTextString().trim() + " ";
					}
					resultMap.put("PropertyIdentificationSet.PropertyDescription", legalDescription.trim());
				} else if(i == 3) {
					resultMap.put("PropertyIdentificationSet.AddressOnServer", table.getRows()[0].getColumns()[0].toPlainTextString().trim());
					subdivisionName = table.getRows()[1].getColumns()[0].toPlainTextString().trim();
					//B 5595 case 7), sub name must be cleaned
					subdivisionName = subdivisionName.replaceAll("(?is)(.*COND[^\\s]+).*", "$1");
					resultMap.put("PropertyIdentificationSet.SubdivisionName", subdivisionName);
				} else if(table.toPlainTextString().indexOf("Township") > -1) {
					if (table.getRows().length == 2){
						resultMap.put("PropertyIdentificationSet.SubdivisionSection", table.getRows()[1].getColumns()[0].toPlainTextString().trim());
						resultMap.put("PropertyIdentificationSet.SubdivisionTownship", table.getRows()[1].getColumns()[1].toPlainTextString().trim());
						resultMap.put("PropertyIdentificationSet.SubdivisionRange", table.getRows()[1].getColumns()[2].toPlainTextString().trim());
					} else if (table.getRows().length > 2){
						List<List> bodySTR = new ArrayList<List>();
						List<String> line = new ArrayList<String>();
						for (int j = 1; j < table.getRows().length; j++){
							line = new ArrayList<String>();
							line.add(table.getRows()[j].getColumns()[0].toPlainTextString().trim());
							line.add(table.getRows()[j].getColumns()[1].toPlainTextString().trim());
							line.add(table.getRows()[j].getColumns()[2].toPlainTextString().trim());
							bodySTR.add(line);
						}
						if (!bodySTR.isEmpty()){
							   String [] header = {"SubdivisionSection", "SubdivisionTownship", "SubdivisionRange"};
							   
							   Map<String,String[]> map = new HashMap<String,String[]>();
							   map.put("SubdivisionSection", new String[]{"SubdivisionSection", ""});
							   map.put("SubdivisionTownship", new String[]{"SubdivisionTownship", ""});
							   map.put("SubdivisionRange", new String[]{"SubdivisionRange", ""});
							   
							   ResultTable pis = new ResultTable();	
							   pis.setHead(header);
							   pis.setBody(bodySTR);	
							   pis.setMap(map);		   
							   resultMap.put("PropertyIdentificationSet", pis);
						}
					}
				} else if(table.toPlainTextString().indexOf("Actual Value") > -1) {
					resultMap.put("PropertyAppraisalSet.LandAppraisal", table.getRows()[1].getColumns()[0].toPlainTextString()
							.replaceAll("[,$]","").trim());
					resultMap.put("PropertyAppraisalSet.ImprovementAppraisal", table.getRows()[2].getColumns()[0].toPlainTextString()
							.replaceAll("[,$]","").trim());
					resultMap.put("PropertyAppraisalSet.TotalAppraisal", table.getRows()[3].getColumns()[0].toPlainTextString()
							.replaceAll("[,$]","").trim());
					resultMap.put("PropertyAppraisalSet.TotalAssessment", table.getRows()[3].getColumns()[1].toPlainTextString()
							.replaceAll("[,$]","").trim());
				} else if(table.toPlainTextString().indexOf("Balance Due") > -1) {
					resultMap.put("TaxHistorySet.TotalDue", table.getRows()[1].getColumns()[0].toPlainTextString()
							.replaceAll("[,$]","").trim());
					resultMap.put("TaxHistorySet.PriorDelinquent", table.getRows()[2].getColumns()[0].toPlainTextString()
							.replaceAll("[,$]","").trim());
				} else if(table.toPlainTextString().indexOf("Tax + Special Assessment Amount") > -1) {
					resultMap.put("TaxHistorySet.Year", table.getRows()[1].getColumns()[0].toPlainTextString().trim());
					resultMap.put("TaxHistorySet.BaseAmount", table.getRows()[1].getColumns()[2].toPlainTextString()
							.replaceAll("[,$]","").trim());
										
					String totalDue = (String) resultMap.get("TaxHistorySet.TotalDue");
					if (StringUtils.isEmpty(totalDue) || totalDue.equals("0.00")){
						resultMap.put("TaxHistorySet.AmountPaid", table.getRows()[1].getColumns()[2].toPlainTextString()
								.replaceAll("[,$]","").trim());
					}
				} else if(table.toPlainTextString().indexOf("Transaction Type") > -1) {//for Routt
					resultMap.put("TaxHistorySet.Year", table.getRows()[1].getColumns()[0].toPlainTextString().trim());
					String amountPaid = "0.00";
					
					if (!table.getRows()[1].getColumns()[1].toPlainTextString().toLowerCase().contains("charge")){
						resultMap.put("TaxHistorySet.BaseAmount", table.getRows()[1].getColumns()[2].toPlainTextString()
								.replaceAll("[,$]","").trim());
					}
					amountPaid += "+" + table.getRows()[1].getColumns()[2].toPlainTextString().replaceAll("[,$]","").trim();
					
					String currentYear = table.getRows()[1].getColumns()[0].toPlainTextString().trim();
					
					for (int j = 2; j < table.getRows().length; j++){
						if (table.getRows()[j].getColumns()[0].toPlainTextString().trim().equals(currentYear)){
							if (table.getRows()[j].getColumns()[1].toPlainTextString().toLowerCase().contains("tax amount")){
								resultMap.put("TaxHistorySet.BaseAmount", table.getRows()[j].getColumns()[2].toPlainTextString()
										.replaceAll("[,$]","").trim());
							}
							amountPaid += "+" + table.getRows()[j].getColumns()[2].toPlainTextString().replaceAll("[,$]","").trim();
						}
					}
					
					String totalDue = (String) resultMap.get("TaxHistorySet.TotalDue");
					if (StringUtils.isEmpty(totalDue) || totalDue.equals("0.00")){
						resultMap.put("TaxHistorySet.AmountPaid", GenericFunctions.sum(amountPaid, searchId));
					}
				} else if (table.toPlainTextString().indexOf("Property Class:") > -1){
					String propertyClass = table.getRows()[3].getColumns()[0].toPlainTextString().trim();
					if (propertyClass.toLowerCase().contains("condo")){
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdivisionName);
					}
				}
			}
			
			// Sales details
			List<List> body = new ArrayList<List>();
			
			for(int i = 0; i < salesTables.size(); i++) {
				TableTag table = (TableTag)salesTables.elementAt(i);
				List<String> line = new ArrayList<String>();
				for(int j = 0; j < 6; j++) {
					line.add(table.getRow(1).getColumns()[j].toPlainTextString().replaceAll("[,$]","").trim());
				}
				line.add(table.getRow(3).getColumns()[0].toPlainTextString().trim());
				line.add(table.getRow(3).getColumns()[1].toPlainTextString().trim());
				body.add(line);
			}
			if(body != null && body.size() > 0) {
				ResultTable resultTable = new ResultTable();
				String[] header = {"InstrumentNumber", "Book", "Page" , "InstrumentDate", "SalesPrice", "DocumentType", "Grantor", 	"Grantee"};
				resultTable = GenericFunctions2.createResultTable(body, header);
				resultMap.put("SaleDataSet", resultTable);
			}
			
			//parsing of legal must be before address parsing because we need the unit from legal
			ro.cst.tsearch.servers.functions.COPitkinAO.parseLegalSummary(resultMap, searchId);
			
			
			ro.cst.tsearch.servers.functions.COPitkinAO.parseNames(resultMap, searchId);
			if (crtCounty.equalsIgnoreCase("Pitkin")){
				ro.cst.tsearch.servers.functions.COPitkinAO.parseAddress(resultMap, searchId);
			} else if (crtCounty.equalsIgnoreCase("Routt")){
				ro.cst.tsearch.servers.functions.COPitkinAO.parseAddressCORouttTR(resultMap, searchId);
			}
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		
		return null;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		FilterResponse rejectNonRealEstateFilter = new RejectNonRealEstate(SearchAttributes.OWNER_OBJECT, searchId);
		rejectNonRealEstateFilter.setThreshold(new BigDecimal("0.65"));
		
		// search by PIN
		String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
		
		if(!isEmpty(pin)){
			if (pin.matches("[A-Z]\\d+")){
				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PROP_NO_IDX));			
				module.clearSaKeys();
				module.getFunction(0).forceValue(pin);  
				modules.add(module);
			} else {
				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));			
				module.clearSaKeys();
				module.getFunction(0).forceValue(pin);
				modules.add(module);
			}
		}
		
		// search by Address
		String strNo = getSearchAttribute(SearchAttributes.P_STREETNO);
		String strName = getSearchAttribute(SearchAttributes.P_STREETNAME);
		String city = getSearchAttribute(SearchAttributes.P_CITY);
		boolean hasAddress = !isEmpty(strNo) && !isEmpty(strName);
		if(hasAddress){
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(strName);  
			module.getFunction(1).forceValue(strNo);
			if(!isEmpty(city)) {
				module.getFunction(2).forceValue(city);
			}
			module.getFunction(3).forceValue("500");
			module.addFilter(rejectNonRealEstateFilter);
			module.addFilter(addressFilter);
			modules.add(module);			
		}
		
		// search by name - filter by address
		if(hasOwner()) {
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.getFunction(1).forceValue("500");
			module.addFilter(rejectNonRealEstateFilter);
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module));
			module.addFilter(addressFilter);
			
			module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, new String[] {"L F M;;","L F;;","L f;;"});
			module.addIterator(nameIterator);
			
			modules.add(module);			
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
	
	private String getDetails(String rsResponse, StringBuilder accountId, String requestLink) {
		
		try {
			StringBuilder details = new StringBuilder();
			String accountNo = null;
			
			//Parcel details
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "70%"), true);
			NodeList subtitles = nodeList.extractAllNodesThatMatch(new TagNameFilter("h3"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("align", "center"), true);
			
			if(requestLink == null) {  //case ID_SAVE_TO_TSD
				TableTag table = (TableTag)tables.elementAt(0);
				accountId.append(table.getRows()[1].getColumns()[2].toPlainTextString());
				return rsResponse;
			}

			details.append("<div align=\"center\"><h2>Parcel Detail Information</h2>");
			for(int i = 0, j = 0; i < tables.size(); i++) {
				TableTag table = (TableTag)tables.elementAt(i);
				
				try { 
					if(i == 0) {
						accountNo = table.getRows()[1].getColumns()[1].toPlainTextString().trim();
						if (StringUtils.isNotEmpty(table.getRows()[1].getColumns()[1].toPlainTextString().trim())){
							accountId.append(table.getRows()[1].getColumns()[1].toPlainTextString().trim());
						} else {
							accountId.append(table.getRows()[1].getColumns()[2].toPlainTextString().trim());
						}
					} else if(i >= 1 && i <= 3) {
						details.append("<h3 align=\"center\">" + subtitles.elementAt(j++).toPlainTextString() + "</h3>");
					} else if(table.toPlainTextString().indexOf("Actual Value") > -1
							|| table.toPlainTextString().indexOf("Number of Residential Buildings") > -1
							|| table.toPlainTextString().indexOf("Balance Due") > -1
							|| table.toPlainTextString().indexOf("Document Type:") > -1
							|| table.toPlainTextString().indexOf("Transaction Type") > -1) {
						details.append("<h3 align=\"center\">" + subtitles.elementAt(j++).toPlainTextString() + "</h3>");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
						
				table.setAttribute("id", "parcel_table");
				details.append(table.toHtml());
			}
			details.append("</div>");
			
			//Value details
			String valueDetailLink = requestLink.replaceAll("(?i)Parcel[.]asp.*", "Value.asp?AccountNumber=" + accountNo);
			String valueDetailHtml = getLinkContents(valueDetailLink);
			
			htmlParser = org.htmlparser.Parser.createParser(valueDetailHtml, null);
			nodeList = htmlParser.parse(null);
			
			NodeList valueTables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"), true);
			NodeList valueSubtitles = nodeList.extractAllNodesThatMatch(new TagNameFilter("h3"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("align", "center"), true);
		
			if(valueTables != null && valueTables.size() > 0) {
				details.append("<div align=\"center\"><h2>Value Detail Information</h2>");
				for(int i = 0; i < valueTables.size(); i++) {
					TableTag table = (TableTag)valueTables.elementAt(i);
					details.append("<h3 align=\"center\">" + valueSubtitles.elementAt(i).toPlainTextString() + "</h3>");
					table.setAttribute("id", "value_table");
					details.append(table.toHtml());
				}
				details.append("</div>");
			}
			
			//Sales details
			String salesDetailLink = requestLink.replaceAll("(?i)Parcel[.]asp.*", "Sales.asp?AccountNumber=" + accountNo);
			String salesDetailHtml = getLinkContents(salesDetailLink);
			
			htmlParser = org.htmlparser.Parser.createParser(salesDetailHtml, null);
			nodeList = htmlParser.parse(null);
			
			NodeList salesTables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"), true);
		
			if(salesTables != null && salesTables.size() > 0) {
				details.append("<div align=\"center\"><h2>Sales Detail Information</h2>");
				for(int i = 0; i < salesTables.size(); i++) {
					TableTag table = (TableTag)salesTables.elementAt(i);
					table.setAttribute("id", "sales_table");
					String tableHtml = table.toHtml();
					// remove links
					Pattern linkPattern = Pattern.compile("<a\\s+.*?>(.*?)</a>");
					Matcher linkMatcher = linkPattern.matcher(tableHtml);
					while(linkMatcher.find()) {
						tableHtml = tableHtml.replace(linkMatcher.group(0), linkMatcher.group(1));
					}
					details.append(tableHtml);
				}
				details.append("</div>");
			}
			
			//Land details
			String landDetailLink = requestLink.replaceAll("(?i)Parcel[.]asp.*", "Land.asp?AccountNumber=" + accountNo);
			String landDetailHtml = getLinkContents(landDetailLink);
			
			htmlParser = org.htmlparser.Parser.createParser(landDetailHtml, null);
			nodeList = htmlParser.parse(null);
			
			NodeList landTables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "70%"), true);
		
			if(landTables != null && landTables.size() > 0) {
				details.append("<div align=\"center\"><h2>Land Detail Information</h2>");
				for(int i = 0; i < landTables.size(); i++) {
					TableTag table = (TableTag)landTables.elementAt(i);
					table.setAttribute("id", "land_table");
					details.append(table.toHtml());
				}
				details.append("</div>");
			}
			
			return details.toString();
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTableList = nodeList
				.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"));
			
			if(mainTableList.size() != 1) {
				return intermediaryResponse;
			}
			
			TableTag tableTag = (TableTag) mainTableList.elementAt(0);
			TableRow[] rows  = tableTag.getRows();
		
			for (int i = 0; i < rows.length; i++) {
				TableRow row = rows[i];
				
				String rowPlainTextString = row.toPlainTextString().trim();
				
				if(rowPlainTextString.isEmpty()
						|| rowPlainTextString.contains("Account Number") 
						|| rowPlainTextString.contains("Physical Address")) {
					continue;
				}
				
				TableRow additionalRow = rows[++i];
					
				LinkTag linkTag = (LinkTag)row.getColumns()[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0);
				String link = CreatePartialLink(TSConnectionURL.idGET) + "/assessor/" + linkTag.extractLink().trim().replaceAll("\\s", "%20");
				linkTag.setLink(link);
				
				String recordHtml = row.toHtml() + additionalRow.toHtml();
					
				ParsedResponse currentResponse = new ParsedResponse();
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, recordHtml);
				currentResponse.setOnlyResponse(recordHtml);
				currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
				
				ResultMap m = ro.cst.tsearch.servers.functions.COPitkinAO.parseIntermediaryRow(crtCounty, row, additionalRow, searchId);
				Bridge bridge = new Bridge(currentResponse, m, searchId);
				
				DocumentI document = null;
				if (crtCounty.equalsIgnoreCase("Pitkin")){
					document = (AssessorDocumentI)bridge.importData();
				} else {
					document = (TaxDocumentI)bridge.importData();
				}
								
				currentResponse.setDocument(document);
				
				intermediaryResponse.add(currentResponse);
			}
			
			outputTable.append(table);
		}
		catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}
}
