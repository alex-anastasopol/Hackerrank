package ro.cst.tsearch.servers.types;

/**
*
* implements the following counties from NV: Clark (including City of North Las Vegas, City of Las Vegas and City of Henderson), 
* Douglas and Washoe (including City of Reno)
*
*/

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.pin.AMPinFilterResponse;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.AssessorDocument;
import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.AssessorManagementDocument;
import com.stewart.ats.base.document.AssessorManagementDocumentI;
import com.stewart.ats.base.document.DocumentI;

public class NVGenericCornerAM extends TSServer {
	
	static final long serialVersionUID = 737484975686364L;
	
	public NVGenericCornerAM(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	public NVGenericCornerAM(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch (viParseID) {
		case ID_SEARCH_BY_PARCEL:
		case ID_SEARCH_BY_NAME:	
		case ID_SEARCH_BY_ADDRESS:
			
			if (rsResponse.indexOf("No Records were found in the database") > -1) {
				Response.getParsedResponse().setError("No results found in the database.");
				return;
			}
			
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
			
			if(smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
            }
			
			break;	
				
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
			ParseResponse(sAction, Response, ID_DETAILS);
			break;	
		
		default:
			break;
		}
		
	}	
		
	protected String getDetails(String rsResponse, StringBuilder parcelNumber) {
		try {
			
			StringBuilder details = new StringBuilder();
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
									
			/* If from memory - use it as is */
			if(!rsResponse.toLowerCase().contains("<html")){
				NodeList parcelList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "98%"));
				TableTag table = (TableTag)parcelList.elementAt(0);
				String parcelID = table.getRow(2).getColumns()[0].toPlainTextString().trim();
				String district = table.getRow(2).getColumns()[1].toPlainTextString();
				int index = district.indexOf("/");
				district = district.substring(0, index).replaceAll("\\s", "");
				if (district.length()!=0) parcelID += "_" + district;
				parcelNumber.append(parcelID);
								
				return rsResponse;
			}
			
			NodeList parcelList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "98%"));
			TableTag table = (TableTag)parcelList.elementAt(0);
			String parcelID = table.getRow(2).getColumns()[0].toPlainTextString().trim();
			String district = table.getRow(2).getColumns()[1].toPlainTextString();
			int index = district.indexOf("/");
			district = district.substring(0, index).replaceAll("\\s", "");
			if (district.length()!=0) parcelID += "_" + district;
			parcelNumber.append(parcelID);
			
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "98%"));
			
			String link = "";							//link for Payoff window
			Matcher matcher = Pattern.compile("popup_payoff.*?'").matcher(tables.elementAt(1).toHtml());
			if (matcher.find()) link = matcher.group(0);
			link = "http://amgnv.com/" + link.substring(0, link.length()-1);
			
			if (tables.size() >=6 )
			{
				details.append(tables.elementAt(0).toHtml() + "<br>");
				details.append(tables.elementAt(1).toHtml().replaceAll("(?is)<a.*?>", "").replaceAll("(?is)</a>", "") + "<br>");	//remove link for Payoff window
				details.append(tables.elementAt(2).toHtml());
				details.append(tables.elementAt(3).toHtml().replaceAll("(?is)<input.*?>", "") + "<br>");	//remove "Pay Online" button
				details.append(tables.elementAt(4).toHtml() + "<br>");
				details.append(tables.elementAt(5).toHtml() + "<br>");
			}
			
			if (link.length() > 0)					//get the content of Payoff window
			{
				String newDetails = "";
				HttpSite payoffHttpSite = HttpManager.getSite(getCurrentServerName(), searchId);	
				try {
					newDetails = ((ro.cst.tsearch.connection.http2.NVGenericCornerAM)payoffHttpSite).getPage(link);
				} catch(Exception e) {
					e.printStackTrace();
				} finally {
					HttpManager.releaseSite(payoffHttpSite);
				}
				
				htmlParser = org.htmlparser.Parser.createParser(newDetails, null);
				nodeList = htmlParser.parse(null);
				tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				 	.extractAllNodesThatMatch(new HasAttributeFilter("width", "275"));	
				details.append("<table id=\"payoff\" width=\"325\">");
				if (tables.size() == 2)
				{
					details.append("<tr><td>" + tables.elementAt(0).toHtml() + "</td></tr>");
					details.append("<tr><td>" + tables.elementAt(1).toHtml() + "</td></tr>");
				}
				details.append("</table>");
			}
			
			return details.toString();
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	protected String createPartialLink(int iActionType, int dispatcher) {
		StringBuilder build = new StringBuilder(msRequestSolverName);
		build.append("?");
		build.append(msServerID);
		build.append("&");
		build.append(RequestParams.SEARCH_ID);
		build.append("=");
		build.append(mSearch.getSearchID());
		build.append("&");
		build.append("dispatcher=");
		build.append(dispatcher);
		return build.toString();
	}
		
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		TableTag resultsTable = null;
		String header = "";
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTable = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width","98%"), true);
				
			if (mainTable.size() != 0)
				resultsTable = (TableTag)mainTable.elementAt(0); 
			
			//if there are results
			if (resultsTable != null && resultsTable.getRowCount() != 0)		
			{
				TableRow[] rows  = resultsTable.getRows();
				
				//row 0 is the header
				for (int i = 1; i < rows.length; i++) {
					TableRow row = rows[i];
										
					String htmlRow = row.toHtml();
					ParsedResponse currentResponse = new ParsedResponse();
					
					String link = row.getColumns()[3].toHtml();
					Matcher matcher = Pattern.compile("(?is)href=\"(.*?)\"").matcher(link);
					if (matcher.find()) link = matcher.group(1).trim(); 
										
					link = CreatePartialLink(TSConnectionURL.idGET) + link;
					
					//replace the links
					htmlRow = htmlRow.replaceAll("(?is)href=\".*?\"", "href=" + link);
										
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
									
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,htmlRow);
					currentResponse.setOnlyResponse(htmlRow);
					
					ResultMap m = ro.cst.tsearch.servers.functions.NVGenericCornerAM.parseIntermediaryRow( row, searchId ); 
					Bridge bridge = new Bridge(currentResponse, m, searchId);
					
					DocumentI document = (AssessorDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				}
								
				header = "<table width=\"98%\" cellpadding=\"2\" cellspacing=\"1\" border=\"0\" align=\"center\">" +
						 "<tr bgcolor=\"#666666\">" +  
						 "<td width=\"40%\"><b><font color=\"#f8f8f8\" size=\"2\" face=\"Arial, Helvetica, sans-serif\">Name</font></b></td>" +
						 "<td width=\"5%\"><b><font face=\"Arial, Helvetica, sans-serif\" size=\"2\" color=\"#f8f8f8\">Street Number</font></b></td>" +
						 "<td width=\"40%\"><b><font color=\"#f8f8f8\" size=\"2\" face=\"Arial, Helvetica, sans-serif\">Street Name</font></b></td>" +
						 "<td width=\"10%\"><div align=\"right\"><b><font color=\"#f8f8f8\" size=\"2\" face=\"Arial, Helvetica, sans-serif\">Parcel#</font></b></div></td>" +
						 "<td width=\"5%\"><center><b><font face=\"Arial, Helvetica, sans-serif\" size=\"2\" color=\"#f8f8f8\">District</font></b></center></td></tr>";
				
				response.getParsedResponse().setHeader(header);
				response.getParsedResponse().setFooter("</table>");
																
				outputTable.append(table);
			}
			
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap) {
	
		try {
			
			detailsHtml = detailsHtml.replaceAll("(?is)\r\n", "").replaceAll("(?is)\\s{2,}", " ");
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"AM");
				
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
							
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "98%"));
			TableTag table = (TableTag)tableList.elementAt(0);
			String parcelID = table.getRow(2).getColumns()[0].toPlainTextString().trim();
			
			String name = table.getRow(2).getColumns()[2].toPlainTextString().trim();
			resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name);
			
			String status = table.getRow(2).getColumns()[3].toPlainTextString().trim();
			if (status.length()!=0){
				status = status.substring(0,1).toUpperCase() + status.substring(1).toLowerCase();
				resultMap.put(OtherInformationSetKey.REMARKS.getKeyName(), "Status:" + status);
			} 
			
			String district = table.getRow(2).getColumns()[1].toPlainTextString();
			int index = district.indexOf("/");
			district = district.substring(0, index).replaceAll("\\s", "");
			if (district.length() != 0){
				parcelID += "_" + district;
				resultMap.put(PropertyIdentificationSetKey.DISTRICT.getKeyName(), district);
			}
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
			
			table = (TableTag)tableList.elementAt(1);
			String address = table.getRow(1).getColumns()[0].toPlainTextString().trim();
			resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
			
			String initialPrincipal = table.getRow(1).getColumns()[1].toPlainTextString().trim();
			if (StringUtils.isNotEmpty(initialPrincipal)){
				initialPrincipal = initialPrincipal.replaceAll("[,\\$]+", "").replaceAll("(?is)&nbsp;", " ");
				resultMap.put("tmpInitialPrincipal", initialPrincipal.trim());
			}
			
			String legal = table.getRow(2).getColumns()[0].toPlainTextString().replaceAll("&nbsp;", "").trim();
			resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legal);
			
			table = (TableTag)tableList.elementAt(5);
			String county = table.getRow(0).getColumns()[1].toPlainTextString().toLowerCase();
			if (county.contains("clark") || county.contains("vegas") || county.contains("henderson")) resultMap.put(PropertyIdentificationSetKey.COUNTY.getKeyName(), "Clark");
			else if (county.contains("douglas")) resultMap.put(PropertyIdentificationSetKey.COUNTY.getKeyName(), "Douglas");
			else if (county.contains("reno") || county.contains("washoe")) resultMap.put(PropertyIdentificationSetKey.COUNTY.getKeyName(), "Washoe");
								
			ro.cst.tsearch.servers.functions.NVGenericCornerAM.parseNames(resultMap, searchId);
			ro.cst.tsearch.servers.functions.NVGenericCornerAM.parseAddress(resultMap);
			ro.cst.tsearch.servers.functions.NVGenericCornerAM.parseLegalSummary(resultMap);
			
			
			tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "payoff"));
			if (tableList.size() > 0){

				String prepaidPrincipal = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodeList, "Prepaid Principal"), "", false);
				if (StringUtils.isNotEmpty(prepaidPrincipal)){
					prepaidPrincipal = prepaidPrincipal.replaceAll("[,\\$]+", "").replaceAll("(?is)&nbsp;", " ");
					resultMap.put("tmpPrepaidPrincipal", prepaidPrincipal.trim());
				}
				
				String currentDue = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodeList, "Current Du"), "", false);
				if (StringUtils.isNotEmpty(currentDue)){
					currentDue = currentDue.replaceAll("[,\\$]+", "").replaceAll("(?is)&nbsp;", " ");
					resultMap.put("tmpCurrentDue", currentDue.trim());
				}
				
				String totalPayoff = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodeList, "Total Payoff"), "", false);
				if (StringUtils.isNotEmpty(totalPayoff)){
					totalPayoff = totalPayoff.replaceAll("[,\\$]+", "").replaceAll("(?is)&nbsp;", " ");
					resultMap.put("tmpTotalPayoff", totalPayoff.trim());
				}
				
				String dueDates = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(nodeList, "Due Dates"), "", false).trim();
				if (StringUtils.isNotEmpty(dueDates)){
					dueDates = dueDates.replaceAll("\\bDue\\s+Dates\\s*:", "").replaceAll("(?is)&nbsp;", " ");
					resultMap.put("tmpDueDates", dueDates.trim());
				}
				
				String finalPayment = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(nodeList, "Final Payment"), "", false).trim();
				if (StringUtils.isNotEmpty(finalPayment)){
					finalPayment = finalPayment.replaceAll("\\bFinal Payment\\s*:", "").replaceAll("(?is)&nbsp;", " ");
					resultMap.put("tmpFinalPayment", finalPayment.trim());
				}
			}
		
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
	
	@Override
	public DocumentI smartParseDetails(ServerResponse response, String detailsHtml, boolean fillServerResponse){
		DocumentI document = null;
		try {
			ResultMap map = new ResultMap();
			
			parseAndFillResultMap(response,detailsHtml, map);
			
			String finalPayment = (String) map.get("tmpFinalPayment");
			String prepaidPrincipal = (String) map.get("tmpPrepaidPrincipal");
			String currentDue = (String) map.get("tmpCurrentDue");
			String totalPayoff = (String) map.get("tmpTotalPayoff");
			String dueDates = (String) map.get("tmpDueDates");
			String initialPrincipal = (String) map.get("tmpInitialPrincipal");
			
			map.removeTempDef();//this is for removing tmp items. we remove them here to not remove them in every place when we parse something.
			
			Bridge bridge = new Bridge(response.getParsedResponse(),map,searchId);
			document = bridge.importData();
			
			document.setNote((String)map.get(OtherInformationSetKey.REMARKS.getKeyName()));
			
			document = new AssessorManagementDocument((AssessorDocument) document);
			
			document.getInstrument().setDocSubType("ASSESSMENT");
			
			((AssessorManagementDocumentI) document).setDistrict((String) map.get(PropertyIdentificationSetKey.DISTRICT.getKeyName()));
			((AssessorManagementDocumentI) document).setFinalPayment(finalPayment);
			try {
				((AssessorManagementDocumentI) document).setPrepaidPrincipal(Double.valueOf(prepaidPrincipal));
			} catch (Exception e) {
			}
			try {
				((AssessorManagementDocumentI) document).setCurrentDue(Double.valueOf(currentDue));
			} catch (Exception e) {
			}
			try {
				((AssessorManagementDocumentI) document).setTotalPayoff(Double.valueOf(totalPayoff));
			} catch (Exception e) {
			}
			((AssessorManagementDocumentI) document).setDueDatesAM(dueDates);
			try {
				((AssessorManagementDocumentI) document).setInitialPrincipal(Double.valueOf(initialPrincipal));
			} catch (Exception e) {
			}
			
			document.updateDescription();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(fillServerResponse) {
			response.getParsedResponse().setResponse(detailsHtml);
			if(document!=null) {
				response.getParsedResponse().setDocument(document);
			}
		}
		return document;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
		
		TSServerInfoModule module = null;
		
		if(hasPin()) {
			FilterResponse pinFilter = new AMPinFilterResponse(SearchAttributes.LD_PARCELNO, searchId);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			String county = getDataSite().getCountyName().replace(" ", "").toLowerCase();
			if (county.equals("washoe")) {
				String pid = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO).replaceAll("(?is)\\p{Punct}", "");
				if (!pid.contains("-")) {
					// Convert APN from dddddddd to ddd-ddd-dd
					pid = pid.replaceAll("\\p{Punct}", "");
					pid = pid.replaceFirst("(\\d{3})(\\d{3})(\\d{2})", "$1-$2-$3");
					module.setData(0, pid);
				}
			}
			else {
				module.setSaKey(0, SearchAttributes.LD_PARCELNO);
			}
			module.addFilter(pinFilter);
			moduleList.add(module);
		}
				
		serverInfo.setModulesForAutoSearch(moduleList);
	}
	
}
