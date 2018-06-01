package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.io.File;
import java.math.BigDecimal;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.ExactDateFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotInterval;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotMatchAlgorithm;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.CrossRefSet;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.templates.MultilineElementsMap;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.Subdivision;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShip;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

@SuppressWarnings("deprecation")
public class ARWashingtonRO extends TSServerROLike {

	private static final long serialVersionUID = -3971061795885691551L;
	
	private static final String[] ERROR_LIST = {"When using a 'Begins With' search, the minimum text that must be entered is 2 characters.",
											    "When using a 'Contains' search, the minimum text that must be entered is 3 characters.",
											    "When using a 'Exactly' search, the minimum text that must be entered is 2 characters.",
											    "When using a 'Sounds Like' search, the minimum text that must be entered is 3 characters.",
											    "You must key in at least one set of criteria in the Property Search.",
											    "We're very sorry, but an unexpected error has occurred.",
												"Your search did not return any results."};
	
	private static Map<String, String> TYPE_MAP = new HashMap<String, String>();
	static {
		TYPE_MAP.put("CHA", "CHANCERY CASES");
		TYPE_MAP.put("CIV", "CIVIL CASES");
		TYPE_MAP.put("CRI", "CRIMINAL CASES");
		TYPE_MAP.put("LIE", "LIENS");
		TYPE_MAP.put("MIS", "MISCELLANEOUS");
		TYPE_MAP.put("REL", "REAL ESTATE");
		TYPE_MAP.put("FIN", "UCC");
	}
	
	//for Subdivision and Condominium comboboxes
	private static String SUBDIVISION_SELECT = "";
	private static String CONDOMINIUM_SELECT = "";
	
