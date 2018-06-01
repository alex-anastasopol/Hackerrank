package ro.cst.tsearch.servers.types;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.ScriptTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.bean.recoverdocument.ModuleShortDescription;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http3.HttpManager3;
import ro.cst.tsearch.connection.http3.HttpSite3;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.log.SearchLogFactory;
import ro.cst.tsearch.log.SearchLogPage;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.ImageTransformation;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.ARWashingtonRO.LegalDescriptionIterator;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.threads.GPMaster;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.SearchLogger;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShip;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;


public class OHDelawareRO extends TSServerROLike {

	private static final long serialVersionUID = 4940198725194257741L;

	private static String PROP_TYPES_ALL_CITIES = "";
	private static String PROP_TYPES_ALL_SUBDIVISIONS = "";
	private static String PROP_TYPES_ALL_TOWNSHIPS = "";
	private static String ALL_DOCTYPES = "";
	private static String ALL_KINDS_DEFAULT = "";
	private static String ALL_KINDS_UCC = "";
	private static String ALL_KINDS_OFF = "";
	private static String ALL_KINDS_OFF_AND_UCC = "";
	
	static {
		String folderPath = ServerConfig.getModuleDescriptionFolder(BaseServlet.REAL_PATH + "WEB-INF/classes/resource/module/comments");
		File folder = new File(folderPath);
		if (!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath	+ "] does not exist. Module Information not loaded!");
		}
		try {
			PROP_TYPES_ALL_CITIES = FileUtils.readFileToString(new File(folderPath	+ File.separator + "OHDelawareROPropTypesAllCities.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			PROP_TYPES_ALL_SUBDIVISIONS = FileUtils.readFileToString(new File(folderPath	+ File.separator + "OHDelawareROPropTypesAllSubdivisions.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			PROP_TYPES_ALL_TOWNSHIPS = FileUtils.readFileToString(new File(folderPath	+ File.separator + "OHDelawareROPropTypesAllTownships.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			ALL_DOCTYPES = FileUtils.readFileToString(new File(folderPath	+ File.separator + "OHDelawareROAllDoctypes.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			ALL_KINDS_DEFAULT = FileUtils.readFileToString(new File(folderPath	+ File.separator + "OHDelawareROAllKindsBothTypesUnchecked.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			ALL_KINDS_UCC = FileUtils.readFileToString(new File(folderPath	+ File.separator + "OHDelawareROAllKindsUCC.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			ALL_KINDS_OFF = FileUtils.readFileToString(new File(folderPath	+ File.separator + "OHDelawareROAllKindsOFF.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			ALL_KINDS_OFF_AND_UCC = FileUtils.readFileToString(new File(folderPath	+ File.separator + "OHDelawareROAllKindsOFFandUCC.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	private HashMap<String,String> getCities() {
		HashMap<String,String> allCities = new HashMap<String, String>();
		String text = PROP_TYPES_ALL_CITIES;
//		text = text.replaceAll("(?is)</?select[^>]*>", "");
		text = text.replaceAll("&amp;", "&");
		Matcher m = Pattern.compile("<option value=\"([\\d\\s-A-Z&;]+)\">([^<]+)</option>").matcher(text);
		while (m.find()) {
			allCities.put(m.group(1), m.group(2)); // e.g: (2167, CENTER VILLAGE)
		}
		
		return allCities;
	}
	
	private HashMap<String,String> getSubdivisions() {
		HashMap<String,String> allSubdiv = new HashMap<String, String>();
		String text = PROP_TYPES_ALL_SUBDIVISIONS;
		text = text.replaceAll("&amp;", "&");
		Matcher m = Pattern.compile("<option value=\"([\\d\\s-A-Z&;]+)\">([^<]+)</option>").matcher(text);
		
		while (m.find()) {
			allSubdiv.put(m.group(1), m.group(2));
		}
		
		return allSubdiv;
	}


	private HashMap<String,String> getTownships() {
		HashMap<String,String> allTwnsp = new HashMap<String, String>();
		String text = PROP_TYPES_ALL_TOWNSHIPS;
		text = text.replaceAll("&amp;", "&");
		Matcher m = Pattern.compile("<option value=\"([\\d\\s-A-Z&;]+)\">([^<]+)</option>").matcher(text);
		
		while (m.find()) {
			allTwnsp.put(m.group(1), m.group(2));
		}
		
		return allTwnsp;
	}
	
	private HashMap<String,String> getDoctypes() {
		HashMap<String,String> allDoctypes = new HashMap<String, String>();
		String text = ALL_DOCTYPES;
		Matcher m = Pattern.compile("<option value=\"([^\"]+)\">([^<]+)</option>").matcher(text);
		
		while (m.find()) {
			allDoctypes.put(m.group(1), m.group(2));
		}
		
		return allDoctypes;
	}
	
	private String getDoctypeName(String doctypeCode) {
		HashMap<String,String> allDoctypes = getDoctypes();
		if (allDoctypes != null) {
			return allDoctypes.get(doctypeCode);
		}
		return null;
	}
	

	private HashMap<String,String> getKinds(boolean isOFFchecked, boolean isUCCchecked) {
		HashMap<String,String> allKinds = new HashMap<String, String>();
		String text = "";
		if (!isOFFchecked && !isUCCchecked) {
			text = ALL_KINDS_DEFAULT;
		} else if (isOFFchecked && isUCCchecked) {
			text = ALL_KINDS_OFF_AND_UCC;
		} else if (isOFFchecked && !isUCCchecked) {
			text = ALL_KINDS_OFF;
		} else if (!isOFFchecked && isUCCchecked) {
			text = ALL_KINDS_UCC;
		}
		
		Matcher m = Pattern.compile("(?is)<option value=\"([\\d\\s-A-Z\\$]+)\">([^<]+)</option>").matcher(text);
		
		while (m.find()) {
			allKinds.put(m.group(1), m.group(2));
		}
		
		return allKinds;
	}
	

	private String getKindName(String kindCode, boolean isOFFchecked, boolean isUCCchecked) {
		HashMap<String,String> allKinds = getKinds(isOFFchecked, isUCCchecked);
		if (allKinds != null) {
			return allKinds.get(kindCode);
		}
		return null;
	}
	
	public OHDelawareRO(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	public OHDelawareRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	
	private static int seq = 0;

	protected synchronized static int getSeq() {
		return seq++;
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
	
	
	public StringBuilder getPrevAndNextLinks (ServerResponse response, HtmlParser3 htmlParser, String classTable, Map<String, String> params) {
		String linkN = "";
		String linkP = "";
		String links = "";
		
		String url = response.getLastURI().toString();
		url = url.replaceFirst("//cotthosting.com", "");
		StringBuilder footer = new StringBuilder("");
		Map<String,String> paramsOfReq = new HashMap<String, String>();
		paramsOfReq.putAll(params);
		int seq = getSeq();
		

		// create links for Next/Prev buttons
		NodeList pagesTable = htmlParser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "cottPager"), false);
				
		if (pagesTable != null) {
			TableTag tableTag = null;
			if (pagesTable.size() == 4) {
				tableTag = (TableTag) pagesTable.elementAt(2);
			} else if (pagesTable.size() == 2) {
				tableTag = (TableTag) pagesTable.elementAt(0);
			}
			
			if (tableTag != null) {
				String noOfResPerPg = "";
				String idResPerPage = "ctl00$cphMain$tcMain$tpInstruments$ucInstrumentsGridV2$cpInstruments_Top$ddlResultsPerPage";
				String idCurrentPg  = "ctl00_cphMain_tcMain_tpInstruments_ucInstrumentsGridV2_cpInstruments_Top_txtResultsCurrentPage";
				String pgNoParam = "ctl00$cphMain$tcMain$tpInstruments$ucInstrumentsGridV2$cpInstruments_Top$txtResultsCurrentPage";
				String currentPageNo = "";
				String paramN = "ctl00$cphMain$tcMain$tpInstruments$ucInstrumentsGridV2$cpInstruments_Top$ibResultsNextPage";
				String paramP = "ctl00$cphMain$tcMain$tpInstruments$ucInstrumentsGridV2$cpInstruments_Top$ibResultsPreviousPage";
				
				NodeList tmpList = tableTag.getChildren().extractAllNodesThatMatch(new TagNameFilter("select"), true);
				
				if (tmpList != null) {
					tmpList = tmpList.elementAt(0).getChildren()
							.extractAllNodesThatMatch(new HasAttributeFilter("selected", "selected"), false);
					noOfResPerPg = tmpList.elementAt(0).getFirstChild().toHtml().trim();
					if (StringUtils.isNotEmpty(noOfResPerPg)) {
						paramsOfReq.put(idResPerPage, noOfResPerPg);
						//paramsOfReq.put("ctl00_cphMain_tcMain_ClientState","{\"ActiveTabIndex\":3,\"TabState\":[true,true,false,true,true]}");
					}
				}
				
//				String regExp = "(?is)Images/24/navigate_(right|left)\\.png\" onClick\\s*=\\s*\\\"javascript:WebForm_DoPostBackWithOptions[^&]+&quot;([^&]+)&quot;[^\\\"]+";
				mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, paramsOfReq);
				
				TableRow row = tableTag.getRow(0); //just one row
				if (row.getColumnCount() == 3) {
					TableColumn col = row.getColumns()[1];
					tmpList = col.getChildren().extractAllNodesThatMatch(new TagNameFilter("input"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("type", "image"), true);
					InputTag currentPg = (InputTag) col.getChildren().extractAllNodesThatMatch(new TagNameFilter("input"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("id", idCurrentPg), true).elementAt(0);
					if (currentPg != null) {
						currentPageNo = currentPg.getAttribute("value").trim();
					}
					
					if (tmpList.size() == 1 && tmpList.toHtml().contains("bullet_triangle_grey.png")) {
						// no Prev and Next buttons
					} else if (tmpList.size() > 1) {
						if (tmpList.toHtml().contains("navigate_right.png")) {
							linkN = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + url + 
									"?ctl00$smScriptMan=ctl00$cphMain$upMain|" + paramN + 
									"&" + pgNoParam + "=" + currentPageNo + "&seq=" + seq +
									"\"> Next </a>";
						}
						if (tmpList.toHtml().contains("navigate_left.png")) {
							linkP = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + url + 
									"?ctl00$smScriptMan=ctl00$cphMain$upMain|" + paramP + 
									"&" + pgNoParam + "=" + currentPageNo + "&seq=" + seq +
									"\"> Prev </a>";
						}
					}
				}	
			}
		}
		if (StringUtils.isNotEmpty(linkP) && StringUtils.isNotEmpty(linkN)) {
			links =  linkP + "&nbsp; &nbsp;" + linkN + "&nbsp; &nbsp;";
			response.getParsedResponse().setNextLink(linkN);
		}
		
		if (StringUtils.isNotEmpty(links)) {
			footer.append("<tr><td colspan=\"13\"> </br> &nbsp; &nbsp;");
			footer.append(links);
			footer.append(" </td> </tr>");
		}
		
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
	
	
	public String[] getHiddenParam(NodeList nodeList)  {
		String[] info = new String[] {"", ""};
		String hiddenVal = "";
		String hiddenKey = "";
		String url = "";
		
		if (nodeList!= null && nodeList.size() > 0) {
			for (int i=0; i< nodeList.size(); i++) {
				ScriptTag script = (ScriptTag) nodeList.elementAt(i);
				if (script.getAttribute("src") != null) {
					Pattern p = Pattern.compile("(?is)([^?]+)\\?_TSM_HiddenField_=([^&]+)[^_]+_TSM_CombinedScripts_=([^\"]+)");
					Matcher m = p.matcher(script.getAttribute("src"));
				
					if (script != null && m.find()) {
						hiddenKey = m.group(2);
						if ("ctl00_smScriptMan_HiddenField".equals(hiddenKey)) {
							url = m.group(1);
							hiddenVal = m.group(3);
							try {
								hiddenVal = URLDecoder.decode(hiddenVal, "UTF-8");
							} catch (UnsupportedEncodingException e) {							
								e.printStackTrace();
								logger.trace(e);
							}
							break;
						}
					}
				}
			}
		}
		
		info[0] = url;
		info[1] = hiddenVal;
		return info;
	}
	
	
	//for Subdivisions, Cities and Township list
	private static String SUBDIVISION_SELECT = "";
//	private static String CITY_SELECT = "";
//	private static String TOWNSHIP_SELECT = "";
	
	static {
		String folderPath = ServerConfig.getModuleDescriptionFolder(BaseServlet.REAL_PATH
				+ "WEB-INF/classes/resource/module/comments/");
		File folder = new File(folderPath);
		if (!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath + "] does not exist. Module Information not loaded!");
		}
		try {
			String select1 = org.apache.commons.io.FileUtils.readFileToString(new File(folderPath
					+ File.separator + "OHDelawareROPropTypesAllSubdivisions.xml"));
			SUBDIVISION_SELECT = select1;
		} catch (Exception e) {
			e.printStackTrace();
		}
//		try {
//			String select2 = org.apache.commons.io.FileUtils.readFileToString(new File(folderPath
//					+ File.separator + "OHDelawareROPropTypesAllCities.xml"));
//			CITY_SELECT = select2;
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		try {
//			String select3 = org.apache.commons.io.FileUtils.readFileToString(new File(folderPath
//					+ File.separator + "OHDelawareROPropTypesAllTownships.xml"));
//			TOWNSHIP_SELECT = select3;
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}
		
	private static List<String> SUBDIVISION_NAME_LIST = new ArrayList<String>();
	private static final String OPTION_PATTERN = "(?is)<option\\s+value=\"([^\"]+)\"\\s*>([^<]+)</option>";
	private static Map<String, String> SUBDIVISION_NAME_MAP = new HashMap<String, String>();
	static {
		Matcher matcher1 = Pattern.compile(OPTION_PATTERN).matcher(SUBDIVISION_SELECT);
		while (matcher1.find()) {
			String subdivision_name = matcher1.group(2);
			subdivision_name = subdivision_name.replaceAll("(?is)&amp;", "&");
			SUBDIVISION_NAME_LIST.add(subdivision_name);
			subdivision_name = subdivision_name.replaceAll("\\s{2,}", " ");
			subdivision_name = subdivision_name.trim().toUpperCase();
			SUBDIVISION_NAME_MAP.put(matcher1.group(1), subdivision_name);
		}
	}
	
	public static List<String> getSubdivisionNameList() {
		return SUBDIVISION_NAME_LIST;
	}
	
	public static String getSubdivisionCodeFromName(String name) {
		for (Map.Entry<String, String> entry : SUBDIVISION_NAME_MAP.entrySet()) {
			if (name.equals(entry.getValue())) {
	        	return entry.getKey();
	        }
		}
		return "";
	}
	

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		LinkedHashMap<String, ParsedResponse> responses = new LinkedHashMap<String, ParsedResponse>();
		
		StringBuilder header = new StringBuilder("<table border=\"1\" cellspacing=\"0\" cellpadding=\"13\">\n  <tr> " +
				"<th>" + SELECT_ALL_CHECKBOXES + "</th>" +
				"<th>#</th>    <th>Index</th>    <th>Date Filed</th>   <th>Document<br/>Type</th>    <th>Grantor</th>    <th>Grantee</th>    " +
				"<th>Remarks<br/>Not Warranted</th>    <th>Instrument Number</th>   <th>Volume/Page</th>   <th>Ref</th>   <th>Amount</th>   <th>Images</th>  </tr>");
		StringBuilder footer = new StringBuilder();
		
		StringBuilder newTable = new StringBuilder();
		
		int numberOfUncheckedElements = 0;
//		int idCount = 0;
		
		if (table.contains("Your search returned\\s+<strong>0</strong>\\s+results")) {
			return responses.values();
			
		} else
		try {	
			Map<String, String> params = new HashMap<String, String>();			 			
			params = HttpSite3.fillConnectionParams(table, ro.cst.tsearch.connection.http3.OHDelawareRO.getTargetArgumentParameters(), "aspnetForm");
			
			String urlInfo = "";
			String smScriptMan_HiddenField_value = "";
			HtmlParser3 htmlParser = new HtmlParser3(table);
			NodeList nodeList = htmlParser.getNodeList();
			
			nodeList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "aspnetForm"))
					.extractAllNodesThatMatch(new TagNameFilter("script"), true);
			
			
			if (nodeList != null) {
				String[] hiddenInfo = getHiddenParam(nodeList);
				params.remove("ctl00_smScriptMan_HiddenField");
				params.put("ctl00_smScriptMan_HiddenField", hiddenInfo[1]);
				urlInfo = hiddenInfo[0];
				smScriptMan_HiddenField_value = hiddenInfo[1];
			}
			
			nodeList = htmlParser.getNodeList()
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMain_tcMain_tpInstruments_ucInstrumentsGridV2_cpgvInstruments"), true);
			
			if (nodeList.size() == 0) {
				return responses.values();
			}
			
			footer = getPrevAndNextLinks(response, htmlParser, "cottPager", params);
			
			TableTag tableTag = (TableTag) nodeList.elementAt(0);
			TableRow[] rows  = tableTag.getRows();
			
			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				TableColumn[] columns = row.getColumns();
				
				if(columns.length == 13) {
					int seq = getSeq();
					ParsedResponse currentResponse = new ParsedResponse();
					
					String index = columns[1].getChildrenHTML().replaceAll("(?is)&nbsp;", "").trim();
					String dateFiled = columns[2].getChildrenHTML().replaceAll("(?is)&nbsp;", "").trim();
					String kind = columns[3].getChildrenHTML().replaceAll("(?is)&nbsp;", "").trim();
					
					String grantors = extractInfoFromTable(columns, 4);
					
					String grantees = extractInfoFromTable(columns, 5);
					
					String description = extractInfoFromTable(columns, 6);
					
					String instrNumber = "";
					String link = "";
					String instrNoInfo = "";
					NodeList infoFromInstr = columns[7].getChildren().extractAllNodesThatMatch(new TagNameFilter("div"), true);
					if (infoFromInstr.size() == 2) {
						String detPgPattern = "(?is)href=\\\"javascript:WebForm_DoPostBackWithOptions\\([^\\(]+\\(&quot;([^&]+)&quot;\\s*,\\s*[^\\\"]+\\\"";
						link = infoFromInstr.elementAt(1).getFirstChild().toHtml();
						instrNumber = link;
						String imgId = RegExUtils.getFirstMatch(detPgPattern, link, 1);
						if (StringUtils.isNotEmpty(imgId)) {
							params.put("ctl00$smScriptMan", "ctl00$cphMain$upMain|" + imgId);
							params.remove("__EVENTTARGET");
							params.put("__EVENTTARGET", imgId);
							params.put("ctl00_smScriptMan_HiddenField", smScriptMan_HiddenField_value);
							mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
							link = CreatePartialLink(TSConnectionURL.idPOST) + urlInfo + "?seq=" + seq + "&__EVENTTARGET=" + imgId; 
							try {
								link = URLDecoder.decode(link, "UTF-8");
							} catch (UnsupportedEncodingException e) {							
								e.printStackTrace();
								logger.trace(e);
							}
						}
						
						instrNumber = instrNumber.replaceFirst("(?is)<a[^>]+>\\s*([^<]+)</a>", "$1").trim();
						if (instrNumber.length() == 13) { // YYYY-0..0dd..d
							instrNumber = instrNumber.substring(0,4) + instrNumber.substring(5);
						}
						LinkTag linkTag = ((LinkTag)infoFromInstr.elementAt(1).getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0));
						linkTag.setLink(link);
						instrNoInfo = linkTag.toHtml();
					}
//					responses.put(instrNumber, currentResponse);
					
					String bk = "";
					String pg = "";
					String BkPgInfo = "";
					NodeList infoFromBkPg = columns[8].getChildren().extractAllNodesThatMatch(new TagNameFilter("div"), true);
					if (infoFromBkPg.size() == 2) {
						BkPgInfo = infoFromBkPg.elementAt(1).getFirstChild().toHtml();
						BkPgInfo = BkPgInfo.replaceFirst("(?is)<a[^>]+>\\s*([^<]*)</a>", "$1").trim();
						Matcher m = Pattern.compile("(?is)(\\d+)\\s*/\\s*(\\d+)").matcher(BkPgInfo);
						if (m.find()) {
							bk = m.group(1);
							pg = m.group(2);
						}
						BkPgInfo = BkPgInfo.replaceFirst("\\s*/\\s*", " / ");
					}
					
					//Instr 2006-00004076, 1993-00027522 --> multiple references
					String infoFromRef = extractInfoFromTable(columns, 9);
					String crossRefsInfo = infoFromRef;
					crossRefsInfo = crossRefsInfo.replaceAll("(?is)@@@\\s*(\\d{4}\\s*-\\s*\\d{8})\\s*", " ($1)");
					
					//instr 2002-00039948, 2002-00039944, 2002-00041597 (amount la description); 1996-00009294 (pe col Amount)
					String amount = columns[10].getChildrenHTML().trim();
					
					String imgCol = columns[11].getChildrenHTML();
					String imageLink = "";
					String imagePattern = "(?is)onclick=\\\"javascript:WebForm_DoPostBackWithOptions\\([^\\(]+\\(&quot;([^&]+)&quot;\\s*,\\s*[^\\\"]+\\\"";
					String imgId = RegExUtils.getFirstMatch(imagePattern, imgCol, 1);
					
					if (columns[11].getChildCount() > 1) {
						imgCol = columns[11].getChild(1).toHtml().trim();
						if (StringUtils.isNotEmpty(imgId)) {
							params.remove("ctl00$smScriptMan");
							params.put("ctl00$smScriptMan", "ctl00$cphMain$upMain|" + imgId);
							
							mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsImage:" + seq, params);
							
							imageLink = CreatePartialLink(HTTPRequest.POST) + response.getLastURI().toString().replaceFirst("(?is)[^\\.]+\\.com", "") 
									+ "?seq=" + seq + "&ImageFromInterm=true" + "&imgID=" + imgId;
							try {
								imageLink = URLDecoder.decode(imageLink, "UTF-8");
							} catch (UnsupportedEncodingException e) {							
								e.printStackTrace();
								logger.trace(e);
							}
							
							imgCol = "<a href=\"" + imageLink + "\" target=\"blank\">" + imgCol + "page(s) </a>";
//							currentResponse.addImageLink(new ImageLinkInPage(imageLink, instrNumber + ".tif"));
						}
						
					} else {
						imgCol = "";
					}
					
					
					String type = kind;
					if (StringUtils.isEmpty(type)) {
						type = index;
					}
					
					String responseHtml = columns[0].toHtml() +
							"<td>" + index 		  + "</td>" +
							"<td>" + dateFiled    + "</td>" +
							"<td>" + kind		  + "</td>" +
							"<td>" + grantors	  + "</td>" +
							"<td>" + grantees	  + "</td>" +
							"<td>" + description  + "</td>" +
							"<td>" + instrNoInfo + "</td>" +
							"<td>" + BkPgInfo + "</td>" +
							"<td>" + crossRefsInfo  + "</td>" +
							"<td>" + amount		  + "</td>" +
							"<td>" + imgCol		  + "</td>";
					
					String responseHtmlInterm = "<tr>" + responseHtml + "</tr>";
					
					ResultMap resultMap = new ResultMap();
					resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO");
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNumber);
					resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), dateFiled);
					resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), type);
					if (StringUtils.isNotEmpty(bk) && StringUtils.isNotEmpty(pg)) {
						resultMap.put(SaleDataSetKey.BOOK.getKeyName(), bk);
						resultMap.put(SaleDataSetKey.PAGE.getKeyName(), pg);
					}
					
					if (StringUtils.isNotEmpty(grantors)) {
						resultMap.put("tmpGrantor", grantors);
					}
					if (StringUtils.isNotEmpty(grantees)) {
						resultMap.put("tmpGrantee", grantees);
					}
					
					if (StringUtils.isNotEmpty(description)) {
						resultMap.put("tmpDescription", description);
					}
					if (StringUtils.isNotEmpty(infoFromRef) && (!"&nbsp;<br>".equals(infoFromRef))) {
						resultMap.put("tmpCrossRef", infoFromRef);
					}
					
					
					ro.cst.tsearch.servers.functions.OHDelawareRO.parseNames(resultMap, searchId);
					ro.cst.tsearch.servers.functions.OHDelawareRO.parseLegalDesc(resultMap);
					ro.cst.tsearch.servers.functions.OHDelawareRO.parseCrossReferences(resultMap);
				   	
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, responseHtmlInterm);
					currentResponse.setOnlyResponse(responseHtmlInterm);
					
					
					Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
					RegisterDocumentI document = (RegisterDocumentI)bridge.importData();
					currentResponse.setDocument(document);
					
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("instrno", instrNumber);
					data.put("book", StringUtils.defaultString((String) resultMap.get(SaleDataSetKey.BOOK.getKeyName())));
					data.put("page", StringUtils.defaultString((String) resultMap.get(SaleDataSetKey.PAGE.getKeyName())));
					data.put("type", type);
					
					String checkBox = "checked";
//					idCount++;
					
					if (isInstrumentSaved(instrNumber, document, data, true) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
						checkBox = "saved";
						
					} else {
						numberOfUncheckedElements ++;
						LinkInPage linkInPage = new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD);
//						checkBox = "<input type='checkbox' autocomplete='off' name='docLink' value='" + link + "' id='docIndex" + idCount + "'>";
						checkBox = "<input type='checkbox' name='docLink' value='" + link + "'>";
					   	
						currentResponse.setPageLink(linkInPage);
						
						ImageLinkInPage imgLinkInPage = new ImageLinkInPage(imageLink, instrNumber + ".tif");
						currentResponse.addImageLink(imgLinkInPage);   						    			
				 	}
					
					responseHtmlInterm = responseHtmlInterm.replaceFirst("(?is)<tr>\\s*<td>", 
							"<tr> <td align='center'>" + Matcher.quoteReplacement(checkBox) + "</td> <td>");
					currentResponse.setOnlyResponse(responseHtmlInterm);
					newTable.append(responseHtmlInterm);
					responses.put(instrNumber, currentResponse);
				}
			}
			
					
			response.getParsedResponse().setHeader(header.toString());
			response.getParsedResponse().setFooter(footer + "</table> <br><br>");
			
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
			t.printStackTrace();
		}
		
