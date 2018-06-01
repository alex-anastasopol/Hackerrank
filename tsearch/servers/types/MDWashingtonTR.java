package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.bean.recoverdocument.ModuleShortDescription;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.log.SearchLogFactory;
import ro.cst.tsearch.log.SearchLogPage;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.threads.GPMaster;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.SearchLogger;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

public class MDWashingtonTR extends TSServer {

	private static final long serialVersionUID = 1L;

	
	public MDWashingtonTR(long searchId) {
		super(searchId);
	}
	
	
	public MDWashingtonTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		
		TableColumn[] cols = row.getColumns();
		if(cols.length == 3) {
			String pid = cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true)
					.elementAt(0).getChildren().toHtml().trim();
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pid);
			
			String address = cols[2].getChildrenHTML().trim();
			address = address.replaceAll("\\s+", " ");
			
			if (StringUtils.isNotEmpty(address)) {
				resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
				resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
				resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
			}
			
			

		}
		return resultMap;
	}
	
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		String regExp = "(?is).*<table[^>]+>\\s*<tr[^>]+>\\s*<td[^>]+>\\s*Parcel No[^>]+>\\s*<td[^>]+>\\s*Bill No[^>]+>\\s*<td[^>]+>Address[^>]+>\\s*</tr>\\s*</table>.*";
		
		if (table.matches(regExp)) {
			return intermediaryResponse;
		
		} else
		try {
			HtmlParser3 htmlParser = new HtmlParser3(table);	
			NodeList nodeList = htmlParser.getNodeList().extractAllNodesThatMatch(new HasAttributeFilter("id", "mainContent"), true);
						
			if(nodeList.size() == 0) {
				return intermediaryResponse;
			}
			
			TableTag tableTag = (TableTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
			tableTag.setAttribute("id", "\"intermResults\"");
			
			if (tableTag != null) {
				TableRow[] rows  = tableTag.getRows();
				
				for (int i = 1; i < rows.length; i++) {
					TableRow row = rows[i];
					if(row.getColumnCount() == 3) {
						ParsedResponse currentResponse = new ParsedResponse();
						LinkTag linkTag = ((LinkTag)row.getColumns()[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0));
						String link = linkTag.extractLink().trim();
						link = link.replaceAll("\\s", "%20").replaceAll("&amp;", "&");
						
						link = CreatePartialLink(TSConnectionURL.idGET) + "/retax/" + link;
						
						linkTag.setLink(link);
						
						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
						currentResponse.setOnlyResponse(row.toHtml());
						currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
						
						ResultMap map = parseIntermediaryRow(row, searchId); 
						Bridge bridge = new Bridge(currentResponse, map, searchId);
						
						DocumentI document = (TaxDocumentI)bridge.importData();				
						currentResponse.setDocument(document);
						
						intermediaryResponse.add(currentResponse);					
					}
				}
			}

			response.getParsedResponse().setHeader("<table border=\"1\" cellspacing=\"0\" cellpadding=\"3\">\n" + "<tr><th>Parcel No.</th> <th>Bill No.</th> <th>Address</th></tr>");
			response.getParsedResponse().setFooter("</table>");
		
			outputTable.append(table);
						
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}
	
	
	String getTaxYear (NodeList list, String id) {
		String taxYear = "";
		
		Node year = list.extractAllNodesThatMatch(new HasAttributeFilter("id", id), true).elementAt(0);
		
		if (year != null) {
			String tmp = year.getFirstChild().getFirstChild().toHtml().trim();
			if (StringUtils.isNotEmpty(tmp)) {
				taxYear = tmp.trim();
			}
		}
		
		return taxYear;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		// 	no result returned - error message on official site
		if (rsResponse.contains("An unexpected error has occurred:")) {
			Response.getParsedResponse().setError("Official site not functional");
    		return;
    	} else if (rsResponse.indexOf("No results found for query") > -1) {
			Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
			return;
    	}
		
		switch (viParseID) {			
			case ID_SEARCH_BY_ADDRESS:		
			case ID_SEARCH_BY_PARCEL:	
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
								
				StringBuilder accountId = new StringBuilder();
				String details = getDetails(rsResponse, accountId);
				String filename = accountId + ".html";
				
				if (viParseID != ID_SAVE_TO_TSD) {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
	
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","CNTYTAX");
					data.put("year", getTaxYear(new HtmlParser3(details).getNodeList(), "taxYear"));
					
					if (isInstrumentSaved(accountId.toString(),null,data)){
						details += CreateFileAlreadyInTSD();
					
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);					
						details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
					}
	
					Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
					Response.getParsedResponse().setResponse(details);
					
				} else {
					smartParseDetails(Response,details);
					
					msSaveToTSDFileName = filename;
					Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
					Response.getParsedResponse().setResponse(details);
					
					msSaveToTSDResponce = details + CreateFileAlreadyInTSD();					
				}
				break;
				
			case ID_GET_LINK :
				if (rsResponse.contains("Property Owner(s)")) {
					ParseResponse(sAction, Response,ID_DETAILS);
				}
				break;
				
			default:
				break;
		}
	}	

	
	protected String getDetails(String rsResponse, StringBuilder accountId) {
		try {
			/* If from memory - use it as is */
			if(!rsResponse.contains("<html")){
				return rsResponse;
			
			} else {
				try {
					StringBuilder details = new StringBuilder();	
					HtmlParser3 htmlParser = new HtmlParser3(rsResponse);
					NodeList nodeList = htmlParser.getNodeList()
							.extractAllNodesThatMatch(new TagNameFilter("div"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("id", "mainContent"), true);				
					
					if(nodeList.size() == 0) {
						return rsResponse;
					
					} else {						
						NodeList detailsList = nodeList.elementAt(0).getChildren()
								.extractAllNodesThatMatch(new TagNameFilter("table"), true);

						if (detailsList != null) {
							TableTag ownerInfo = (TableTag) detailsList.elementAt(0);
							ownerInfo.setAttribute("id", "\"ownerInfo\"");
							ownerInfo.removeAttribute("width");
							ownerInfo.setAttribute("width", "\"600\"");
							
							details.append("<table border = \"1\">");
							details.append("<tr align=\"center\"><td colspan=\"6\"></br> <b> Real Estate Property Tax Inquiry & Payment </b></td></tr>");		
							details.append("<tr><td colspan=\"6\">");
							
							String content = ownerInfo.toHtml().trim();
							content = content.replaceAll("(?is)<span class=\"txt_reg_bold\">([^<]+)</span>", "$1");
							content = content.replaceAll("(?is)<tr class=\"txt_reg_bold\">(\\s*<[^>]+>\\s*<div[^>]+>)([^<]+)(\\s*</div>)", "<tr>" + "$1" + "<b>" + "$2" + "</b>" + "$3");		
									
							details.append(content);
							details.append("</br>");
							
							TableTag personalInfo = (TableTag) detailsList.elementAt(1);
							personalInfo.removeAttribute("id");
							personalInfo.setAttribute("id", "\"TaxInfoTable\"");
							personalInfo.removeAttribute("width");
							personalInfo.setAttribute("width", "\"600\"");
								
							if (personalInfo.getRowCount() > 7) {
								TableColumn col = personalInfo.getRows()[1].getColumns()[0];
								col.setAttribute("id", "\"parcelID\"");
								String apn = col.getChild(0).getFirstChild().toHtml().trim();
								apn = apn.replaceFirst("(\\d+{2})\\s*-\\s*(\\d+)", "$1" + "$2");
								accountId.append(apn);
								
								col = personalInfo.getRows()[1].getColumns()[1];
								col.setAttribute("id", "\"taxYear\"");
								
								col = personalInfo.getRows()[2].getColumns()[0];
								col.setAttribute("id", "\"legalDesc\"");
								
//								TableRow row = personalInfo.getRows()[7];
//								row.setAttribute("id", "\"taxDetails\"");
							}
							
							content = personalInfo.toHtml();
							content = content.replaceAll("(?is)<span class=\"txt_reg_bold\"></span>", "");
							content = content.replaceAll("(?is)<div[^>]+></div>", "");
							content = content.replaceAll("(?is)<tr[^>]+>\\s*<td[^>]+></td>\\s*<td[^>]+>\\s*</td>\\s*<td[^>]+>\\s*</td>\\s*(?:<td[^>]+>\\s*</td>\\s*)?</tr>", "");
							content = content.replaceAll("(?is)<tr\\s*class=\"txt_reg\"\\s*>", "<tr>");
							
							content = content.replaceAll("(?is)<span class=\"title_text\">([^<]+)</span>", "<b> $1 </b>");
							content = content.replaceAll("(?is)<span class=\"txt_reg_bold\">([^<]+)</span>", "$1");
							

							if (content.contains("TOTAL TAX")) {
								content = content.replaceFirst("(?is)<tr>(\\s*<td[^>]+>\\s*</td>\\s*<td[^>]+>.*TOTAL TAX)", "<tr id=\"baseAmount\">$1");
							}
							if (content.contains("PRIOR PAYMENTS")) {
								content = content.replaceFirst("(?is)<tr>(\\s*<td[^>]+>\\s*</td>\\s*<td[^>]+><div[^>]+>\\s*PRIOR PAYMENTS)", "<tr id=\"amountPaid\">$1");
							}
							if (content.contains("TOTAL DUE")) {
								content = content.replaceFirst("(?is)<tr>(\\s*<td[^>]+>\\s*</td>\\s*<td[^>]+>(?:\\s*<div[^>]+>\\s*)?TOTAL DUE)", "<tr id=\"amountDue\">$1");
							}
							
							
							details.append("<tr><td colspan=\"6\">");
							details.append(content + "</td></tr>");
							
							details.append("</table> </br>");
						
							return details.toString();
						}
					}
				} catch (Throwable t){
					logger.error("Error while parsing details page data", t);
				}
			}
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return rsResponse;
	}

	
	@Override
	protected Object parseAndFillResultMap(ServerResponse rsResponse, String detailsHtml, ResultMap map) {
		try {	
			String accountID = "";
			String names = "";
			String address = ""; 
			String legalDesc  = "";
			String currentTaxYear = "";
			
			map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"TR");
			
			HtmlParser3 htmlParser = new HtmlParser3(detailsHtml);
			NodeList list = htmlParser.getNodeList();		
			
			Node parcelInfo = list.extractAllNodesThatMatch(new HasAttributeFilter("id", "parcelID"), true).elementAt(0);
			if (parcelInfo  != null) {
				accountID = parcelInfo.getFirstChild().toHtml().trim();
				accountID = accountID.replaceFirst("(?is)(?:<div[^>]+>)?(\\d{2})\\s*-\\s*(\\d{6})\\s*(?:</div>)?", "$1" + "$2");
			}
			
			
			if (StringUtils.isNotEmpty(accountID)) {
				map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountID);
			}
			
			
			if (detailsHtml.contains("id=\"taxYear\"")) {
				currentTaxYear = list.extractAllNodesThatMatch(new HasAttributeFilter("id", "taxYear"), true)
						.elementAt(0).getFirstChild().toHtml().trim();
				currentTaxYear = currentTaxYear.replaceFirst("(?is)(?:<div[^>]+>)?(\\d{4})\\s*(?:</div>)?", "$1");
			}
			else 
				currentTaxYear = FormatDate.getDateFormat(FormatDate.PATTERN_yyyy).format(dataSite.getPayDate());
			
			if (StringUtils.isNotEmpty(currentTaxYear)) {
				map.put(TaxHistorySetKey.YEAR.getKeyName(), currentTaxYear);
			}
			
			TableTag tmpTbl = (TableTag) list.extractAllNodesThatMatch(new HasAttributeFilter("id", "ownerInfo"), true).elementAt(0);
			if (tmpTbl != null) {
				if (tmpTbl.getRowCount() > 3) {
					TableRow row = tmpTbl.getRow(0);  //Owners names
					TableColumn col = null;
					if (row.getColumnCount() == 2) {
						col = row.getColumns()[1];
						names = col.getChildrenHTML().trim();
					}
					
					row = tmpTbl.getRow(1);  //add C/O names if available
					if (row.getColumnCount() == 2) {
						col = row.getColumns()[1];
						String info = col.getChildrenHTML().trim();
						info = info.replaceFirst("(?is)<span[^>]+>\\s*</span>", "");
						if (StringUtils.isNotEmpty(info)) {
							names += "; " + info;
						}
					}
					
					row = tmpTbl.getRow(2);  //Address
					if (row.getColumnCount() == 2) {
						col = row.getColumns()[1];
						address = col.getChildrenHTML().trim();
						address = address.replaceAll("\\s+", " ");
					}
				}
			}
			

			if (StringUtils.isNotEmpty(names)) {
				map.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(),names.trim());	
			}
			if (StringUtils.isNotEmpty(address)) {
				map.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
				map.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
				map.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
			}
			
			legalDesc = list.extractAllNodesThatMatch(new HasAttributeFilter("id", "legalDesc"), true)
					.elementAt(0).getChildren().toHtml().trim();
			legalDesc = legalDesc.replaceAll("(?is).*<span[^>]+>(.*)</span>", "$1");
			legalDesc = legalDesc.replaceFirst("(?is)<br\\s*/>\\s*\\d+[A-Z\\s-\\.',]+(?:STREET|ST|AVENUE|AVE|AV|DRIVE|DR|HGWY|HGW|RD|ROAD|CIRC|CIRCLE|LN|LANE|PIKE)\\s*<br\\s*/>([^<]+<br\\s*/>)\\s*(?:NOT A )?PRINCIPAL RESIDENCE", "$1");
			legalDesc = legalDesc.replaceFirst("(?is)(?:NOT A )?PRINCIPAL RESIDENCE", "");
			legalDesc = legalDesc.replaceFirst("(?is)<br\\s*/>\\s*\\d+[A-Z\\s-\\.',]+(?:STREET|ST|AVENUE|AVE|AV|DRIVE|DR|HGWY|HGW|RD|ROAD|CIRC|CIRCLE|LN|LANE|PIKE)?\\s*<br\\s*/>", "");
			
			if (StringUtils.isNotEmpty(legalDesc)) {
					map.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legalDesc);
			}	
				
			parseTaxes(list, map, searchId);
			parseLegalSummary(map, searchId);
			
			
			parseNames(map, searchId);
		
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		
		return null;	
	}
	
	
	public static void parseNames(ResultMap resultMap, long searchId) {
		String unparsedName = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		List<List> body = new ArrayList<List>();
		
		if(StringUtils.isNotEmpty(unparsedName)) {
			unparsedName = unparsedName.replaceAll("\\s*-\\s*(ETAL),?\\s*", " $1 - ");
			
			unparsedName = unparsedName.replaceAll("(?is)\\b(A|F)KA\\b", " - @@@FML@@@");
			unparsedName = unparsedName.replaceAll("(?is)\\s+-\\s+ETAL", " ETAL - ");
			
			String[] mainTokens = unparsedName.split("\\s*;\\s*");
											
			for (int i = 0; i < mainTokens.length; i++) {
				String currentToken = mainTokens[i];
				String[] names = currentToken.split("<br>");
					
				if (currentToken.matches("(?is)[\\w'-]+\\s*,\\s*(?:[A-Z]\\b\\s*\\w+|\\w+\\s*[A-Z])\\s*(?:II|I|SR|JR)?\\s*&\\s*(?:\\w+\\s*[A-Z]?\\s[A-Z]\\w+)")) {  
					//JUDGE ELLA & PATRICIA A DEMARTINI; NELSON KEVIN M & WENDY D SCOTT  
					names = StringFormats.parseNameDesotoRO(currentToken);
				
				} else {
					//NELSON RONALD K & RACHEL L;
					names = StringFormats.parseNameNashville(currentToken, true);	
				}
					
				String[] types = GenericFunctions.extractAllNamesType(names);
				String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
				String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);
					
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);												
			}
			
			try {
				GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	
	private void parseTaxes(NodeList list, ResultMap map, long searchId) {
		String baseAmt = "";
		String amtPaid = "";
		String amtDue = "";
		int index = 0;
		
		TableRow row = (TableRow) list.extractAllNodesThatMatch(new HasAttributeFilter("id", "amountDue"), true).elementAt(0);
		if (row != null) {
			index = row.getColumnCount() - 1;
			amtDue = row.getColumns()[index].getFirstChild().toHtml(); 
			amtDue = amtDue.replaceFirst("(?is)(?:<div[^>]+>)?([\\d\\.,]+)(?:</div>)?", "$1");
			amtDue = amtDue.replaceFirst(",", "");
			if (StringUtils.isNotEmpty(amtDue)) {
				map.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amtDue);
			}
		}
		
		row = (TableRow) list.extractAllNodesThatMatch(new HasAttributeFilter("id", "amountPaid"), true).elementAt(0);
		if (row != null) {
			if (row.getColumnCount() == 4)
				index = 3;
			else if (row.getColumnCount() == 3)
				index = 2;
			
			amtPaid = row.getColumns()[index].getFirstChild().toHtml(); 
			amtPaid = amtPaid.replaceFirst("(?is)(?:<div[^>]+>)?([\\d\\.,]+)(?:</div>)?", "$1");
			amtPaid = amtPaid.replaceFirst(",", "");
			if (StringUtils.isNotEmpty(amtPaid)) {
				map.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amtPaid);
			}
		}
		
		row = (TableRow) list.extractAllNodesThatMatch(new HasAttributeFilter("id", "baseAmount"), true).elementAt(0);
		if (row != null) {
			if (row.getColumnCount() == 4)
				index = 3;
			else if (row.getColumnCount() == 3)
				index = 2;
			
			baseAmt = row.getColumns()[index].getFirstChild().toHtml(); 
			baseAmt = baseAmt.replaceFirst("(?is)(?:<div[^>]+>)?([\\d\\.,]+)(?:</div>)?", "$1");
			baseAmt = baseAmt.replaceFirst(",", "");
			if (StringUtils.isNotEmpty(baseAmt)) {
				map.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmt);
			}
		}
	}


	public static void parseLegalSummary(ResultMap resultMap,long searchId) {
		String legalDescription = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		String lot = "";
		String blk = "";
		String unit = "";
		String phase = "";
		String subdiv = "";
		
		if(StringUtils.isEmpty(legalDescription)) {
			return;
		}
		
		legalDescription = legalDescription.replaceAll("(?is)(?:\\d+(?:\\.\\d+)?|\\.\\d+)\\s*(AC(?:RE?S?)?|A)\\b\\s*", "");
		legalDescription = legalDescription.replaceAll("(?is)(\\d+|[A-Z])\\s*&\\s*(\\d+|[A-Z])", " $1 " + "/" + " $2");
		legalDescription = legalDescription.replaceAll("\\s*<br\\s*/>\\s*"," ");
		legalDescription = legalDescription.replaceFirst("(?is)\\s*\\d+\\s*/\\d+\\s*(LO?TS?)", "$1");
		legalDescription = legalDescription.replaceFirst("(?is)\\b(?:LO?T)S?\\s*(\\d+|[A-Z](?:\\s*/\\s*(?:\\d+|[A-Z]))?)\\s*PT\\s*LTS?\\s*(\\d+|[A-Z](?:\\s*/\\s*(?:\\d+|[A-Z]))?)", "LOTS $1 $2");
		
		if (legalDescription.contains("LOT") || legalDescription.contains("PTLT") || legalDescription.contains("LT")) {
			lot = legalDescription.trim();
			if (lot.matches("(?is)\\bLO?TS?\\b\\s*([A-Z\\s</>])+")) {  //PID: 25-012623
				lot = lot.replaceAll("<\\s*br\\s*/\\s*>", "");
				lot = lot.replaceFirst("(?is)\\bLO?TS?\\b\\s*([A-Z\\s])+", "");
				
			} else if (lot.matches("(?is)(?:PT\\s*)?LO?TS?\\s*(\\d+)\\s*(?:\\.\\d+)?X(?:\\d+(?:[A-Z]+)?)?.*")) {
				//LOT 18.3X119
				lot = lot.replaceFirst("(?is)(?:PT\\s*)?LO?TS?\\s*(\\d+)\\s*(?:\\.\\d+)?X(?:\\d+(?:[A-Z]+)?)?.*", "$1");
				legalDescription = legalDescription.replaceFirst("(?is)(?:PT\\s*)?LO?TS?\\s*(\\d+)\\s*(?:\\.\\d+)?X(?:\\d+(?:[A-Z]+)?)?\\s*(?:\\.\\d+)?", "");
			
			} else if (lot.matches("(?is)(?:PT\\s*)?LO?TS?\\s*(\\d+)\\s*(/\\d+\\s*(?:/\\d+\\s*/?)?)\\s*(?:\\d+\\.)?\\d+X(?:\\d+(?:[A-Z]+)?)?")) {
				lot = lot.replaceFirst("(?is)(?:PT\\s*)?LO?TS?\\s*(\\d+)\\s*(/\\d+\\s*(?:/\\d+\\s*/?)?)\\s*(?:\\d+\\.)?\\d+X(?:\\d+(?:[A-Z]+)?)?", "$1" + "$2");
				lot = lot.replaceAll("/", " ");
				lot = lot.replaceAll("\\s+", " ");
				legalDescription = legalDescription.replaceFirst("(?is)(?:PT\\s*)?LO?TS?\\s*(\\d+)\\s*(/\\d+\\s*(?:/\\d+\\s*/?)?)\\s*(?:\\d+\\.)?\\d+X(?:\\d+(?:[A-Z]+)?)?\\s*(?:\\.\\d+)?", "");
				
			} else if (lot.matches("(?is)(?:PT\\s*)?LO?TS?\\s*(\\d+\\s*/\\s*\\d+|[A-Z]\\s*/\\s*[A-Z])\\s*(?:\\d+\\.)?\\d+X(?:\\d+(?:[A-Z]+)?)?.*")) {
				lot = lot.replaceFirst("(?is)(?:PT\\s*)?LO?TS?\\s*(\\d+\\s*/\\s*\\d+|[A-Z]\\s*/\\s*[A-Z])\\s*(?:\\d+\\.)?\\d+X(?:\\d+(?:[A-Z]+)?)?.*", "$1");
				legalDescription = legalDescription.replaceFirst("(?is)(?:PT\\s*)?LO?TS?\\s*(?:\\d+\\s*/\\s*\\d+|[A-Z]\\s*/\\s*[A-Z])\\s*(?:\\d+\\.)?\\d+X(?:\\d+(?:[A-Z]+)?)?\\s*(.*)", "$1");
			
			} else if (lot.matches("(?is)LO?TS?\\s*([A-Z]|\\d+)(?:\\s*/\\s*([A-Z]|\\d+))?\\s*PT\\s*(?:(?:\\d+|[A-Z])\\s*(?:/\\s*(?:\\d+|[A-Z]))?)(?:\\s*\\.\\s*)?.*")) {
				lot = lot.replaceFirst("(?is)LO?TS?\\s*(([A-Z]|\\d+)(?:\\s*/\\s*([A-Z]|\\d+))?)\\s*PT\\s*(?:(?:\\d+|[A-Z])\\s*(?:/\\s*(?:\\d+|[A-Z]))?).*", "$1");
				legalDescription = legalDescription.replaceFirst("(?is)LO?TS?\\s*(?:[A-Z]|\\d+)(?:\\s*/\\s*(?:[A-Z]|\\d+))?\\s*PT\\s*(?:(?:\\d+|[A-Z])\\s*(?:/\\s*(?:\\d+|[A-Z]))?)(?:\\s*\\.\\s*)?(.*)", "$1").trim();
				
			} else {
				lot = lot.replaceAll("(?is)(?:PT\\s*)?LO?TS?\\s*(\\d+(?:\\s*-\\d+)?)\\s+(?:\\d+\\.)?\\d+X(?:\\d+(?:[A-Z]+)?)?\\s*(?:\\.\\d+)?", "$1");
				if (lot.matches("(?is)(?:PT\\s*)?LO?TS?\\s*\\d+\\s*[A-Z'-,\\s]+")) {
					String regExp = "(?is)(?:PT\\s*)?LO?TS?\\s*(\\d+)\\s*([A-Z'-,\\s]+)";
					lot = lot.replaceFirst(regExp, "$1");
					legalDescription = legalDescription.replaceFirst(regExp, "$2");
					
				} else if (lot.matches("(?is)(?:LO?T)S?\\s*([\\d\\s]+).*")) {
					String regExp = "(?is)(?:LO?T)S?\\s*([\\d\\s]+)(.*)";
					lot = lot.replaceFirst(regExp, "$1");
					legalDescription = legalDescription.replaceFirst(regExp, "$2");
				
				} else if (lot.matches("(?is)\\d+\\s*[A-Z\\s]+")) {
					String regExp = "(?is)(\\d+)\\s*([A-Z\\s]+)";
					legalDescription = lot.replaceFirst(regExp, "$2").trim();
					lot = lot.replaceFirst(regExp, "$1");
				}
				legalDescription = legalDescription.replaceFirst("(?is)(?:PT\\s*)?LO?TS?\\s*(\\d+(?:\\s*-\\d+)?)\\s+(?:\\d+\\.)?\\d+X(?:\\d+(?:[A-Z]+)?)?\\s*(?:\\.\\d+)?", "");
			}
			
			lot = lot.replaceAll("\\s*-\\s*", " ");
			lot = lot.replaceAll("\\s*/\\s*", " ");
			
			if(StringUtils.isNotEmpty(lot))  {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), Roman.normalizeRomanNumbers(lot));	
			}
		}
		
		if (legalDescription.contains("UNIT") || legalDescription.contains("UN ")) {
			unit = legalDescription;
			unit = unit.replaceFirst("(?is)\\bUN(?:IT)?\\b\\s*([A-Z\\d-]+).*", "$1");
			legalDescription = legalDescription.replaceFirst("(?is)\\bUN(?:IT)?\\b\\s*([A-Z\\d-]+)", "");
			
			if(StringUtils.isNotEmpty(unit))  {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), Roman.normalizeRomanNumbers(unit));	
			}
		}
		
		if (legalDescription.contains("PHASE") || legalDescription.contains("PH ")) {
			phase = legalDescription;
			phase = phase.replaceFirst("(?is)\\bPH(?:ASE)?\\b\\s*([A-Z\\d-]+).*", "$1");
			legalDescription = legalDescription.replaceFirst("(?is)\\bPH(?:ASE)?\\b\\s*([A-Z\\d-]+)", "");
			
			if(StringUtils.isNotEmpty(phase))  {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), Roman.normalizeRomanNumbers(phase));	
			}
		}
		
		if (legalDescription.contains("BLK") || legalDescription.contains("BLOCK")) {
			blk = legalDescription;
			if (blk.matches("(?is)\\s*BL?O?C?K\\s*([A-Z]?\\s*-?\\s*\\d+(?:[A-Z])?).*")) {
				blk = blk.replaceFirst("(?is)\\s*BL?O?C?K\\s*([A-Z]?\\s*-?\\s*\\d+(?:[A-Z])?).*", "$1");
				legalDescription = legalDescription.replaceFirst("(?is)\\s*BL?O?C?K\\s*([A-Z]?\\s*-?\\s*\\d+(?:[A-Z])?)", "");
			}
			
			if(StringUtils.isNotEmpty(blk))  {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), Roman.normalizeRomanNumbers(blk));
			}
		}
		
		subdiv = legalDescription;
		subdiv = subdiv.replaceAll("(?is)(?:\\d+\\s*X\\s*\\d+\\s*)?(?:\\d+)?(?:\\.\\d+\\s*AC(?:RES?)?)?", "");
		if (subdiv.contains("<br")) {
			subdiv = subdiv.replaceAll("<\\s*br\\s*/\\s*>", "<br>");
			subdiv = subdiv.replaceFirst("(?is).*\\b(ROAD|RD|AVENUE|AVE|STREET|ST|LANE|LN)\\b\\s*<br>", "");
			subdiv = subdiv.replaceAll("<br>", "");
		}
		if (subdiv.matches("(?is)\\s*(?:\\d+\\s*)?[A-Z\\s-\\.',]+(?:STREET|ST|AVENUE|AVE|AV|DRIVE|DR|HGWY|HGW|RD|ROAD|CIRC|CIRCLE|LN|LANE|PIKE)\\s*")) {
			subdiv = subdiv.replaceFirst("(?is)\\s*(?:\\d+)?[A-Z\\s-\\.',]+(?:STREET|ST|AVENUE|AVE|AV|DRIVE|DR|HGWY|HGW|RD|ROAD|CIRC|CIRCLE|LN|LANE|PIKE)\\s*", "");
		}
