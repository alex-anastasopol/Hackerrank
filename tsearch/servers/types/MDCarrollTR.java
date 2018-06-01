package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

public class MDCarrollTR extends TSServer {

	private static final long serialVersionUID = 1L;

	
	public MDCarrollTR(long searchId) {
		super(searchId);
	}
	
	
	public MDCarrollTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

//	
//	protected void loadDataHash(HashMap<String, String> data, String year) {
//		if(data != null) {
//			data.put("type","CNTYTAX");
//		}
//	}
//	
	
	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		
		TableColumn[] cols = row.getColumns();
		if(cols.length == 4) {	
			String billNo = cols[0].toPlainTextString().trim();
			String billDate = cols[1].toPlainTextString().trim();
			
			if (StringUtils.isNotEmpty(billNo)) {
				if (billNo.contains("RE")) {
					resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "Real Estate");
				} else {
					resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "Other type");
				}
			}
			if (StringUtils.isNotEmpty(billDate)) {
				billDate = billDate.replaceFirst("\\d{2}\\s*/\\s*\\d{2}\\s*/\\s*(\\d{4})","$1");
				resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), billDate);
			}
		}
		
		return resultMap;
	}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		if (table.contains("We could not locate any parcels that match")) {
			return intermediaryResponse;
			
		} else
		try {
			HtmlParser3 htmlParser = new HtmlParser3(table);	
			NodeList nodeList = htmlParser.getNodeList()
					.extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "text"), true);
						
			if(nodeList.size() == 0) {
				return intermediaryResponse;
			}
			
			TableTag tableTag = (TableTag) nodeList.elementAt(0).getChildren()
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.elementAt(0);
			
			TableTag tmpTable = (TableTag) tableTag.getChildren()
					.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
			
			TableRow[] rows  = tmpTable.getRows();
			
			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				if(row.getColumnCount() == 4) {
					ParsedResponse currentResponse = new ParsedResponse();
					LinkTag linkTag = ((LinkTag)row.getColumns()[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0));
					
					Matcher m = Pattern.compile("RE\\d+").matcher(linkTag.getChildrenHTML().trim());
					
					if (m.find()) {
						String link = CreatePartialLink(TSConnectionURL.idGET) + "/ccg/collect/" +
								linkTag.extractLink().trim().replaceAll("\\s", "%20");
							
						linkTag.setLink(link);
						currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
						
					} else {
						TableColumn col = row.getColumns()[0];
						col.getChildren().add(linkTag.childAt(0));
						col.removeChild(0);
					}
					
					
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(row.toHtml());
					
					
					ResultMap map = parseIntermediaryRow(row, searchId); 
					Bridge bridge = new Bridge(currentResponse, map, searchId);
					
					DocumentI document = (TaxDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);					
				}
			}
			

			response.getParsedResponse().setHeader("<table border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + "<tr><th>Bill ID No.</th> <th>Bill Date</th> <th>Type</th> <th>Status</th> </tr>");
			response.getParsedResponse().setFooter("</table>");
		
			outputTable.append(table);
						
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}
	
	

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		// 	no result returned - error message on official site
		if (rsResponse.contains("An unexpected error has occurred:")) {
			Response.getParsedResponse().setError("Official site not functional");
    		return;
    	} else if (rsResponse.indexOf("No Bill(s) found for Account No") > -1) {
			Response.getParsedResponse().setError(NO_DATA_FOUND);
			return;
		} 
		
		switch (viParseID) {			
			case ID_SEARCH_BY_TAX_BIL_NO:	
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
				String details = "";
				
				details = getDetails(rsResponse, accountId);
				String filename = accountId + ".html";
				
				if (viParseID != ID_SAVE_TO_TSD) {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
	
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","CNTYTAX");
					data.put("year", getTaxYear("personalInfo", new HtmlParser3(details).getNodeList()));
					
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
				ParseResponse(sAction, Response, ID_DETAILS);
				break;
				
			default:
				break;
		}
	}	

	
	protected String getDetails(String rsResponse, StringBuilder accountId) {
		try {
			/* If from memory - use it as is */
			if(!rsResponse.contains("<html")){
				if (rsResponse.contains("id=\"accountInfo\"")) { 
					rsResponse = rsResponse.replaceAll("(?is)(\\\"width\\s*(?:=|:))\\s*(?:30|70)\\s*(%\\\")","$1 50$2");
					rsResponse = rsResponse.replaceAll("(?is)(width\\s*=\\s*\\\")\\s*100\\s*(%\\s*\\\")","$1 500px\\\"");
					
					Matcher m = Pattern.compile("(?is).*Account\\s*No.[^\\d]+(\\d{2}\\s*)-\\s*\\d{1}\\s*-\\s*(\\d{6}).*").matcher(rsResponse);
					
					if (m.find()) {
						accountId.append(m.group(1) + m.group(2));
					}
				
					return rsResponse;
				}
			
			} else {
			
				try {
					StringBuilder details = new StringBuilder();
					HtmlParser3 htmlParser = new HtmlParser3(rsResponse);
					NodeList nodeList = htmlParser.getNodeList()
							.extractAllNodesThatMatch(new TagNameFilter("div"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("class", "text"), true);				
						
					if(nodeList.size() == 0) {
						return rsResponse;
					
					} else {						
						NodeList detailsList = nodeList.elementAt(0).getChildren()
								.extractAllNodesThatMatch(new TagNameFilter("table"), true); 
						String urlForAOinfo = "";
						
						if (detailsList.size() > 3) {
								
							details.append("<table border = \"1\">");					
							details.append("<tr align=\"center\"><td></br><b> Collections/Taxes - Bill Inquiry ");
							
							//Task 8690	
							urlForAOinfo = detailsList.extractAllNodesThatMatch(new TagNameFilter("a"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("class", "link")).elementAt(0).toHtml().trim();
							urlForAOinfo = urlForAOinfo.replaceFirst("(?is)<a class\\s*=\\s*\\\"link\\\"\\s*href\\s*=\\\"([^\\\"]+)\\\".*", "$1");
							
							TableTag tmpTable = (TableTag) detailsList.elementAt(1);
							tmpTable.setAttribute("id", "\"accountInfo\"");
								
							TableRow[] rows = tmpTable.getRows();
								
							if (rows.length > 1) {
								TableColumn col = rows[0].getColumns()[0];
								col.removeChild(1);
									
								details.append(" </b></td></tr>");
								
								col = rows[1].getColumns()[1];
								if (col.getChildCount() > 4) {
									String apn = col.getChild(4).toHtml().trim(); 
									//11-1-011233 on TR <=> 11-011233 on NB
									apn = apn.replaceFirst("(?is)(\\d{2})\\s*-\\s*\\d{1}\\s*-\\s*(\\d{6})", "$1$2");
									accountId.append(apn);
								} 
								
								details.append("<tr><td>");
								details.append(tmpTable.toHtml());
								details.append("</br>");
								details.append("</td></tr>");
							}
								
							tmpTable = (TableTag) detailsList.elementAt(2);
							tmpTable.setAttribute("id", "\"personalInfo\"");
							//Task 8690
							String personalInfo = tmpTable.toHtml();
							personalInfo = personalInfo.replaceFirst("(?is)<tr>\\s*<td[^>]+>\\s*(?:<br\\s*/\\s*>\\s*)?<b>Property Description[^>]+>\\s*</td>\\s*" +
									"<td>\\s*(?:[\\w\\d-\\.\\s/]+<br\\s*/>)+(?:[\\w\\d-\\.\\s/]+)?\\s*</td>\\s*</tr>", "");
							
							details.append("<tr><td>");
							details.append(personalInfo);
							details.append("</br>");
							details.append("</td></tr>");
								
							tmpTable = (TableTag) detailsList.elementAt(3);
							tmpTable.setAttribute("id", "\"taxInfo\"");
							String taxInfo = tmpTable.toHtml();
							taxInfo = taxInfo.replaceAll("(?is)(?:<p[^>]+>)?\\s*<a class=\\\"link\\\"[^/]+/a>\\s*(?:</p>)?", "");
							taxInfo = taxInfo.replaceAll("(?is)(\\\"width\\s*(?:=|:))\\s*(?:30|70)\\s*(%\\\")","$1 50$2");
							taxInfo = taxInfo.replaceAll("(?is)(width\\s*=\\s*\\\")\\s*100\\s*(%\\s*\\\")","$1 500px\\\"");
							details.append("<tr><td>");
							details.append(taxInfo);
							details.append("</br>");
							details.append("</td></tr>");
						}						
						//Task 8690
						if (StringUtils.isNotEmpty(urlForAOinfo)) {
							String rspFromAO = getLinkContents(urlForAOinfo);
							rspFromAO = rspFromAO.replaceAll("(?is)style=\"BORDER-BOTTOM:2px solid;width:100%;\"", "style=\"width:100%;BORDER-BOTTOM:2px solid;\"");	
							htmlParser = new HtmlParser3(rspFromAO);
							nodeList = htmlParser.getNodeList()
								.extractAllNodesThatMatch(new TagNameFilter("form"), true);
								
							if (nodeList != null) {
								TableTag tmpTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "yellowbox"), true)
										.elementAt(0);	
									String infoFromAO = tmpTable.toHtml().trim();
									infoFromAO = infoFromAO.replaceFirst("(?is)<td valign=\"Middle\" width=\"20%\">\\s*" +
											"(?:<a[^>]+>\\s*[\\w\\s]+\\s*</a>\\s*(?:<br>)?)+\\s*</td>", "");
									infoFromAO = infoFromAO.replaceFirst("(?is)<small>[^>]+>","");
									infoFromAO = infoFromAO.replaceFirst("(?is)(<td[^>]+)","$1 colspan=\\\"2\\\"");
									//infoFromAO = infoFromAO.replaceFirst("(?is)(<table[^>]+)","$1 style=\"background-color:aliceblue;\"");		
									details.append("<tr style=\"background-color:aliceblue;\"><td><br>");
									details.append(infoFromAO);
									details.append("</td></tr>");
							}
							
							
							TableTag tmpTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "Results"), true)
									.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
							
							if (tmpTable != null) {
								TableRow[] rows = tmpTable.getRows();
								if (rows.length > 12) {
									String infoFromAO = rows[3].toHtml();
									infoFromAO = infoFromAO.replaceFirst("(?is)(<td[^>]+>)([^<]+)(</td>)", "$1 <b> $2 </b> $3");
									details.append("<tr><td><br><table id=\"addressLegalAndReferences\">");
									details.append(infoFromAO);
									infoFromAO = rows[4].toHtml();
									infoFromAO = infoFromAO.replaceAll("(?is)<a[^>]+>([^<]+)</a>", "$1");
									infoFromAO = infoFromAO.replaceFirst("(?is)(Premises Address)", "<b> $1 </b>");
									infoFromAO = infoFromAO.replaceFirst("(?is)(Legal Description)", "<b> $1 </b>");
									details.append(infoFromAO);
									infoFromAO = rows[11].toHtml();
									infoFromAO = infoFromAO.replaceFirst("(?is)(<td[^>]+>)([^<]+)(</td>)", "$1 <b> $2 </b> $3");
									details.append(infoFromAO);
									
									nodeList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
											.extractAllNodesThatMatch(new HasAttributeFilter("style", "width:100%;BORDER-BOTTOM:2px solid;"));
									if (nodeList.size() > 0) {
										int size = nodeList.size();
										
										for (int i=0; i< size; i++) {
											tmpTable = (TableTag) nodeList.elementAt(i);
											if (tmpTable.toHtml().contains("Seller:")) {
												tmpTable.setAttribute("class","\"referencesRO\"");
												details.append("<tr><td>");	
												infoFromAO = tmpTable.toHtml();
												infoFromAO = infoFromAO.replaceAll("(?is)<a[^>]+>([^<]+)</a>", "$1");
												details.append(infoFromAO);
												details.append("</td></tr>");
											}
										}
									}
									
									details.append("</table></td></tr>");
								}
							}
						}
							
						details.append("</table> </br>");
						
						return details.toString();
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
			
			map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"TR");
			
			HtmlParser3 htmlParser = new HtmlParser3(detailsHtml);
			NodeList list = htmlParser.getNodeList();		
			
			TableTag tmpTable = (TableTag) list.extractAllNodesThatMatch(new HasAttributeFilter("id", "accountInfo"), true).elementAt(0);
			
			if (tmpTable != null) {
				if (tmpTable.getRowCount() == 2) {
					accountID = tmpTable.getRows()[1].getColumns()[1].getChildren().toHtml().trim();
					accountID = accountID.replaceFirst("(?is).*Account\\s*No.[^\\d]+(\\d{2}\\s*)-\\s*\\d{1}\\s*-\\s*(\\d{6}).*", "$1$2");
					
					if (StringUtils.isNotEmpty(accountID)) {
						map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountID);
					}
				}
			}
			
			tmpTable = (TableTag) list.extractAllNodesThatMatch(new HasAttributeFilter("id", "personalInfo"), true).elementAt(0);
			
			if (tmpTable != null) {
				if (tmpTable.getRowCount() > 3) {
					names = tmpTable.getRows()[0].getColumns()[1].getChildrenHTML().trim();
					names = names.replaceFirst("<br\\s*/>\\s*", "");
					names = names.replaceFirst("(?is)\\s*<br\\s*/>\\s+", ";").trim();
					
					map.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(),names.replaceAll("\\s*<br\\s*/>\\s*", "; ").trim());	
					
				}
				
				parseTaxes(list, map, searchId);
				parseNames(map, searchId);
				
				tmpTable = (TableTag) list.extractAllNodesThatMatch(new HasAttributeFilter("id", "addressLegalAndReferences"), true)
						.elementAt(0);
				if (tmpTable != null) {
					getDetailsFromAOsite(map, tmpTable, searchId);
				}
				
			} else {
				DataSite site = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), getServerID());
				int taxYear = Integer.parseInt(site.getPayDate().toString().replaceAll("(?is)(?:.*)(?:[A-Z]+T|WIB)\\s*(\\d{4})", "$1"));
				map.put(TaxHistorySetKey.YEAR.getKeyName(), Integer.toString(taxYear));
			}
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		
		return null;	
	}
	
	
	private void getDetailsFromAOsite(ResultMap map, TableTag tmpTable, long searchId) {
		//tmpTable = (TableTag) list.extractAllNodesThatMatch(new HasAttributeFilter("id", "addressLegalAndReferences"), true).elementAt(0);
//		System.out.println(tmpTable.toHtml());
		String address = "";
		String legalDesc = "";
		int rowsCount = 0;
		
		if (tmpTable.getRowCount() > 3) {
			TableRow[] rows = tmpTable.getRows();
			int size = rows.length;
					
			boolean lookForAddress = true;
			TableTag table = (TableTag)rows[1].getColumns()[0].getFirstChild();
			
			if (table != null) {
				rowsCount = table.getRowCount();
				TableColumn col = table.getRow(0).getColumns()[0];
				//row1=header, next rows: col1=address info, col2=blank, col3=legal info
				if (rowsCount > 1) {
					for (int i=1; i < rowsCount; i++) {
						TableRow row = table.getRow(i);
						if (lookForAddress) {
							col = row.getColumns()[0]; //address
							address += col.getFirstChild().toHtml().trim();
							
							if (StringUtils.isNotEmpty(address)) {
								String regExp = "(?is)((?:[\\d-\\s]+)?\\s*[\\w\\s'-]+\\s*\\b(AVENUE|AVE|STREET|ST|ROAD|RD|CIRCLE|CIR|HGGHWAY|DRIVE|DRV|DR|LN|LANE)\\b)\\s*";
								Pattern p = Pattern.compile(regExp);
								Matcher m = p.matcher(address);
								if (m.find()) {
									lookForAddress = false;
									address = m.group(1);
								} else {
									regExp = "(?is)((?:[\\d-\\s]+)?\\s*[\\w\\s'-]+\\s*\\b(AVENUE|AVE|STREET|ST|ROAD|RD|CIRCLE|CIR|HGGHWAY|DRIVE|DRV|DR|LN|LANE)\\b)\\s*\\d+\\s*-\\s*\\d+";
									m = Pattern.compile(regExp).matcher(address);
									if (m.find()) {
										lookForAddress = false;
										address = m.group(1);
		
									} else {
										regExp = "(?is)((?:[\\d-\\s]+)?\\s*[\\w\\s'-]+\\s*\\b(AVENUE|AVE|STREET|ST|ROAD|RD|CIRCLE|CIR|HGGHWAY|DRIVE|DRV|DR|LN|LANE)\\b)\\s*(?:\\d+\\s*-\\s*\\d+)?\\s*(\\bSEC\\b\\s*[A-Z\\d])\\s*";
										m = Pattern.compile(regExp).matcher(address);
										
										if (m.find()) {
											address = m.group(1).trim();
											lookForAddress = false;
										
										} else {
											regExp = "(?is)((?:[\\d-\\s]+)?\\s*[\\w\\s'-]+\\s*\\b(AVENUE|AVE|STREET|ST|ROAD|RD|CIRCLE|CIR|HGGHWAY|DRIVE|DRV|DR|LN|LANE)\\b)\\s*([A-Z\\s-']+)\\s*";
											m = Pattern.compile(regExp).matcher(address);
											if (m.find()) {
												address = m.group(1).trim();
												lookForAddress = false;
											}
										}
									}
								}
							}
						} else {//stop adding info from address columns
							if (map.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName()) == null) {
								map.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
								map.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
								map.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
							}
						}
							
						col = row.getColumns()[2]; //legal
						if (StringUtils.isNotEmpty(col.getFirstChild().toHtml().trim())) {
							String tmpInfo = col.getFirstChild().toHtml().trim();
							if (!tmpInfo.contains(" AVENUE") && !tmpInfo.contains(" AVE") && !tmpInfo.contains(" STREET") && !tmpInfo.contains(" ST") && !tmpInfo.contains(" ROAD")
									&& !tmpInfo.contains(" RD") && !tmpInfo.contains(" CIRCLE") && !tmpInfo.contains(" CIR") && !tmpInfo.contains(" HIGHWAY") && !tmpInfo.contains(" DRIVE")
									&& !tmpInfo.contains(" DRV") && !tmpInfo.contains(" DR") && !tmpInfo.contains(" LN") && !tmpInfo.contains(" LANE")) {
								legalDesc += " " + tmpInfo;
							}
							
						}
					}
				}
			}
			
			//parsing references
			List<List> body = new ArrayList<List>();
			
			for (int i=3; i < size; i++) {
				table = (TableTag) rows[i].getColumns()[0].getFirstChild(); 
				String transfDate = "";
				String instNo = "";
				String book = "";
				String page = "";
				List<String> line = null;
				
				TableRow row = table.getRow(0);
				if (row.getColumnCount() == 6) {
					TableColumn col = row.getColumns()[3];
					if (StringUtils.isNotEmpty(col.toHtml())) {
						if (col.getChildren() != null)
							transfDate = col.getFirstChild().toHtml().trim();	
					} else 
						break;
					
				}
				
				row = table.getRow(1);
				if (row.getColumnCount() == 6) {
					TableColumn col = row.getColumns()[3];
					String info = "";
					Matcher m = null;
					if (col.getChildren() != null) {
						info = col.getFirstChild().toHtml().trim();
						m = Pattern.compile("(?is)(\\d+\\s*|\\s*)/\\s*(\\d{5})\\s*/\\s*(\\d{5})\\s*").matcher(info);
						
						if (m.find()) {
							instNo = m.group(1).trim().replaceAll("\\b0*", "");
							book = m.group(2).replaceAll("\\b0*", "");
							page = m.group(3).replaceAll("\\b0*", "");
						}
					}
					
					if (StringUtils.isNotEmpty(transfDate)) {
						line = new ArrayList<String>();
						if (StringUtils.isNotEmpty(instNo) || StringUtils.isNotEmpty(book) || StringUtils.isNotEmpty(page)) {
							line.add(transfDate);
							line.add(instNo);
							line.add(book);
							line.add(page);
							body.add(line);
						}
						
						instNo = "";
						book = "";
						page = "";
						col = row.getColumns()[5];
						if (col.getChildren() != null) {
							info = col.getFirstChild().toHtml().trim();
							m = Pattern.compile("(?is)(\\d+\\s*|\\s*)/\\s*(\\d{5})\\s*/\\s*(\\d{5})\\s*").matcher(info);
							
							if (m.find()) {
								instNo = m.group(1).trim().replaceAll("\\b0*", "");
								book = m.group(2).replaceAll("\\b0*", "");
								page = m.group(3).replaceAll("\\b0*", "");
							}
						}
						
						if (StringUtils.isNotEmpty(instNo) || StringUtils.isNotEmpty(book) || StringUtils.isNotEmpty(page)) {
							line = new ArrayList<String>();
							line.add(transfDate);
							line.add(instNo);
							line.add(book);
							line.add(page);
							
							body.add(line);
						}
					}
				}
			}
			
			//adding all cross references - should contain transfer table and info parsed from legal description
			if(body != null && body.size() > 0) {
				ResultTable rt = new ResultTable();
				String[] header = {"InstrumentDate", "InstrumentNumber", "Book", "Page"};
				rt = GenericFunctions2.createResultTable(body, header);
				map.put("SaleDataSet", rt);
			}
		}
		
		if (StringUtils.isNotEmpty(legalDesc)){
			map.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legalDesc);
			parseLegalSummary(map, searchId);
		}
		
		
	}


	public static void parseNames(ResultMap resultMap, long searchId) {
		String unparsedName = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		List<List> body = new ArrayList<List>();
		
		if(StringUtils.isNotEmpty(unparsedName)) {
			unparsedName = unparsedName.replaceAll("\\s*-\\s*(ETAL),?\\s*", " $1 - ");
			unparsedName = unparsedName.replaceAll("(?is)\\(\\b\\s*DEC\\s*\\b\\)*", "");
			
			unparsedName = unparsedName.replaceAll("(?is)\\b(A|F)KA\\b", " - @@@FML@@@");
			unparsedName = unparsedName.replaceAll("(?is)\\bInCareOfName\\b", " - @@@FML@@@");
			unparsedName = unparsedName.replaceAll("(?is)\\s+-\\s+ETAL", " ETAL - ");
			
			String[] mainTokens = unparsedName.split(";");
											
			for (int i = 0; i < mainTokens.length; i++) {
				String currentToken = mainTokens[i];
				String[] names = StringFormats.parseNameNashville(currentToken, true);	
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
	
	public static void parseLegalSummary(ResultMap resultMap,long searchId) {
		String legalDescription = (String) resultMap.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		
		if(StringUtils.isEmpty(legalDescription)) {
			return;
		}
		
		if(StringUtils.isNotEmpty(legalDescription))  {
			legalDescription = legalDescription.replaceAll("(?is)-?\\s*[\\d\\.]+\\s*(?:ACS?|SQF|SQ FT|SQ|SF)", "");
			
			if (legalDescription.contains(" LT ") || legalDescription.contains(" LT.") || legalDescription.contains(" LOT ")) {
				String lot = legalDescription.trim();
				lot = lot.replaceAll("(?is).*\\bLO?T\\.?\\s*(\\d+).*", "$1 ");
				legalDescription = legalDescription.trim().replaceAll("(?is)\\bLO?T\\b\\s*\\.?\\d+(.*)", "$1").trim();
				if (StringUtils.isNotEmpty(lot)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), Roman.normalizeRomanNumbers(lot));	
				}
			}
			if (legalDescription.contains(" TRACT")) {
				String tract = legalDescription;
				tract = tract.replaceAll("(?is).*TRACT\\s*(\\d+).*", "$1 ");
				legalDescription = legalDescription.replaceAll("(?is)\\bTRACT\\b\\s*\\d+(.*)","$1");
				if (StringUtils.isNotEmpty(tract)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), Roman.normalizeRomanNumbers(tract));	
				}
			}
			if (legalDescription.matches(".*[A-Z\\s]+\\s*(?:\\bSC\\b\\s*\\d+)?")) {
				String section = legalDescription;
				String subdiv = legalDescription;
				if (legalDescription.contains(" SC ")) {
					section = section.replaceFirst("(?is).*\\bSC\\b\\s*(\\d+)", "$1");
				}
				subdiv = subdiv.replaceFirst("(?is)(.*)(?:\\bSC\\b\\s*\\d+)?", "$1");
				if (StringUtils.isNotEmpty(subdiv)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);	
				}
				if (StringUtils.isNotEmpty(section)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), section);	
				}
			}
			
		}
		
		return;
	}
	
	
	String getTaxYear (String tableId, NodeList list) {
		String taxYear = "";
		TableTag tmpTable = (TableTag) list.extractAllNodesThatMatch(new HasAttributeFilter("id", "personalInfo"), true).elementAt(0);
		
		if (tmpTable != null) {
			TableRow[] rows = tmpTable.getRows();
				
			if (rows.length >= 4) {
				String tmp = rows[2].getColumns()[1].getChildrenHTML();
				tmp = tmp.replaceFirst("(?is)\\s*<br\\s*/>\\s*(?:\\d{2}/\\d{2}/(\\d{4})\\s*-\\s*[\\d/]+).*", "$1");
					
				if (StringUtils.isNotEmpty(tmp)) {
					taxYear = tmp.trim();
				}	
			}
		}
		
		return taxYear;
	}
	
	public void parseTaxes(NodeList nodeList, ResultMap map, long searchId) {
		String baseAmount = "";
		String amountDue = "";
		String amountPaid = "", amountPaid1 = "";
		String payDate = "", payDate1 = "";
		String taxYear = "";
		String paymentStatus = "";
		int paymentType = 0; 
		
		TableTag tmpTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "taxInfo"), true).elementAt(0);
		
		if (tmpTable != null && tmpTable.getRowCount() == 1) {
			paymentStatus = tmpTable.getRows()[0].getColumns()[1].getChildrenHTML().trim();
			paymentStatus = paymentStatus.replaceFirst("<br\\s*/>\\s*", "");
			
			if (paymentStatus.contains("Paid in Full")) {
				paymentType = 1; //full paid
			} else if (paymentStatus.contains("No Payment Activity")) {
				paymentType = 0;  //unpaid
			}
		}
		
		tmpTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "personalInfo"), true).elementAt(0);
		
		if (tmpTable != null) {
			TableRow[] rows = tmpTable.getRows();
			
			if (rows.length >= 4) {
				String tmp = rows[3].getColumns()[0].getChildrenHTML();
				
				if (tmp.contains("2nd Payment")) {
					paymentType= 2; //paid in two installments
				}
				
//				tmp = rows[2].getColumns()[1].getChildrenHTML();
//				taxYear = tmp;
//				taxYear = taxYear.replaceFirst("(?is)\\s*<br\\s*/>\\s*(?:\\d{2}/\\d{2}/(\\d{4})\\s*-\\s*[\\d/]+).*", "$1");
				taxYear = getTaxYear("personalInfo", nodeList);
				
				if (StringUtils.isNotEmpty(taxYear)) {
					map.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear.trim());
				}	
				
				tmp = rows[3].getColumns()[1].getChildrenHTML().trim();
				tmp = tmp.replaceFirst("<br\\s*/>\\s*", "");
				Matcher m = null;
				
				if (paymentType == 1) {
					m = Pattern.compile("(?is)([\\$\\d,\\.]+)\\s*<br\\s*/>([^<]+)<br\\s*/>([^<]+)<br\\s*/>\\s*").matcher(tmp);
					if (m.find()) {
						//BA <br/> ReceiptDate <br/> AP <br/>
						baseAmount = m.group(1).trim().replaceAll("[\\$,]+", "");
						payDate = m.group(2).trim();
						amountPaid = m.group(3).trim().replaceAll("[\\$,]+", "");
					}
				} else if (paymentType == 2) {
						m = Pattern.compile("(?is)\\$([\\d,\\.]+)\\s*<br\\s*/>([^<]+)<br\\s*/>([^<]+)<br\\s*/>\\s*<br\\s*/>\\s*(\\d{2}/\\d{2}/\\d{4})\\s*<br\\s*/>\\s*([\\d\\$\\.,]+)").matcher(tmp);
						if (m.find()) {
							//BA <br/> ReceiptDate <br/> AP <br/>
							baseAmount = m.group(1).trim().replaceAll("[\\$,]+", "");
							payDate = m.group(2).trim();
							amountPaid = m.group(3).trim().replaceAll("[\\$,]+", "");
							payDate1 = m.group(4).trim();
							amountPaid1 = m.group(5).trim().replaceAll("[\\$,]+", "");
						}
					
					} else if (paymentType == 0)  {
						m = Pattern.compile("(?is)(\\$[\\d,\\.]+)\\s*<br\\s*/>([^<]+)<br\\s*/>\\s*(\\$[\\d\\.,]+)\\s*<br\\s*/>").matcher(tmp);
						if (m.find()) {
							amountPaid = m.group(3).trim().replaceAll("[\\$,]+", "");
							if (".00".equals(amountPaid)) 
								amountPaid = "0.00";
							payDate = m.group(2).trim();
							baseAmount = m.group(1).trim().replaceAll("[\\$,]+", "");
							
							DataSite site = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), getServerID());
							String valueOfDD = site.getPayDate().toString().replaceAll("(?is)(?:.*)(?:[A-Z]+T|WIB)\\s*(\\d{4})", "$1");
							
							tmpTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "taxInfo"), true).elementAt(0);
							rows = tmpTable.getRows();
							tmp = rows[0].getColumns()[1].getChildrenHTML().trim();
							
							String regExp = "(?is)<br\\s*/>([^<]+)<br\\s*/>\\s*((\\$[\\d\\.,]+)\\s*\\(thru\\s*(\\d{2}/\\d{2}/\\d{4})\\s*\\))[^<]+<!--[^\\$]+((\\$[\\d\\.,]+)\\s*\\(thru\\s*(\\d{2}/\\d{2}/\\d{4})\\s*\\))<br\\s*/>\\s*<br\\s*/>";
							m = Pattern.compile(regExp).matcher(tmp);
							
							if (m.find()) {
								if ("No Payment Activity".equals(m.group(1).trim())) {
									String ad1 = m.group(3).trim().replaceAll("[\\$,]+", "");
									String dd1 = m.group(4).trim();
									String ad2 = m.group(6).trim().replaceAll("[\\$,]+", "");
									String dd2 = m.group(7).trim();
									
									amountDue = ad2;
									if (StringUtils.isNotEmpty(amountDue)) {
										map.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(),amountDue);
									}
									
								}
							}
						}
					}
				}
			
				if (StringUtils.isNotEmpty(baseAmount)) {
					map.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
				}
				
				if (StringUtils.isNotEmpty(amountPaid)) {
					if (paymentType == 1) {
						map.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(),amountPaid);
						if (StringUtils.isNotEmpty(payDate)) {
							map.put(TaxHistorySetKey.DATE_PAID.getKeyName(), payDate);
						}
					} else if (paymentType == 2) {
						double paidValue = Double.parseDouble(amountPaid);
						NumberFormat formatter = new DecimalFormat("#.##");	
						
						if (StringUtils.isNotEmpty(amountPaid1)) {
							paidValue += Double.parseDouble(amountPaid1);
							amountPaid = formatter.format(paidValue);
							map.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(),amountPaid);
						}
					}  else if (paymentType == 0) {
						 if (!"0.00".equals(amountPaid)) {
							 map.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(),amountPaid);
							 if (StringUtils.isNotEmpty(payDate)) {
									map.put(TaxHistorySetKey.DATE_PAID.getKeyName(), payDate);
								}
						 }
					 }
					