	static {
		String folderPath = ServerConfig
				.getModuleDescriptionFolder(BaseServlet.REAL_PATH
						+ "WEB-INF/classes/resource/module/comments/");
		File folder = new File(folderPath);
		if (!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath
					+ "] does not exist. Module Information not loaded!");
		}
		try {
			String select1 = org.apache.commons.io.FileUtils.readFileToString(new File(folderPath
					+ File.separator + "ARWashingtonROSubdivision.xml"));
			SUBDIVISION_SELECT = select1;
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			String select2 = org.apache.commons.io.FileUtils.readFileToString(new File(folderPath
					+ File.separator + "ARWashingtonROCondominium.xml"));
			CONDOMINIUM_SELECT = select2;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	private static List<String> SUBDIVISION_NAME_LIST = new ArrayList<String>();
	private static final String OPTION_PATTERN = "(?is)<option\\s+value=\"([^\"]+)\"\\s*>([^<]+)</option>";
	private static Map<String, String> SUBDIVISION_NAME_MAP = new HashMap<String, String>();
	private static Map<String, String> CONDOMINIUM_NAME_MAP = new HashMap<String, String>();
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
		Matcher matcher2 = Pattern.compile(OPTION_PATTERN).matcher(CONDOMINIUM_SELECT);
		while (matcher2.find()) {
			String condominium_name = matcher2.group(2);
			condominium_name = condominium_name.replaceAll("(?is)&amp;", "&");
			SUBDIVISION_NAME_LIST.add(condominium_name);
			condominium_name = condominium_name.replaceAll("\\s{2,}", " ");
			condominium_name = condominium_name.trim().toUpperCase();
			CONDOMINIUM_NAME_MAP.put(matcher2.group(1), condominium_name);
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
	
	public static String getCondominiumCodeFromName(String name) {
		for (Map.Entry<String, String> entry : CONDOMINIUM_NAME_MAP.entrySet()) {
			if (name.equals(entry.getValue())) {
	        	return entry.getKey();
	        }
		}
		return "";
	}
	
	private static CachedDate fromDate = null; 

	public ARWashingtonRO(long searchId) {
		super(searchId);
	}

	public ARWashingtonRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {

		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule1 = msiServerInfoDefault
				.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX);

		if (tsServerInfoModule1 != null) {
			setupSelectBox(tsServerInfoModule1.getFunction(0), SUBDIVISION_SELECT);
		}
		
		TSServerInfoModule tsServerInfoModule2 = msiServerInfoDefault
			.getModule(TSServerInfo.CONDOMIN_MODULE_IDX);
		if (tsServerInfoModule2 != null) {
			setupSelectBox(tsServerInfoModule2.getFunction(0), CONDOMINIUM_SELECT);
		}

		setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		for (int i=0;i<4;i++) {
			if (rsResponse.indexOf(ERROR_LIST[i]) > -1) {
				Response.getParsedResponse().setError(ERROR_LIST[i]);
				return;
			}
		}
		
		if (rsResponse.indexOf(ERROR_LIST[4]) > -1) {
			Response.getParsedResponse().setError("You must key in at least one of: Properties, Block, Lot.");
			return;
		}
		
		if (rsResponse.indexOf(ERROR_LIST[5]) > -1) {
			Response.getParsedResponse().setError("Official site error!");
			Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
			return;
		}
		
		if (rsResponse.indexOf(ERROR_LIST[6]) > -1) {
			Response.getParsedResponse().setError("No data found.");
			Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
			return;
		}
		
		if (rsResponse.indexOf("Error while downloading the image") > -1) {
			Response.getParsedResponse().setError("Error while downloading the image!");
			return;
		}
		
		switch (viParseID) {
			
			case ID_SEARCH_BY_NAME:					//name
			case ID_SEARCH_BY_INSTRUMENT_NO:		//file number
			case ID_SEARCH_BY_SUBDIVISION_NAME:		//property->subdivision
			case ID_SEARCH_BY_CONDO_NAME:			//property->condominium
			case ID_SEARCH_BY_MODULE20:				//property->township

				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				
				if(smartParsedResponses.size() > 0) {
					
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
					
		            if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
		            	String header = parsedResponse.getHeader();
		               	String footer = parsedResponse.getFooter();
		               	String navigationLinks = getNavigationLinks(Response, sAction);
		            	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
		            	header += makeHeader(rsResponse, navigationLinks);
		            	
		            	Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
		            	footer = makeTableHeader(rsResponse).replaceFirst("(?is)<input[^>]*>", "") + "\n</table><br>" + navigationLinks;
		            	if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
		            		footer += CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer) numberOfUnsavedDocument);
		            	} else {
		            		footer += CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
		            	}
		            	
		            	parsedResponse.setHeader(header);
		            	parsedResponse.setFooter(footer);
		            }
					
				}
	
			break;

			case ID_DETAILS:
			
				DocumentI document = parsedResponse.getDocument();
				
				if(document!= null) {
					msSaveToTSDFileName = document.getInstno() + getRecordedDate(document) + ".html";
					Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
					msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();
				}
										
			break;
			
			case ID_SAVE_TO_TSD:
				
				document = parsedResponse.getDocument();
				
				if(document!= null) {
					msSaveToTSDFileName = document.getInstno() + getRecordedDate(document) + ".html";
					Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
					msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();
					addLinksInstrumentNumber(parsedResponse);
				} else {
					ParseResponse(sAction, Response, ID_DETAILS);
				}
				
			break;
				
			case ID_GET_LINK:
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			break;
				
		}
	}
	
	private String getRecordedDate(DocumentI document) {
		String recordedDate = "";
		
		if (document instanceof RegisterDocumentI) {
			RegisterDocumentI registerDocument = (RegisterDocumentI)document;
			Date date = registerDocument.getRecordedDate();
			if (date!=null) {
				DateFormat formatter = new SimpleDateFormat("MM-dd-yyyy");
				recordedDate = "_" + formatter.format(date);
			}
		}	
		
		return recordedDate;
	}
	
	public static int getFromYear() {
		int fromYear = 0;
		if (fromDate!=null && fromDate.getTstamp().get(Calendar.DAY_OF_MONTH) == Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) {
			try {
				fromYear = Integer.parseInt(fromDate.getValue());
			} catch (NumberFormatException nfe) {
				fromYear = 0;
			}
		}
		return fromYear;
	}

	private String makeHeader(String response, String navigationLinks) {
		String header = "";

		Matcher matcher = Pattern.compile("(?is)<caption[^>]*>(.*?)</caption>").matcher(response);
		if (matcher.find()) {
			header += "<div align=\"left\">" + matcher.group(1).replaceFirst("(?is)\\bat\\b.*", "").trim() + "</div><br>";
		}
		header += navigationLinks;
		
		header += "<table cellspacing=\"0\" border=\"1\" width=\"100%\" align=\"center\">";
		
		header += makeTableHeader(response);

		return header;
	}
	
	private String makeTableHeader(String response) {
		
		return "<tr bgcolor=\"#6699CC\"><th>&nbsp;</th><th>" +
			SELECT_ALL_CHECKBOXES + "</th>" +
			makePartialHeader(response, 0) + "</tr>";
	}
	
	private String makePartialHeader(String response, int omitLastColumns) {
		
		StringBuilder header = new StringBuilder();
		
		try {
			
			org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(response, null);
			NodeList nodeList = parser.parse(null);
			NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMain_lrrgResults_cgvResults"), true);
			
			if (mainTableList.size()>0) {
				
				TableTag mainTable = (TableTag)mainTableList.elementAt(0);
				if (mainTable.getRowCount()>1) {
					
					TableRow row = (TableRow)mainTable.getRows()[1];
					Node[] children = row.getChildrenAsNodeArray();
					
					if (children.length>2+omitLastColumns) {
						
						for (int i=3;i<children.length-(2+omitLastColumns);i++) {
							header.append("<th align=\"center\">")
								.append(children[i].toPlainTextString().replaceAll("(\\w)(\\()", "$1<br>$2"))
								.append("</th>");
						}
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return header.toString();
		
	}

	private String getNavigationLinks(ServerResponse resp, String sAction) {

		String response = resp.getResult();

		String prev_next = "\n<table id=\"prev_next_links\"><tr>";
		try {
			org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(response, null);
			NodeList nodeList = parser.parse(null);
			
			nodeList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "0"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("border", "0"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("cellspacing", "0"), true);
			
			if (nodeList.size() > 1) {

				//empty table
				TableTag table = (TableTag)nodeList.elementAt(1);
				if (table.getRowCount()==1) {
					TableRow row = table.getRows()[0];
					if (row.getColumnCount()==1) {
						if (row.getColumns()[0].toPlainTextString().trim().length()==0) {
							return "";
						}
					}
				}
				
				prev_next += table.toHtml().replaceAll("(?is)</td>", "&nbsp;</td>");
				
				String address = "/eSearch/LandRecords/protected/";
				if (sAction.contains("SrchQuickName.aspx")) {
					address += "SrchQuickName.aspx";
				} else if (sAction.contains("SrchInstNumber.aspx")) {
					address += "SrchInstNumber.aspx";
				} else if (sAction.contains("SrchQuickProperty.aspx")) {
					address += "SrchQuickProperty.aspx";
				}
				
				prev_next = prev_next.replaceAll("(?is)href=\"javascript:__doPostBack\\('([^']*)','([^']*)'\\)\"",
						"href=\"" +  CreatePartialLink(TSConnectionURL.idPOST) + address + "&param=prevnext&__EVENTTARGET=$1&__EVENTARGUMENT=$2\"");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		prev_next += "</tr></table>\n<hr>";
		return prev_next;
	}

	private static String getTypeFromMap(String type){
		
		String result = TYPE_MAP.get(type);
		if (StringUtils.isEmpty(result)) {
			result = "";
		}
		return result;
	}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		LinkedHashMap<String, ParsedResponse> responses = new LinkedHashMap<String, ParsedResponse>();
		StringBuilder newTable = new StringBuilder();
		newTable.append("<table width=\"100%\"");
		int numberOfUncheckedElements = 0;
		
		try {
			org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = parser.parse(null);
			NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMain_lrrgResults_cgvResults"), true);
			
			if (mainTableList.size()>0) {
				
				TableTag mainTable = (TableTag)mainTableList.elementAt(0);
				TableRow[] rows = mainTable.getRows();
				
				//header for details page omits the last column (Images)
				String tableHeader = "<tr>" + makePartialHeader(response.getResult(), 1) + "</tr>";
				
				newTable.append(tableHeader);
			
				for (int i = 2; i < rows.length-2; i++) {
					
					TableColumn[] columns = rows[i].getColumns();
					
					String index = columns[2].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim();
					String dateFiled = columns[3].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim();
					String kind = columns[4].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim();
					String grantorPartiesOne = columns[5].toHtml();
					String granteesPartiesTwo = columns[6].toHtml();
					String description = columns[7].toHtml();
					String fileNumber = columns[8].toPlainTextString().replaceAll("(?is)&nbsp;", "").replaceAll("\\s", "");
					//leading zeroes from the beginning have importance
					//e.g. 0097-2385 versus 97-2385
					//only leading zeroes after a dash are removed
					//e.g. 97-2385 = 97-00002385
					fileNumber = fileNumber.replaceAll("-0+", "-");
					//just in case, so far I haven't found any document with book/page
					//String bookPage = columns[9].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim();
					
					//some documents have the same file number
					//to differentiate them, recorded date is appended
					String fileNumberExtended = fileNumber + "_" + dateFiled.replaceAll("/", "-");
					String type = kind;
					if (StringUtils.isEmpty(type)) {
						type = getTypeFromMap(index);
					}
					
					ParsedResponse currentResponse = new ParsedResponse();
					responses.put(fileNumberExtended, currentResponse);
					ResultMap resultMap = new ResultMap();
							
					String imageLink = CreatePartialLink(HTTPRequest.POST) + response.getLastURI();
					String imageColumn = columns[10].toHtml();
					imageColumn = imageColumn.replaceAll("(?is)<a\\s+disabled=\"disabled\">([^<]*)</a>", "$1");
					String imagePattern = "(?is)href=\"javascript:__doPostBack\\('([^']+)','([^']+)'\\)\"";
					String eventTarget = RegExUtils.getFirstMatch(imagePattern, imageColumn, 1);
					String eventArgument = RegExUtils.getFirstMatch(imagePattern, imageColumn, 2);
					String hiddenField = RegExUtils.getFirstMatch("(?is)_TSM_CombinedScripts_=([^\"]+)\"", response.getResult(), 1);
					imageLink += "?ctl00$smScriptMan=" + eventTarget + "|ctl00$cphMain$lrrgResults$cgvResults" +
						"&ctl00_smScriptMan_HiddenField=" +  hiddenField + 
						"&__EVENTTARGET=" + eventTarget + "&__EVENTARGUMENT=" + eventArgument + 
						"&__LASTFOCUS=&__VIEWSTATE=&__SCROLLPOSITIONX=0&__SCROLLPOSITIONY=0" +
						"&__VIEWSTATEENCRYPTED=&ctl00$ddlCountySelect=-1&ctl00$txtJobReference=" +
						"&param=image";
					imageLink = imageLink.replace("$", "\\$");	//to avoid taking $ from link as group reference
					imageColumn = imageColumn.replaceAll("(?is)<a\\s+href=\"[^\"]+\">", "<a href=\"" + imageLink + "\">");
					
					if (imageColumn.toLowerCase().contains("<a")) {
						currentResponse.addImageLink(new ImageLinkInPage(imageLink, fileNumber+ ".tif"));
					}
					
					String column2 = columns[2].toHtml();
					String responseHtml = columns[3].toHtml() + 
										  columns[4].toHtml() +
										  columns[5].toHtml() +
										  columns[6].toHtml() +
										  columns[7].toHtml() + 
										  columns[8].toHtml().replaceAll("(?is)<a[^>]*>([^<]*)</a>", "$1") +	//File/Case Number
										  columns[9].toHtml();
					String responseHtmlInterm = "<tr>" +  columns[0].toHtml() + columns[1].toHtml() + 
					                            column2 + responseHtml + 
					                            imageColumn + "</tr>";
					column2 = column2.replaceFirst("(?is)(<td[^>]*>)[^<]*(</td>)", "$1" + getTypeFromMap(index) + "$2");
					String responseHtmlDetails = "<tr valign=\"top\">" + column2 + responseHtml + "</tr>";
					responseHtmlDetails = responseHtmlDetails.replaceAll("(?is)<div\\s+title='[^']+'>", "<div>");
					responseHtmlDetails = responseHtmlDetails.replaceAll("(?is)<a[^>]*>\\[[\\+-]\\]</a>", "");
					
					resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO");
					resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), dateFiled);
					resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), type);
				   	resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), fileNumber);
				   						    				
					Pattern pattern = Pattern.compile("(?is)<td[^>]*>([^<]+)</td>");
				   	
					StringBuilder grantor = new StringBuilder();
					grantorPartiesOne = grantorPartiesOne.replaceAll("(?is)</?b>", "");
					Matcher matcher1 = pattern.matcher(grantorPartiesOne);
				   	while (matcher1.find()) {
				   		grantor.append(matcher1.group(1)).append("<br>");
				   	}
				   	resultMap.put("tmpGrantor", grantor.toString());
				   	
				   	StringBuilder grantee = new StringBuilder();
				   	granteesPartiesTwo = granteesPartiesTwo.replaceAll("(?is)</?b>", "");
					Matcher matcher2 = pattern.matcher(granteesPartiesTwo);
				   	while (matcher2.find()) {
				   		grantee.append(matcher2.group(1)).append("<br>");
				   	}
				   	resultMap.put("tmpGrantee", grantee.toString());
				   	
				   	StringBuilder legal = new StringBuilder();
				   	description = description.replaceAll("(?is)</?b>", "");
					Matcher matcher3 = pattern.matcher(description);
				   	while (matcher3.find()) {
				   		legal.append(matcher3.group(1)).append("<br>");
				   	}
				   	resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legal.toString());
				   	
				   	ro.cst.tsearch.servers.functions.ARWashingtonRO.parseNames(resultMap);
				   	ro.cst.tsearch.servers.functions.ARWashingtonRO.parseLegal("", resultMap);
				   	
				   	Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
															
					//details page
				   	currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<table BORDER='1' CELLPADDING='2'>" +
							tableHeader + responseHtmlDetails.replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1") + "</table>");
					RegisterDocumentI document = (RegisterDocumentI)bridge.importData();
					currentResponse.setDocument(document);
							
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("instrno", fileNumber);
					data.put("type", type);
					
					String checkBox = "checked";
					if (isInstrumentSaved(fileNumber, document, data, true) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
						checkBox = "saved";
					} else {
						numberOfUncheckedElements++;
						String linkPrefix = getLinkPrefix(TSConnectionURL.idPOST);
						LinkInPage linkInPage = new LinkInPage(
								linkPrefix + fileNumberExtended, 
								linkPrefix + fileNumberExtended, 
								TSServer.REQUEST_SAVE_TO_TSD);
						checkBox = "<input type='checkbox' name='docLink' value='" + 
							linkPrefix + fileNumberExtended + "'>";
					   	
						if(getSearch().getInMemoryDoc(linkPrefix + fileNumberExtended)==null){
							getSearch().addInMemoryDoc(linkPrefix + fileNumberExtended, currentResponse);
						}
						currentResponse.setPageLink(linkInPage);
				           						    			
				 	}
					
					responseHtmlInterm = responseHtmlInterm.replaceFirst("(?is)<td[^>]*>\\s*<input[^>]*>\\s*</td>", "<td align='center'>" + checkBox + "</td>");
					responseHtmlInterm = responseHtmlInterm.replaceAll("(?is)title='Collapsed'", "").replaceAll("(?is)title='Expanded'", "");
					responseHtmlInterm = responseHtmlInterm.replaceAll("(?is)<a[^>]*>\\[[-\\+]\\]</a>", "");
					responseHtmlInterm = responseHtmlInterm.replaceAll("(?is)(<td[^>]*)(>)", "$1 valign='top'$2");
					
					currentResponse.setOnlyResponse(responseHtmlInterm);
					newTable.append(responseHtmlInterm);
				}
				
			}
				
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}		
		
		newTable.append("</table>");
		outputTable.append(newTable);
		SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
		return responses.values();
	}
	
	private void addLinksInstrumentNumber(ParsedResponse pr) {
		int moduleIdx = TSServerInfo.INSTR_NO_MODULE_IDX;

		for (int i = 0; i < pr.getCrossRefSetCount(); i++) {

			CrossRefSet crs = pr.getCrossRefSet(i);
			String instrumentNumber = crs.getAtribute(CrossRefSetKey.INSTRUMENT_NUMBER.getShortKeyName());
			if (!StringUtils.isStringBlank(instrumentNumber)) {
				
				TSServerInfoModule module = getDefaultServerInfo()
						.getModuleForSearch(moduleIdx, new SearchDataWrapper());
				module.getFunction(0).setData("ALL");
				module.getFunction(1).setData(instrumentNumber);
				module.getFunction(2).setData("");
				module.getFunction(3).setData("Search");
				module.getFunction(4).setData("-1");

				ParsedResponse prChild = new ParsedResponse();
				LinkInPage linkInPage = new LinkInPage(module, TSServer.REQUEST_SEARCH_BY_REC);
				linkInPage.setOnlyLink(instrumentNumber + ".html");
				prChild.setPageLink(linkInPage);
				pr.addOneResultRowOnly(prChild);
			}
		}
	}
	
	@Override
	protected DownloadImageResult saveImage(ImageI image) throws ServerResponseException {
		
		String link = image.getLink(0);

		byte[] imageBytes = null;

		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		try {
			imageBytes = ((ro.cst.tsearch.connection.http2.ARWashingtonRO)site).getImage(link);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager.releaseSite(site);
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
			FileUtils.writeByteArrayToFile(resp.getImageResult().getImageContent(), image.getPath());
		}

		DownloadImageResult dres = resp.getImageResult();

		return dres;
	}

	@Override
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId){
    	
		if(StringUtils.isEmpty(instrumentNo))
    		return false;
    	
    	DocumentsManagerI documentManager = getSearch().getDocManager();
    	try {
    		documentManager.getAccess();
    		if(documentToCheck != null) {
    			DocumentI documentFromTSRI = documentManager.getDocument(documentToCheck.getInstrument());
    			if(documentFromTSRI!= null && getRecordedDate(documentToCheck).equals(getRecordedDate(documentFromTSRI)))
    				return true;
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
	    		
	    		if(documentManager.getDocument(instr) != null) {
	    			return true;
	    		} else {
	    			List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, instr);
	    			
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
			    	    	String dataSource = data.get("dataSource");
			    	    	for (DocumentI documentI : almostLike) {
			    	    		if (serverDocType.equals("ASSESSOR") && dataSource != null) {
									if(documentI.getDocType().equals(docCateg) && documentI.getDataSource().equals(dataSource))//B 4435, must save NDB and ISI doc of the same instrNo
										return true;
			    	    		} else if (serverDocType.equals("CNTYTAX") && dataSource != null) {
			    	    			if(documentI.getDocType().equals(docCateg) && documentI.getDataSource().equals(dataSource))
										return true;
			    	    		} else if (serverDocType.equals("CITYTAX") && dataSource != null) {
			    	    			if(documentI.getDocType().equals(docCateg) && documentI.getDataSource().equals(dataSource))
										return true;
			    	    		}else if( (!checkMiServerId || miServerID==documentI.getSiteId()) && documentI.getDocType().equals(docCateg)){
									return true;
			    	    		}
							}	
    					}
		    		} else {
		    			EmailClient email = new EmailClient();
		    			email.addTo(MailConfig.getExceptionEmail());
		    			email.setSubject("isInstrumentNoSaved problem on " + URLMaping.INSTANCE_DIR + this.getClass().getName());
		    			email.addContent("We should at least have type!!!!\nSearchId=" + searchId);
		    			email.sendAsynchronous();
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
	
	@Override
	protected void setCertificationDate() {
		try {
			
			if (CertificationDateManager.isCertificationDateInCache(dataSite)){
				String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
				
				return;
			}
			
			String html = "";
			HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			try {
				html = ((ro.cst.tsearch.connection.http2.ARWashingtonRO)site).getCertificationDate();
			} catch (RuntimeException e) {
				e.printStackTrace();
			} finally {
				HttpManager.releaseSite(site);
			}

			if (StringUtils.isNotEmpty(html)) {
				try {
					NodeList mainList = new HtmlParser3(html).getNodeList();

					mainList = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMain_gvStatsLandRecords"));
					if (mainList.size() > 0) {
						TableTag t = (TableTag) mainList.elementAt(0);
						DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
						String currentFromDateString = "";
						Date currentFromDate = null;
						String certificationDateString = "";
						Date certificationDate = null;
						int rowCount = t.getRowCount();
						if (rowCount>1) {
							currentFromDateString = t.getRow(1).getColumns()[1].toPlainTextString();
							currentFromDate = dateFormat.parse(currentFromDateString);
							certificationDateString = t.getRow(1).getColumns()[2].toPlainTextString();
							certificationDate = dateFormat.parse(certificationDateString);
						}
						for (int i=2;i<rowCount;i++) {
							String newFromDateString = t.getRow(i).getColumns()[1].toPlainTextString();
							Date newFromDate = dateFormat.parse(newFromDateString);
							if (newFromDate.before(currentFromDate)) {
								currentFromDate = newFromDate;
								currentFromDateString = newFromDateString;
							}
							String newCertificationDateString = t.getRow(i).getColumns()[2].toPlainTextString();
							Date newCertificationDate = dateFormat.parse(newCertificationDateString);
							if (newCertificationDate.before(certificationDate)) {
								certificationDate = newCertificationDate;
								certificationDateString = newCertificationDateString;
							}
						}
						if (certificationDate != null){
							CertificationDateManager.cacheCertificationDate(dataSite, certificationDateString);
							getSearch().getSa().updateCertificationDateObject(dataSite, certificationDate);
						}
						
						if (currentFromDate!=null) {
							String fromYear = null;
							if (currentFromDateString.length()>=2) {
								fromYear = currentFromDateString.substring(currentFromDateString.length()-2);
							}
							if (!StringUtils.isEmpty(fromYear)) {
								fromDate = new CachedDate(fromYear, Calendar.getInstance());
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}
	
	private static Set<InstrumentI> getAllAoAndTaxReferences(Search search) {
		DocumentsManagerI manager = search.getDocManager();
		Set<InstrumentI> allAoRef = new HashSet<InstrumentI>();
		try {
			manager.getAccess();
			List<DocumentI> list = manager.getDocumentsWithType(true, DType.ASSESOR, DType.TAX);
			for (DocumentI assessor : list) {
				if (HashCountyToIndex.isLegalBootstrapEnabled(search.getCommId(), assessor.getSiteId())) {
					for (RegisterDocumentI reg : assessor.getReferences()) {
						allAoRef.add(reg.getInstrument());
					}
					allAoRef.addAll(assessor.getParsedReferences());
				}
			}
		} finally {
			manager.releaseAccess();
		}
		return removeEmptyReferences(allAoRef);
	}

	private static Set<InstrumentI> removeEmptyReferences(Set<InstrumentI> allAo) {
		Set<InstrumentI> ret = new HashSet<InstrumentI>();
		for (InstrumentI i : allAo) {
			if (i.hasBookPage() || i.hasInstrNo()) {
				ret.add(i);
			}
		}
		return ret;
	}

	private boolean addAoAndTaxReferenceSearches(TSServerInfo serverInfo, List<TSServerInfoModule> modules, 
			Set<InstrumentI> allAoRef, long searchId, boolean isUpdate) {
		boolean atLeastOne = false;
		final Set<String> searched = new HashSet<String>();

		for (InstrumentI inst : allAoRef) {
			boolean temp = addInstNoSearch(inst, serverInfo, modules, searchId, searched, isUpdate);
			atLeastOne = atLeastOne || temp;
		}
		return atLeastOne;
	}

	private boolean addInstNoSearch(InstrumentI inst, TSServerInfo serverInfo, List<TSServerInfoModule> modules, 
			long searchId, Set<String> searched, boolean isUpdate) {
		
		if (inst.hasInstrNo() || inst.hasBookPage()) {

			String instr = inst.getInstno().replaceAll("-0+", "-");
			if (StringUtils.isEmpty(instr)) {
				//consider instrument number as book-page
				instr = inst.getBook() + "-" + inst.getPage().replaceFirst("^0+", "");
			}
			if (!searched.contains(instr)) {
				searched.add(instr);
			} else {
				return false;
			}
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.forceValue(0, "ALL");
			module.forceValue(1, instr);
			module.forceValue(3, "Search");
			module.forceValue(4, "-1");
			if (isUpdate) {
				module.addFilter(new BetweenDatesFilterResponse(searchId));
			}
			module.addFilter(new RejectAlreadySavedDocumentsFilterResponse(searchId));
			modules.add(module);
			return true;
		}
		return false;
	}
	
	private void addIteratorModule(TSServerInfo serverInfo, List<TSServerInfoModule> modules, 
			int code, long searchId, boolean isCondo) {
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
	
	private void addIteratorModuleSTR(TSServerInfo serverInfo, List<TSServerInfoModule> modules, int code, long searchId) {

		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_SECTION_TWP_RANGE);

		FilterResponse nameFilter = NameFilterFactory.getDefaultNameFilter(
				SearchAttributes.OWNER_OBJECT, searchId, module);
		((GenericNameFilter) nameFilter).setIgnoreMiddleOnEmpty(true);
		((GenericNameFilter) nameFilter).setUseArrangements(false);
		((GenericNameFilter) nameFilter).setInitAgain(true);
		FilterResponse defaultLegalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);

		module.clearSaKeys();
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId, false);
		it.setEnableTownshipLegal(true);
		module.addFilter(nameFilter);
		module.addFilter(defaultLegalFilter);
		module.addIterator(it);
		modules.add(module);
	}

	static protected class LegalDescriptionIterator extends GenericRuntimeIterator<PersonalDataStruct> {

		private static final long serialVersionUID = 4961895575209899356L;
		private boolean enableSubdividedLegal	= false;
		private boolean enableTownshipLegal		= false;
		private boolean isCondo = false;

		LegalDescriptionIterator(long searchId, boolean isCondo) {
			super(searchId);
			this.isCondo = isCondo;
		}

		public boolean isEnableSubdividedLegal() {
			return enableSubdividedLegal;
		}

		public void setEnableSubdividedLegal(boolean enableSubdividedLegal) {
			this.enableSubdividedLegal = enableSubdividedLegal;
		}

		public boolean isEnableTownshipLegal() {
			return enableTownshipLegal;
		}

		public void setEnableTownshipLegal(boolean enableTownshipLegal) {
			this.enableTownshipLegal = enableTownshipLegal;
		}
		
		public boolean isCondo() {
			return isCondo;
		}

		public void setisCondo(boolean isCondo) {
			this.isCondo = isCondo;
		}

		@SuppressWarnings("unchecked")
		List<PersonalDataStruct> createDerivationInternal(long searchId) {
			Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			DocumentsManagerI m = global.getDocManager();
			String key = "AR_WASHINGTON_RO_LOOK_UP_DATA";
			if (!isCondo) {
				key+= "_SUBDIV";
			} else {
				key+= "_CONDO";
			}
			if (isEnableSubdividedLegal())
				key += "_SLB";
			else if (isEnableTownshipLegal())
				key += "_STR";
			List<PersonalDataStruct> legalStructList = (List<PersonalDataStruct>) global.getAdditionalInfo(key);

			String aoAndTrLots = global.getSa().getAtribute(SearchAttributes.LD_LOTNO);
			String[] allAoAndTrlots = new String[0];

			if (!StringUtils.isEmpty(aoAndTrLots)) {
				Vector<LotInterval> lots = LotMatchAlgorithm.prepareLotInterval(aoAndTrLots);
				HashSet<String> lotExpanded = new LinkedHashSet<String>();
				for (Iterator<LotInterval> iterator = lots.iterator(); iterator.hasNext();) {
					lotExpanded.addAll(((LotInterval) iterator.next()).getLotList());
				}
				allAoAndTrlots = lotExpanded.toArray(allAoAndTrlots);
			}
			boolean hasLegal = false;
			if (legalStructList == null) {
				legalStructList = new ArrayList<PersonalDataStruct>();

				try {

					m.getAccess();
					List<RegisterDocumentI> listRodocs = m.getRoLikeDocumentList(true);
					DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_ASC);
					for (RegisterDocumentI reg : listRodocs) {
						for (PropertyI prop : reg.getProperties()) {
							if (prop.hasLegal()) {
								LegalI legal = prop.getLegal();

								if (legal.hasSubdividedLegal() && isEnableSubdividedLegal()) {
									hasLegal = true;
									PersonalDataStruct legalStructItem = new PersonalDataStruct();
									SubdivisionI subdiv = legal.getSubdivision();

									String subName = subdiv.getName();
									subName = ro.cst.tsearch.servers.functions.ARWashingtonRO.correctSubdivisionName(subName);
									if (!isCondo) {
										subName = getSubdivisionCodeFromName(subName);
									} else {
										subName = getCondominiumCodeFromName(subName);
									}
									String block = subdiv.getBlock();
									String lot = subdiv.getLot();
									String[] lots = lot.split("  ");
									if (StringUtils.isNotEmpty(subName)) {
										for (int i = 0; i < lots.length; i++) {
											legalStructItem = new PersonalDataStruct();
											legalStructItem.subName = subName;
											legalStructItem.block = StringUtils.isEmpty(block) ? "" : block;
											legalStructItem.lot = lots[i];
											if (!testIfExist(legalStructList, legalStructItem, "subdivision")) {
												legalStructList.add(legalStructItem);
											}
										}
									}
								}
								if (legal.hasTownshipLegal() && isEnableTownshipLegal()) {
									PersonalDataStruct legalStructItem = new PersonalDataStruct();
									TownShipI township = legal.getTownShip();
								
									String sec = township.getSection();
									String tw = township.getTownship();
									String rg = township.getRange();
									if (StringUtils.isNotEmpty(sec)) {
										legalStructItem.section = StringUtils.isEmpty(sec) ? "" : sec;
										legalStructItem.township = StringUtils.isEmpty(tw) ? "" : tw;
										legalStructItem.range = StringUtils.isEmpty(rg) ? "" : rg;
								
										if (!testIfExist(legalStructList, legalStructItem, "sectional")) {
											legalStructList.add(legalStructItem);
										}
									}
								}
							}
						}
					}

					global.setAdditionalInfo(key, legalStructList);

				} finally {
					m.releaseAccess();
				}
			} else {
				for (PersonalDataStruct struct : legalStructList) {
					if (StringUtils.isNotEmpty(struct.subName)) {
						hasLegal = true;
					}
				}
				if (hasLegal) {
					legalStructList = new ArrayList<PersonalDataStruct>();
				}
			}
			return legalStructList;
		}

		protected List<PersonalDataStruct> createDerrivations() {
			return createDerivationInternal(searchId);
		}

		protected void loadDerrivation(TSServerInfoModule module, PersonalDataStruct str) {
			FilterResponse lotFilter = LegalFilterFactory.getDefaultLotFilter(searchId);
			FilterResponse blockFilter = LegalFilterFactory.getDefaultBlockFilter(searchId);

			if (module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION)
					.equals(TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_BLOCK) ||
				module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION)
					.equals(TSServerInfoConstants.VALUE_PARAM_CONDOMINIUM_LOT_BLOCK)) {
				if (StringUtils.isNotEmpty(str.subName) || StringUtils.isNotEmpty(str.lot) || StringUtils.isNotEmpty(str.block)) {
					module.setData(0, StringUtils.isNotEmpty(str.subName) ? str.subName : "");
					module.setData(1, StringUtils.isNotEmpty(str.block) ? str.block : "");
					module.setData(2, StringUtils.isNotEmpty(str.lot) ? str.lot : "");
					module.addValidator(lotFilter.getValidator());
					module.addValidator(blockFilter.getValidator());
					module.setVisible(true);
				} else {
					module.setVisible(false);
				}
			} else if (module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION)
					.equals(TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_SECTION_TWP_RANGE)){
				if (StringUtils.isNotEmpty(str.section)){
					module.setData(0, StringUtils.isNotEmpty(str.section) ? str.section : "");
					module.setData(1, StringUtils.isNotEmpty(str.township) ? str.township : "");
					module.setData(2, StringUtils.isNotEmpty(str.range) ? str.range : "");
					module.setVisible(true);
				} else {
					module.setVisible(false);
				}
			}
		}
	}

	protected static class PersonalDataStruct implements Cloneable {
		String subName = "";
		String lot = "";
		String block = "";
		String section = "";
		String township = "";
		String range = "";

		@Override
		protected Object clone() throws CloneNotSupportedException {
			return super.clone();
		}

		public boolean equalsSubdivision(PersonalDataStruct struct) {
			return this.block.equals(struct.block) && this.lot.equals(struct.lot) && this.subName.equals(struct.subName);
		}

		public boolean equalsSectional(PersonalDataStruct struct) {
			return this.section.equals(struct.section) && this.township.equals(struct.township) && this.range.equals(struct.range);
		}

	}

	private static boolean testIfExist(List<PersonalDataStruct> legalStruct2, PersonalDataStruct l, String string) {

		if ("subdivision".equalsIgnoreCase(string)) {
			for (PersonalDataStruct p : legalStruct2) {
				if (l.equalsSubdivision(p)) {
					return true;
				}
			}
		}
		else if ("sectional".equalsIgnoreCase(string)) {
			for (PersonalDataStruct p : legalStruct2) {
				if (l.equalsSectional(p)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		Set<InstrumentI> allAoRef = getAllAoAndTaxReferences(global);

		Search search = getSearch();
		TSServerInfoModule m = null;
		SearchAttributes sa = search.getSa();

		FilterResponse rejectSavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId);
		FilterResponse defaultLegalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);
		((GenericLegal) defaultLegalFilter).setEnableLot(true);

		FilterResponse subdivisionNameFilter = NameFilterFactory.getDefaultNameFilterForSubdivision(searchId);
		subdivisionNameFilter.setThreshold(new BigDecimal(0.90d));

		DocsValidator subdivisionNameValidator = subdivisionNameFilter.getValidator();

		//1 instrument list search from AO and TR for finding Legal
		addAoAndTaxReferenceSearches(serverInfo, modules, allAoRef, searchId, isUpdate());

		//2a search with lot/block/subdivision from RO documents
		addIteratorModule(serverInfo, modules, TSServerInfo.SUBDIVISION_MODULE_IDX, searchId, false);
		
		//2b search with lot/block/condominium from RO documents
		addIteratorModule(serverInfo, modules, TSServerInfo.CONDOMIN_MODULE_IDX, searchId, true);

		//3 search with section/township/range from RO documents
		addIteratorModuleSTR(serverInfo, modules, TSServerInfo.SECTION_LAND_MODULE_IDX, searchId);

		//4a search by subdivision name
		String subdivisionName = ro.cst.tsearch.servers.functions.ARWashingtonRO.correctSubdivisionName
			(sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME));
		subdivisionName = getSubdivisionCodeFromName(subdivisionName);
		if (StringUtils.isNotEmpty(subdivisionName)) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT);
			m.clearSaKeys();
			m.forceValue(0, subdivisionName);
			m.setSaKey(4, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setSaKey(5, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			m.addFilter(subdivisionNameFilter);
			m.addValidator(subdivisionNameValidator);
			m.addValidator(defaultLegalFilter.getValidator());
			modules.add(m);
		}
		
		//4b search by condominium name
		String condoName = ro.cst.tsearch.servers.functions.ARWashingtonRO.correctSubdivisionName
			(sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME));
		condoName = getCondominiumCodeFromName(condoName);
		if (StringUtils.isNotEmpty(condoName)) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.CONDOMIN_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_CONDOMINIUM_LOT);
			m.clearSaKeys();
			m.forceValue(0, condoName);
			m.setSaKey(4, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setSaKey(5, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			m.addFilter(subdivisionNameFilter);
			m.addValidator(subdivisionNameValidator);
			m.addValidator(defaultLegalFilter.getValidator());
			modules.add(m);
		}

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
			m.setSaKey(10, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setSaKey(11, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);

			m.addFilter(rejectSavedDocuments);
			m.addFilter(defaultNameFilter);
			m.addFilter(new LastTransferDateFilter(searchId));
			addFilterForUpdate(m, true);

			m.addValidator(defaultLegalFilter.getValidator());
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE);
			m.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			m.setIteratorType(10, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
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
			m.setSaKey(10, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
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
			m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE);
			m.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			m.setIteratorType(10, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
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
		m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
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
		m.setSaKey(10, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		m.setSaKey(11, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
		m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
		m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE);
		m.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
		m.setIteratorType(10, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
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

	@Override
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {

		ConfigurableNameIterator nameIterator = null;
		String endDate = new SimpleDateFormat("MM/dd/yyyy").format(new Date());

		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
		GBManager gbm = (GBManager) sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

		for (String id : gbm.getGbTransfers()) {

			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.setIndexInGB(id);
			module.setTypeSearchGB("grantor");
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);

			String date = gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
			if (date != null) {
				module.getFunction(10).forceValue(date);
			}
			module.setValue(11, endDate);

			module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE);
			module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId,
					new String[] { "L;F;M", "L;F;" });
			module.addIterator(nameIterator);
			module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
			module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
			modules.add(module);

			if (gbm.getNamesForBrokenChain(id, searchId).size() > 0) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.setIndexInGB(id);
				module.setTypeSearchGB("grantee");
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				
				date = gbm.getDateForSearchBrokenChain(id, "MM/dd/yyyy", searchId);
				if (date != null)
					module.getFunction(10).forceValue(date);
				module.setValue(11, endDate);
				
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE);
				module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId,
						new String[] { "L;F;M", "L;F;" });
				module.addIterator(nameIterator);
				module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
				module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				modules.add(module);
			}
		}
		serverInfo.setModulesForGoBackOneLevelSearch(modules);
	}

	@Override
	protected NameI getNameFromModule(TSServerInfoModule module) {
		NameI name = new Name();
		if (module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX && module.getFunctionCount() > 5) {
			String lastFirmName = module.getFunction(0).getParamValue();
			String middleName = module.getFunction(2).getParamValue();
			String firstName = module.getFunction(3).getParamValue();
			String suffix = module.getFunction(5).getParamValue();
			if (StringUtils.isEmpty(lastFirmName)) {
				return null;
			}
			if (NameUtils.isCompany(lastFirmName)) {
				name.setLastName(lastFirmName);
				name.setCompany(true);
			} else {
				name.setLastName(lastFirmName);
				if (!StringUtils.isEmpty(middleName)) {
					name.setMiddleName(middleName);
				}
				if (!StringUtils.isEmpty(firstName)) {
					name.setFirstName(firstName);
				}
				if (!StringUtils.isEmpty(suffix)) {
					name.setSufix(suffix);
				}
			}
			return name;
		}
		return null;
	}

	@Override
	protected int[] getModuleIdsForSavingLegal() {
		return new int[]{TSServerInfo.SUBDIVISION_MODULE_IDX, TSServerInfo.CONDOMIN_MODULE_IDX, 
				TSServerInfo.SECTION_LAND_MODULE_IDX};
	}
	
	@Override
	protected LegalI getLegalFromModule(TSServerInfoModule module) {
		LegalI legal = new Legal();

		if (module.getModuleIdx() == TSServerInfo.SUBDIVISION_MODULE_IDX 
			 && module.getFunctionCount() > 2) {
			
			SubdivisionI subdivision = new Subdivision();

			subdivision.setName(SUBDIVISION_NAME_MAP.get(module.getFunction(0).getParamValue().trim()));
			subdivision.setBlock(module.getFunction(1).getParamValue().trim());
			subdivision.setLot(module.getFunction(2).getParamValue().trim());
			
			legal = new Legal();
			legal.setSubdivision(subdivision);
		} else if (module.getModuleIdx() == TSServerInfo.CONDOMIN_MODULE_IDX 
			 && module.getFunctionCount() > 2) {
			
			SubdivisionI subdivision = new Subdivision();

			subdivision.setName(CONDOMINIUM_NAME_MAP.get(module.getFunction(0).getParamValue().trim()));
			subdivision.setBlock(module.getFunction(1).getParamValue().trim());
			subdivision.setLot(module.getFunction(2).getParamValue().trim());
			
			legal = new Legal();
			legal.setSubdivision(subdivision);
		} else if (module.getModuleIdx() == TSServerInfo.SECTION_LAND_MODULE_IDX 
				 && module.getFunctionCount() > 2) {
			TownShipI townShip = new TownShip();
			
			townShip.setSection(module.getFunction(0).getParamValue().trim());
			townShip.setTownship(module.getFunction(1).getParamValue().trim());
			townShip.setRange(module.getFunction(2).getParamValue().trim());
			
			legal = new Legal();
			legal.setTownShip(townShip);
		}

		return legal;
	}

	@Override
	public TSServerInfoModule getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		
		if(restoreDocumentDataI == null) {
			return null;
		}
		
		TSServerInfoModule module = null;
		String instrumentNumber = restoreDocumentDataI.getInstrumentNumber();
		if(StringUtils.isNotEmpty(instrumentNumber)) {
			HashMap<String, String> filterCriteria = new HashMap<String, String>();
			module = new TSServerInfoModule(getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			filterCriteria.put("InstrumentNumber", instrumentNumber);
			module.forceValue(0, "ALL");
			module.forceValue(1, instrumentNumber);
			module.forceValue(3, "Search");
			module.forceValue(4, "-1");
			GenericInstrumentFilter filter = new GenericInstrumentFilter(searchId, filterCriteria);
			module.getFilterList().clear();
			module.addFilter(filter);
			Date recordedDate = restoreDocumentDataI.getRecordedDate();
			if (recordedDate!=null) {
				ExactDateFilterResponse dateFilter = new ExactDateFilterResponse(searchId, recordedDate);
				module.addFilter(dateFilter);
			}
		}
		return module;
	}
	
}