//		StandardAddress shortAddress = new StandardAddress(subdiv);
//		String adrSuffix = shortAddress.getAddressElement(StandardAddress.STREET_SUFFIX);
//		if (StringUtils.isNotEmpty(adrSuffix)) {
//			String regExp = "(?is)(?:\\d+)?\\s*.*" + adrSuffix + "\\s*";
//			if (subdiv.matches(regExp)) {
//				subdiv = subdiv.replaceFirst(regExp, "");
//			}
//		}
		if(StringUtils.isNotEmpty(subdiv))  {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), Roman.normalizeRomanNumbers(subdiv));
		}
		
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		if(hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX)); 			
			moduleList.add(module);
		}
		
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d , true);
		
		if(hasStreet() && hasStreetNo()) {	
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX)); 
			module.addFilter(addressFilter);
			moduleList.add(module);		
		}
		
		if(hasStreet() && hasStreetNo()) {	
			//e.g: 18-H CEDAR;  18-A MAIN;  4-E MAIN; 1 W ELM; 	241 S MAIN	
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX)); 
			String strNo = getSearchAttribute(SearchAttributes.P_STREETNO);
			strNo = strNo.replaceAll("(?is)(\\d+)[\\s*-?][A-Z]", "$1");
			module.clearSaKey(0);
			module.forceValue(0, strNo);
			module.addFilter(addressFilter);
			moduleList.add(module);		
		}
		
		serverInfo.setModulesForAutoSearch(moduleList);
	}

	
	protected void logSearchBy(TSServerInfoModule module, Map<String, String> params) {

		if (module.isVisible() || "GB_MANAGER_OBJECT".equals(module.getSaObjKey())) {// B 4511

			// get parameters formatted properly
			Map<String, String> moduleParams = params;
			if (moduleParams == null) {
				moduleParams = module.getParamsForLog();
			}
			Search search = getSearch();
			// determine whether it's an automatic search
			boolean automatic = (search.getSearchType() != Search.PARENT_SITE_SEARCH) || (GPMaster.getThread(searchId) != null);
			boolean imageSearch = module.getLabel().equalsIgnoreCase("image search") || module.getModuleIdx() == TSServerInfo.IMG_MODULE_IDX;

			// create the message
			StringBuilder sb = new StringBuilder();
			SearchLogFactory sharedInstance = SearchLogFactory.getSharedInstance();
			SearchLogPage searchLogPage = sharedInstance.getSearchLogPage(searchId);
			sb.append("</div>");

			Object additional = GetAttribute("additional");
			if (Boolean.TRUE != additional) {
				searchLogPage.addHR();
				sb.append("<hr/>");
			}
			int fromRemoveForDB = sb.length();

			// searchLogPage.
			sb.append("<span class='serverName'>");
			String serverName = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getName();
			sb.append(serverName);
			sb.append("</span> ");

			sb.append(automatic ? "automatic" : "manual");
			Object info = module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION);
			if (StringUtils.isNotEmpty(module.getLabel())) {

				if (info != null) {
					sb.append(" - " + info + "<br>");
				}
				sb.append(" <span class='searchName'>");
				sb.append(module.getLabel());
			} else {
				sb.append(" <span class='searchName'>");
				if (info != null) {
					sb.append(" - " + info + "<br>");
				}
			}
			sb.append("</span> by ");

			boolean firstTime = true;
			for (Entry<String, String> entry : moduleParams.entrySet()) {
				String value = entry.getValue();
				value = value.replaceAll("(, )+$", "");
				
				if (!firstTime && !"-".equals(entry.getKey())) {
					sb.append(", ");
				} else {
					firstTime = false;
				}
				if (!"-".equals(entry.getKey())) 
					sb.append(entry.getKey().replaceAll("&lt;br&gt;", "") + " = <b>" + value + "</b>");
			}
			//because ALL does not have a value.
			if (!moduleParams.containsKey("File Type")){
				sb.append(", ").append("File Type = <b>ALL</b>");
			}
			
			int toRemoveForDB = sb.length();
			// log time when manual is starting
			if (!automatic || imageSearch) {
				sb.append(" ");
				sb.append(SearchLogger.getTimeStamp(searchId));
			}
			sb.append(":<br/>");

			// log the message
			SearchLogger.info(sb.toString(), searchId);
			ModuleShortDescription moduleShortDescription = new ModuleShortDescription();
			moduleShortDescription.setDescription(sb.substring(fromRemoveForDB, toRemoveForDB));
			moduleShortDescription.setSearchModuleId(module.getModuleIdx());
			search.setAdditionalInfo(TSServerInfoConstants.TS_SERVER_INFO_MODULE_DESCRIPTION, moduleShortDescription);
			String user = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser().getAttribute(1).toString();
			SearchLogger.info(ro.cst.tsearch.utils.StringUtils.createCollapsibleHeader(), searchId);
			searchLogPage.addModuleSearchParameters(serverName, additional, info, moduleParams, module.getLabel(), automatic, imageSearch, user);
		}
	}
	
}