//					if (paymentType ==1 || paymentType == 2) {
						try {
							ResultTable receipts = new ResultTable();
							Map<String, String[]> tmpMap = new HashMap<String, String[]>();
							String[] header = { "ReceiptAmount", "ReceiptDate" };
							List<List<String>> bodyRT = new ArrayList<List<String>>();
							
							List<String> paymentRow = new ArrayList<String>();
							paymentRow.add(amountPaid);
							paymentRow.add(payDate);
							bodyRT.add(paymentRow);
							
							if (StringUtils.isNotEmpty(amountPaid))
								if (StringUtils.isNotEmpty(payDate1)) {
									paymentRow = new ArrayList<String>();
									paymentRow.add(amountPaid1);
									paymentRow.add(payDate1);
									bodyRT.add(paymentRow);
									
									map.put(TaxHistorySetKey.DATE_PAID.getKeyName(), payDate1);
								}
							
							if (StringUtils.isNotEmpty(amountPaid1))
								if (StringUtils.isNotEmpty(payDate1)) {
									paymentRow = new ArrayList<String>();
									paymentRow.add(amountPaid1);
									paymentRow.add(payDate1);
									bodyRT.add(paymentRow);
									
									map.put(TaxHistorySetKey.DATE_PAID.getKeyName(), payDate1);
								}
							
							tmpMap.put("ReceiptAmount", new String[] { "ReceiptAmount", "" });
							tmpMap.put("ReceiptDate", new String[] { "ReceiptDate", "" });
							receipts.setHead(header);
							receipts.setMap(tmpMap);
							receipts.setBody(bodyRT);
							receipts.setReadOnly();
							map.put("TaxHistorySet", receipts);
							
						} catch (Exception e) {
							e.printStackTrace();
						}
				}
			}
		
