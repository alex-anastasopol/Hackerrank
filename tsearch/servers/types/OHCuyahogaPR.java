package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.ScriptTag;
import org.htmlparser.tags.Span;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http3.HttpManager3;
import ro.cst.tsearch.connection.http3.HttpSite3;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.CourtDocumentIdentificationSet.CourtDocumentIdentificationSetKey;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.BaseServlet;

import com.stewart.ats.base.document.DocumentI;


public class OHCuyahogaPR extends TSServer {

	private static final long serialVersionUID = 4940198725194257741L;

	private static String ALL_CASE_CATEGORIES = "";
	private static String ALL_PARTY_ROLES = "";
	
	static {
		String folderPath = ServerConfig.getModuleDescriptionFolder(BaseServlet.REAL_PATH + "WEB-INF/classes/resource/module/comments");
		File folder = new File(folderPath);
		if (!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath	+ "] does not exist. Module Information not loaded!");
		}
		try {
			 ALL_CASE_CATEGORIES = FileUtils.readFileToString(new File(folderPath	+ File.separator + "OHCuyahogaPRAllCaseCategories.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			ALL_PARTY_ROLES = FileUtils.readFileToString(new File(folderPath	+ File.separator + "OHCuyahogaPRAllPartyRoles.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String getALL_CASE_CATEGORIES() {
		return ALL_CASE_CATEGORIES;
	}

	public static String getALL_PARTY_ROLES() {
		return ALL_PARTY_ROLES;
	}
	
	public OHCuyahogaPR(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	public OHCuyahogaPR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	
	private static int seq = 0;

	protected synchronized static int getSeq() {
		return seq++;
	}
	
	
	public String getHiddenParam(NodeList nodeList)  {
		String hiddenVal = "";
		String hiddenKey = "";
		
		if (nodeList!= null && nodeList.size() > 2) {
			ScriptTag s = (ScriptTag) nodeList.elementAt(2);			
			
			if (s != null) {
				Pattern p = Pattern.compile("(?is)[^?]+\\?_TSM_HiddenField_=([^&]+)[^_]+_TSM_CombinedScripts_=([^\"]+)");
				Matcher m = p.matcher(s.getAttribute("src"));
				
				if (m.find()) {
					hiddenKey = m.group(1);
					if ("ctl00_ctl00_ScriptManager1_HiddenField".equals(hiddenKey)) {
						hiddenVal = m.group(2);
						try {
							hiddenVal = URLDecoder.decode(hiddenVal, "UTF-8");
						} catch (UnsupportedEncodingException e) {							
							e.printStackTrace();
							logger.trace(e);
						}
					}
				}
			}
		}
		
		return hiddenVal;
	}

	
	public Map<String,String> extractPostParams(String htmlResponse, String formName) {
		Map<String, String> paramsToExtract = new HashMap<String, String>();	
		HttpSite3 site = HttpManager3.getSite(getCurrentServerName(), searchId);
			if (site != null) {
				try {
					paramsToExtract = HttpSite3.fillConnectionParams(htmlResponse,
							((ro.cst.tsearch.connection.http3.OHCuyahogaPR) site).getTargetArgumentParameters(), formName);
				} finally {
					// always release the HttpSite
					HttpManager3.releaseSite(site);
				}
			}
		return paramsToExtract;
	}
	
	
	public Map<String,String> extractHiddenParams(String htmlResponse) {
		Map<String, String> paramsToExtract = new HashMap<String, String>();
		String scriptToParse = htmlResponse.substring(htmlResponse.lastIndexOf("</table>") + 8).trim();
		Matcher m = Pattern.compile("(?is).*\\|hiddenField\\|__VIEWSTATE\\|([^\\|]+).*\\|hiddenField\\|__EVENTVALIDATION\\|([^\\|]+)").matcher(scriptToParse);
		
		if (m.find()) {
			try {
				String viewstate =  URLDecoder.decode(m.group(1), "UTF-8");
				paramsToExtract.put("__VIEWSTATE", viewstate);
				
				String eventvalidation =  URLDecoder.decode(m.group(2), "UTF-8");
				paramsToExtract.put("__EVENTVALIDATION", eventvalidation);
			} catch (UnsupportedEncodingException e) {							
				e.printStackTrace();
				logger.trace(e);
			}
		}
		
		return paramsToExtract;
	}
	
	
	public StringBuilder getPrevAndNextLinks (ServerResponse response, HtmlParser3 htmlParser, String sourceTag) {
		String linkN = "";
		String linkP = "";
		String links = "";
		
		String url = response.getLastURI().toString();
		url = url.substring(url.indexOf("/pa/"));
		StringBuilder footer = new StringBuilder("<tr><td colspan=\"5\"> </br> &nbsp; &nbsp;");
		HashMap<String,String> paramsOfReq = htmlParser.getListOfPostParams(sourceTag);
		int seq = getSeq();
		paramsOfReq.remove("__EVENTTARGET");
		paramsOfReq.remove("__EVENTARGUMENT");

		// create links for Next/Prev buttons
		NodeList pagesTable = htmlParser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "mpContentPH_gvSearchResults"), true)
				.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true); 
				
		if (pagesTable != null) {
			TableTag tableTag = (TableTag) pagesTable.elementAt(0);
			
			if (tableTag != null) {
				TableRow row = tableTag.getRow(0); //just one row
				int noOfCols = row.getColumnCount();
				String paramsPostBack = "";
				String regExp = "(?is)javascript:__doPostBack\\(([^,]+),([^\\)]+)\\)";
				
				mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, paramsOfReq);
				
				if (noOfCols == 2) {
					Span currentPageNo = (Span) row.getColumns()[0].getFirstChild();
					if (currentPageNo != null) {
						linkP = "<a style=\"color:grey; text-decoration: none;\">PREV</a>";
						paramsPostBack = ((LinkTag) row.getColumns()[1].getFirstChild()).extractLink().trim().replaceAll("\\s", "%20");
						if (paramsPostBack != null) {
							Matcher m = Pattern.compile(regExp).matcher(paramsPostBack);
							if (m.find()) {
								linkN = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + url + "?__EVENTTARGET=" + m.group(1).replaceAll("&#39;", "")
										+ "&__EVENTARGUMENT=" + m.group(2).replaceAll("&#39;", "") + "&seq=" + seq +  "\">NEXT</a>";
							}
						}
					} else {
						currentPageNo = (Span) row.getColumns()[0].getFirstChild();
						if (currentPageNo != null) {
							linkN = "<a style=\"color:grey; text-decoration: none;\">NEXT</a>";
							paramsPostBack = ((LinkTag) row.getColumns()[0].getFirstChild()).extractLink().trim().replaceAll("\\s", "%20");
							if (paramsPostBack != null) {
								Matcher m = Pattern.compile(regExp).matcher(paramsPostBack);
								if (m.find()) {
									linkP = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + url + "?__EVENTTARGET=" + m.group(1).replaceAll("&#39;", "")
											+ "&__EVENTARGUMENT=" + m.group(2).replaceAll("&#39;", "") + "&seq=" + seq +  "\">PREV</a>";
								}
							}
						}
					}
				
				} else if (noOfCols > 2) {
					for (int i=1; i <= noOfCols-2; i++ ) {
						TableColumn prevCol = row.getColumns()[i-1];
						TableColumn currentCol = row.getColumns()[i];
						TableColumn nextCol = row.getColumns()[i+1];
						Matcher m;
						
						//<span>N</span> means that N=current page
						Node nodeCurrentPage = currentCol.getFirstChild();
						
						if (nodeCurrentPage != null && nodeCurrentPage.toHtml().contains("<span>")) {
							paramsPostBack = ((LinkTag) prevCol.getFirstChild()).extractLink().trim().replaceAll("\\s", "%20");
							if (paramsPostBack != null) {
								m = Pattern.compile(regExp).matcher(paramsPostBack);
								if (m.find()) {
									linkP = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + url + "?__EVENTTARGET=" + m.group(1).replaceAll("&#39;", "")
											+ "&__EVENTARGUMENT=" + m.group(2).replaceAll("&#39;", "") + "&seq=" + seq +  "\">PREV</a>";
								}
							}
							paramsPostBack = ((LinkTag) nextCol.getFirstChild()).extractLink().trim().replaceAll("\\s", "%20");
							if (paramsPostBack != null) {
								m = Pattern.compile(regExp).matcher(paramsPostBack);
								if (m.find()) {
									linkN = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + url + "?__EVENTTARGET=" + m.group(1).replaceAll("&#39;", "")
											+ "&__EVENTARGUMENT=" + m.group(2).replaceAll("&#39;", "") + "&seq=" + seq +  "\">NEXT</a>";
								}
							}	
							
							break;
							
						} else {
							nodeCurrentPage = prevCol.getFirstChild();
							if (nodeCurrentPage != null && nodeCurrentPage.toHtml().contains("<span>")) {
								//first page, so no Prev link should be added
								linkP = "<a style=\"color:grey; text-decoration: none;\">PREV</a>";
								paramsPostBack = ((LinkTag) currentCol.getFirstChild()).extractLink().trim().replaceAll("\\s", "%20");
								if (paramsPostBack != null) {
									m = Pattern.compile(regExp).matcher(paramsPostBack);
									if (m.find()) {
										linkN = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + url + "?__EVENTTARGET=" + m.group(1).replaceAll("&#39;", "")
												+ "&__EVENTARGUMENT=" + m.group(2).replaceAll("&#39;", "") + "&seq=" + seq +  "\">NEXT</a>";
									}
								}
								break;
							
							} else {
								nodeCurrentPage = nextCol.getFirstChild();
								if (nodeCurrentPage != null && nodeCurrentPage.toHtml().contains("<span>") && i == noOfCols-2) {
									//this is the last page, so no NEXT link should be added
									linkN = "<a style=\"color:grey; text-decoration: none;\">NEXT</a>";
									paramsPostBack = ((LinkTag) currentCol.getFirstChild()).extractLink().trim().replaceAll("\\s", "%20");
									if (paramsPostBack != null) {
										m = Pattern.compile(regExp).matcher(paramsPostBack);
										if (m.find()) {
											linkP = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + url + "?__EVENTTARGET=" + m.group(1).replaceAll("&#39;", "")
													+ "&__EVENTARGUMENT=" + m.group(2).replaceAll("&#39;", "") + "&seq=" + seq +  "\">PREV</a>";
										}
									}
									break;
								}
							}
						}
					}
				}
			}
		}
		
		if (StringUtils.isNotEmpty(linkP) && StringUtils.isNotEmpty(linkN)) {
			links = linkP + "&nbsp; &nbsp;" + linkN + "&nbsp; &nbsp;";
			response.getParsedResponse().setNextLink(linkN);
			
		}
		
		footer.append(links);
		footer.append(" </td> </tr>");
		return footer;
	}
	
	
	public StringBuilder getNextDetailPages (String lastURL, String response, HtmlParser3 htmlParser, String sourceTag, String eventTargetVal, String eventArgVal) {
		StringBuilder nextDetails = new StringBuilder("");
		Map<String, String> paramsToUse = new HashMap<String, String>();
		String nextInfo = "";
		
		NodeList list =  htmlParser.getNodeList().extractAllNodesThatMatch(new HasAttributeFilter("id", sourceTag), true);
		
		if (list != null) {
			paramsToUse = htmlParser.getListOfPostParams(sourceTag);
			paramsToUse.remove("__EVENTTARGET");
			paramsToUse.remove("__EVENTARGUMENT");
		
		} else {
			paramsToUse = extractHiddenParams(response);
		}
		
		paramsToUse.put("__EVENTTARGET", eventTargetVal);
		paramsToUse.put("__EVENTARGUMENT", eventArgVal);
		int seq = getSeq();
		mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, paramsToUse);
		HttpSite3 site = HttpManager3.getSite(getCurrentServerName(), searchId);
		String url = lastURL;
		