		//return intermediaryResponse;
		newTable.append("</table>");
		outputTable.append(newTable);
		SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
		return responses.values();
	}
	
	

	
	public static String extractInfoFromTable(TableColumn[] columns, int idx) {
		String tmp = columns[idx].toHtml();
		if (tmp.contains("<div title='Expanded'")) {
			tmp = columns[idx].getChildren().extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("title", "Expanded")).elementAt(0).toHtml();
		} else {
			tmp = columns[idx].getChildren().extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("title", "Collapsed")).elementAt(0).toHtml();
		}
		tmp = tmp.replaceAll("(?is)</?div[^>]*>", "");
		tmp = tmp.replaceAll("(?is)<td[^>]+>\\s*<a[^>]+>\\s*\\[[-+]\\]\\s*</a>\\s*</td>", "");
		tmp = tmp.replaceAll("(?is)<tr>\\s*<td[^>]+>\\s*</td>\\s*</tr>","");
		
		if (tmp.contains("<table")) {
			tmp = tmp.replaceAll("(?is)(?:\\s*</tr>\\s*)?\\s*</?table[^>]*>\\s*(?:\\s*<tr>\\s*)?", "");
			tmp = tmp.replaceAll("(?is)\\s*</tr>\\s*<tr>\\s*", "");
			tmp = tmp.replaceAll("<\\s*/?\\s*br\\s*/?\\s*>", "#br#");
			tmp = tmp.replaceAll("(?is)\\s*<td[^>]*>\\s*(?:<b>)?([^<]+)(?:</b>)?</td>\\s*", "$1" + "<br>");
			tmp = tmp.replaceAll("(?is)\\s*<b>([^<]+)</b>", " $1");
			tmp = tmp.replaceAll("(?is)<td[^>]*>([^<]+)</td>", "$1");
			if (idx == 9) {
				//only for cross ref table
				tmp = tmp.replaceAll("#br#\\s*(\\d+{4}\\s*-\\s*\\d+{8})", "@@@$1");
				tmp = tmp.replaceAll("#br#", "<br>");
			} else {
				tmp = tmp.replaceAll("#br#", "<br>");
			}
			
			if (tmp.contains("Parcel Number") || tmp.contains("Parcel:")) {
				//\d{3}\s*-\s*\d{3}\s*-\s*\d{2}\s*-\s*\d{3}\s*-\s*\d{3}  --> Parcel: 319-314-01-021-605
				//\d{14}  --> Parcel: 51944401006000
				//junk [Parcel: SEE NUMBER]  --> instr# 2002-00039163
				tmp = tmp.replaceFirst("(?is)\\s*\\[?\\s*Parcel\\b\\s*:\\s*SEE\\s+(?:DOC(?:UMENT)?|NUMBER)\\b\\s*\\]?\\s*", " ");
				tmp = tmp.replaceFirst("(?is)<a[^>]+>([^<]+)</a>", "$1");
				tmp = tmp.replaceFirst("(?is)<td[^>]*>\\s*((?:<b>\\s*)?Parcel Number:?\\s*(?:</b>\\s*)?[^<]+)</td>", "$1");
				/* (?is)<td[^>]*>\\s*((?:<b>|(?:[^\\[]*)\\[)?\\bParcel\\b\\s*(?:Number)?:?\\s*(?:</b>)?\\s*(\\d{3}\\s*-\\s*\\d{3}\\s*-\\s*\\d{2}\\s*-\\s*\\d{3}\\s*-\\s*\\d{3}|\\d{14}|SEE\\b\\s+(?:DOC(?:UMENT)?|NUMBER|NO))\\s*\\]?\\s*[^<]*)</td> */
			}
		}
		
		tmp = tmp.replaceFirst("(?is)&nbsp;\\s*<br>", "");
		return tmp;
	}
	
	
	private String getRecordedDate(DocumentI document) {
		String recordedDate = "";
		
		if (document instanceof RegisterDocumentI) {
			RegisterDocumentI registerDocument = (RegisterDocumentI)document;
			Date date = registerDocument.getRecordedDate();
			if (date!=null) {
				DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
				recordedDate = formatter.format(date);
			}
		}	
		
		return recordedDate;
	}
	
	 public static void processInstrumentNo(InstrumentI instr) {
			try {
				String instrNo = instr.getInstno();
				Matcher m = Pattern.compile("(\\d{4})(\\d{8})").matcher(instrNo);
				if (m.find()) {
					int instYear = Integer.parseInt(m.group(1));
					if (instYear <=  Calendar.getInstance().get(Calendar.YEAR)) {
						if ( Util.isValidDate(m.group(1))) {
							if (m.group(2).trim().length() == 8)
								instr.setEnableInstrNoTailMatch(true);
						}
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	
	@Override
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId) {
		if(StringUtils.isEmpty(instrumentNo))
    		return false;
    	
    	DocumentsManagerI documentManager = getSearch().getDocManager();
    	try {
    		documentManager.getAccess();
    		if (documentToCheck != null) {
    			DocumentI documentFromTSRI = documentManager.getDocument(documentToCheck.getInstrument());
    			DocumentI dToCheckClone = documentToCheck.clone();
    			if (documentFromTSRI != null) {
    				RegisterDocumentI docFound = (RegisterDocumentI) documentFromTSRI;
    				RegisterDocumentI docToCheck = (RegisterDocumentI) documentToCheck;
    					
    				if (docFound != null) {
    					if (docFound.isFake() && isAutomaticSearch()){
    	    				if (docToCheck.isOneOf(DocumentTypes.PLAT)){
    		    				return false;
    		    			}
    	    				documentManager.remove(docFound);
    	    				SearchLogger.info("<span class='error'>Document was a fake one " + docFound.getDataSource() 
    	    										+ " and was removed to be saved from RO.</span><br/>", searchId);
    	    				return false;
    	    			}
    					docToCheck.mergeDocumentsInformation(docFound, searchId, true, false);
        				return true;
    				}    				
    			} else {
    				processInstrumentNo(dToCheckClone.getInstrument());
    				dToCheckClone.getInstrument().setEnableIgnoreLeadingLetterFromBook(true);
					List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, dToCheckClone.getInstrument());
					if (almostLike != null && !almostLike.isEmpty()) {
						if (isAutomaticSearch()){
    						if (documentToCheck.isOneOf(DocumentTypes.PLAT)){
        	    				return false;
        	    			}
	    					for (DocumentI documentI : almostLike) {
	    						if (documentI.isFake()){
	    							documentManager.remove(documentI);
	    							SearchLogger.info("<span class='error'>Document was a fake one from " + documentI.getDataSource() 
	    									+ " and was removed to be saved from RO.</span><br/>", searchId);
	        	    				return false;
	        	    			}
							}
    					}
						return true;
					}
    			}
    		} else {
	    		InstrumentI instr = new com.stewart.ats.base.document.Instrument(instrumentNo);
	    		if(data != null) {
		    		if(!StringUtils.isEmpty(data.get("type"))) {
		        		String serverDocType = data.get("type");
		    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId); 
		            	instr.setDocType(docCateg);
		            	instr.setDocSubType(DocumentTypes.getDocumentSubcategory(serverDocType, searchId));
		    		}
		    		
		    		instr.setBook(data.get("book"));
		    		instr.setPage(data.get("page"));
		    		instr.setDocno(data.get("docno"));
	    		}
	    		
	    		try {
	    			instr.setYear(Integer.parseInt(data.get("year")));
	    		} catch (Exception e) {}
	    		
	    		DocumentI documentFromTSRI = documentManager.getDocument(instr);
	    		if (documentFromTSRI != null) {
	    			RegisterDocumentI docFound = (RegisterDocumentI) documentFromTSRI;
	    					
	    			if (docFound != null) {
	    				if (docFound.isFake() && isAutomaticSearch()){
	    					documentManager.remove(docFound);
	    					SearchLogger.info("<span class='error'>Document was a fake one " + docFound.getDataSource() 
	    	    										+ " and was removed to be saved from RO.</span><br/>", searchId);
	    					return false;
	    				}
	    			}
	    			return true;
	    		} else {
	    			processInstrumentNo(instr);
	    			List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, instr);
	    			if(almostLike != null && !almostLike.isEmpty()) {
	    				if (isAutomaticSearch()){
	    					for (DocumentI documentI : almostLike) {
	    						if (documentI.isFake()){
	    							documentManager.remove(documentI);
	    							SearchLogger.info("<span class='error'>Document was a fake one from " + documentI.getDataSource() 
	    									+ " and was removed to be saved from RO.</span><br/>", searchId);
	        	    				return false;
	        	    			}
							}
    					}
						return true;
					}
	    			
	    			if(checkMiServerId) {
		    			boolean foundMssServerId = false;
	    				for (DocumentI documentI : almostLike) {
	    					if(miServerID==documentI.getSiteId()){
	    						foundMssServerId  = true;
	    						break;
	    					}
	    				}
		    			
	    				if(!foundMssServerId){
	    					return false;
	    				}
	    			}
	    			
    				if(data!=null) {
    					if(!StringUtils.isEmpty(data.get("type"))){
			        		String serverDocType = data.get("type"); 
			    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId);
			    	    	for (DocumentI documentI : almostLike) {
			    	    		if( (!checkMiServerId || miServerID==documentI.getSiteId()) && documentI.getDocType().equals(docCateg)){
			    	    			if (isAutomaticSearch() && documentI.isFake()){
			    	    				documentManager.remove(documentI);
			    	    				SearchLogger.info("<span class='error'>Document was a fake one from " + documentI.getDataSource() 
			    	    							+ " and was removed to be saved from RO.</span><br/>", searchId);
			    	    				return false;
			    					}
									return true;
			    	    		}
							}	
    					}
		    		}
	    		}
    		}
    		
    	} catch (Exception e) {
			e.printStackTrace();
		} finally {
			documentManager.releaseAccess();
		}
    	return false;
    }

	
	public DownloadImageResult saveImageFromRO(ImageI image, String instrument) {
		/* Force deletion of parameters from last query */
		getTSConnection().setQuery("");
		
//		String imageLink = "http://cotthosting.com/ohdelaware/LandRecords/protected/DocumentDetails.aspx?instrNo=" + instrument;
		String url = dataSite.getServerHomeLink() + "ohdelaware/LandRecords/protected/DocumentDetails.aspx?instrNo=" + instrument;
		HashSet<String> linkRO = new HashSet<String>();
    	linkRO.add(url);
		image.setLinks(linkRO);
		ImageLinkInPage imgLinkInPg = ImageTransformation.imageToImageLinkInPage(image);
		DownloadImageResult res = null;
		synchronized (OHDelawareRO.class) {
			try {
				res = saveImg(imgLinkInPg);
			} catch (ServerResponseException e) {
				e.printStackTrace();
			}
		}
		if( res == null){
			return null;
		}
		if (res.getStatus() == DownloadImageResult.Status.OK){		
			image.setSaved(true);
		}
		
		return res;
	}
	
	
	protected DownloadImageResult saveImg(ImageLinkInPage image) throws ServerResponseException {
		String link = image.getLink();
		byte[] imageBytes = null;
		
		HttpSite3 site = HttpManager3.getSite(getCurrentServerName(), searchId);
		try {
			imageBytes = ((ro.cst.tsearch.connection.http3.OHDelawareRO)site).getImageFromRO(link);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager3.releaseSite(site);
		}

		ServerResponse resp = new ServerResponse();

		if (imageBytes != null) {
			afterDownloadImage(true);
		} else {
			return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
		}

		String imageName = image.getPath();
		if (ro.cst.tsearch.utils.FileUtils.existPath(imageName)) {
			imageBytes = ro.cst.tsearch.utils.FileUtils.readBinaryFile(imageName);
			return new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, image.getContentType());
		}

		resp.setImageResult(new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, image.getContentType()));

		if (!ro.cst.tsearch.utils.FileUtils.existPath(imageName)) {
			ro.cst.tsearch.utils.FileUtils.writeByteArrayToFile(resp.getImageResult().getImageContent(), image.getPath());
		}

		DownloadImageResult dres = resp.getImageResult();

		return dres;
	}
	
	
	@Override
	protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {
		String link = image.getLink();
		byte[] imageBytes = null;

		HttpSite3 site = HttpManager3.getSite(getCurrentServerName(), searchId);
		try {
			imageBytes = ((ro.cst.tsearch.connection.http3.OHDelawareRO)site).getImage(link);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager3.releaseSite(site);
		}

		ServerResponse resp = new ServerResponse();

		if (imageBytes != null) {
			afterDownloadImage(true);
		} else {
			return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
		}

		String imageName = image.getPath();
		if (ro.cst.tsearch.utils.FileUtils.existPath(imageName)) {
			imageBytes = ro.cst.tsearch.utils.FileUtils.readBinaryFile(imageName);
			return new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, image.getContentType());
		}

		resp.setImageResult(new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, image.getContentType()));

		if (!ro.cst.tsearch.utils.FileUtils.existPath(imageName)) {
			ro.cst.tsearch.utils.FileUtils.writeByteArrayToFile(resp.getImageResult().getImageContent(), image.getPath());
		}

		DownloadImageResult dres = resp.getImageResult();

		return dres;
	}
	

	public String[] getDetailedContent(String rsResponse) {
		String[] result = new String[6];
		try {
			if(rsResponse != null) {
				HtmlParser3 parser = new HtmlParser3(rsResponse);
				NodeList nodeList = parser.getNodeList();
				
				if(rsResponse.startsWith("<table")) {
					result[0] = rsResponse;
					
					return result;
					
				} else {
				
					NodeList infoList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "docInfoTable"));
					
					if (infoList.size() > 0) {
						TableTag tmpTable = (TableTag) infoList.elementAt(0);
						if (tmpTable.getRowCount() > 1) {
							TableRow row = tmpTable.getRow(1);
							TableColumn[] cols = row.getColumns();
							
							if (cols.length == 9 || cols.length == 6) {
								//length = 9 => OFF RECORDS docs
								//length = 6 => UCC docs
								String instr = cols[0].getFirstChild().getText().trim();
								if (StringUtils.isNotEmpty(instr)) {
									if (instr.matches("\\d{4}\\s*-\\s*\\d{8}")) {
										result[0] = instr;
										result[1] = instr.substring(0,4);
									}
								}
								
								String bkPg = cols[1].getFirstChild().getText().trim();
								if (StringUtils.isNotEmpty(bkPg) && bkPg.matches("\\d+\\s*/\\s*\\d+")) {
									bkPg = bkPg.replaceFirst("\\s*/\\s*", "/");
									int idx = bkPg.indexOf("/");
									if (idx > 0) {
										result[2] = bkPg.substring(0, idx);
										result[3] = bkPg.substring(idx + 1);
									}
								} else {
									result[2] = "";
									result[3] = "";
								}
								
								String doctype = cols[3].getFirstChild().getText().trim();
								if (StringUtils.isNotEmpty(doctype) && !"&nbsp;".equals(doctype)) {
									result[5] = doctype;
								} else {
									doctype = cols[2].getFirstChild().getText().trim();
									if ("OFFICIAL RECORDS".equals(doctype)) {
										result[5] = "OFF";
									} else {
										result[5] = "UCC";
									}
								}
								
								String date = cols[5].getFirstChild().getText().trim();
								int idx = date.lastIndexOf("/") + 4;
								if (idx > 0) {
									result[4] = date.substring(0, idx+1);
								}
							}
						}
						return result;
					}
				}
			}
		} catch (Exception e) {
			logger.error("SearchId: " + searchId + ": Error while geting detailed response", e);
		}	
		return null;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		// 	no result returned - error message on official site
		if (rsResponse.contains("an unexpected error has occurred")) {
			Response.getParsedResponse().setError("Error message received from Official site");
    		return;
    	} else if (rsResponse.contains("Your search returned\\s*<strong>0</strong>\\s*results")) {
    		Response.getParsedResponse().setError("No results were found for the provided search criteria");
    		return;
    	}
		
		switch (viParseID) {			
			case ID_SEARCH_BY_INSTRUMENT_NO:	//Search by Instrument 
			case ID_SEARCH_BY_PARCEL:  			//Search by Parcel Number
			case ID_SEARCH_BY_NAME:				//Search by Quick Name
			case ID_SEARCH_BY_BOOK_AND_PAGE:	//Search by Book-Page
			case ID_SEARCH_BY_PROP_NO:			//Search by Property
			case ID_SEARCH_BY_MODULE30:			//Search by Book-Page
			
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				
				if(smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
					
					 if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
			            	String header = parsedResponse.getHeader();
			               	String footer = parsedResponse.getFooter();
			            	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
			            	
			            	Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
			            	
			            	if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
			            		footer += CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer) numberOfUnsavedDocument);
			            	} else {
			            		footer += CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
			            	}
			            	
			            	parsedResponse.setHeader(header);
			            	parsedResponse.setFooter(footer);
			            }
					
					parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
	            }			
				break;
				
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:						
				StringBuilder instrNo = new StringBuilder();
				String details = getDetails(rsResponse, instrNo, Response);
				String filename = instrNo + ".html";
				
				String imgLink = "";
				try {
					HtmlParser3 parser = new HtmlParser3(details);
					NodeList nodes = parser.getNodeList();
					
					if (nodes.size() > 0) {
						Node tmpNode = HtmlParser3.getNodeByID("imageLink", nodes, true);
						if (tmpNode != null) {
							LinkTag aList = (LinkTag) HtmlParser3.getNodeByID("imageLink", nodes, true).getFirstChild();
							if (aList != null){
								imgLink = aList.getLink();
								imgLink += "&InstrumentNo=" + instrNo;
							}
						}
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if (Response.getParsedResponse().getImageLinksCount() == 0) {
					if (StringUtils.isNotEmpty(imgLink)){
						Response.getParsedResponse().addImageLink(new ImageLinkInPage(imgLink, instrNo + ".tif"));
					}
				}
				
				if (viParseID != ID_SAVE_TO_TSD) {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
	
					HashMap<String, String> data = new HashMap<String, String>();
					String[] infoForDoc = getDetailedContent(details);
					if (infoForDoc != null) {
						data.put("instrument", infoForDoc[0]);
						data.put("year", infoForDoc[1]);
		    			data.put("book", infoForDoc[2]);
		    			data.put("page", infoForDoc[3]);
		    			data.put("date", infoForDoc[4]);
		    			data.put("type", infoForDoc[5]);
					}
					
					
					if (isInstrumentSaved(instrNo.toString(),null,data)){
						details += CreateFileAlreadyInTSD();
					
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);					
						details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
					}
	
					Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
					Response.getParsedResponse().setResponse(details);
					
				} else {
					String resultForCross = details;
					details = details.replaceAll("(?is)<a [^>]+>([^<]+)</a>", "$1");
					smartParseDetails(Response,details);
					
					Pattern crossRefLinkPattern = Pattern.compile("(?ism)<a[^>]*?href=[\\\"|'](.*?)[\\\"|']>");
	                Matcher crossRefLinkMatcher = crossRefLinkPattern.matcher(resultForCross);
	                while(crossRefLinkMatcher.find()) {
		                ParsedResponse prChild = new ParsedResponse();
		                String link = crossRefLinkMatcher.group(1) + "&isSubResult=true";
		                LinkInPage pl = new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD);
		                prChild.setPageLink(pl);
		                Response.getParsedResponse().addOneResultRowOnly(prChild);
	                }
					
					msSaveToTSDFileName = filename;
					Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
					Response.getParsedResponse().setResponse(details);
					
					msSaveToTSDResponce = details + CreateFileAlreadyInTSD();					
				}
				break;
				
			case ID_GET_LINK :
				ParseResponse(sAction, Response, rsResponse.contains("Include Criteria")
						? ID_SEARCH_BY_NAME
						: ID_DETAILS);
				break;
				
			default:
				break;
		}
	}	
	
	
	protected String getDetails(String rsResponse, StringBuilder accountId, ServerResponse Response) {
		try {
			/* If from memory - use it as is */
			if(!rsResponse.contains("<html")) {
				NodeList list = new HtmlParser3(rsResponse).getNodeList();
				if (list.size() > 0) {
					NodeList tmpList = HtmlParser3.getNodesByID("instrNoValue", list, true);
					if (tmpList.size() > 0) {
						String instrNoInfo = tmpList.elementAt(0).getChildren().toHtml().trim();
						if (instrNoInfo.length() == 13) {
							instrNoInfo = instrNoInfo.substring(0,3) + instrNoInfo.substring(5);
						}
						accountId.append(instrNoInfo);
					}
				}
				return rsResponse;
			
			} else {
				try {
					int seq = getSeq();
					StringBuilder details = new StringBuilder();
					HtmlParser3 htmlParser = new HtmlParser3(rsResponse);
					
					NodeList nodeList = htmlParser.getNodeList();
					String actionLink = "";
					
					if(nodeList.size() == 0) {
						return rsResponse;
					
					} else {
						NodeList formList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("id", "aspnetForm"));
						Map<String, String> params = new HashMap<String, String>();	
						
						if (formList != null && formList.size() > 0){
							FormTag form = (FormTag) formList.elementAt(0);
							if (form != null){
								actionLink = form.getAttribute("action");
							}
							NodeList scriptList = formList.extractAllNodesThatMatch(new TagNameFilter("script"), true);
							
							if (scriptList != null && scriptList.size() > 0) {
								String[] hiddenInfo = getHiddenParam(scriptList);
								params.put("ctl00_smScriptMan_HiddenField", hiddenInfo[1]);
								
								mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsImage:" + seq, params);
							}
						}
						
						NodeList detailsList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("class", "cphMainContainer"), false);  // get table with details info info

						if (detailsList.size() != 0) {
							detailsList = detailsList.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("div"), false);
							details.append("<div id=\"detailsInfo\"> <b> <span style=\"font-size:14px; color: darkblue;\"> Document Details </span> </b> <br/>");
							
							// <!-- General Information -->
							TableTag table = (TableTag) detailsList.elementAt(0).getChildren()
									.extractAllNodesThatMatch(new TagNameFilter("table"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMain_gvDetails1"), true).elementAt(0);
							
							if (table != null) {
								boolean hasImg = false;
								
								table.removeAttribute("id");
								table.setAttribute("id", "\"docInfoTable\"");
								
								String apn = "";
								if (table.getRowCount() == 2) {
									apn =  table.getRow(1).getColumns()[0].getChildrenHTML().trim();
									if (StringUtils.isNotEmpty(apn)) {
										if (apn.length() == 13) { // YYYY-0..0dd..d
											apn = apn.substring(0,4) + apn.substring(5);
										}
										
										accountId.append(apn);	
										table.getRow(1).getColumns()[0].setAttribute("id", "\"instrNoValue\"");
									}
									
									if (table.getRow(1).getColumnCount() == 6) {
										//UCC document
										table.getRow(1).getColumns()[2].setAttribute("id", "\"indexType\"");
										table.getRow(1).getColumns()[3].setAttribute("id", "\"docType\"");
										table.getRow(1).getColumns()[5].setAttribute("id", "\"dateFiled\"");

									} else {
										//OFF RECORD docs
										String imgLink = table.getRow(1).getColumns()[7].getChildrenHTML().trim();
										if (imgLink.contains("<a href") && imgLink.contains("</a>")) {
											hasImg = true;
											table.getRow(1).getColumns()[7].setAttribute("id", "\"imageLink\"");
										}
										
										table.getRow(1).getColumns()[2].setAttribute("id", "\"indexType\"");
										table.getRow(1).getColumns()[3].setAttribute("id", "\"docType\"");
										table.getRow(1).getColumns()[5].setAttribute("id", "\"dateFiled\"");
										table.getRow(1).getColumns()[6].setAttribute("id", "\"dateRelased\"");
									}
								}
								
								String htmlTable = table.toHtml();
								htmlTable = htmlTable.replaceFirst("class=\\\"GridView_Header\\\"", "style=\"border-color:Gray;border-width:1px;border-style:Solid;background-color:darkblue;color:white;\"");
								
								if (hasImg) {
									String link = CreatePartialLink(TSConnectionURL.idPOST) + "/ohdelaware/LandRecords/protected/" + actionLink + "&__EVENTTARGET=$1&__EVENTARGUMENT=$2&seq=" + seq;
									htmlTable = htmlTable.replaceFirst("(?is)href=\\\"javascript:__doPostBack\\('([^']+)'\\s*,\\s*'(Image[^']+)'\\)", 
											"href=\"" + link  + "\"  target=\"blank\"");
								}
								
								details.append(htmlTable);
								details.append("  <br/>");
								
								//Response.getParsedResponse().addImageLink(new ImageLinkInPage(link, apn + ".tif"));
							}
							
							table = (TableTag) detailsList.elementAt(0).getChildren()
									.extractAllNodesThatMatch(new TagNameFilter("table"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMain_gvParties1"), true).elementAt(0);
							if (table != null) {
								details.append("<div style=\"width: 50%; clear: left; float: left;\" id=\"grantorDiv\">");
								String htmlTable = table.toHtml();
								htmlTable = htmlTable.replaceFirst("class=\\\"GridView_Header\\\"", "style=\"border-color:Gray;border-width:1px;border-style:Solid;background-color:darkblue;color:white;\"");
								details.append(htmlTable);
								details.append("</div>");
							}
							
							table = (TableTag) detailsList.elementAt(0).getChildren()
									.extractAllNodesThatMatch(new TagNameFilter("table"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMain_gvParties2"), true).elementAt(0);
							if (table != null) {
								details.append("<div style=\"width: 50%; clear: right; float: right;\" id=\"granteeDiv\">");
								String htmlTable = table.toHtml();
								htmlTable = htmlTable.replaceFirst("class=\\\"GridView_Header\\\"", "style=\"border-color:Gray;border-width:1px;border-style:Solid;background-color:darkblue;color:white;\"");
								details.append(htmlTable);
								details.append("</div>");
							}
							
							details.append("  <br/>");
							
							table = (TableTag) detailsList.elementAt(0).getChildren()
									.extractAllNodesThatMatch(new TagNameFilter("table"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMain_gvProperty"), true).elementAt(0);
							if (table != null) {
								String htmlTable = table.toHtml();
								htmlTable = htmlTable.replaceFirst("class=\\\"GridView_Header\\\"", "style=\"border-color:Gray;border-width:1px;border-style:Solid;background-color:darkblue;color:white;\"");
								details.append(htmlTable);
							}
							details.append("</div>");
							details.append("<br/> <br/> </br> </br>");
							
							//<!-- Related Documents -->
							Node divReferences = detailsList.elementAt(0).getChildren()
									.extractAllNodesThatMatch(new TagNameFilter("div"), false)
									.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMain_divRelatedDocs"), true)
									.elementAt(0);
							if (divReferences != null) {
								details.append("<div id=\"divRelatedDocs\"> <h3>References</h3> <br/>");
								
								table = (TableTag) divReferences.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
										.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMain_ctrlResults_cgvResults"), true)
										.elementAt(0);
								if (table != null) {
									if (table.getRowCount() > 3) {
										details.append("<table id=\"crossRefTable\" cellspacing=\"0\" border=\"1\" style=\"border-color:Black;border-width:1px;border-style:Solid;width:100%;border-collapse:collapse;\">" +
												"<tr style=\"border-color:Gray;border-width:1px;border-style:Solid;background-color:darkblue;color:white;\"> " +
												"<th>&nbsp;#&nbsp;</th>  <th> Index </th>  <th> Date Filed </th>   <th> Document Type </th>   <th> GRANTORS </th>   <th> GRANTEES </th>   " +
												"<th> Remarks<br>Not Warranted </th>   <th> Instrument Number </th>   <th> Volume/Page </th>   <th>  Ref </th>   <th> Amount </th> <th> Images </th>  </tr> ");
										
										TableRow[] rows = table.getRows();
										for (int i=2; i<rows.length-2; i++) {
											seq = getSeq();
											String valForEventTarget = "";
											String valForEventArgument = "";
											String refDocLink = "";
											String refImgLink = "";
											TableColumn[] cols = rows[i].getColumns();
											if (cols.length == 14) {
												details.append("<tr>");
												details.append(cols[0].toHtml());
												details.append(cols[2].toHtml());
												details.append(cols[3].toHtml());
												details.append(cols[4].toHtml());
												
												String party1 = extractInfoFromTable(cols, 5);
												if (party1 != null && StringUtils.isNotEmpty(party1)) {
													details.append("<td>" + party1 + "</td>");
												} else {
													details.append("<td>" + "&nbsp;<br>" + "</td>");
												}
												
												String party2 = extractInfoFromTable(cols, 6);
												if (party2 != null && StringUtils.isNotEmpty(party2)) {
													details.append("<td>" + party2 + "</td>");
												} else {
													details.append("<td>" + "&nbsp;<br>" + "</td>");
												}
												
												String legalDesc =  extractInfoFromTable(cols, 7);
												if (legalDesc != null && StringUtils.isNotEmpty(legalDesc)) {
													details.append("<td>" + legalDesc + "</td>");
												} else {
													details.append("<td>" + "&nbsp;<br>" + "</td>");
												}
												
												// cols[8] <=> ref's Instr#;      cols[9] <=> ref's Bk-Pg; 
												String refInstr = cols[8].toHtml();
												refDocLink = refInstr;
												refInstr = refInstr.replaceFirst("(?is)<td>\\s*<a[^>]+>\\s*(\\d+{4}\\s*-\\s*\\d+)\\s*</a>\\s*</td>", "$1");
												refDocLink = refDocLink.replaceFirst("(?is)<td>\\s*<a href=(\\\"[^\\\"]+\\\")>[^<]+</a>\\s*</td>", "$1");
												
												Matcher mParams = Pattern.compile("(?is)\\\"javascript\\:[^']+'([^']+)'\\s*,\\s*'([^']+)'[^\\\"]+\\\"").matcher(refDocLink);
												if (mParams.find() && !refDocLink.matches("(?is).*<a onclick=\"javascript:\\s*return false;\".*")) {
													valForEventTarget 	= mParams.group(1);
													valForEventArgument = mParams.group(2);
													params.remove("ctl00$smScriptMan");
													params.put("ctl00$smScriptMan", "ctl00$cphMain$upMain|" + valForEventTarget);
													params.remove("__EVENTTARGET");
													params.put("__EVENTTARGET", valForEventTarget);
													
													mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsRefInstr:" + seq, params);

													
													refDocLink = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + "/ohdelaware/LandRecords/protected/" + actionLink + 
															"&__EVENTARGUMENT=" + valForEventArgument + "&seq=" + seq + "&isRefInstr=true" +
															"\">" + refInstr + "</a>";
													refInstr = refDocLink;
												}
												details.append("<td>" + refInstr + "</td>");
												
												String refBkPg = cols[9].toHtml();
												refBkPg = refBkPg.replaceFirst("(?is)(?:<td>\\s*)?<a[^>]+>\\s*(\\d+\\s*/\\s*\\d+)\\s*</a>(?:\\s*</td>)?", "$1");
												details.append("<td>" + refBkPg + "</td>");
												
												//cols[10] <=> ref's References;  cols[11] <=> amount;
												String refsInfo =  extractInfoFromTable(cols, 10);
												if (refsInfo != null && StringUtils.isNotEmpty(refsInfo)) {
													details.append("<td>" + refsInfo + "</td>");
												} else {
													details.append("<td>" + "&nbsp;<br>" + "</td>");
												}
												details.append(cols[11].toHtml());
												
												//cols[12] <=> ref's Image 
												//details.append(cols[12].toHtml());
												String refImg = cols[12].toHtml();
												refImgLink = refImg;
												refImg = refImg.replaceFirst("(?is)<td>\\s*<a[^>]+>\\s*(\\d+\\s*pages?)\\s*</a>\\s*</td>", "$1");
												refImgLink = refImgLink.replaceFirst("(?is)<td>\\s*<a href=(\\\"[^\\\"]+\\\")>[^<]+</a>\\s*</td>", "$1");
												
												mParams = Pattern.compile("(?is)\\\"javascript\\:[^']+'([^']+)'\\s*,\\s*'([^']+)'[^\\\"]+\\\"").matcher(refImgLink);
												if (mParams.find()) {
													valForEventTarget 	= mParams.group(1);
													valForEventArgument = mParams.group(2);
													params.remove("ctl00$smScriptMan");
													params.put("ctl00$smScriptMan", "ctl00$cphMain$upMain|" + valForEventTarget);
													params.remove("__EVENTTARGET");
													params.put("__EVENTTARGET", valForEventTarget);
													
													mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsRefImg:" + seq, params);

													
													refImgLink = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + "/ohdelaware/LandRecords/protected/" + actionLink + 
															"&__EVENTARGUMENT=" + valForEventArgument + "&seq=" + seq + "&isRefImg=true" +
															"\">" + refImg + "</a>";
													refImg = refImgLink;
												}
												details.append("<td>" + refImg + "</td>");
												details.append("</tr>");
											}
										}
										
										details.append("</table>");
										details.append("<br/>");
									}
									
									details.append("</div>");
								}
							}
							details.append("  <br/> <br/>");
						}
						
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
	protected Object parseAndFillResultMap(ServerResponse rsResponse, String detailsHtml, ResultMap resultMap) {
		try {	
			String instrNo = "";
			String bkPgInfo = "";
			String idxType = "";
			String serverDocType = "";
//			String remarks = "";
			String dateFiled = "";
			String dateReleased = "";
			String amount = "";
			
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),dataSite.getSiteTypeAbrev());
			
			HtmlParser3 htmlParser = new HtmlParser3(detailsHtml);
			NodeList list = htmlParser.getNodeList();		
			TableTag table = (TableTag) list.extractAllNodesThatMatch
					(new HasAttributeFilter("id","docInfoTable"), true).elementAt(0);
			if (table != null) {
				//instrNo = htmlParser.getNodeContentsById("instrNoValue");
				if (table.getRowCount() == 2) {
					TableRow row = table.getRow(1);
					TableColumn[] cols = row.getColumns();
					if (cols.length >= 6) {
						instrNo = cols[0].getChildrenHTML().trim();
						if (StringUtils.isNotEmpty(instrNo)) {
							if (instrNo.length() == 13) { // YYYY-NNNNNNNN
								instrNo = instrNo.substring(0,4) + instrNo.substring(5);
							}
							resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
						}
						
						bkPgInfo = cols[1].getChildrenHTML().trim();
						bkPgInfo = bkPgInfo.replaceFirst("\\s*/\\s*", "/");
						if (StringUtils.isNotEmpty(bkPgInfo) && !"&nbsp;".equals(bkPgInfo)) {
							int idx = bkPgInfo.indexOf("/");
							if (idx > 0) {
								String bk = bkPgInfo.substring(0,idx).trim();
								String pg = bkPgInfo.substring(idx+1).trim();
								if (StringUtils.isNotEmpty(bk) && StringUtils.isNotEmpty(pg)) {
									resultMap.put(SaleDataSetKey.BOOK_PAGE.getKeyName(), bkPgInfo);
									resultMap.put(SaleDataSetKey.BOOK.getKeyName(), bk);
									resultMap.put(SaleDataSetKey.PAGE.getKeyName(), pg);
								}
							}
						}
						
						idxType = cols[2].getChildrenHTML().trim();
						serverDocType = cols[3].getChildrenHTML().trim();
						if (StringUtils.isNotEmpty(serverDocType) && !"&nbsp;".equals(serverDocType)) {
							resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), serverDocType);
						} else {
							if ("UCC PRE 2005".equals(idxType) || "UCC".equals(idxType)) {
								resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "UCC");
							} else {
								resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "OFF");
							}
						}
						
						dateFiled = cols[5].getChildrenHTML().trim();
						if (StringUtils.isNotEmpty(dateFiled) && !"&nbsp;".equals(dateFiled)) {
							dateFiled = dateFiled.replaceFirst("(?is)(\\d{1,2}\\s*/\\s*\\d{1,2}\\s*/\\s*\\d{4})\\s*(?:\\d{1,2}:\\d{1,2}:\\d{1,2}\\s*(?:A\\.?M\\.?|P\\.?M\\.?))?", "$1");
							resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), dateFiled);
						}
						
						if (cols.length > 7) {
							dateReleased = cols[6].getChildrenHTML().trim();
							if (StringUtils.isNotEmpty(dateReleased) && !"&nbsp;".equals(dateReleased)) {
								dateFiled = dateFiled.replaceFirst("(?is)(\\d{1,2}\\s*/\\s*\\d{1,2}\\s*/\\s*\\d{4})\\s*(?:\\d{1,2}:\\d{1,2}:\\d{1,2}\\s*(?:A\\.?M\\.?|P\\.?M\\.?))?", "$1");
								resultMap.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), dateReleased);
							}
							
							amount = cols[8].getChildrenHTML().trim();
							if (!"$0.00".equals(amount) && StringUtils.isNotEmpty(amount) && !"&nbsp;".equals(amount)) {
								amount = amount.replaceAll("[\\$,\\.]", "");
								String doctype = DocumentTypes.getDocumentCategory(serverDocType, searchId);
								if (doctype.equals(DocumentTypes.MORTGAGE)) {
									resultMap.put(SaleDataSetKey.MORTGAGE_AMOUNT.getKeyName(), amount);
								} else {
									resultMap.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), amount);
								}
							}
						}
					}
				}
			}
		
			table = (TableTag) list.extractAllNodesThatMatch(new HasAttributeFilter("id","ctl00_cphMain_gvParties1"), true)
					.elementAt(0);
			if (table != null) {
				StringBuilder grantors = new StringBuilder();
				int size = table.getRowCount();
				if (size > 0) {
					for (int i=1; i < size; i++) {
						String name = table.getRow(i).getColumns()[0].getChildrenHTML().trim();
						grantors.append(name);
						if (i != size-1) 
							grantors.append("<br>");
					}
				}
				resultMap.put("tmpGrantor", grantors.toString());
			}
			
			table = (TableTag) list.extractAllNodesThatMatch(new HasAttributeFilter("id","ctl00_cphMain_gvParties2"), true)
					.elementAt(0);
			if (table != null) {
				StringBuilder grantees = new StringBuilder();
				int size = table.getRowCount();
				if (size > 0) {
					for (int i=1; i < size; i++) {
						String name = table.getRow(i).getColumns()[0].getChildrenHTML().trim();
						grantees.append(name);
						if (i != size-1) 
							grantees.append("<br>");
					}
				}
				resultMap.put("tmpGrantee", grantees.toString());
			}
			ro.cst.tsearch.servers.functions.OHDelawareRO.parseNames(resultMap, searchId);
			
			table = (TableTag) list.extractAllNodesThatMatch(new HasAttributeFilter("id","ctl00_cphMain_gvProperty"), true)
					.elementAt(0);
			if (table != null) {
				StringBuilder description = new StringBuilder();
				int size = table.getRowCount();
				if (size > 0) {
					for (int i=1; i < size; i++) {
						String info = table.getRow(i).getColumns()[0].getChildrenHTML().trim();
						description.append(info);
					}
				}
				resultMap.put("tmpDescription", description.toString());
			}
			ro.cst.tsearch.servers.functions.OHDelawareRO.parseLegalDesc(resultMap);
			
			//table = (TableTag) list.extractAllNodesThatMatch(new HasAttributeFilter("id","ctl00_cphMain_ctrlResults_cgvResults"), true)
			table = (TableTag) list.extractAllNodesThatMatch(new HasAttributeFilter("id","crossRefTable"), true)
					.elementAt(0);
			if (table != null) {
				StringBuilder refs = new StringBuilder();
				int size = table.getRowCount();
				if (size > 1) {
					for (int i=1; i < size; i++) {
						TableRow r = table.getRow(i);
						if (r.getColumnCount() == 12 || r.getColumnCount() == 13) {
							String refInstr = r.getColumns()[7].getChildrenHTML().trim();
							String refBkPg = r.getColumns()[8].getChildrenHTML().trim();
							String refInfo = "";
							if (StringUtils.isNotEmpty(refBkPg) && StringUtils.isNotEmpty(refInstr)) {
								refInfo = refBkPg + "@@@" + refInstr;
							} else if (StringUtils.isNotEmpty(refBkPg)) {
								refInfo = refBkPg;
							} else if (StringUtils.isNotEmpty(refInstr)) {
								refInfo = refInstr;
							}
							
							refs.append(refInfo + "<br>");
						}
					}
				}
				resultMap.put("tmpCrossRef", refs.toString());
			}
			ro.cst.tsearch.servers.functions.OHDelawareRO.parseCrossReferences(resultMap);
			
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		
		return null;	
	}
	
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		
//		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.CASE_NAME_MODULE_IDX);
//		if (tsServerInfoModule != null) {
//			setupSelectBox(tsServerInfoModule.getFunction(2), getALL_CASE_CATEGORIES());
//		}
//
//		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
//		if (tsServerInfoModule != null) {
//			setupSelectBox(tsServerInfoModule.getFunction(4), getALL_PARTY_ROLES());
//			setupSelectBox(tsServerInfoModule.getFunction(6), getALL_CASE_CATEGORIES());
//		}
		return msiServerInfoDefault;
	}
	
	private void addIteratorModule(TSServerInfo serverInfo, List<TSServerInfoModule> modules, int code, long searchId, boolean isCondo) {
		FilterResponse lotFilter = LegalFilterFactory.getDefaultLotFilter(searchId);
		FilterResponse blockFilter = LegalFilterFactory.getDefaultBlockFilter(searchId);
		
		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
		if (!isCondo) {
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
					TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_BLOCK);
		} else {
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
					TSServerInfoConstants.VALUE_PARAM_CONDOMINIUM_LOT_BLOCK);
		}
		module.clearSaKeys();
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId, isCondo);
		it.setEnableSubdividedLegal(true);
		module.addValidator(blockFilter.getValidator());
		module.addValidator(lotFilter.getValidator());
		module.addIterator(it);
		modules.add(module);
	}
	