//		map.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), formatter.format(priorDelinq));
		
	}
	
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		String[] parts = { "", "", "" };
		
		RejectNonRealEstate propertyTypeFilter = new RejectNonRealEstate(searchId);
		propertyTypeFilter.setThreshold(new BigDecimal("0.95"));
		
		TaxYearFilterResponse yearFilter = new TaxYearFilterResponse(searchId, true, numberOfYearsAllowed, true);

		String pid = getSearchAttribute(SearchAttributes.LD_PARCELNO);
		if (pid.length() == 8) {
			// 06037194
			parts[0] = pid.substring(0,2);
			parts[2] = pid.substring(2,8);
			
		} else {
			Matcher mat = Pattern.compile("(\\d{2})-(\\d{1})-(\\d{6})").matcher(pid);
			if (mat.find()) {
				parts[0] = mat.group(1);
				parts[1] = mat.group(2);
				parts[2] = mat.group(3);
			}
			
		}
		
		if (InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchType() == Search.AUTOMATIC_SEARCH) {
			if(hasPin()) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX)); 		
				module.clearSaKeys();
				
				module.forceValue(1, parts[0]);
				if (StringUtils.isNotEmpty(parts[1])) {
					module.forceValue(2, parts[1]);
				} else {
					module.forceValue(2, "0");
				}
				module.forceValue(3, parts[2]);
				
				module.addFilter(propertyTypeFilter);
				module.addFilter(yearFilter);
				
				moduleList.add(module);
			}
			
			if(hasPin()) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX)); 		
				module.clearSaKeys();
				
				module.forceValue(1, parts[0]);
				if (StringUtils.isNotEmpty(parts[1])) {
					module.forceValue(2, parts[1]);
				} else {
					module.forceValue(2, "1");
				}
				module.forceValue(3, parts[2]);
				
				module.addFilter(propertyTypeFilter);
				module.addFilter(yearFilter);
				
				moduleList.add(module);
			}
		}
		
		serverInfo.setModulesForAutoSearch(moduleList);
	}
	
}