		try {
			nextInfo = ((ro.cst.tsearch.connection.http3.OHCuyahogaPR)site).getNextDetailsPage(url, paramsToUse);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager3.releaseSite(site);
		}

		nextDetails.append(nextInfo);
		return nextDetails;
	}
	
	
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		final String DETAILS_PATT = "(?is)<tr.*<a[^<]+</a>\\s*</td>\\s*<td[^>]*>([^<]+)</td>.*";
		final String LINK_PATT = "(?is).*<a.*href=\\\"([^\\\"]+)\\\"[^>]*>([^<]*)</a>.*";
		
		StringBuilder header = new StringBuilder("<table border=\"1\" cellspacing=\"0\" cellpadding=\"5\">\n <tr> <th>Case Number</th> <th>Name</th> <th>Address</th> <th>Role</th> <th>Alias</th> </tr>");
		StringBuilder footer = new StringBuilder();

		String tmpLnk = "";
		
		try {						
			Map<String, String> params = new HashMap<String, String>();			 			
			HttpSite3 site = HttpManager3.getSite(getCurrentServerName(), searchId);
			if (site != null) {
				try {
					params = HttpSite3.fillConnectionParams(table,
							((ro.cst.tsearch.connection.http3.OHCuyahogaPR) site).getTargetArgumentParameters(), "form1");
				} finally {
					// always release the HttpSite
					HttpManager3.releaseSite(site);
				}
			}

			HtmlParser3 htmlParser = new HtmlParser3(table);
			NodeList nodeList = htmlParser.getNodeList();
			
			FormTag form = (FormTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "form1")).elementAt(0);
			form.setAttribute("name", "\"form1\"");
			String action = form.getFormLocation();	
			
			
			nodeList = htmlParser.getNodeList()
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "mpContentPH_gvSearchResults"), true);
						
			if(nodeList.size() == 0) {
				return intermediaryResponse;
			}
			
			LinkedHashMap<String, List<TableRow>> rowsMap = new  LinkedHashMap<String, List<TableRow>>();
			
			TableTag tableTag = (TableTag) nodeList.elementAt(0);
			tableTag.removeAttribute("style");
			tableTag.setAttribute("border", "1");
			
			TableRow[] rows  = tableTag.getRows();
			if (rows.length > 1) {
				for (int i = 1; i < rows.length; i++) {
					TableRow row = rows[i];
					if(row.getColumnCount() == 5) {
						//interm results
						int seq = getSeq();
						HashMap<String,String> paramsOfReq = htmlParser.getListOfPostParams("form1");
						
						LinkTag linkTag = ((LinkTag)row.getColumns()[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0));
						tmpLnk = linkTag.extractLink().trim();					
						if (tmpLnk.contains("&#39;"))
							tmpLnk = tmpLnk.replaceAll("(?is)[^&]+&#39;([^&]+)[^\\)]+\\)", "$1");
						else if (tmpLnk.contains("&#39;"))
							tmpLnk = tmpLnk.replaceAll("(?is)[^']+'([^']+)[^\\)]+\\)", "$1");
						
						if (paramsOfReq != null) {
							params.putAll(paramsOfReq);
							params.remove("__EVENTTARGET");
//							params.put("__EVENTTARGET", tmpLnk);
						}
						
						mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
						
						String link = CreatePartialLink(TSConnectionURL.idPOST) + "/pa/" + action + "?seq=" + seq + "&__EVENTTARGET=" + tmpLnk;
						linkTag.setLink(link);
						
						//group the rows which have the same caseNo
						String htmlRow = row.toHtml();
						Matcher matcher = Pattern.compile(DETAILS_PATT).matcher(htmlRow);
						if (matcher.find()) {
							String key = matcher.group(1);
							if (rowsMap.containsKey(key)) {	//caseNo already exists
								List<TableRow> value = rowsMap.get(key);
								value.add(row);
							} else {						//add new caseNo
								List<TableRow> value = new ArrayList<TableRow>();
								value.add(row);
								rowsMap.put(key, value);
							}
						}
					
					} else if (row.getColumnCount() == 1) {
						//paging links
						footer = getPrevAndNextLinks(response, htmlParser, "form1");
					}
				}
			}
			
			Iterator<Entry<String, List<TableRow>>> it = rowsMap.entrySet().iterator();
			StringBuilder sbIntermRes = new StringBuilder();
			
			while (it.hasNext()) {
				ParsedResponse currentResponse = new ParsedResponse();	
				String link = "";
				Map.Entry<String, List<TableRow>> pair = (Map.Entry<String, List<TableRow>>)it.next();
				List<TableRow> value = pair.getValue();
							
				StringBuilder sb = new StringBuilder();
				String caseNo = "";
				String names = "";
				String addresses = "";
				String roles = "";
				String aliases = "";
				boolean multipleRowInfo = false;
				String separator = "@@@";

				if (value != null) {
					if (value.size() > 1)
						multipleRowInfo = true;
					for (int ii=0; ii < value.size(); ii++) {
						if (ii == 0)
							caseNo = value.get(ii).getColumns()[1].getChildrenHTML().replaceAll("&nbsp;", " ").trim();
						names += value.get(ii).getColumns()[0].getChildrenHTML().replaceAll("&nbsp;", " ").trim();
						addresses += value.get(ii).getColumns()[2].getChildrenHTML().replaceAll("&nbsp;", " ").trim();
						roles += value.get(ii).getColumns()[3].getChildrenHTML().replaceAll("&nbsp;", " ").trim();
						aliases += value.get(ii).getColumns()[4].getChildrenHTML().replaceAll("&nbsp;", " ").trim();
							
						Matcher ma = Pattern.compile(LINK_PATT).matcher(names);
						if (ma.find()) {
							if (ii == 0)
								link = ma.group(1);
							if (names.contains("@@@<a")) {
								names = names.replaceFirst("(?is)([^<]+)<a[^<]+</a>", "$1");
								names += ma.group(2); 
							} else {
								names = ma.group(2);
							}
							
						}
							
						if (multipleRowInfo) {
							names += separator;
							addresses += separator;
							roles += separator;
							aliases += separator;
						}
					}
					if (multipleRowInfo) {	
						names = names.replaceAll("@{3}", "<br/>").trim();
						names = names.replaceAll("@{3}$", "").trim();
						addresses = addresses.replaceAll("@{3}", "<br/>").trim();
						addresses = addresses.replaceAll("@{3}$", "").trim();
						roles = roles.replaceAll("@{3}", "<br/>").trim();
						roles = roles.replaceAll("@{3}$", "").trim();
						aliases = aliases.replaceAll("@{3}", "<br/>").trim();
						aliases = aliases.replaceAll("@{3}$", "").trim();
					}
					
					sb.append("<tr> <td valign=\"center\"> <a href=\"" + link + "\">" + caseNo + "</a> </td> ");
					sb.append("<td> " +  names + "</td> ");
					sb.append("<td> " + addresses + "</td> ");
					sb.append("<td> " + roles + "</td> ");
					sb.append("<td> " + aliases + "</td> ");
					sb.append("</tr> ");
						
					String htmlRow = sb.toString();
						
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, htmlRow);
					currentResponse.setOnlyResponse(htmlRow);
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
						
					ResultMap map = parseIntermediaryRow(htmlRow, searchId);
					Bridge bridge = new Bridge(currentResponse, map, searchId);
						
					DocumentI document = bridge.importData();				
					currentResponse.setDocument(document);
							
					intermediaryResponse.add(currentResponse);
					
					sbIntermRes.append(htmlRow);
				}
			}
		
			response.getParsedResponse().setHeader(header.toString());			
			response.getParsedResponse().setFooter(footer + "</table>");
			
			outputTable.append(sbIntermRes.toString());
						
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}
	
	
//	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
	public static ResultMap parseIntermediaryRow(String rowInfo, long searchId) {
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "PR");
		
		HtmlParser3 htmlParser = new HtmlParser3(rowInfo);
		TableRow row = (TableRow) htmlParser.getNodeList().elementAt(0); 
		TableColumn[] cols = row.getColumns();
		if(cols.length == 5) {					
			String caseNo = cols[0].toPlainTextString().trim();
			String name = cols[1].getChildrenHTML().trim().replaceFirst("(<br/>){1,}$", "");
			//String address = cols[2].getChildrenHTML().trim().replaceFirst("(<br/>){1,3}$", "");
			String role = cols[3].getChildrenHTML().trim().replaceFirst("(<br/>){1,}$", "");
			String alias = cols[4].getChildrenHTML().trim().replaceFirst("(<br/>){1,}$", "");
			
//			if (StringUtils.isNotEmpty(address)) {
//				address = address.replaceAll("[\\.,]"," ").trim();
//				resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
//				resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
//				resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
//			}
			
			if (StringUtils.isNotEmpty(caseNo)) {
				Matcher m = Pattern.compile("(\\d{4})([A-Z]+)(\\d+)").matcher(caseNo);
				if (m.find()) {
					resultMap.put(CourtDocumentIdentificationSetKey.FILLING_DATE.getKeyName(), m.group(1).trim());
					resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(),  m.group(1).trim());
					resultMap.put(CourtDocumentIdentificationSetKey.CASE_TYPE.getKeyName(), m.group(2).trim());
					resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), m.group(2).trim());
					resultMap.put(CourtDocumentIdentificationSetKey.CASE_NUMBER.getKeyName(), m.group(3).trim());
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), m.group(3).trim());
				}
				
			}
					
			role = role.replaceAll("&nbsp;", "");
			if (StringUtils.isNotEmpty(role)) {
				resultMap.put(CourtDocumentIdentificationSetKey.PARTY_TYPE.getKeyName(), role.replaceAll("<br/>", " & "));
			}
			
			alias = alias.replaceAll("&nbsp;", "");
			
			if (StringUtils.isNotEmpty(name)) {
				if (StringUtils.isNotEmpty(alias)) 
					name = name +"& " + alias.replaceAll("<br/>", " & ");
				resultMap.put(CourtDocumentIdentificationSetKey.PARTY_NAME.getKeyName(), name.replaceAll("<br/>", " & "));
				resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name.replaceAll("<br/>", " & "));
				resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), name.replaceAll("<br/>", " & "));
				try {
					parseNamesIntermediary (resultMap, searchId);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return resultMap;
	}
	
	
	@SuppressWarnings("rawtypes")
	public static void parseNamesIntermediary(ResultMap resultMap, long searchId) throws Exception {
		   List<List> body = new ArrayList<List>();
		   String owner = (String) resultMap.get(CourtDocumentIdentificationSetKey.PARTY_NAME.getKeyName());
		   
		   if (StringUtils.isEmpty(owner))
			   return;
		   
		   else {
			   	String[] allOwners = owner.split(" & ");
			   	for (int i=0; i<allOwners.length; i++) {
			   		String[] names = null;
					owner = allOwners[i].trim();
					names = StringFormats.parseNameNashville(owner);
							
					String[] types = GenericFunctions.extractAllNamesType(names);
					String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);
								
					GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
							NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
					
					try {
						GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
						
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
		   	}
	}
	
	
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		// 	no result returned - error message on official site
		if (rsResponse.contains("An unexpected error has occurred:")) {
			Response.getParsedResponse().setError("Official site not functional");
    		return;
    	} else if (rsResponse.contains("No results were found for the provided search criteria")) {
    		Response.getParsedResponse().setError("No results were found for the provided search criteria");
    		return;
    	}
		
		switch (viParseID) {			
			case ID_SEARCH_BY_NAME:		 //Search by Party 
			case ID_SEARCH_BY_MODULE19:  //Search by Case Number 
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
					data.put("type","COURT");
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
				ParseResponse(sAction, Response, rsResponse.contains("Search Results")
						? ID_SEARCH_BY_NAME
						: ID_DETAILS);
				break;
				
			default:
				break;
		}
	}	
	
	
	
	
	
	protected String getDetails(String rsResponse, StringBuilder accountId) {
//		final String MULTIPLEPAGES_PATT = "(?is).*<a.*href=\\\"javascript:__doPostBack\\(([^,]+),([^\\)]+)\\)[^>]*>([^<]*)</a>.*";
		boolean needsExtraReq = false;
		try {
			/* If from memory - use it as is */
			if(!rsResponse.contains("<html")){
				return rsResponse;
			
			} else {
				try {
					StringBuilder details = new StringBuilder();
					HtmlParser3 htmlParser = new HtmlParser3(rsResponse);
					String[] detPg = new String[6];
					DataSite site = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), getServerID());
					String serverHomeLink = site.getServerHomeLink();
					
					NodeList nodeList = htmlParser.getNodeList();
					NodeList linksToFollow = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "tabList"), true);
					
					if(nodeList.size() == 0) {
						return rsResponse;
					
					} else {
						if (linksToFollow.size() > 0) {
							linksToFollow = linksToFollow.elementAt(0).getChildren().extractAllNodesThatMatch(new HasAttributeFilter("class", "mpLI"), true);
							if (linksToFollow.size() == 11) {
								// get link to Costs page
								detPg[0] = serverHomeLink + "pa/" +  ((LinkTag)linksToFollow.elementAt(4).getChildren().elementAt(0)).extractLink().trim();
								//get link to Docket page
								detPg[1] = serverHomeLink + "pa/" +  ((LinkTag)linksToFollow.elementAt(5).getChildren().elementAt(0)).extractLink().trim();
								//get link to Events page
								detPg[2] = serverHomeLink + "pa/" +  ((LinkTag)linksToFollow.elementAt(6).getChildren().elementAt(0)).extractLink().trim();
								//get link to Parties page
								detPg[3] = serverHomeLink + "pa/" +  ((LinkTag)linksToFollow.elementAt(7).getChildren().elementAt(0)).extractLink().trim();
								//get link to Requirements page
								detPg[4] = serverHomeLink + "pa/" +  ((LinkTag)linksToFollow.elementAt(8).getChildren().elementAt(0)).extractLink().trim();
								//get link to Service page
								detPg[5] = serverHomeLink + "pa/" +  ((LinkTag)linksToFollow.elementAt(9).getChildren().elementAt(0)).extractLink().trim();
							}
						}
						
						
						NodeList detailsList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("class", "postBODY"), true);  // get table with details info info

						if (detailsList.size() != 0) {
							details.append("<div id=\"detailsInfo\"> <b> <span style=\"font-size:14px; color: darkblue;\"> CASE SUMMARY </span> </b> <br/>");
							
							TableTag caseSummary = (TableTag) detailsList.elementAt(0).getChildren()
									.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
							if (caseSummary != null) {
								caseSummary.setAttribute("id", "\"caseSummary\"");
								caseSummary.removeAttribute("cellpadding");
								caseSummary.removeAttribute("style");
								
								String apn =  caseSummary.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("id", "mpContentPH_txtCaseNum"), true)
										.elementAt(0).getFirstChild().toHtml().trim();
								
								if (StringUtils.isNotEmpty(apn)) {
									accountId.append(apn);		
								}
								String htmlTable = caseSummary.toHtml().replaceAll("(?is)<a[^>]*>\\s*[^<]+</a>", "").replaceAll("Printer Friendly Version\\s*","");
								htmlTable = htmlTable.replaceFirst("(?is)<td.*rowspan=\"12\"[^>]+>\\s*</td>", "");
								htmlTable = htmlTable.replaceFirst("(?is)<td(?:\\s*width=\\\"20%\\\")?\\s+style=\\\"text-align: right;\\\"(\\s*width=\\\"20%\\\")?\\s*>\\s*</td>", "");
								htmlTable = htmlTable.replaceAll(" style=\"text-align:left;\"","");
								details.append(htmlTable);
							}
							
							details.append("  <br/>");
						}
						
						for (int i=0; i<6; i++) {
							String headerText = "<b> <span style=\"font-size:14px; color: darkblue;\"> ";
							switch(i) {
								case 0: 
									headerText = headerText + "COSTS";
									break;
								case 1: 
									headerText = headerText + "DOCKET";
									break;
								case 2: 
									headerText = headerText + "EVENTS";
									break;
								case 3: 
									headerText = headerText + "PARTIES";
									break;
								case 4: 
									headerText = headerText + "REQUIREMENTS";
									break;
								case 5: 
									headerText = headerText + "SERVICE";
									break;
							}
							
							headerText = headerText + " </span> </b> <br/>";
							
							details.append(headerText);
							
							String rsp = getLinkContents(detPg[i]);
							String htmlInfo = "";
							if (StringUtils.isNotEmpty(rsp)) {
								rsp = rsp.replaceAll("(width:)\\s*(\\d+)\\s*((?:%|px)\\s*;)", "$1" + "$2" + "$3");
								htmlParser = new HtmlParser3(rsp);
								nodeList = htmlParser.getNodeList();
								nodeList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "postBODY"), true)
										.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true);
													
								if (nodeList.size() != 0) {
									details.append("<div id=\"detailsInfo" + (i+1) + "\" style=\"border-width: 1px; border-style: solid; width:1000px;\">");
									
									TableTag table = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("style", "width:90%;")).elementAt(0);
									if (table != null) {
										if (table.getAttribute("style") != null)
											table.removeAttribute("style");
										if (table.getAttribute("cellpadding") != null)
											table.removeAttribute("cellpadding");
										htmlInfo = table.toHtml();
										htmlInfo = htmlInfo.replaceFirst("(?is)<td[^>]+>\\s*<a[^>]*>\\s*Printer Friendly Version\\s*</a>\\s*</td>", "");
										htmlInfo = htmlInfo.replaceAll("(?is)<a[^>]*>\\s*([^<]+)</a>", "$1");
										htmlInfo = htmlInfo.replaceFirst("(?is)<td.*rowspan=\"8\"[^>]*>\\s*</td>", "");
										details.append(htmlInfo);
									}
									
									table = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "gridview")).elementAt(0);	
									if (table != null) {
										if (table.getAttribute("style") != null)
											table.removeAttribute("style");
										if (table.getAttribute("cellspacing") != null)
											table.removeAttribute("cellspacing");
										
										String evTarget = "";
										String evArgument = "";
										int noOfPg = 0;
										if (table.toHtml().contains(",&#39;Page$")) {
											needsExtraReq = true;
											TableTag pgTable = (TableTag) table.getRow(table.getRowCount()-1).getColumns()[0].getFirstChild();
											if (pgTable != null && pgTable.getRowCount() == 1) {
												noOfPg = pgTable.getRows()[0].getColumnCount();
												if (noOfPg > 1) {
													TableColumn col = pgTable.getRow(0).getColumns()[1];
													if (col != null) {
														String link = col.getFirstChild().toHtml();
														Matcher tmpMtch = Pattern.compile("(?is).*<a.*href=\\\"javascript:__doPostBack\\(([^,]+),([^\\)]+)\\)[^>]*>([^<]*)</a>.*").matcher(link);
														if (tmpMtch.find()) {
															evTarget = tmpMtch.group(1).replaceAll("(?is)&#39;", "");
															evArgument = tmpMtch.group(2).replaceAll("(?is)&#39;", "");
														}
													}
												}
											}
											
											table = cleanTable(table, false, true);
										}
										htmlInfo = table.toHtml().replaceAll("(?is)<td[^>]+>\\s*<a[^>]*>Printer Friendly Version\\s*</a>\\s*</td>", "");
										htmlInfo = htmlInfo.replaceAll("(?is)<a[^>]*>\\s*([^<]+)</a>", "$1");
										htmlInfo = htmlInfo.replaceAll("(?is)<a[^>]*>\\s*<img[^>]*>[A-Z\\s]*(?:</img>\\s*)?</a>", "");
										htmlInfo = htmlInfo.replaceFirst("(?is)<span[^>]*>(?:\\s*<\\s*/?\\s*br\\s*/?\\s*>)*\\s*(No case [A-Z]+\\s*(?:were|was)? found for the selected case)(?:\\s*<\\s*/?\\s*br\\s*/?\\s*>)*\\s*</span>", 
												" <b> <i> <span style=\"font-size:13px;\"> " + "$1" + "<br/> <br/> </span> </i> </b>");
										
										details.append(htmlInfo);
										
										if (needsExtraReq) {
											String rspToUse = rsResponse; 
											HtmlParser3 parserToUse = htmlParser;
											for (int idx=1; idx <= noOfPg-1; idx++) {
												evArgument = evArgument.substring(0, evArgument.length()-1) + (idx+1);
												rspToUse = getNextDetailPages(detPg[i], rspToUse, parserToUse, "form1", evTarget, evArgument).toString();
												parserToUse = new HtmlParser3(rspToUse);
												table = (TableTag) parserToUse.getNodeList()
														.extractAllNodesThatMatch(new HasAttributeFilter("class", "gridview"), true).elementAt(0);
												if (table != null) {
													table = cleanTable(table, true, true);
													String tableContent = table.toHtml();
													tableContent = tableContent.replaceFirst("<TABLE>", "");
													tableContent = tableContent.replaceAll("(?is)<a[^>]*>\\s*<img[^>]*>[A-Z\\s]*(?:</img>\\s*)?</a>", "");
													details.append(tableContent);
												}
											}
											details.append(" </table> <br/>");
										} 
										
										details.append(" </div>");
										
									} else {
										if (nodeList.size() >= 5) {
											details.append("<div style=\"border-width: 1px; border-style: solid; width:500px;\">");
											for (int ii = 1; ii < nodeList.size(); ii++) {
												table = (TableTag) nodeList.elementAt(ii);	
												if (table != null) {
													if (table.getAttribute("id") == null) {
														table.setAttribute("id", "\"party" + ii + "\"");
													}
													if (table.getAttribute("cellspacing") != null)
														table.removeAttribute("cellspacing");
													String info = table.getRow(0).getColumns()[0].getFirstChild().getFirstChild().toHtml();
													if (info.equals("&nbsp;") || StringUtils.isEmpty(info.trim())) {
														ii = nodeList.size();
													} else {
														htmlInfo = table.toHtml();
														htmlInfo = htmlInfo.replaceAll("(?is)<a[^>]*>\\s*([^<]+)</a>", "$1");
														htmlInfo = htmlInfo.replaceFirst("(?is)rowspan=\"5\"", "");
														details.append(htmlInfo);
													}
												}
											}
											details.append(" <br/> </div>");
										}
										details.append(" </div>");
									}
									
									
								}
								details.append("<br/>");
							}
						}
						details.append(" <br/> </br>  </div>");
						
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

	
	public TableTag cleanTable(TableTag originalTable, boolean withoutFirstRow, boolean withoutLastRow) {
		TableTag tmpTbl = new TableTag();
		NodeList children = new NodeList();
		int noOfRows = originalTable.getRowCount()-1;
		int idxStart = 0;
		int idxFinish = noOfRows;
		
		if (withoutFirstRow)
			idxStart ++;
		if (withoutLastRow)
			idxFinish --;
			
		for (int i=idxStart; i <= idxFinish; i++) {
			children.add(originalTable.getRow(i));
		}
		tmpTbl.setChildren(children);
		
		return tmpTbl;
	}
	
	public static void parseNames(ResultMap m, long searchId) throws Exception {
		String grantor = (String) m.get(SaleDataSetKey.GRANTOR.getKeyName());
		String grantee = (String) m.get(SaleDataSetKey.GRANTEE.getKeyName());
	
		grantor = ro.cst.tsearch.utils.StringUtils.prepareStringForHTML(grantor);
		grantee = ro.cst.tsearch.utils.StringUtils.prepareStringForHTML(grantee);
		grantor = NameCleaner.cleanFreeformName(grantor);
		grantee = NameCleaner.cleanFreeformName(grantee);
		
		ArrayList<List> grantorList = new ArrayList<List>();
		ArrayList<List> granteeList = new ArrayList<List>();
		
		parseNameInner(m, grantor, grantorList, searchId, false);
		parseNameInner(m, grantee, granteeList, searchId, true);
				
		m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantorList, true));
		m.put("GranteeSet", GenericFunctions.storeOwnerInSet(granteeList, true));
	}
	
	public static void parseNameInner(ResultMap m, String name, ArrayList<List> namesList, long searchId, boolean isGrantee) {
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes, type, otherType;
		
		String[] nameItems = name.split(" & ");
		for (int i = 0; i < nameItems.length; i++){
			names = StringFormats.parseNameNashville(nameItems[i], true);
						
			type = GenericFunctions.extractNameType(names);
			otherType = GenericFunctions.extractNameOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(nameItems[i], names, suffixes[0], suffixes[1], type, otherType, 
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), namesList);
		}
	}
	
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse rsResponse, String detailsHtml, ResultMap resultMap) {
		try {	
			String caseNo = "";			
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"PR");
			
			HtmlParser3 htmlParser = new HtmlParser3(detailsHtml);
			NodeList list = htmlParser.getNodeList();		
			TableTag table = (TableTag) list.extractAllNodesThatMatch
					(new HasAttributeFilter("id","caseSummary"), true).elementAt(0);
			if (table != null) {
				caseNo = table.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("id", "mpContentPH_txtCaseNum"), true)
						.elementAt(0).getFirstChild().toHtml().trim();
				if (StringUtils.isNotEmpty(caseNo)) {
					Matcher m = Pattern.compile("(\\d{4})([A-Z]+)(\\d+)").matcher(caseNo);
					if (m.find()) {
						resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), getDocTypeMapping(m.group(2).trim()));
						resultMap.put(CourtDocumentIdentificationSetKey.CASE_NUMBER.getKeyName(), m.group(3).trim());
						resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), caseNo);
					}
				}
				
				String caseType = table.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("id", "mpContentPH_txtCaseType"), true)
						.elementAt(0).getFirstChild().toHtml().trim();
				if (StringUtils.isNotEmpty(caseType)) {
					resultMap.put(CourtDocumentIdentificationSetKey.CASE_TYPE.getKeyName(), caseType);
//					resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), caseType);
				}
				
				String fillingDate = table.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("id", "mpContentPH_txtCaseInit"), true)
						.elementAt(0).getFirstChild().toHtml().trim();
				if (StringUtils.isNotEmpty(fillingDate))
				{
					fillingDate = fillingDate.replaceFirst("(?is)[A-Z]+\\s*,\\s*([^,]+,\\s*\\d{4})", "$1");
					SimpleDateFormat sdf = new SimpleDateFormat ("MM/dd/yyyy");
					fillingDate = sdf.format(new Date(fillingDate));

					resultMap.put(CourtDocumentIdentificationSetKey.FILLING_DATE.getKeyName(), fillingDate);
					resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(),  fillingDate);
				}
				
				NodeList parties = list.extractAllNodesThatMatch(new HasAttributeFilter("id","detailsInfo4"), true).elementAt(0)
						.getChildren().extractAllNodesThatMatch(new TagNameFilter("div"), true).elementAt(0)
						.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true);
				if (parties != null) {
					List<String> gtorsRoles = Arrays.asList("NATURAL FATHER", "NATURAL PARENT", "GUARDIAN", "OLD NAME", "NEW NAME", "GROOM", "BRIDE", "DECEDENT", "ALIAS", "DEPOSITOR");
					List<String> gteesRoles = Arrays.asList("FIDUCIARY", "WARD");
					List<String> ignoredRoles = Arrays.asList("ATTORNEY", "X ATTORNEYS", "WARD", "UNKNOWN");
					
					String gtors = "";
					String gtees = "";
					boolean isGtee = false;
					boolean isGtor = false;
					String role = "";
					for (int i=0; i < parties.size(); i++) {
						TableTag tmpTable = (TableTag) parties.elementAt(i);
						if (tmpTable != null) {
							if (tmpTable.getRowCount() >= 2) {
								//row1: col1=party role, col2=party name
								TableRow row = tmpTable.getRow(0);
								if (row.getColumnCount() >= 2) {
									role = row.getColumns()[0].getFirstChild().getFirstChild().toHtml().trim();
									if (!ignoredRoles.contains(role)) {
										String name = row.getColumns()[1].getFirstChild().getFirstChild().toHtml().trim() + " & ";
										name = name.replaceAll("(?is)\\(DOD\\s*:\\s*\\d{2}/\\d{2}/\\d{4}\\s*\\)", "").trim();
										if (gtorsRoles.contains(role)) {
											gtors += name + " & ";
											isGtor = true;
										
										} else if (gteesRoles.contains(role)) {
											gtees += name + " & ";
											isGtee = true;
										}
									}
								}
								//row2: col1=alias, col2 (if any)=alias name
								row = tmpTable.getRow(1);
								if (row.getColumnCount() >= 2) {
									String alias = row.getColumns()[1].getFirstChild().getFirstChild().toHtml().trim();
									alias = alias.replaceFirst("(?is)\\b(AKA|FKA|A\\.K\\.A\\.?|F\\.K\\.A\\.?|A/K/A|F/K/A)\\b\\s*", "");
									if (isGtor)
										gtors += alias + " & ";
									else if (isGtee)
										gtees += alias + " & ";
								}
							}
						}
					}
					
					if (StringUtils.isNotEmpty(gtors)) 
//						resultMap.put(CourtDocumentIdentificationSetKey.PARTY_NAME.getKeyName(), names);
						resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), gtors);
						resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), gtees);
						parseNames(resultMap, searchId);
						resultMap.get("PartyNameSet");
				}
			}
		
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		
		return null;	
	}
	
	
	private Object getDocTypeMapping(String code) {
		String rightDocTypeCode = "";
		
		if ("ADV".equals(code.trim()))
			rightDocTypeCode = code + "-ADVERSARIAL";
		if ("EST".equals(code.trim()))
			rightDocTypeCode = code + "-ESTATE";
		if ("GRD".equals(code.trim()))
			rightDocTypeCode = code + "-GUARDIANSHIP";
		if ("ML".equals(code.trim()))
			rightDocTypeCode = code + "-MARRIAGE LICENSE";
		if ("MSC".equals(code.trim()))
			rightDocTypeCode = code + "-CIVIL/MISCELLANEOUS";
		if ("TRS".equals(code.trim()))
			rightDocTypeCode = code + "-TRUSTEESHIP";
		if ("WIL".equals(code.trim()))
			rightDocTypeCode = code + "-WILL";		
			
		return rightDocTypeCode;
	}

	@Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.CASE_NAME_MODULE_IDX);
		if (tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(2), getALL_CASE_CATEGORIES());
		}

		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
		if (tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(4), getALL_PARTY_ROLES());
			setupSelectBox(tsServerInfoModule.getFunction(6), getALL_CASE_CATEGORIES());
		}
		return msiServerInfoDefault;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
			