//	private void addIteratorModuleSTR(TSServerInfo serverInfo, List<TSServerInfoModule> modules, int code, long searchId) {
//
//		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
//		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
//				TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_SECTION_TWP_RANGE);
//
//		FilterResponse nameFilter = NameFilterFactory.getDefaultNameFilter(
//				SearchAttributes.OWNER_OBJECT, searchId, module);
//		((GenericNameFilter) nameFilter).setIgnoreMiddleOnEmpty(true);
//		((GenericNameFilter) nameFilter).setUseArrangements(false);
//		((GenericNameFilter) nameFilter).setInitAgain(true);
//		FilterResponse defaultLegalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);
//
//		module.clearSaKeys();
//		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId, false);
//		it.setEnableTownshipLegal(true);
//		module.addFilter(nameFilter);
//		module.addFilter(defaultLegalFilter);
//		module.addIterator(it);
//		modules.add(module);
//	}
	
	
	public InstrumentGenericIterator getInstrumentIterator(boolean instrumentType) {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId) {

			private static final long serialVersionUID = 5399351945130601258L;

			@Override
			protected String cleanInstrumentNo(String instno, int year) {
				if (StringUtils.isNotEmpty(instno)) {
					if (year != -1) {
						String yearString = Integer.toString(year);
						String fullInstrForRO = yearString + "-";
						if (instno.length() < 8) {
							fullInstrForRO += StringUtils.leftPad(instno, 8, "0");
						} else if (instno.length() == 8) {
							fullInstrForRO += instno;
						}
						return fullInstrForRO;
						
					} else if (year == -1 && instno.length() == 12) {
						instno = instno.substring(0,4) + "-" + instno.substring(4); 
						return instno;
					} else {
						if (isLoadFromRoLike()) {
							return instno;
						} else {
							return "";
						}
					}
				} else {
					return "";
				}
			}

			@Override
			protected void processEnableInstrumentNo(List<InstrumentI> result,
					HashSet<String> listsForNow, DocumentsManagerI manager,
					InstrumentI instrumentI) {
				String instrumentNo = cleanInstrumentNo(instrumentI.getInstno(), instrumentI.getYear());
				
				if(org.apache.commons.lang.StringUtils.isBlank(instrumentNo)) {
					return;
				} else {
					if(instrumentI.hasBookPage() && instrumentI.hasInstrNo()) {
						instrumentI.setBook("");
						instrumentI.setPage("");
					}
				}
				
				super.processEnableInstrumentNo(result, listsForNow, manager, instrumentI);
				
			}
			
			@Override
			protected List<DocumentI> getDocumentsWithInstrumentsFlexible(DocumentsManagerI manager, InstrumentI instrumentI) {
				List<DocumentI> result = manager.getDocumentsWithInstrumentsFlexible(false, instrumentI);
				if (!result.isEmpty()) {
					return result;
				} else {
					String instno = instrumentI.getInstno();
					int year = instrumentI.getYear();
					String yearFromDate = instrumentI.getDate().toString().replaceFirst("(?is).*(\\d{4})$", "$1");
					boolean hasYear = year!=SimpleChapterUtils.UNDEFINED_YEAR || StringUtils.isNotEmpty(yearFromDate);
					
					//if (year!=SimpleChapterUtils.UNDEFINED_YEAR) {
					if (StringUtils.isNotEmpty(instno)) {
						String fullInstrno = "";
						if (instno.contains("-")){
							fullInstrno = instno.substring(instno.indexOf("-") + 1);
							fullInstrno = StringUtils.stripStart(fullInstrno, "0");
						} else{
							if (hasYear) {
								String yearString = "";
								if (year!=SimpleChapterUtils.UNDEFINED_YEAR) {
									yearString = Integer.toString(year);
								} else {
									yearString = yearFromDate;
								}
								
								if (yearString.length() == 4 && instno.length() <=8) {
									fullInstrno = StringUtils.leftPad(instno, 8, "0");
									fullInstrno = yearString + "-" + fullInstrno;
								}
							}
						}
						if (StringUtils.isNotBlank(fullInstrno)){
							InstrumentI clone = instrumentI.clone();
							clone.setInstno(fullInstrno);
							return manager.getDocumentsWithInstrumentsFlexible(false, clone);
						}
					}
					
					return result;
				}
			}
			
			@Override
			public String getInstrumentNoFromBookAndPage(InstrumentI state, HashMap<String, String> filterCriteria) {
				String book = state.getBook();
				String page = state.getPage();
				if (StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
					if (filterCriteria != null) {
						filterCriteria.put("Book", book);
						filterCriteria.put("Page", page);
					}
					return book + "/" + page;
				}
				return "";
			}

			@Override
			public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
				if (StringUtils.isNotEmpty(state.getInstno())) {
					if (filterCriteria != null) {
						filterCriteria.put("InstrumentNumber", state.getInstno());
					}
				}
				return state.getInstno();
			}
			
		};

		if (instrumentType) {
			instrumentGenericIterator.enableInstrumentNumber();
		} else {
			instrumentGenericIterator.enableBookPage();
			instrumentGenericIterator.setRemoveLeadingZerosBP(true);
	
		}
		instrumentGenericIterator.setDoNotCheckIfItExists(false);	
		
		return instrumentGenericIterator;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		
