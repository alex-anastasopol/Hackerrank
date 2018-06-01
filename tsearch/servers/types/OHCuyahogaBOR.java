package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http3.HttpManager3;
import ro.cst.tsearch.connection.http3.HttpSite3;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.RegisterDocumentI;

public class OHCuyahogaBOR extends TSServer {

	private static final long serialVersionUID = -5017114631670042036L;
	
	public static final String IMG_PATT = "(?is)<img[^>]+>";
	public static final String DETAILS_PATT = "(?is)<a[^>]+href=\"javascript:viewComplaintInfo\\('([^']+)'[^)]+\\);\">";

	public OHCuyahogaBOR(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public OHCuyahogaBOR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		if (rsResponse.indexOf("NO RESULTS FOR CURRENT SEARCH") > -1) {
			Response.getParsedResponse().setError(NO_DATA_FOUND);
			Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
			return;
		}

		switch (viParseID) {
		
		case ID_SEARCH_BY_PARCEL:		//Search for Complaint
			
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if (smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,	outputTable.toString());
			}
			
			String header = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
			header += parsedResponse.getHeader();

			String footer = parsedResponse.getFooter();
			Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
			
			if (numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
				footer += CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer) numberOfUnsavedDocument);
			} else {
				footer += CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
			}

			parsedResponse.setHeader(header);
			parsedResponse.setFooter(footer);

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
				loadDataHash(data);
				if (isInstrumentSaved(serialNumber.toString(), null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);

			} else {
				smartParseDetails(Response, details);

				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				Response.getParsedResponse().setResponse(details);

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}
			break;

		case ID_GET_LINK:
			ParseResponse(sAction, Response, rsResponse.contains("Total Found:")?ID_SEARCH_BY_PARCEL:ID_DETAILS);
			break;

		default:
			break;
		}

	}

	protected void loadDataHash(HashMap<String, String> data) {
		if (data != null) {
			data.put("type", "COMPLAINT");
		}
	}
	
	protected String getDetails(String rsResponse, StringBuilder instno) {
		try {

			TableTag resultsTable = null;
			StringBuilder details = new StringBuilder();
			String complaintNumber = "";
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "clsTblBgrd"), true);
					
			if (mainTableList.size()>0) {
				resultsTable = (TableTag)mainTableList.elementAt(0);
				resultsTable.setAttribute("width", "100%");
			}
			
			if (resultsTable != null && resultsTable.getRowCount()>2) {
					
				TableRow firstRow = resultsTable.getRow(2);
				if (firstRow.getColumnCount()>2) {
					complaintNumber = firstRow.getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim();
					instno.append(complaintNumber);
				}
				
				/* If from memory - use it as is */
				if (!rsResponse.toLowerCase().contains("<html")) {
					return rsResponse;
				}
				
				details.append("<table width=\"700\">");
				
				String tableString = resultsTable.toHtml();
				tableString = tableString.replaceFirst("(?is)class=\"style35 style6\"", "align=\"center\"");
				tableString = tableString.replaceFirst(IMG_PATT, "<b>Complaint info</b>");
				tableString = tableString.replaceAll(IMG_PATT, "");
				tableString = tableString.replaceFirst("(?is)(<div[^>]+id=\"AttorneyInfo\")[^>]*(>)", "<b>Compl Attorney Info</b>$1$2");
				tableString = tableString.replaceFirst("(?is)(<div[^>]+id=\"OwnAttorneyInfo\")[^>]*(>)", "<b>Owner Attorney Info</b>$1$2");
				details.append("<tr><td>");
				details.append(tableString);	//add Complaint info, Compl Attorney Info and Owner Attorney Info
				details.append("</td></tr>");
				
				String link1 = getBaseLink() + "ComplaintsReports/display_par.asp?complaintnumber=" + complaintNumber;
				String parcelInfoGeneral = "";
				HttpSite3 parcelInfoGeneralPage = HttpManager3.getSite(getCurrentServerName(), searchId);
				try {
					parcelInfoGeneral = ((ro.cst.tsearch.connection.http3.OHCuyahogaBOR)parcelInfoGeneralPage).getPageWithPost(link1);
				} catch(Exception e) {
					e.printStackTrace();
				} finally {
					HttpManager3.releaseSite(parcelInfoGeneralPage);
				}
				if (!StringUtils.isEmpty(parcelInfoGeneral)) {
					org.htmlparser.Parser htmlParserParcelInfoGeneral = org.htmlparser.Parser.createParser(parcelInfoGeneral, null);
					NodeList list = htmlParserParcelInfoGeneral.parse(null);
					TableTag table = (TableTag)HtmlParser3.getNodeByTypeAndAttribute(list, "table", "class", "clsTblBgrd", true);
					if (table!=null) {
						table.setAttribute("width", "100%");
						table.setAttribute("style", "padding-left:10px;");
						tableString = table.toHtml();
						tableString = tableString.replaceFirst("(?is)(<td[^<]+)class=\"clsFormTitle\"([^<]*>)([^<]+)(</td>)", "$1 align=\"center\" style=\"color:#ffffff;\"$2<strong>$3</strong>$4");
						tableString = tableString.replaceAll(IMG_PATT, "");
						
						String parcelListPatt = "(?is)<a[^>]+href=\\\"javascript:updateParcelInfo\\('([^']+)'\\);\\\">([^<]+)</a>";
						List<String> parcelListLink = RegExUtils.getMatches(parcelListPatt, tableString, 1);
						List<String> parcelListDisplay = RegExUtils.getMatches(parcelListPatt, tableString, 2);
						List<String> addedParcelListLink  = new ArrayList<String>();
						List<String> addedParcelListDisplay  = new ArrayList<String>();
						List<String> parcelInfoList  = new ArrayList<String>();
						for (int i=0;i<parcelListLink.size();i++) {
							String parcel = parcelListLink.get(i);
							String link2 = getBaseLink() + "ComplaintsReports/display_parinfo.asp?complaintnumber=" + complaintNumber + "&parcelnumber=" + parcel;
							String parcelInfo = "";
							HttpSite3 parcelInfoPage = HttpManager3.getSite(getCurrentServerName(), searchId);
							try {
								parcelInfo = ((ro.cst.tsearch.connection.http3.OHCuyahogaBOR)parcelInfoPage).getPageWithPost(link2);
							} catch(Exception e) {
								e.printStackTrace();
							} finally {
								HttpManager3.releaseSite(parcelInfoPage);
							}
							if (!StringUtils.isEmpty(parcelInfoGeneral)) {
								parcelInfoList.add(parcelInfo);
								addedParcelListLink.add(parcel);
								addedParcelListDisplay.add(parcelListDisplay.get(i));
							}
						}
						
						StringBuilder sb = new StringBuilder();
						for (int j=0;j<parcelInfoList.size();j++) {
							String parcelInfo = parcelInfoList.get(j);
							parcelInfo = parcelInfo.replaceAll("(?s)<!--.*?-->", "");
							parcelInfo = Tidy.tidyParse(parcelInfo, null);
							org.htmlparser.Parser htmlParserParcelInfo = org.htmlparser.Parser.createParser(parcelInfo, null);
							NodeList lst = htmlParserParcelInfo.parse(null);
							TableTag tbl = (TableTag)HtmlParser3.getNodeByTypeAndAttribute(lst, "table", "class", "clsTableBorder", true);
							if (tbl!=null) {
								tbl.setAttribute("width", "100%");
								tbl.setAttribute("id", "parcelInfo");
								String tblHtml = tbl.toHtml(); 
								String part1 = tblHtml;
								String part2 = "";
								int idx = tblHtml.toLowerCase().lastIndexOf("</table>");
								if (idx!=-1) {
									part1 = tblHtml.substring(0, idx);
									part2 = tblHtml.substring(idx);
								}
								sb.append("<tr><td colspan=\"2\"><b>Parcel ").append(addedParcelListDisplay.get(j)).append("</b><br>").append(part1);
								NodeList tblList = lst.extractAllNodesThatMatch(new TagNameFilter("table"), true);
								TableTag tbl2 = null;
								for (int i=0;i<tblList.size();i++) {
									if (tblList.elementAt(i).toPlainTextString().contains("Owner Street Dir.:")) {
										tbl2 = (TableTag)tblList.elementAt(i);
										break;
									}
								}
								if (tbl2!=null) {
									TableRow[] rows = tbl2.getRows();
									for (int i=0;i<rows.length;i++) {
										sb.append(rows[i].toHtml());
									}
								}
								sb.append(part2);
							}
							
							sb.append("</td></tr>");
						}
						
						int index = tableString.toLowerCase().indexOf("</tr>");
						if (index>-1) {
							tableString = tableString.substring(0, index + "</tr>".length()) + sb.toString() + "</table>";
						}
						
						details.append("<tr><td>");
						details.append(tableString);	//add Parcel Info
						details.append("</td></tr>");
					}
				}
				
				String link3 = getBaseLink() + "ComplaintsReports/display_dec.asp?complaintnumber=" + complaintNumber;
				String decisionsGeneral = "";
				HttpSite3 decisionsPage = HttpManager3.getSite(getCurrentServerName(), searchId);
				try {
					decisionsGeneral = ((ro.cst.tsearch.connection.http3.OHCuyahogaBOR)decisionsPage).getPageWithPost(link3);
				} catch(Exception e) {
					e.printStackTrace();
				} finally {
					HttpManager3.releaseSite(decisionsPage);
				}
				if (!StringUtils.isEmpty(decisionsGeneral)) {
					org.htmlparser.Parser htmlParserParcelInfoGeneral = org.htmlparser.Parser.createParser(decisionsGeneral, null);
					NodeList list = htmlParserParcelInfoGeneral.parse(null);
					TableTag table = (TableTag)HtmlParser3.getNodeByTypeAndAttribute(list, "table", "class", "clsTblBgrd", true);
					if (table!=null) {
						table.setAttribute("width", "100%");
						table.setAttribute("style", "padding-left:10px;");
						tableString = table.toHtml();
						tableString = tableString.replaceFirst("(?is)(<td[^<]+)class=\"clsFormTitle\"([^<]*>)([^<]+)(</td>)", "$1 align=\"center\" style=\"color:#ffffff;\"$2<strong>$3</strong>$4");
						
						String parcelListPatt = "(?is)<a[^>]+href=\\\"javascript:updateDecisInfo\\('([^']+)'\\);\\\">([^<]+)</a>";
						List<String> decisionListLink = RegExUtils.getMatches(parcelListPatt, tableString, 1);
						List<String> decisionDisplay = RegExUtils.getMatches(parcelListPatt, tableString, 2);
						List<String> addedDecisionListLink  = new ArrayList<String>();
						List<String> addedDecisionListDisplay  = new ArrayList<String>();
						List<String> decisionList  = new ArrayList<String>();
						for (int i=0;i<decisionListLink.size();i++) {
							String parcel = decisionListLink.get(i);
							String link4 = getBaseLink() + "ComplaintsReports/display_decinfo.asp?complaintnumber=" + complaintNumber + "&parcelnumber=" + parcel;
							String decision = "";
							HttpSite3 parcelInfoPage = HttpManager3.getSite(getCurrentServerName(), searchId);
							try {
								decision = ((ro.cst.tsearch.connection.http3.OHCuyahogaBOR)parcelInfoPage).getPageWithPost(link4);
							} catch(Exception e) {
								e.printStackTrace();
							} finally {
								HttpManager3.releaseSite(parcelInfoPage);
							}
							if (!StringUtils.isEmpty(decisionsGeneral)) {
								decisionList.add(decision);
								addedDecisionListLink.add(parcel);
								addedDecisionListDisplay.add(decisionDisplay.get(i));
							}
						}
						
						StringBuilder sb = new StringBuilder();
						for (int j=0;j<decisionList.size();j++) {
							String decision = decisionList.get(j);
							org.htmlparser.Parser htmlParserParcelInfo = org.htmlparser.Parser.createParser(decision, null);
							NodeList lst = htmlParserParcelInfo.parse(null);
							TableTag tbl = (TableTag)HtmlParser3.getNodeByTypeAndAttribute(lst, "table", "class", "clsTableBorder", true);
							if (tbl!=null) {
								tbl.setAttribute("width", "100%");
								String tblString = tbl.toHtml();
								tblString = tblString.replaceFirst("(?is)\\A(\\s*<table[^>]*>\\s*)<tr[^>]*>.*?</tr>", "$1");	//remove the first row
								tblString = tblString.replaceFirst("(?is)<tr>\\s*<td[^>]*>\\s*<div[^>]*>[\\s(&nbsp;)]*<input[^>]+value=\"Go\\s+Back\"[^>]*>\\s*</div>\\s*</td>\\s*</tr>(\\s*</table>\\s*)\\Z",
									"$1");	//remove the last row
								sb.append("<tr><td colspan=\"2\"><b>Parcel ").append(addedDecisionListDisplay.get(j)).append("</b><br>").append(tblString);
							}
							
							sb.append("</td></tr>");
						}
						
						int index = tableString.toLowerCase().indexOf("</tr>");
						if (index>-1) {
							tableString = tableString.substring(0, index + "</tr>".length()) + sb.toString() + "</table>";
						}
						
						details.append("<tr><td>");
						details.append(tableString);	//add Decisions
						details.append("</td></tr>");
					}
				}
				
				String link5 = getBaseLink() + "ComplaintsReports/display_appeal.asp?complaintnumber=" + complaintNumber;
				String appealInfoGeneral = "";
				HttpSite3 appealInfoPage = HttpManager3.getSite(getCurrentServerName(), searchId);
				try {
					appealInfoGeneral = ((ro.cst.tsearch.connection.http3.OHCuyahogaBOR)appealInfoPage).getPageWithPost(link5);
				} catch(Exception e) {
					e.printStackTrace();
				} finally {
					HttpManager3.releaseSite(appealInfoPage);
				}
				if (!StringUtils.isEmpty(appealInfoGeneral)) {
					StringBuilder sb = new StringBuilder();
					sb.append("<table width=\"100%\" style=\"padding-left:10px;\">");
					org.htmlparser.Parser htmlParserParcelInfoGeneral = org.htmlparser.Parser.createParser(appealInfoGeneral, null);
					NodeList list = htmlParserParcelInfoGeneral.parse(null);
					Node tr = HtmlParser3.getNodeByTypeAndAttribute(list, "tr", "bgcolor", "#7DA7D9", true);
					if (tr!=null) {
						sb.append(tr.toHtml());
					}
					TableTag table = (TableTag)HtmlParser3.getNodeByTypeAndAttribute(list, "table", "bgcolor", "#e7eefa", true);
					if (table!=null) {
						table.setAttribute("width", "100%");
						tableString = table.toHtml();
						tableString = tableString.replaceFirst("(?is)(<td[^<]+)class=\"clsFormTitle\"([^<]*>)([^<]+)(</td>)", "$1 align=\"center\" style=\"color:#ffffff;\"$2<strong>$3</strong>$4");
						tableString = tableString.replaceFirst("(?is)<tr>\\s*<td[^>]*>\\s*<div[^>]*>[\\s(&nbsp;)]*<input[^>]+value=\"Go\\s+Back\"[^>]*>\\s*</div>\\s*</td>\\s*</tr>(\\s*</table>\\s*)\\Z",
							"$1");	//remove the last row
						sb.append("<tr><td>").append(tableString).append("</td></tr>");
					}
					sb.append("</table>");
					details.append("<tr><td>");
					details.append(sb.toString());	//add Appeal Info
					details.append("</td></tr>");
				}	
				
				details.append("</table>");
									
			}
			
			return details.toString();

		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
	
	public static boolean contains(StringBuilder sb, String s, boolean isIntermediary) {
		
		if (StringUtils.isEmpty(s)) {
			return true;
		}
		
		if (isIntermediary) {
			if (sb.indexOf("<td>")==-1) {
				return false;
			}
		} else {
			if (sb.indexOf(" / ")==-1) {
				return false;
			}
		}
		
		if (sb.indexOf(s)!=-1) {
			return true;
		}
		
		List<String> list = new ArrayList<String>();
		if (isIntermediary) {
			list = RegExUtils.getMatches("(?is)<td>(.*?)\\.(&nbsp;)?</td>", sb.toString(), 1);
		} else {
			String[] split = sb.toString().split(" / ");
			list = Arrays.asList(split);
		}
				
		for (int i=0;i<list.size();i++) {
			list.set(i, list.get(i).replaceFirst(",", ""));
		}
		if (list.contains(s.replaceFirst("(?is)&nbsp;\\s*$", "").replaceFirst("\\.\\s*$", "").replaceFirst(",", ""))) {
			return true;
		}
		return false;
	}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		int numberOfUncheckedElements = 0;
		
		try {
			TableTag resultsTable = null;
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"), true);
			
			if (mainTableList.size()>0) {
				resultsTable = (TableTag)mainTableList.elementAt(0);
			}
			
			if (resultsTable!=null && resultsTable.getRowCount()>1) {
				
				//group the rows which have the same complaint # (the key is the link)
				LinkedHashMap<String, List<TableRow>> rowsMap = new  LinkedHashMap<String, List<TableRow>>();
				
				TableRow[] rows  = resultsTable.getRows();
				
				int len = rows.length;
				for (int i=1;i<len;i++) {
					
					TableRow row = rows[i];
						
					if (row.getColumnCount()>4) {
						String htmlRow = row.toHtml();
						Matcher ma = Pattern.compile(DETAILS_PATT).matcher(htmlRow);
						if (ma.find()) {
							String key = ma.group(0);
							if (rowsMap.containsKey(key)) {	//complaint already added
								List<TableRow> value = rowsMap.get(key);
								value.add(row);
							} else {						//add new complaint
								List<TableRow> value = new ArrayList<TableRow>();
								value.add(row);
								rowsMap.put(key, value);
							}
						}
					}
					
					
				}
				
				int i=1;
				Iterator<Entry<String, List<TableRow>>> it = rowsMap.entrySet().iterator();
				while (it.hasNext()) {
				
					Map.Entry<String, List<TableRow>> pair = (Map.Entry<String, List<TableRow>>)it.next();
					List<TableRow> value = pair.getValue();
					
					StringBuilder sb = new StringBuilder();
					
					sb.append(value.get(0).getColumns()[0].toHtml());	//Complaint #
					
					sb.append(value.get(0).getColumns()[1].toHtml());	//Tax Year
					
					StringBuilder sb1 = new StringBuilder();
					sb1.append("<td><table>");
					for (int j=0;j<value.size();j++) {
						sb1.append("<tr>");
						sb1.append(value.get(j).getColumns()[2].toHtml());
						sb1.append("</tr>");
					}
					sb1.append("</table></td>");
					sb.append(sb1);										//Parcel #
					
					StringBuilder sb2 = new StringBuilder();
					sb2.append("<table>");
					StringBuilder sb3 = new StringBuilder();
					sb3.append("<table>");
					for (int j=0;j<value.size();j++) {
						String s = value.get(j).getColumns()[3].toHtml();
						String[] split = s.split("(?is)<br\\s*/?\\s*>");
						if (split.length==2) {
							String s1 = split[0].trim();
							s1 = s1.replaceAll("(?is)</?td[^>]*>", "");
							if (!contains(sb2, s1, true)) {
								sb2.append("<tr><td>");
								sb2.append(s1);
								sb2.append("</td></tr>");
							}
							String s2 = split[1].trim();
							s2 = s2.replaceAll("(?is)</?td[^>]*>", "");
							sb3.append("<tr><td>");
							sb3.append(s2);
							sb3.append("</td></tr>");
						}
					}
					sb2.append("</table>");
					sb3.append("</table>");
					sb.append("<td>");									
					sb.append(sb2);										//Owner Name
					sb.append(sb3);										//Address
					sb.append("</td>");
					
					sb.append(value.get(0).getColumns()[4].toHtml());	//Hearing Date
					
					String htmlRow = sb.toString();
					
					ResultMap m = ro.cst.tsearch.servers.functions.OHCuyahogaBOR.parseIntermediaryRow(htmlRow, searchId);
					String recordedDate = (String)m.get(SaleDataSetKey.RECORDED_DATE.getKeyName());
					
					String link = CreatePartialLink(TSConnectionURL.idPOST) + "/ComplaintsReports/display_comp.asp?";
					if (!StringUtils.isEmpty(recordedDate)) {
						recordedDate = recordedDate.replaceAll("/", "-");
						link += "date=" + recordedDate + "&";
					}
					link += "complaintnumber=";
					
					Matcher ma = Pattern.compile(DETAILS_PATT).matcher(htmlRow);
					if (ma.find()) {
						link += ma.group(1);
					}
					htmlRow = htmlRow.replaceAll(DETAILS_PATT, "<a href=\"" +  link + "\">");
					
					String rowType = "1";
					if (i%2==0) {
						rowType = "2";
					}
						
					ParsedResponse currentResponse = new ParsedResponse();
						
					currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
					
					
					String instrNo = org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName()));
					String date = org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.RECORDED_DATE.getKeyName()));
					String year = "";
					int index = date.lastIndexOf("/");
					if (index>-1 && index<date.length()-1) {
						year = date.substring(index+1);
					}
					
					String checkBox = "checked";
					
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("instrno", instrNo);
					data.put("type", "COMPLAINT");
					data.put("year", year);
					
					if (isInstrumentSaved(instrNo, null, data, false)) {
						checkBox = "saved";
					} else {
						numberOfUncheckedElements++;
						LinkInPage linkInPage = new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD);
						checkBox = "<input type='checkbox' name='docLink' value='" + link + "'>";
						currentResponse.setPageLink(linkInPage);
					}
					String rowHtml = "<tr class=\"row" + rowType + "\"><td align=\"center\">" + checkBox + "</td>" + htmlRow + "</tr>";
										
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
					currentResponse.setOnlyResponse(rowHtml);
					 
					Bridge bridge = new Bridge(currentResponse, m, searchId);
						
					DocumentI document = (RegisterDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
					
					i++;
					
				}	
				
				String headerRow = "";
				headerRow = "<tr bgcolor=\"#7DA7D9\">" +
							"<td align=\"center\">" + SELECT_ALL_CHECKBOXES + "</td>" +
							"<td><b>Complaint #</b></td>" +
							"<td><b>Tax Year</b></td>" +
							"<td><b>Parcel #</b></td>" +
							"<td><b>Owner Name / Address</b></td>" +
							"<td><b>Hearing Date</b></td></tr>";
				
				String prevNextString = "";
				NodeList prevNextList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "700"), true)
					.extractAllNodesThatMatch(new TagNameFilter("td"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "9%"), true)
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("cellspacing", "0"), true);
				if (prevNextList.size()>0) {
					String query = response.getQuerry();
					prevNextString = prevNextList.elementAt(0).toHtml();
					String prevNextLink = CreatePartialLink(TSConnectionURL.idPOST) + "/ComplaintsReports/complaint_list.asp" + "?page=";
					prevNextString = prevNextString.replaceAll("(?is)<a[^>]+href=\"javascript:submitFormQuery\\(([^)]+)\\);\"[^>]*>", 
						"<a href=\"" +  prevNextLink + "$1&" + query + "\">");
					prevNextString = "<tr><td align=\"right\">" + prevNextString + "</td></tr>";
				}
				
				String header = "<table>" + prevNextString + "<tr><td><table>" + headerRow;
				String footer = "</table></td></tr></table>";
				
				response.getParsedResponse().setHeader(header);
				response.getParsedResponse().setFooter(footer);
												
				outputTable.append(table);
				SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			}

		} catch (Throwable t) {
			logger.error("Error while parsing intermediary data", t);
		}

		return intermediaryResponse;
	}

	@SuppressWarnings({ "rawtypes" })
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
	
		try {
						
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "BOR");
			resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "COMPLAINT");
				
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			String complaintNumber = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Complaint Number:"), "", true, false);
			if (!StringUtils.isEmpty(complaintNumber)) {
				complaintNumber = complaintNumber.replaceAll("(?is)</?div[^>]*>", "").replaceAll("(?is)&nbsp;", "").trim();
				resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), complaintNumber);
			}
			
			LinkInPage lip = response.getParsedResponse().getPageLink();
			if (lip!=null) {
				String link = lip.getOriginalLink();
				if (!StringUtils.isEmpty(link)) {
					String date = StringUtils.extractParameter(link, "date=([^&?]*)");
					if (!StringUtils.isEmpty(date)) {
						date = date.replaceAll("-", "/");
						resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), date);
					}
				}
			}
			
			
			List<List> bodyPIS = new ArrayList<List>();
			List<String> linePIS;
			StringBuilder names = new StringBuilder();
							
			NodeList parcelInfoList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "parcelInfo"));
			
			for (int i=0;i<parcelInfoList.size();i++) {
				
				linePIS = new ArrayList<String>();
								
				Node node = parcelInfoList.elementAt(i);
				NodeList list = node.getChildren();
				
				String parcelNumber = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(list, "Parcel Number :"), "", true, false);
				parcelNumber = parcelNumber.replaceAll("(?is)</?div[^>]*>", "").replaceAll("(?is)&nbsp;", "").trim();
				
				String owner = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(list, "Owner:"), "", true, false);
				owner = owner.replaceAll("(?is)</?div[^>]*>", "").replaceAll("(?is)&nbsp;", "").trim();
				if (!contains(names, owner, false)) {
					names.append(owner).append(" / ");					
				}
				
				String coOwner = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(list, "Owner c/o:"), "", true, false);
				coOwner = coOwner.replaceAll("(?is)</?div[^>]*>", "").replaceAll("(?is)&nbsp;", "").trim();
				if (!contains(names, coOwner, false)) {
					names.append(coOwner).append(" / ");					
				}
				
				String streetNo = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(list, "Owner Street #:"), "", true, false);
				streetNo = streetNo.replaceAll("(?is)</?div[^>]*>", "").replaceAll("(?is)&nbsp;", "").trim().replaceFirst("^0+", "");
				
				String streetDir = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(list, "Owner Street Dir.:"), "", true, false);
				streetDir = streetDir.replaceAll("(?is)</?div[^>]*>", "").replaceAll("(?is)&nbsp;", "").trim();
				String streetName = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(list, "Owner Str. Name:"), "", true, false);
				streetName = streetName.replaceAll("(?is)</?div[^>]*>", "").replaceAll("(?is)&nbsp;", "").trim();
				streetName =  streetDir + " " + streetName;
				streetName = streetName.trim();
				
				String city = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(list, "Owner City:"), "", true, false);
				city = city.replaceAll("(?is)</?div[^>]*>", "").replaceAll("(?is)&nbsp;", "").trim();
				
				String zip = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(list, "Owner Zip:"), "", true, false);
				zip = zip.replaceAll("(?is)</?div[^>]*>", "").replaceAll("(?is)&nbsp;", "").trim();
				
				linePIS.add(parcelNumber);
				linePIS.add(streetNo);
				linePIS.add(streetName);
				linePIS.add(city);
				linePIS.add(zip);
				bodyPIS.add(linePIS);
				
				String land = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(list, "Mkt Land Value ($):"), "", true, false);
				land = land.replaceAll("(?is)</?div[^>]*>", "").replaceAll("(?is)&nbsp;", "").trim();
				
				String impr = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(list, "Mkt Bldg Value ($):"), "", true, false);
				impr = impr.replaceAll("(?is)</?div[^>]*>", "").replaceAll("(?is)&nbsp;", "").trim();
				
			}
			
			String namesString = names.toString();
			namesString = namesString.replaceFirst("\\s*/\\s*$", "");
			resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), namesString);
			
			ro.cst.tsearch.servers.functions.OHCuyahogaBOR.parseNames(resultMap, searchId);
			
			String[] headerPIS = {PropertyIdentificationSetKey.PARCEL_ID.getShortKeyName(),
								 PropertyIdentificationSetKey.STREET_NO.getShortKeyName(),
								 PropertyIdentificationSetKey.STREET_NAME.getShortKeyName(),
								 PropertyIdentificationSetKey.CITY.getShortKeyName(),
								 PropertyIdentificationSetKey.ZIP.getShortKeyName()};
			ResultTable rt = GenericFunctions2.createResultTable(bodyPIS, headerPIS);
			resultMap.put("PropertyIdentificationSet", rt);
		
		} catch (ParserException e) {
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
			module.setSaKey(1, SearchAttributes.LD_PARCELNO);
			moduleList.add(module);
		}

		serverInfo.setModulesForAutoSearch(moduleList);
	}
	
}