//		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.CASE_NAME_MODULE_IDX)); 
//		module.clearSaKeys();
//		moduleList.add(module);
		
		if (hasOwner()) {
			{
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX)); 
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				module.forceValue(0, "C");
//				ConfigurableNameIterator iteratorC = (ConfigurableNameIterator) ModuleStatesIteratorFactory
//						 .getConfigurableNameIterator(module, searchId, new String[] {"L;F;"});
				ConfigurableNameIterator iteratorC = new ConfigurableNameIterator(searchId, false, new String[]{"L;F;"});
				module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
				iteratorC.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.COMPANY_NAME);
				iteratorC.setInitAgain(true);
				module.addIterator(iteratorC);
				
				GenericNameFilter nameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
				module.addFilter(nameFilter);
				moduleList.add(module);	
			}
			{
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX)); 
				module.clearSaKeys();
				module.forceValue(0, "P");
				module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
//				ConfigurableNameIterator iteratorP = (ConfigurableNameIterator) ModuleStatesIteratorFactory
//						 .getConfigurableNameIterator(module, searchId, new String[] {"L;F;"});
				ConfigurableNameIterator iteratorP = new ConfigurableNameIterator(searchId, false, new String[]{"L;F;"});
				iteratorP.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
				iteratorP.setInitAgain(true);
				
				module.addIterator(iteratorP);
				
				GenericNameFilter nameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
				module.addFilter(nameFilter);
				moduleList.add(module);	
			}
		}
		
		if (hasBuyer()) {
			{
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX)); 
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.BUYER_OBJECT);
				module.forceValue(0, "C");