//		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter (searchId , 0.8d , true);		
//		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter (SearchAttributes.OWNER_OBJECT , searchId , module );
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		boolean isUpdate = (isUpdate()) || global.getSa().isDateDown();

		TSServerInfoModule m = null;

		FilterResponse rejectSavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId);
		FilterResponse defaultLegalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);
		((GenericLegal) defaultLegalFilter).setEnableLot(true);

		FilterResponse subdivisionNameFilter = NameFilterFactory.getDefaultNameFilterForSubdivision(searchId);
		subdivisionNameFilter.setThreshold(new BigDecimal(0.90d));

		{
			InstrumentGenericIterator instrumentGenericIterator = getInstrumentIteratorForFakeDocsFromDTG(true);

			if (!instrumentGenericIterator.createDerrivations().isEmpty()) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
		    			"Search again for Fake docs from DTG");
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
				if (isUpdate) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				module.addFilter(defaultLegalFilter);
				module.addFilter(subdivisionNameFilter);
				module.addIterator(instrumentGenericIterator);

				modules.add(module);
			}
		}
		{
			InstrumentGenericIterator bpGenericIterator = getInstrumentIteratorForFakeDocsFromDTG(false);
			bpGenericIterator.setRemoveLeadingZerosBP(true);

			if (!bpGenericIterator.createDerrivations().isEmpty()) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
		    			"Search again for Fake docs from DTG");
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
				module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
				
				if (isUpdate) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				module.addFilter(defaultLegalFilter);
				module.addFilter(subdivisionNameFilter);
				module.addIterator(bpGenericIterator);
				modules.add(module);
			}
		}
		
		//1 instrument list search from AO and TR for finding Legal
		//addAoAndTaxReferenceSearches(serverInfo, modules, allAoRef, searchId, isUpdate());
		InstrumentGenericIterator instrumentGenericIterator = null;
		InstrumentGenericIterator bpGenericIterator = null;
		{
			instrumentGenericIterator = getInstrumentIterator(true);

			if (!instrumentGenericIterator.createDerrivations().isEmpty()) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Search with references from AO/Tax like documents");
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
				if (isUpdate) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				//module.addFilter(genericMultipleLegalFilter);
				module.addIterator(instrumentGenericIterator);

				modules.add(module);
			}
		}
		{
			bpGenericIterator = getInstrumentIterator(false);
			bpGenericIterator.setRemoveLeadingZerosBP(true);

			if (!bpGenericIterator.createDerrivations().isEmpty()) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Search with references from AO/Tax like documents");
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
				module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
//				module.forceValue(0, "ALL");			
//				module.forceValue(5, "Search");
//				module.forceValue(6, "50");
				if (isUpdate) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				//module.addFilter(genericMultipleLegalFilter);
				module.addIterator(bpGenericIterator);
				modules.add(module);
			}
		}

//		//2 search with lot/block/subdivision from RO documents
//		addIteratorModule(serverInfo, modules, TSServerInfo.PROP_NO_IDX, searchId, false);
//		
//		//3 search with section/township/range from RO documents
//		addIteratorModuleSTR(serverInfo, modules, TSServerInfo.ADV_SEARCH_MODULE_IDX, searchId);

//		//4a search by subdivision name
//		String subdivisionName = ro.cst.tsearch.servers.functions.OHDelawareRO.correctSubdivisionName
//			(sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME));
//		subdivisionName = getSubdivisionCodeFromName(subdivisionName);
//		if (StringUtils.isNotEmpty(subdivisionName)) {
//			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PROP_NO_IDX));
//			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT);
//			m.clearSaKeys();
//			m.forceValue(0, subdivisionName);
//			m.setSaKey(4, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
//			m.setSaKey(5, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
//			m.addFilter(subdivisionNameFilter);
//			m.addValidator(subdivisionNameValidator);
//			m.addValidator(defaultLegalFilter.getValidator());
//			modules.add(m);
//		}
		

		ConfigurableNameIterator nameIterator = null;
		GenericNameFilter defaultNameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
		defaultNameFilter.setIgnoreMiddleOnEmpty(true);
		defaultNameFilter.setUseArrangements(false);
		defaultNameFilter.setInitAgain(true);

		//5a name modules with names from search page.
		if (hasOwner()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.clearSaKeys();
			m.setSaKey(6, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setSaKey(11, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);

			m.addFilter(rejectSavedDocuments);
			m.addFilter(defaultNameFilter);
			m.addFilter(new LastTransferDateFilter(searchId));
			addFilterForUpdate(m, true);

			m.addValidator(defaultLegalFilter.getValidator());
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			//m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE);
			m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			m.setIteratorType(6, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[] { "L;F;M", "L;F;" });
			m.addIterator(nameIterator);
			modules.add(m);
		}

		//5b search by buyers
		if (hasBuyer()
				&& !InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().isProductType(SearchAttributes.SEARCH_PROD_REFINANCE)) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS);
			m.setSaObjKey(SearchAttributes.BUYER_OBJECT);
			m.clearSaKeys();
			m.setSaKey(6, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setSaKey(11, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);

			FilterResponse nameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.BUYER_OBJECT, searchId, m);
			((GenericNameFilter) nameFilter).setIgnoreMiddleOnEmpty(true);
			((GenericNameFilter) nameFilter).setUseArrangements(false);
			((GenericNameFilter) nameFilter).setInitAgain(true);
			m.addFilter(rejectSavedDocuments);
			m.addFilter(nameFilter);
			addFilterForUpdate(m, true);

			m.addValidator(defaultLegalFilter.getValidator());
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			//m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE);
			m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			m.setIteratorType(6, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			ConfigurableNameIterator buyerNameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId,
					new String[] { "L;F;M", "L;F;" });
			buyerNameIterator.setAllowMcnPersons(false);
			m.addIterator(buyerNameIterator);
			modules.add(m);
		}

		//6 OCR last transfer - instrument search
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.clearSaKeys();
		m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
		m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
		m.addFilter(new GenericInstrumentFilter(searchId));
		m.addFilter(defaultLegalFilter);
		modules.add(m);

		//7 name module with names added by OCR
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
		m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
		m.clearSaKeys();

		m.addFilter(rejectSavedDocuments);
		m.addFilter(defaultNameFilter);
		m.addFilter(new LastTransferDateFilter(searchId));
		m.addValidator(defaultLegalFilter.getValidator());
		m.setSaKey(6, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		m.setSaKey(11, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
		m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
//		m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE);
		m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
		m.setIteratorType(6, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
		ArrayList<NameI> searchedNames = null;
		if (nameIterator != null) {
			searchedNames = nameIterator.getSearchedNames();
		} else {
			searchedNames = new ArrayList<NameI>();
		}
		nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[] { "L;F;M", "L;F;" });
		// get your values at runtime
		nameIterator.setInitAgain(true);
		nameIterator.setSearchedNames(searchedNames);
		m.addIterator(nameIterator);
		modules.add(m);
		
		serverInfo.setModulesForAutoSearch(modules);
	}
	
	public InstrumentGenericIterator getInstrumentIteratorForFakeDocsFromDTG(boolean instrumentType) {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId){

			private static final long serialVersionUID = 5399351945130601258L;

			@Override
			protected String cleanInstrumentNo(String instno, int year) {
				if (StringUtils.isNotEmpty(instno)) {
					if (year != -1) {
						String yearString = Integer.toString(year);
						String fullInstrForRO = yearString + "-";
						if (instno.length() < 8) {
							fullInstrForRO += StringUtils.leftPad(instno, 8, "0");
						} else if (instno.length() == 8) {
							fullInstrForRO += instno;
						}
						return fullInstrForRO;
						
					} else if (year == -1 && instno.length() == 12) {
						instno = instno.substring(0,4) + "-" + instno.substring(4); 
						return instno;
					} else {
						if (isLoadFromRoLike()) {
							return instno;
						} else {
							return "";
						}
					}
				} else {
					return "";
				}
			}

			@Override
			protected String cleanBook(String book) {
				if (StringUtils.isNotEmpty(book)){
					if (book.matches("(?is)[A-Z]\\d+")){
						return book.substring(1);
					}
				}
				return "";
			}
			
			@Override
			protected void processEnableInstrumentNo(List<InstrumentI> result,
					HashSet<String> listsForNow, DocumentsManagerI manager,
					InstrumentI instrumentI) {
				String instrumentNo = cleanInstrumentNo(instrumentI.getInstno(), instrumentI.getYear());
				
				if(org.apache.commons.lang.StringUtils.isBlank(instrumentNo)) {
					return;
				} else {
					if(instrumentI.hasBookPage() && instrumentI.hasInstrNo()) {
						instrumentI.setBook("");
						instrumentI.setPage("");
					}
				}
				
				super.processEnableInstrumentNo(result, listsForNow, manager, instrumentI);
				
			}
			
			@Override
			protected List<DocumentI> getDocumentsWithInstrumentsFlexible(DocumentsManagerI manager, InstrumentI instrumentI) {
				List<DocumentI> result = manager.getDocumentsWithInstrumentsFlexible(false, instrumentI);
				if (!result.isEmpty()) {
					return result;
				} else {
					String instno = instrumentI.getInstno();
					int year = instrumentI.getYear();
					String yearFromDate = instrumentI.getDate().toString().replaceFirst("(?is).*(\\d{4})$", "$1");
					boolean hasYear = year!=SimpleChapterUtils.UNDEFINED_YEAR || StringUtils.isNotEmpty(yearFromDate);
					
					//if (year!=SimpleChapterUtils.UNDEFINED_YEAR) {
					if (StringUtils.isNotEmpty(instno)) {
						if (hasYear) {
							String yearString = "";
							if (year!=SimpleChapterUtils.UNDEFINED_YEAR) {
								yearString = Integer.toString(year);
							} else {
								yearString = yearFromDate;
							}
							
							if (yearString.length() == 4 && instno.length() <=8) {
								String fullInstrno = StringUtils.leftPad(instno, 8, "0");
								fullInstrno = yearString + "-" + fullInstrno;
								
								InstrumentI clone = instrumentI.clone();
								clone.setInstno(fullInstrno);
								return manager.getDocumentsWithInstrumentsFlexible(false, clone);
							}
						}
					}
					
					return result;
				}
			}
			
			@Override
			public String getInstrumentNoFromBookAndPage(InstrumentI state, HashMap<String, String> filterCriteria) {
				String book = state.getBook();
				String page = state.getPage();
				if (StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
					if (filterCriteria != null) {
						filterCriteria.put("Book", book);
						filterCriteria.put("Page", page);
					}
					return book + "/" + page;
				}
				return "";
			}

			@Override
			public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
				if (StringUtils.isNotEmpty(state.getInstno())) {
					if (filterCriteria != null) {
						filterCriteria.put("InstrumentNumber", state.getInstno());
					}
				}
				return state.getInstno();
			}
			
		};

		if (instrumentType) {
			instrumentGenericIterator.enableInstrumentNumber();
		} else {
			instrumentGenericIterator.enableBookPage();
		}
		instrumentGenericIterator.setDoNotCheckIfItExists(true);
		instrumentGenericIterator.setCheckOnlyFakeDocs(true);
		instrumentGenericIterator.setLoadFromRoLike(true);
		instrumentGenericIterator.setDsToLoad(new String[]{"DG"});
		return instrumentGenericIterator;
	}
	
	// SearchLog particularizations
	@Override
	 public String getPrettyFollowedLink (String initialFollowedLnk){ 
	  if (initialFollowedLnk.contains("instNum=")){
	      String retStr =  "Instrument " + StringUtils.substringBetween(initialFollowedLnk, "instNum=", "&");
	      
	      if (initialFollowedLnk.contains("year=")){
	       retStr += ":" + StringUtils.substringBetween(initialFollowedLnk, "year=", "&");
	      }
	      
	      retStr += " has already been processed from a previous search in the log file.";
	      
	      return  "<br/><span class='followed'>" + retStr + "</span><br/>";
	     }
	     return "<br/><span class='followed'>Link already followed: </span>" + preProcessLink(initialFollowedLnk) + "<br/>";
	    }
	
	public String[] getSubdivisionValues(String legalDescType, String[] subdivs) {
		String[] results = {"","[]"};
		if (subdivs != null) {
			for (int i=0; i<subdivs.length; i++) {
				if ("City".equals(legalDescType)) {
					results[0] = "City";
					results[1] += org.apache.commons.lang.StringUtils.defaultString(getCities().get(subdivs[i]));
				} else if ("Sub".equals(legalDescType)) {
					results[0] = "Subdivision";
					results[1] += org.apache.commons.lang.StringUtils.defaultString(getSubdivisions().get(subdivs[i]));
				} else if ("Township".equals(legalDescType)) {
					results[0] = "Township";
					results[1] += org.apache.commons.lang.StringUtils.defaultString(getTownships().get(subdivs[i]));
				}
				
				if (i != subdivs.length - 1) {
					results[1] += "; ";
				}
			}
		}
		
		if (results[1].length() > 2) {
			results[1] = results[1].replaceFirst("(?is)\\]", "");
			results[1] += "]" ;
		}
		
		return results;
	}
	
	private String getMultipleParamsValues(String[] paramValues) {
		if (paramValues != null) {
			String results = "";
			for (int i=0; i<paramValues.length; i++) {
				results += paramValues[i];
				if (i != paramValues.length - 1) {
					results += "; ";
				}
			}
			return results;
		} 
		
		return null;
	}
	
	//@Override
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
			
			String propType = "";
			String prefix 		= "ctl00$cphMain$tcMain$tpNewSearch$";
			String propKeyword	= "ucSrchProperty$";
			String quickNameKeyword = "ucSrchNames$";
			String advKeyword 	= "ucSrchAdvName$";
			String keyIndexTypeOff 	= "Index Types (2):";
			String keyIndexTypeUcc 	= prefix;
			String keyWildcardLast 	= prefix;
			String keyWildcardFirst = prefix;
			String keySubdiv = prefix;
			String keyDoctype = prefix + advKeyword + "repGroups$ctl";
			String keyKinds = prefix + advKeyword + "repKinds$ctl";
			boolean isPropertySrchModule = false;
			boolean isAdvSrchModule = false;
			boolean isQuickNameSrchModule = false;
			boolean isOFFchecked = false;
			boolean isUCCchecked = false;
			String[] selectedSubdiv = {};
			String selectedDoctypes = "[]";
			String selectedKinds = "[]";
			
			if (moduleParams.containsKey("Property Type")) {
				propType = moduleParams.get("Property Type");
				
				if ("Property Search".equals(module.getLabel())) {
					isPropertySrchModule = true;
				} else if ("Advanced Name Search".equals(module.getLabel())) {
					isAdvSrchModule = true;
				}
				
				if (isPropertySrchModule || isAdvSrchModule) {
					String[] subdValues = {};
//					String[] doctypeValues = {};
//					String[] kindValues = {};
					
					if (isPropertySrchModule) {
						keySubdiv += propKeyword + "lbSubdivisions";
						keyWildcardLast	+= propKeyword + "ddlWildcardLast";
						keyWildcardFirst += propKeyword + "ddlWildcardFirst";
						keyIndexTypeUcc += propKeyword + "repIndexTypes$ctl02$cbIndexType";
					} else if (isAdvSrchModule) {
						keySubdiv += advKeyword + "lbSubdivisions";
						keyWildcardLast	+= advKeyword + "ddlWildcardLast";
						keyWildcardFirst += advKeyword + "ddlWildcardFirst";
						keyIndexTypeUcc += advKeyword + "repIndexTypes$ctl02$cbIndexType";
					}
					
					List<TSServerInfoFunction> moduleFunctions = module.getFunctionList();
					for (Iterator<TSServerInfoFunction> it = moduleFunctions.iterator(); it.hasNext(); ) {
						TSServerInfoFunction fct = it.next();
						if (keySubdiv.equals(fct.getParamName())) {
							if (subdValues.length == 0) {
								subdValues = fct.getParamValues();
							}
						} else if (isAdvSrchModule) {
							if (keyIndexTypeUcc.equals(fct.getParamName()) && fct.getParamValue().equals("on")) {
								isUCCchecked = true;
							} else if ("ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$repIndexTypes$ctl00$cbIndexType".equals(fct.getParamName()) && fct.getParamValue().equals("on")) {
								isOFFchecked = true;
							} else if (fct.getParamName().contains(keyDoctype)) {
								selectedDoctypes = selectedDoctypes.replaceFirst("(?is)\\]", "");
								selectedDoctypes += getDoctypeName(fct.getParamName()) + "; ";
							} else if (fct.getParamName().contains(keyKinds)) {
								selectedKinds = selectedKinds.replaceFirst("(?is)\\]", "");
								//selectedKinds += getKindName(fct.getParamName()) + "; ";
								selectedKinds += getKindName(fct.getParamName(), isOFFchecked, isUCCchecked) + "; ";
 							}
						}
					}
					
					if (subdValues != null) {
						selectedSubdiv = getSubdivisionValues(propType, subdValues);
					}
					if (isAdvSrchModule) {
						int idx = 0;
						if (!selectedDoctypes.contains("]")) {
							idx = selectedDoctypes.lastIndexOf(";");
							selectedDoctypes = selectedDoctypes.substring(0,idx) + "]";
						}
						if (!selectedKinds.contains("]")) {
							idx = selectedKinds.lastIndexOf(";");
							selectedKinds = selectedKinds.substring(0,idx) + "]";
						}
					}
				} 
			
			} else if ("Quick Name Search".equals(module.getLabel())) {
				keyWildcardLast	+= quickNameKeyword + "ddlWildcardLast";
				keyWildcardFirst += quickNameKeyword + "ddlWildcardFirst";
				keyIndexTypeUcc += quickNameKeyword + "repIndexTypes$ctl02$cbIndexType";
				isQuickNameSrchModule = true;
			}
			
			for (Entry<String, String> entry : moduleParams.entrySet()) {
				String value = entry.getValue();
				String key = entry.getKey();
				if (isPropertySrchModule || isAdvSrchModule || isQuickNameSrchModule) {
					if (keyWildcardLast.equals(key)) {
						key = "Wildcard Last name";
					} else if (keyWildcardFirst.equals(key)) {
						key = "Wildcard First name";
					} else if ((prefix + propKeyword +"ddlSortDir").equals(key)) {
						key = "Sort";
					} else if (keyIndexTypeOff.equals(key)) {
						key = "Index Type - OFFICIAL RECORDS";
					} else if (keyIndexTypeUcc.equals(key)) {
						key = "Index Type - UCC PRE 2005";
					} else if (keySubdiv.equals(key)) {
						if (selectedSubdiv != null && StringUtils.isNotEmpty(selectedSubdiv[0]) && StringUtils.isNotEmpty(selectedSubdiv[1])) {
							key = selectedSubdiv[0];
							value = selectedSubdiv[1];
						}
					}
					if (isAdvSrchModule) {
						if (key.contains("Kind Groups")) {
							if (selectedDoctypes != null && StringUtils.isNotEmpty(selectedDoctypes)) {
								key = "Kind Groups";
								value = selectedDoctypes;
							}
						} else if (key.contains("Kinds")) {
							if (selectedKinds != null && StringUtils.isNotEmpty(selectedKinds)) {
								key = "Kinds";
								value = selectedKinds;
							}	
						}
					}
				} 
				
				if (!firstTime) {
					sb.append(", ");
				} else {
					firstTime = false;
				}
				sb.append(key + " = <b>" + value + "</b>");
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
	
	
	//for Save with Search Parameters - salvare informatii de Nume si de Legal 
	 @Override
	 protected NameI getNameFromModule(TSServerInfoModule module){
		 NameI name = new Name();
		 if (module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX && module.getFunctionCount() > 1) {
			 String last = module.getFunction(0).getParamValue();
			 String first = module.getFunction(2).getParamValue();
			 
			 if (StringUtils.isEmpty(last)){
				 return null;
			 }

			 name.setLastName(last);
			 name.setFirstName(first);
			 return name;
			 
		 } else if (module.getModuleIdx() == TSServerInfo.ADV_SEARCH_MODULE_IDX && module.getFunctionCount() > 1) {
			 String last = module.getFunction(0).getParamValue();
			 String first = module.getFunction(2).getParamValue();
			 String middle = module.getFunction(4).getParamValue();
			 
			 if (StringUtils.isEmpty(last)){
				 return null;
			 }

			 name.setLastName(last);
			 name.setFirstName(first);
			 name.setMiddleName(middle);
			 return name;
		 }
		 
		 return null;
	}

	 @Override
	 protected LegalI getLegalFromModule(TSServerInfoModule module){
		 LegalI legal = null;
		 SubdivisionI subdivision = null;
		 TownShipI townShip = null;
			
		 if (module.getModuleIdx() == TSServerInfo.PROP_NO_IDX && module.getFunctionCount() > 12) {
//			 subdivision = new Subdivision();
//			 String subdivisionName = module.getFunction(12).getParamValue().trim();
//			 subdivision.setName(subdivisionName);
//			 subdivision.setLot(module.getFunction(2).getParamValue().trim());
//			 subdivision.setBlock(module.getFunction(3).getParamValue().trim());
//			 subdivision.setPlatBook(module.getFunction(4).getParamValue().trim());
//			 subdivision.setPlatPage(module.getFunction(5).getParamValue().trim());
			 townShip = new TownShip();
			 townShip.setSection(module.getFunction(10).getParamValue().trim());
			 townShip.setTownship(module.getFunction(11).getParamValue().trim());
			 townShip.setRange(module.getFunction(12).getParamValue().trim());
		 }
		 
		 if (module.getModuleIdx() == TSServerInfo.ADV_SEARCH_MODULE_IDX && module.getFunctionCount() > 24){
			 townShip = new TownShip();
				
			 townShip.setSection(module.getFunction(22).getParamValue().trim());
			 townShip.setTownship(module.getFunction(23).getParamValue().trim());
			 townShip.setRange(module.getFunction(24).getParamValue().trim());
			
		 }
//		 if (subdivision != null){
//			 legal = new Legal();
//			 legal.setSubdivision(subdivision);
//		 }
//		 
		 if (townShip != null){
			 if (legal == null){
				 legal = new Legal();
			 }
			 legal.setTownShip(townShip);
		 }
		 
		 return legal;
	}
	 
	@Override
	public Object getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if(restoreDocumentDataI == null) {
			return null;
		}
		
		List<TSServerInfoModule> list = new ArrayList<TSServerInfoModule>();
		
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		TSServerInfoModule module = null;
		
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(book) && ro.cst.tsearch.utils.StringUtils.isNotEmpty(page)) {
			module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
			module.forceValue(1, book);
			module.forceValue(2, page);
			module.forceValue(0, "ALL");			
			module.forceValue(5, "Search");
			module.forceValue(6, "50");
			list.add(module);
		}
		
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(restoreDocumentDataI.getInstrumentNumber()) 
				&& restoreDocumentDataI.getRecordedDate() != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(restoreDocumentDataI.getRecordedDate());
			
			module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
			module.forceValue(0, restoreDocumentDataI.getInstrumentNumber());
			module.forceValue(1, "ALL");			
			module.forceValue(3, "Search");
			module.forceValue(4, "50");
			list.add(module);
		}
		
		module = getDefaultServerInfo().getModule(TSServerInfo.FAKE_MODULE_IDX);
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_RESTORE_DOCUMENT_SOURCE, restoreDocumentDataI);
		list.add(module);
		
		return list;
	}
	
	
}