//				ConfigurableNameIterator iteratorC = (ConfigurableNameIterator) ModuleStatesIteratorFactory
//						 .getConfigurableNameIterator(module, searchId, new String[] {"L;F;"});
				ConfigurableNameIterator iteratorC = new ConfigurableNameIterator(searchId, false, new String[]{"L;F;"});
				module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
				iteratorC.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.COMPANY_NAME);
				iteratorC.setInitAgain(true);
				module.addIterator(iteratorC);
				
				GenericNameFilter buyerFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(SearchAttributes.BUYER_OBJECT, searchId, module);
				module.addFilter(buyerFilter);
				moduleList.add(module);
			}
			{
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.clearSaKeys();
				module.forceValue(0, "P");
				module.setSaObjKey(SearchAttributes.BUYER_OBJECT);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
//				ConfigurableNameIterator iteratorP = (ConfigurableNameIterator) ModuleStatesIteratorFactory
//						 .getConfigurableNameIterator(module, searchId, new String[] {"L;F;"});
				ConfigurableNameIterator iteratorP = new ConfigurableNameIterator(searchId, false, new String[]{"L;F;"});
				iteratorP.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
				iteratorP.setInitAgain(true);
				module.addIterator(iteratorP);
				
				GenericNameFilter buyerFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(SearchAttributes.BUYER_OBJECT, searchId, module);
				module.addFilter(buyerFilter);
				moduleList.add(module);	
			}
		}
		
		serverInfo.setModulesForAutoSearch(moduleList);
	}
	
}
