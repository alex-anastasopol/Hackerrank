package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.joda.time.DateTime;
import org.joda.time.Days;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.bean.recoverdocument.ModuleShortDescription;
import ro.cst.tsearch.connection.http3.HttpManager3;
import ro.cst.tsearch.connection.http3.HttpSite3;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.log.SearchLogFactory;
import ro.cst.tsearch.log.SearchLogPage;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.fornext.ParcelNumberFilterResponseForNext;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericMultipleLegalFilter;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterForNext;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.module.OcrOrBootStraperIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.CrossRefSet;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.ImageTransformation;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.threads.GPMaster;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;

@SuppressWarnings("deprecation")
public class OHCuyahogaRO extends TSServerROLike {

	private static final long	serialVersionUID	= 5233249802430436299L;
	
	public static final String DETAILS_LINK_PATT = "(?is)<a[^>]+href=\"javascript:__doPostBack\\('([^\"]+)','([^\"]+)'\\)\"[^>]*>";
	public static final String IMAGE_LINK_PATT = "(?is)<a[^>]+href=\"([^\"]+)\"[^>]*>(\\s*\\[View\\s+Image\\]\\s*)</a>";
	public static final String DOCUMENT_ID_PATT = "(?is)<input[^>]+value=\"([^\"]+)\"[^>]+id=\"ctl00_ContentPlaceHolder1_reference_ctl\\d+_txtDocumentID\"[^>]*/>";
	public static final String CROSS_REF_PATT = "(?is)(<td>\\s*)<a[^>]+href=\"javascript:__doPostBack\\('([^\\\"]+)','([^\\\"]+)'\\)\\\"[^>]*>([^<]+)</a>(\\s*</td>\\s*<td>[^<]*</td>)\\s*<td[^>]*>\\s*<input[^<]+value=\"([^\"]+)\"[^<]*>\\s*</td>";
	
	private static final String INVALID_START_DATE_MESSAGE = "Invalid STARTING date!";
	private static final String INVALID_END_DATE_MESSAGE = "Invalid ENDING date!";
	private static final String DATE_GENERAL_MESSAGE = "Your search by date is too great for Date Only search, please limit your date up to 15 days or enter AFN or Books name for your search";
	private static final String DATE_PARCEL_MESSAGE = "Your search by date is too great for Date Only search, please limit your date up to 60 days or enter Parcel # for your search";
	private static final String INVALID_PARCEL_NUMBER_MESSAGE = "Invalid Parcel Number!";
	
	private static String DOCUMENT_TYPES_SELECT = "";
	private static HashMap<String, String> DOCUMENTS_TYPES_MAP = new HashMap<String, String>();
	private static HashMap<String, String> SORT_BY_MAP = new HashMap<String, String>();
	
	static {
		String folderPath = ServerConfig.getModuleDescriptionFolder(BaseServlet.REAL_PATH + "WEB-INF/classes/resource/module/comments/");
		File folder = new File(folderPath);
		if (!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath + "] does not exist. Module Information not loaded!");
		}
		try {
			DOCUMENT_TYPES_SELECT = org.apache.commons.io.FileUtils.readFileToString(new File(folderPath + File.separator + "OHCuyahogaRODocumentTypes.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		Matcher ma = Pattern.compile("(?is)<option[^>]+value=\"([^\"]+)\"[^>]*>([^<]+)</option>").matcher(DOCUMENT_TYPES_SELECT);
		while (ma.find()) {
			DOCUMENTS_TYPES_MAP.put(ma.group(1), ma.group(2));
		}
		
		SORT_BY_MAP.put("1", "Recorded Date");
		SORT_BY_MAP.put("2", "Last Name");
		SORT_BY_MAP.put("3", "Document Number");
		SORT_BY_MAP.put("4", "Document Type");
	}
	
	public static String getDocumentType(String type) {
		String res = DOCUMENTS_TYPES_MAP.get(type);
		if (res==null) {
			return type;
		} else {
			return res;
		}
	}
	
	public static String getSortBy(String sort) {
		String res = SORT_BY_MAP.get(sort);
		if (res==null) {
			return sort;
		} else {
			return res;
		}
	}
	
	public OHCuyahogaRO(long searchId) {
		super(searchId);
	}

	public OHCuyahogaRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {

		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);

		if (tsServerInfoModule != null && tsServerInfoModule.getFunctionCount()>18) {
			setupSelectBox(tsServerInfoModule.getFunction(14), DOCUMENT_TYPES_SELECT);
			setupSelectBox(tsServerInfoModule.getFunction(15), DOCUMENT_TYPES_SELECT.replaceAll("\"doc1\"", "\"doc2\""));
			setupSelectBox(tsServerInfoModule.getFunction(16), DOCUMENT_TYPES_SELECT.replaceAll("\"doc1\"", "\"doc3\""));
			setupSelectBox(tsServerInfoModule.getFunction(17), DOCUMENT_TYPES_SELECT.replaceAll("\"doc1\"", "\"doc4\""));
			setupSelectBox(tsServerInfoModule.getFunction(18), DOCUMENT_TYPES_SELECT.replaceAll("\"doc1\"", "\"doc5\""));
		}
		
		setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		if (rsResponse.contains("You are currently locked out")){
			Response.getParsedResponse().setError("You are currently locked out");
			Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
			return;
			//throw new com.sun.star.uno.RuntimeException("You are currently locked out");
		}
		if (rsResponse.contains(ro.cst.tsearch.connection.http3.OHCuyahogaRO.SERVER_ERROR_MESSAGE)) {
			Response.getParsedResponse().setError(ro.cst.tsearch.connection.http3.OHCuyahogaRO.SERVER_ERROR_MESSAGE);
			Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
			return;
		}
		
		if (rsResponse.contains("Specify a Particular Document")) {
			Response.getParsedResponse().setError(NO_DATA_FOUND);
			Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
			return;
		}
		
		Matcher ma = Pattern.compile("(?is)\\breturn\\s*<b>0</b>\\s*result\\(s\\)").matcher(rsResponse);
		if (ma.find()) {
			Response.getParsedResponse().setError(NO_DATA_FOUND);
			Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
			return;
		}
		
		switch (viParseID) {
		
		case ID_SEARCH_BY_NAME:					//General Search
		case ID_SEARCH_BY_PARCEL:				//Parcel Search
			
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if (smartParsedResponses.size() == 0) {
				return;
			}

			parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
			parsedResponse.setOnlyResponse(outputTable.toString());

			String header = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
			String footer = "";

			StringBuilder nextLink = new StringBuilder();
			String navigationLinks = getNavigationLinks(sAction, rsResponse, nextLink);
			if (nextLink.length()>0) {
				parsedResponse.setNextLink(nextLink.toString());
			}
					
			header += "<table><tr><td><table>" + "<tr><th align=\"center\">" + SELECT_ALL_CHECKBOXES + "</th><th>Row</th></th><th>AFN</th><th>Doc. Type</th>" + 
				"<th>Name</th><th>Assoc. Name</th><th>Date Recorded</th><th>References</th><th>Legal Description</th><th>Book/Page</th></tr>";

			Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);

			if (numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
				footer = "</table>" + navigationLinks + "</td></tr></table>"
					+ CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer) numberOfUnsavedDocument);
			} else {
				footer = "</table>" + navigationLinks + "</td></tr></table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
			}

			parsedResponse.setHeader(header);
			parsedResponse.setFooter(footer);
			
			break;

		case ID_DETAILS:
		case ID_SAVE_TO_TSD:
			
			StringBuilder file = new StringBuilder();
			StringBuilder serialNumber = new StringBuilder();
			HashMap<String, String> data = new HashMap<String, String>();
			String details = getDetails(rsResponse, file, serialNumber, data, Response);
			String filename = file.toString() + ".html";
			
			if (viParseID != ID_SAVE_TO_TSD) {
				try {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + URLDecoder.decode(originalLink, "UTF-8");
					
					if (isInstrumentSaved(serialNumber.toString(), null, data, false)) {
						details += CreateFileAlreadyInTSD();
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
					}
					Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
					Response.getParsedResponse().setResponse(details);
					
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				
			} else {
				smartParseDetails(Response, details);
				String detailsWithLinks = details;
				
				//remove links
				details = details.replaceAll("(?is)<a[^>]+href=[^>]*>([^<]*)</a>", "$1");
				
				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
				Response.getParsedResponse().setResponse(details);
				
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				saveRelatedDocuments(Response, detailsWithLinks);
				
			}
			break;

			case ID_GET_LINK:
				String stringUri = "";
				URI lastUri = Response.getLastURI();
				if (lastUri!=null) {
					try {
						stringUri = lastUri.getURI();
					} catch (URIException e) {
						e.printStackTrace();
					}
				}
				if (org.apache.commons.lang.StringUtils.containsIgnoreCase(stringUri, ro.cst.tsearch.connection.http3.OHCuyahogaRO.DETAILS_ADDRESS)) {
					ParseResponse(sAction, Response, ID_DETAILS);
				} else {
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				}
			break;
		}
	}
	
	@SuppressWarnings({ "unchecked" })
	protected void saveRelatedDocuments(ServerResponse Response, String details) {
		
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		//cross-references from "References" (links)
		Matcher ma = Pattern.compile("(?is)<a[^>]+href=\"[^\"]+Link=(" + ro.cst.tsearch.connection.http3.OHCuyahogaRO.DETAILS_ADDRESS + "[^\"]+)\"[^>]*>[^>]+</a>").matcher(details);
        while (ma.find()) {
        	String link = CreatePartialLink(TSConnectionURL.idPOST) + ma.group(1);
			ParsedResponse prChild = new ParsedResponse();
			LinkInPage pl = new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD);
			prChild.setPageLink(pl);
			parsedResponse.addOneResultRowOnly(prChild);
        }
		
		//cross-references from "Street Name:" (book-page)
        Vector<CrossRefSet> vectorCRS = (Vector<CrossRefSet>)parsedResponse.infVectorSets.get("CrossRefSet");
		String srcType = null;
		OtherInformationSet ois = (OtherInformationSet) parsedResponse.infSets.get("OtherInformationSet");
		if (ois!=null) {
			srcType = ois.getAtribute(OtherInformationSetKey.SRC_TYPE.getShortKeyName()); 
		}
        if (srcType == null){
        	srcType = "";
        }
        Set<InstrumentI> crossRefs = ro.cst.tsearch.extractor.xml.Bridge.extractCrossRefs(vectorCRS, srcType, searchId);
        Iterator<InstrumentI> it = crossRefs.iterator();
        while (it.hasNext()) {
        	InstrumentI instr = it.next();
        	String instno = instr.getInstno();
        	String book = instr.getBook();
        	String page = instr.getPage();
        	int year = instr.getYear();
        	if (StringUtils.isEmpty(instno) && !StringUtils.isEmpty(book) && !StringUtils.isEmpty(page)) {
        		int seq = getSeq();
    			String link = "";
    			link = ro.cst.tsearch.connection.http3.OHCuyahogaRO.DETAILS_ADDRESS + "?" + ro.cst.tsearch.connection.http3.OHCuyahogaRO.REF_BP_PARAM + "=true&" + 
    				ro.cst.tsearch.connection.http3.OHCuyahogaRO.SEQ_PARAM + "=" + seq;
    			link = CreatePartialLink(TSConnectionURL.idPOST) + link;
    			
    			Map<String, String> params = new HashMap<String, String>();
    			params.put(CrossRefSetKey.BOOK.getShortKeyName(), book);
    			params.put(CrossRefSetKey.PAGE.getShortKeyName(), page);
    			params.put(CrossRefSetKey.YEAR.getShortKeyName(), Integer.toString(year));
    			
    			mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
    			
    			ParsedResponse prChild = new ParsedResponse();
    			LinkInPage pl = new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD);
    			prChild.setPageLink(pl);
    			Response.getParsedResponse().addOneResultRowOnly(prChild);
        	}
        }
        
    }
	
	@Override
	protected void addLinkVisited(String link){
		if (org.apache.commons.lang.StringUtils.containsIgnoreCase(link, ro.cst.tsearch.connection.http3.OHCuyahogaRO.REF_INST_PARAM + "=true")) {
			link = link.replaceFirst("\\bseq=\\d+&?", "");
		}
		super.addLinkVisited(link);
	}
	
	@Override
	protected boolean isLinkVisited(String link){
		if (org.apache.commons.lang.StringUtils.containsIgnoreCase(link, ro.cst.tsearch.connection.http3.OHCuyahogaRO.REF_INST_PARAM + "=true")) {
			link = link.replaceFirst("\\bseq=\\d+&?", "");
		}
		return super.isLinkVisited(link);
    }
	
	public static String getSpanValue(String text, String id) {
		String ret = "";
		Matcher ma = Pattern.compile("(?is)<span[^>]+id=\"" + id + "\"[^>]*>([^>]*)</span>").matcher(text);
		if (ma.find()) {
			ret = ma.group(1).replaceAll("(?is)&nbsp;", "").trim();
		}
		return ret;
	}
	
	public String replaceRefLinks(String rsResponse, String rawInstrno, String book, String page, String type, String recordedDate) {
		
		//replace links for cross-references
		List<String> documentIDList = new ArrayList<String>();
		Matcher ma2 = Pattern.compile(DOCUMENT_ID_PATT).matcher(rsResponse);
		while (ma2.find()) {
			documentIDList.add(ma2.group(1));
		}
		
		StringBuffer sb = new StringBuffer();
		Matcher ma3 = Pattern.compile(CROSS_REF_PATT).matcher(rsResponse);
		while (ma3.find()) {
			
			int seq = getSeq();
			String docLink = "";
			docLink = ro.cst.tsearch.connection.http3.OHCuyahogaRO.DETAILS_ADDRESS + "?" + ro.cst.tsearch.connection.http3.OHCuyahogaRO.REF_INST_PARAM + "=true&" + 
				ro.cst.tsearch.connection.http3.OHCuyahogaRO.SEQ_PARAM + "=" + seq + "&hiddenId=" + ma3.group(6);
			docLink = CreatePartialLink(TSConnectionURL.idPOST) + docLink;
			
			Map<String, String> params = new HashMap<String, String>();
			params.put(SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(), rawInstrno);
			params.put(SaleDataSetKey.BOOK.getShortKeyName(), book);
			params.put(SaleDataSetKey.PAGE.getShortKeyName(), page);
			params.put(SaleDataSetKey.RECORDED_DATE.getShortKeyName(), recordedDate);
			params.put(SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(), type);
			params.put(SaleDataSetKey.CROSS_REF_INSTRUMENT.getShortKeyName(), ma3.group(4));
			
			mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
			
			ma3.appendReplacement(sb, ma3.group(1) + "<a href=\"" + docLink + "\">" + ma3.group(4) + "</a>" + ma3.group(5));
			
		}
		ma3.appendTail(sb);
		rsResponse = sb.toString();
		
		return rsResponse;
	}
	
	protected String getDetails(String rsResponse, StringBuilder fileName, StringBuilder accountId, HashMap<String, String> data, ServerResponse Response) {

		String instrno = getSpanValue(rsResponse, "ctl00_ContentPlaceHolder1_label1");
		accountId.append(instrno);
		
		String type = getSpanValue(rsResponse, "ctl00_ContentPlaceHolder1_label2");
		String recordedDate = getSpanValue(rsResponse, "ctl00_ContentPlaceHolder1_label3");
		String year = "";
		int index = recordedDate.lastIndexOf("/");
		if (index>-1 && index<recordedDate.length()-1) {
			year = recordedDate.substring(index+1);
		}
		String recordedDateDash = recordedDate.replaceAll("/", "-");
		String book = getSpanValue(rsResponse, "ctl00_ContentPlaceHolder1_label5");
		String page = getSpanValue(rsResponse, "ctl00_ContentPlaceHolder1_label6");
		
		data.put("instrno", instrno);
		data.put("book", book);
		data.put("page", page);
		data.put("year", year);
		data.put("type", type);
		
		fileName.append(instrno).append("_").append(book).append("_").append(page).append("_").append(recordedDateDash).append("_").append(type);
		
		String imageLink = "";
		Matcher ma1 = Pattern.compile(IMAGE_LINK_PATT).matcher(rsResponse);
		if (ma1.find()) {
			imageLink = ro.cst.tsearch.connection.http3.OHCuyahogaRO.IMAGE_ADDRESS + "?instrno=" + instrno + "&book=" + book + "&page=" + page + "&date=" + recordedDateDash + "&type=" + type;
		}
		
		if (!StringUtils.isEmpty(imageLink)) {
			imageLink = CreatePartialLink(TSConnectionURL.idGET) + imageLink;
			String sFileLink = instrno + ".tif";
			ImageLinkInPage ilip = new ImageLinkInPage(imageLink, sFileLink);
			Response.getParsedResponse().addImageLink(ilip);
		}
		
		/* If from memory - use it as is */
		if (!rsResponse.toLowerCase().contains("<html")) {
			return rsResponse;
		}
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("bgcolor", "White"))
				.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("border", "0"));
			if (tableList.size()>0) {
				rsResponse = tableList.elementAt(0).toHtml();
			}
			rsResponse = rsResponse.replaceAll(IMAGE_LINK_PATT, "<a href=\"" + imageLink + "\">$2</a>");
			
			rsResponse = replaceRefLinks(rsResponse, instrno, book, page, type, recordedDateDash);
						
			rsResponse = rsResponse.replaceAll("(?is)<(t[hd])[^>]+class=\"hideit\"[^>]*>.*?</\\1>", "");
			
			String query = Response.getQuerry();
			String hiddenId = StringUtils.extractParameter(query.toString(), "hiddenId=([^&?]*)");
			rsResponse = rsResponse.replaceFirst("(?is)<span[^>]+id=\"ctl00_ContentPlaceHolder1_label1\"[^>]*>[^>]*</span>",
				"$0<span id=\"hiddenId\" style=\"visibility:hidden;display:none\">" + hiddenId + "</span>");
			
			rsResponse = "<table width=\"570px\"><tr><td>" + rsResponse + "</td></tr></table>";
		} catch (Exception e) {
			e.printStackTrace();
		}			
				
		return rsResponse;

	}
	
	protected String getNavigationLinks(String sAction, String response, StringBuilder nextLink) {

		try {
			
			org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(response, null);
			NodeList nodeList = parser.parse(null);
			NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_GridView1"), true);
			
			if (mainTableList.size()>0) {
				TableTag table = (TableTag)mainTableList.elementAt(0);
				TableRow lastRow = table.getRow(table.getRowCount()-1);
				if (lastRow.getColumnCount()==1) {
					String links = lastRow.getColumns()[0].toHtml();
					links = "<table width=\"100%\"><tr align=\"center\" style=\"color:White;background-color:#1C5E55;\">" + links + "</tr></table>";
					String viewState = ro.cst.tsearch.connection.http3.OHCuyahogaRO.getParameterValueFromHtml(ro.cst.tsearch.connection.http3.OHCuyahogaRO.VIEW_STATE_PARAM, response);
					String eventValidation = ro.cst.tsearch.connection.http3.OHCuyahogaRO.getParameterValueFromHtml(ro.cst.tsearch.connection.http3.OHCuyahogaRO.EVENT_VALIDATION_PARAM, response);
					
					StringBuffer sb = new StringBuffer();
					Matcher ma1 = Pattern.compile(DETAILS_LINK_PATT).matcher(links);
					while (ma1.find()) {
						
						int seq = getSeq();
						String docLink = "";
						if (org.apache.commons.lang.StringUtils.containsIgnoreCase(sAction, ro.cst.tsearch.connection.http3.OHCuyahogaRO.GENERAL_SEARCH_ADDRESS) || 
								org.apache.commons.lang.StringUtils.containsIgnoreCase(sAction, ro.cst.tsearch.connection.http3.OHCuyahogaRO.GENERAL_INTERM_ADDRESS)) {
							docLink = ro.cst.tsearch.connection.http3.OHCuyahogaRO.GENERAL_INTERM_ADDRESS;
						} else if (org.apache.commons.lang.StringUtils.containsIgnoreCase(sAction, ro.cst.tsearch.connection.http3.OHCuyahogaRO.PARCEL_SEARCH_ADDRESS) || 
								org.apache.commons.lang.StringUtils.containsIgnoreCase(sAction, ro.cst.tsearch.connection.http3.OHCuyahogaRO.PARCEL_INTERM_ADDRESS)) {
							docLink = ro.cst.tsearch.connection.http3.OHCuyahogaRO.PARCEL_INTERM_ADDRESS;
						}
						docLink += "?" + ro.cst.tsearch.connection.http3.OHCuyahogaRO.SEQ_PARAM + "=" + seq;
						docLink = CreatePartialLink(TSConnectionURL.idPOST) + docLink;
						
						Map<String, String> params = new HashMap<String, String>();
						params.put(ro.cst.tsearch.connection.http3.OHCuyahogaRO.EVENT_TARGET_PARAM, ma1.group(1));
						params.put(ro.cst.tsearch.connection.http3.OHCuyahogaRO.EVENT_ARGUMENT_PARAM, ma1.group(2));
						params.put(ro.cst.tsearch.connection.http3.OHCuyahogaRO.VIEW_STATE_PARAM, viewState);
						params.put(ro.cst.tsearch.connection.http3.OHCuyahogaRO.EVENT_VALIDATION_PARAM, eventValidation);
						mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
						
						ma1.appendReplacement(sb, "<a href=\"" + docLink + "\" style=\"color:White;\">");
						
					}
					ma1.appendTail(sb);
					links = sb.toString();
					
					Matcher ma2 = Pattern.compile("(?is)<td>\\s*<span>[^<]+</span>\\s*</td>\\s*<td>\\s*(<a[^>]+>)[^<]+</a>\\s*</td>").matcher(links);
					if (ma2.find()) {
						nextLink.append(ma2.group(1));
					}
					
					return links;
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
		
		return "";
	}
	
	public static String getHiddenId(String row) {
		String res = "";
		Matcher ma = Pattern.compile("(?is)<input[^>]+value=\"([^\"]+)\"[^>]+id=\"ctl00_ContentPlaceHolder1_GridView1_ctl\\d+_txtDocumentID\"[^>]*>").matcher(row);
		if (ma.find()) {
			res = ma.group(1);
		}
		return res;
	}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Map<String, TableRow> goodRows = new LinkedHashMap<String, TableRow>();
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		int numberOfUncheckedElements = 0;

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);

			NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_GridView1"), true);
			
			if (mainTableList.size()==0) {
				return intermediaryResponse;
			}
			
			int format = ro.cst.tsearch.servers.functions.OHCuyahogaRO.NAME_DESOTO_PARSING;
			String detailsLink = ro.cst.tsearch.connection.http3.OHCuyahogaRO.GENERAL_INTERM_ADDRESS;
			URI uri = response.getLastURI();
			if (uri!=null) {
				String uriString = uri.getURI();
				if (org.apache.commons.lang.StringUtils.containsIgnoreCase(uriString, ro.cst.tsearch.connection.http3.OHCuyahogaRO.PARCEL_INTERM_ADDRESS)) {
					format = ro.cst.tsearch.servers.functions.OHCuyahogaRO.NAME_NASHVILLE_PARSING;
					detailsLink = ro.cst.tsearch.connection.http3.OHCuyahogaRO.PARCEL_INTERM_ADDRESS;
				}
			}

			TableTag tbl = (TableTag) mainTableList.elementAt(0);
			TableRow[] rows = tbl.getRows();
			
			for (int i=1;i<rows.length;i++) {
				TableRow row = rows[i];
				if (row.getColumnCount()<9) {
					continue;
				}
				String hiddenId = getHiddenId(row.toHtml());
				if (!StringUtils.isEmpty(hiddenId)) {
					TableRow foundRow = goodRows.get(hiddenId);
					if (foundRow==null) {	//row not found
						goodRows.put(hiddenId, row);
					} else {
						String newRef = row.getColumns()[6].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim();
						TableColumn col = foundRow.getColumns()[6];
						NodeList children = col.getChildren();
						if (children.size()>0) {
							Node first = children.elementAt(0);
							first.setText(first.getText() + "; " + newRef);
						}
					}
				}
			}

			int i = 1;
			for (Map.Entry<String, TableRow> entry : goodRows.entrySet()) {
				
				TableRow row = entry.getValue();
				if (row.getColumnCount()<9) {
					continue;
				}
				
				TableColumn col = row.getColumns()[0];
				NodeList children = col.getChildren();
				if (children.size()>0) {
					Node first = children.elementAt(0);
					first.setText(Integer.toString(i));
				}
				
				String htmlRow = row.toHtml();
				Map<String, String> params = new HashMap<String, String>();
				if (detailsLink.equals(ro.cst.tsearch.connection.http3.OHCuyahogaRO.PARCEL_INTERM_ADDRESS)) {
					Matcher ma = Pattern.compile("(?is)<input[^>]+name=\"([^\"]+)\"[^>]+value=\"([^\"]+)\"[^>]*>").matcher(htmlRow);
					while (ma.find()) {
						params.put(ma.group(1), ma.group(2));
					}
				}
				
				String hiddenId = getHiddenId(htmlRow);
				
				htmlRow = htmlRow.replaceAll("(?is)</?tr[^>]*>", "");
				htmlRow = htmlRow.replaceAll("(?is)<td[^>]+class=\"hideit\"[^>]*>.*?</td>", "");
				
				int seq = getSeq();
				String docLink = detailsLink + "?" + ro.cst.tsearch.connection.http3.OHCuyahogaRO.SEQ_PARAM + "=" + seq + "&hiddenId=" + hiddenId
					+ "&" + ro.cst.tsearch.connection.http3.OHCuyahogaRO.DETAILS_PARAM + "=true";
				docLink = CreatePartialLink(TSConnectionURL.idPOST) + docLink;
				
				ResultMap m = ro.cst.tsearch.servers.functions.OHCuyahogaRO.parseIntermediaryRow(row, format);
				String instrNo = org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName()));
				String book = org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.BOOK.getKeyName()));
				String page = org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.PAGE.getKeyName()));
				String type = org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName()));
				String date = org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.RECORDED_DATE.getKeyName()));
				String year = "";
				int index = date.lastIndexOf("/");
				if (index>-1 && index<date.length()-1) {
					year = date.substring(index+1);
				}
				
				Matcher ma = Pattern.compile(DETAILS_LINK_PATT).matcher(htmlRow);
				if (ma.find()) {
					params.put(SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(), instrNo);
					params.put(SaleDataSetKey.BOOK.getShortKeyName(), book);
					params.put(SaleDataSetKey.PAGE.getShortKeyName(), page);
					params.put(SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(), type);
					params.put(SaleDataSetKey.RECORDED_DATE.getShortKeyName(), date);
				}
				
				mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
				
				htmlRow = htmlRow.replaceAll(DETAILS_LINK_PATT, "<a href=\"" + docLink + "\">");
					
				ParsedResponse currentResponse = new ParsedResponse();
				currentResponse.setPageLink(new LinkInPage(docLink, docLink, TSServer.REQUEST_SAVE_TO_TSD));
				
				Bridge bridge = new Bridge(currentResponse, m, searchId);
				DocumentI document = (RegisterDocumentI) bridge.importData();
				currentResponse.setDocument(document);
				
				String checkBox = "checked";
				
				HashMap<String, String> data = new HashMap<String, String>();
				data.put("instrno", instrNo);
				data.put("type", type);
				data.put("book", book);
				data.put("page", page);
				data.put("year", year);
				
				if (isInstrumentSaved(instrNo, document, data, false)) {
					checkBox = "saved";
				} else {
					numberOfUncheckedElements++;
					LinkInPage linkInPage = new LinkInPage(docLink, docLink, TSServer.REQUEST_SAVE_TO_TSD);
					checkBox = "<input type='checkbox' name='docLink' value='" + docLink + "'>";
					currentResponse.setPageLink(linkInPage);
				}
				String rowType = "1";
				if (i%2!=0) {
					rowType = "2";
				}
				String rowHtml = "<tr class=\"row" + rowType + "\"><td align=\"center\">" + checkBox + "</td>" + htmlRow + "</tr>";
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
				currentResponse.setOnlyResponse(rowHtml);
				intermediaryResponse.add(currentResponse);
				
				i++;
			
			}
			
			outputTable.append(table);
			SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseName(String last, String first, String middle , ArrayList<List> list) {
		
		first = ro.cst.tsearch.servers.functions.OHCuyahogaRO.clean(first);
		
		String full = last + " " + first + " " + middle;
		full = full.trim();
		
		String names[];
		if (StringUtils.isEmpty(first) && StringUtils.isEmpty(middle) && NameUtils.isCompany(last)) {
			names = new String[]{"", "", last, "", "", ""};
		} else {
			names = new String[]{first, middle, last, "", "", ""};
		}
		
		String[] suffixes, type, otherType;
		type = GenericFunctions.extractAllNamesType(names);
		otherType = GenericFunctions.extractAllNamesOtherType(names);
		suffixes = GenericFunctions.extractNameSuffixes(names);
		GenericFunctions.addOwnerNames(full, names, suffixes[0],
			suffixes[1], type, otherType, NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), list);
		
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(NodeList list, ResultMap map, String setName, String saleDataSetKeyName) {
		if (list.size()>0) {
			TableTag table = (TableTag)list.elementAt(0);
			if (table.getRowCount()>1) {
				ArrayList<List> grantor = new ArrayList<List>();
				for (int i=1;i<table.getRowCount();i++) {
					TableRow row = table.getRow(i);
					if (row.getColumnCount()>3) {
						String last = row.getColumns()[1].toPlainTextString().trim();
						String first = row.getColumns()[2].toPlainTextString().trim();
						String middle = row.getColumns()[3].toPlainTextString().trim();
						parseName(last, first, middle, grantor);
					}
				}
				try {
					map.put(setName, GenericFunctions.storeOwnerInSet(grantor, true));
				} catch (Exception e) {
					e.printStackTrace();
				}
				map.put(saleDataSetKeyName, ro.cst.tsearch.servers.functions.OHCuyahogaRO.concatenateNames(grantor));
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			String instrno = getSpanValue(detailsHtml, "ctl00_ContentPlaceHolder1_label1");
			map.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrno);
			
			String docType1 = getSpanValue(detailsHtml, "ctl00_ContentPlaceHolder1_label2");
			String docType2 = getSpanValue(detailsHtml, "ctl00_ContentPlaceHolder1_label22");
			String docType = "";
			if (!StringUtils.isEmpty(docType1)) {
				docType = docType1;
			}
			if (!StringUtils.isEmpty(docType2)) {
				docType += " - " + docType2;
			}
			if (!StringUtils.isEmpty(docType)) {
				map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), docType);
			}
			
			String recordedDate = getSpanValue(detailsHtml, "ctl00_ContentPlaceHolder1_label3");
			if (!StringUtils.isEmpty(recordedDate)) {
				map.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recordedDate);
			}
			
			String book = getSpanValue(detailsHtml, "ctl00_ContentPlaceHolder1_label5");
			String page = getSpanValue(detailsHtml, "ctl00_ContentPlaceHolder1_label6");
			if (!StringUtils.isEmpty(book) && !StringUtils.isEmpty(page)) {
				map.put(SaleDataSetKey.BOOK.getKeyName(), book);
				map.put(SaleDataSetKey.PAGE.getKeyName(),page);
			}
			
			String amount = getSpanValue(detailsHtml, "ctl00_ContentPlaceHolder1_label7").replaceAll("[$,]", "");
			if (!StringUtils.isEmpty(amount)) {
				map.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), amount);
			}
			
			NodeList grantorList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Output"), true)
				.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			parseNames(grantorList, map, "GrantorSet", SaleDataSetKey.GRANTOR.getKeyName());
			
			NodeList granteeList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Output2"), true)
				.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			parseNames(granteeList, map, "GranteeSet", SaleDataSetKey.GRANTEE.getKeyName());
			
			
			List<List> refBody = new ArrayList<List>();
			NodeList referencesList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_reference"), true);
			if (referencesList.size()>0) {
				TableTag table = (TableTag)referencesList.elementAt(0);
				if (table.getRowCount()>1) {
					List<String> refRow;
					for (int i=1;i<table.getRowCount();i++) {
						TableRow row = table.getRow(i);
						if (row.getColumnCount()>1) {
							String refType = row.getColumns()[0].toPlainTextString().trim();
							String refInstno = row.getColumns()[1].toPlainTextString().trim();
							refRow = new ArrayList<String>();
							if (!StringUtils.isEmpty(refInstno)) {
								refRow.add(refType);	//document type
								refRow.add(refInstno);	//instrument number
								refRow.add("");			//book
								refRow.add("");			//page
								refRow.add("");			//year
								refBody.add(refRow);
							}
						}
						
					}
				}
			}
			
			NodeList sublotList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_subLot"), true);
			if (sublotList.size()>0) {
				String sublot = sublotList.elementAt(0).toPlainTextString().trim();
				if (!StringUtils.isEmpty(sublot)) {
					map.put(PropertyIdentificationSetKey.SUB_LOT.getKeyName(), sublot);
				}
			}
			
			NodeList lotList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_originalLot"), true);
			if (lotList.size()>0) {
				String lot = lotList.elementAt(0).toPlainTextString().trim();
				if (!StringUtils.isEmpty(lot)) {
					map.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
				}
			}
			
			NodeList streetList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_street"), true);
			if (streetList.size()>0) {
				String street = streetList.elementAt(0).toPlainTextString();
				ro.cst.tsearch.servers.functions.OHCuyahogaRO.putStreet(map, new ArrayList<List>(), street);
			}
			
			if (refBody.size()>0) {
				String[] refHeader = {CrossRefSetKey.CROSS_REF_TYPE.getShortKeyName(), CrossRefSetKey.INSTRUMENT_NUMBER.getShortKeyName(),
					CrossRefSetKey.BOOK.getShortKeyName(), CrossRefSetKey.PAGE.getShortKeyName(), CrossRefSetKey.YEAR.getShortKeyName()};
				map.put("CrossRefSet", GenericFunctions2.createResultTableFromList(refHeader, refBody));
			}
			
			NodeList parcelList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_parcel"), true);
			if (parcelList.size()>0) {
				String parcel = parcelList.elementAt(0).toPlainTextString().trim();
				if (!StringUtils.isEmpty(parcel)) {
					map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcel.replaceAll("-", ""));
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public Date parseDate(String value) {
		Date date = null;
		if (value!=null) {
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
			try {
				date = sdf.parse(value);
			} catch (ParseException pe) {}
		}
		return date;
	}
	
	@Override
	public String getPrettyFollowedLink (String initialFollowedLnk) {
		initialFollowedLnk = initialFollowedLnk.replaceFirst("(?is).*Link=", "");
    	return super.getPrettyFollowedLink(initialFollowedLnk);
    }
	
	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
        if (module.getModuleIdx() ==TSServerInfo.NAME_MODULE_IDX) {
        	String startDate = module.getFunction(6).getParamValue();
        	Date start = parseDate(startDate);
        	if (start==null) {		//invalid start date
        		 logSearchBy(module);
        		 ServerResponse response = new ServerResponse();
                 response.getParsedResponse().setError("<html>" + INVALID_START_DATE_MESSAGE + "</html>");
                 logInitialResponse(response);
                 return response;
        	}
        	String endDate = module.getFunction(7).getParamValue();
        	Date end = parseDate(endDate);
        	if (end==null) {		//invalid end date
        		 logSearchBy(module);
        		 ServerResponse response = new ServerResponse();
                 response.getParsedResponse().setError("<html>" + INVALID_END_DATE_MESSAGE + "</html>");
                 logInitialResponse(response);
                 return response;
        	}
        	String afn = module.getFunction(1).getParamValue();
        	String book = module.getFunction(3).getParamValue();
        	String page = module.getFunction(4).getParamValue();
        	String last = module.getFunction(9).getParamValue();
        	String first = module.getFunction(10).getParamValue();
        	String company = module.getFunction(12).getParamValue();
        	if (StringUtils.isEmpty(afn) && StringUtils.isEmpty(book) && StringUtils.isEmpty(page) && 
        		StringUtils.isEmpty(last) && StringUtils.isEmpty(first) && StringUtils.isEmpty(company)) {
        			int days = Math.abs(Days.daysBetween(new DateTime(start), new DateTime(end)).getDays());
        			if (days>15) {
        				logSearchBy(module);
        				ServerResponse response = new ServerResponse();
                        response.getParsedResponse().setError("<html>" + DATE_GENERAL_MESSAGE + "</html>");
                        logInitialResponse(response);
                        return response;
        			}
        	}
        } else if (module.getModuleIdx() ==TSServerInfo.PARCEL_ID_MODULE_IDX) {
        	String startDate = module.getFunction(0).getParamValue();
        	Date start = parseDate(startDate);
        	if (start==null) {		//invalid start date
        		 logSearchBy(module);
        		 ServerResponse response = new ServerResponse();
                 response.getParsedResponse().setError("<html>" + INVALID_START_DATE_MESSAGE + "</html>");
                 logInitialResponse(response);
                 return response;
        	}
        	String endDate = module.getFunction(1).getParamValue();
        	Date end = parseDate(endDate);
        	if (end==null) {		//invalid end date
        		 logSearchBy(module);
        		 ServerResponse response = new ServerResponse();
                 response.getParsedResponse().setError("<html>" + INVALID_END_DATE_MESSAGE + "</html>");
                 logInitialResponse(response);
                 return response;
        	}
        	String parcelNumber = module.getFunction(2).getParamValue();
        	if (parcelNumber!=null && parcelNumber.length()!=0 && !parcelNumber.matches("\\d{8,9}")) {
        		logSearchBy(module);
        		ServerResponse response = new ServerResponse();
                response.getParsedResponse().setError("<html>" + INVALID_PARCEL_NUMBER_MESSAGE + "</html>");
                logInitialResponse(response);
                return response;
        	}
        	if (StringUtils.isEmpty(parcelNumber)) {
        			int days = Math.abs(Days.daysBetween(new DateTime(start), new DateTime(end)).getDays());
        			if (days>60) {
        				logSearchBy(module);
        				ServerResponse response = new ServerResponse();
                        response.getParsedResponse().setError("<html>" + DATE_PARCEL_MESSAGE + "</html>");
                        logInitialResponse(response);
                        return response;
        			}
        	}
        }
		return super.SearchBy(module, sd);
    }
	
	@Override
    public ServerResponse GetLink(String vsRequest, boolean vbEncodedOrIsParentSite) throws ServerResponseException {
    	    	
    	String link = StringUtils.extractParameter(vsRequest, "Link=(.*)");
    	if (org.apache.commons.lang.StringUtils.containsIgnoreCase(link, ro.cst.tsearch.connection.http3.OHCuyahogaRO.IMAGE_ADDRESS)) {
    		String instrno = StringUtils.extractParameter(vsRequest, "instrno=([^&?]*)");
    		String book = StringUtils.extractParameter(vsRequest, "book=([^&?]*)");
    		String page = StringUtils.extractParameter(vsRequest, "page=([^&?]*)");
    		String date = StringUtils.extractParameter(vsRequest, "date=([^&?]*)");
    		String type = StringUtils.extractParameter(vsRequest, "type=([^&?]*)");
    		String fileName = instrno + "_" + book + "_" + page + "_" + date + "_" + type;
    		return GetImageLink(link, fileName, vbEncodedOrIsParentSite);
        }
    	
    	return super.GetLink(vsRequest, vbEncodedOrIsParentSite);
    	
    }
	
	public ServerResponse GetImageLink(String link, String name, boolean writeImageToClient) throws ServerResponseException {
    	
		String folderName = getImageDirectory() + File.separator + searchId + File.separator;
		new File(folderName).mkdirs();
    	
		String fileName = folderName + name + ".tiff";
		boolean existTiff = FileUtils.existPath(fileName);
    	
		if(!existTiff){
			retrieveImage(link, fileName);
		}
		
		existTiff = FileUtils.existPath(fileName);
			
    	// write the image to the client web-browser
		boolean imageOK = false;
		if(existTiff){
			imageOK = writeImageToClient(fileName, "image/tiff");
		} 
		
		// image not retrieved
		if(!imageOK){ 
	        // return error message
			ParsedResponse pr = new ParsedResponse();
			pr.setError("<br><font color=\"red\"><b>Image not found! Close this page and continue your search!</b></font> ");
			throw new ServerResponseException(pr);			
		}
		// return solved response
		return ServerResponse.createSolvedResponse();  
    }
	
	@Override
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId){
    	
    	DocumentsManagerI documentManager = getSearch().getDocManager();
    	
    	try {
    		documentManager.getAccess();
    		if(documentToCheck != null) {
    			RegisterDocumentI docFound = (RegisterDocumentI) documentManager.getDocument(documentToCheck.getInstrument());
    			if (docFound == null) {
    				String year = data.get("year");
    				int len = year.length();
    				if (instrumentNo.length() > len && instrumentNo.startsWith(year)) {

    					DocumentI clone = documentToCheck.clone();
    					clone.setInstno(instrumentNo.substring(len));
    					docFound = (RegisterDocumentI) documentManager.getDocument(clone.getInstrument());
    				}
    			} 
    			if (docFound != null) {
    				
	    			RegisterDocumentI docToCheck = (RegisterDocumentI) documentToCheck;
	    			
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
    			} else if (!checkMiServerId) {
    				List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, documentToCheck.getInstrument());
    				if (almostLike == null || almostLike.isEmpty()){
    					String year = data.get("year");
        				int len = year.length();
    				
    					if (instrumentNo.length() > len && instrumentNo.startsWith(year)) {
        					DocumentI clone = documentToCheck.clone();
        					clone.setInstno(instrumentNo.substring(len));
        					
        					almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, clone.getInstrument());
    					}
    				}
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
    		}
    	} catch (Exception e) {
			e.printStackTrace();
		} finally {
			documentManager.releaseAccess();
		}
    	return false;
    }
    
    protected boolean retrieveImage(String link, String fileName) {
    	
    	byte[] imageBytes = null;

		HttpSite3 site = HttpManager3.getSite(getCurrentServerName(), searchId);
		try {
			imageBytes = ((ro.cst.tsearch.connection.http3.OHCuyahogaRO)site).getImage(link, fileName);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager3.releaseSite(site);
		}

		ServerResponse resp = new ServerResponse();

		if (imageBytes != null) {
			afterDownloadImage(true);
		} else {
			return false;
		}

		resp.setImageResult(new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, "image/tiff"));

		if (!ro.cst.tsearch.utils.FileUtils.existPath(fileName)) {
			FileUtils.writeByteArrayToFile(resp.getImageResult().getImageContent(), fileName);
		}

		return true;
    }
	
    @Override
	public DownloadImageResult saveImage(ImageI image) throws ServerResponseException {
		/**
		 * Force deletion of parameters from last query
		 */
		getTSConnection().setQuery("");
		
		File f = new File(image.getPath());
		ImageLinkInPage ilip = ImageTransformation.imageToImageLinkInPage(image);
		DownloadImageResult res = saveImage( ilip );

		if( res == null){
			return null;
		}
		if (res.getStatus() == DownloadImageResult.Status.OK){		
			if (!f.exists()){
				FileUtils.writeByteArrayToFile(res.getImageContent(), image.getPath());
			}
			
			image.setSaved(true);
		} else {
			return null;
		}
		
		return res;
	}
    
    @Override
    protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {
    	
		String link = image.getLink().replaceFirst("Link=", "");
    	String fileName = image.getPath();
    	if(retrieveImage(link, fileName)){
    		byte[] b = FileUtils.readBinaryFile(fileName);
    		return new DownloadImageResult(DownloadImageResult.Status.OK, b, image.getContentType());
    	}
    	
    	return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
    }
	
    public InstrumentGenericIterator getInstrumentIteratorForFakeDocsFromDTG(boolean instrumentType) {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId) {

			private static final long serialVersionUID = 5399351945130601258L;

			@Override
			protected String cleanInstrumentNo(String inst, int year) {
				if (!StringUtils.isEmpty(inst) && inst.length() <=8 ) {
					inst = org.apache.commons.lang.StringUtils.leftPad(inst, 8, "0");
					if (year != SimpleChapterUtils.UNDEFINED_YEAR) {
						inst = Integer.toString(year) + inst;
					}
				}
				return inst;
			}
			
			@Override
			protected List<DocumentI> getDocumentsWithInstrumentsFlexible(DocumentsManagerI manager, InstrumentI instrumentI) {
				List<DocumentI> result = manager.getDocumentsWithInstrumentsFlexible(false, instrumentI);
				if (!result.isEmpty()) {
					return result;
				} else {
					String instno = instrumentI.getInstno();
					int year = instrumentI.getYear();
					if (year != SimpleChapterUtils.UNDEFINED_YEAR) {
						String yearString = Integer.toString(year);
						int len = yearString.length();
						if (instno.length() > len && instno.startsWith(yearString)) {
							instno = instno.substring(len);
							InstrumentI clone = instrumentI.clone();
							clone.setInstno(instno);
							return manager.getDocumentsWithInstrumentsFlexible(false, clone);
						}	
					}
					return result;
				}
			}

		};

		if (instrumentType) {
			instrumentGenericIterator.enableInstrumentNumber();
		} else {
			instrumentGenericIterator.enableBookPage();
		}
		instrumentGenericIterator.setDoNotCheckIfItExists(true);
		instrumentGenericIterator.setCheckOnlyFakeDocs(true);
		instrumentGenericIterator.setCheckRelatedOfFakeDocs(true);
		instrumentGenericIterator.setLoadFromRoLike(true);
		instrumentGenericIterator.setDsToLoad(new String[]{"DG"});
		return instrumentGenericIterator;
	}
    
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		TSServerInfoModule module = null;
		
		GenericMultipleLegalFilter defaultLegalFilter = new GenericMultipleLegalFilter(searchId);
		defaultLegalFilter.setAdditionalInfoKey(AdditionalInfoKeys.RO_LOOK_UP_DATA);
		defaultLegalFilter.setUseLegalFromSearchPage(true);
		
		FilterResponse pinFilter = PINFilterFactory.getPinFilter(searchId, true, true);
		FilterResponse addressHighPassFilter = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d);
		LastTransferDateFilter lastTransferDateFilter = new LastTransferDateFilter(searchId);
    	GenericNameFilter defaultNameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
    	FilterResponse rejectSavedDocumentsFilter = new RejectAlreadySavedDocumentsFilterResponse(searchId);
		
    	 // search by instrument number list from Fake Documents saved from DTG
		InstrumentGenericIterator instrumentIterator = getInstrumentIteratorForFakeDocsFromDTG(true);
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			"Search again for Fake docs and their related docs from DTG");
		module.clearSaKeys();
		module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
		module.setSaKey(6, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		module.setSaKey(7, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
		
		module.addIterator(instrumentIterator);
        modules.add(module);
        
        // search by book and page list from Fake Documents saved from DTG
        instrumentIterator = getInstrumentIteratorForFakeDocsFromDTG(false);
        module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
        module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			"Search again for Fake docs and their related docs from DTG");
        module.clearSaKeys();
        module.setIteratorType(ModuleStatesIterator.TYPE_BOOK_PAGE_LIST_SEARCH);
        module.setSaObjKey(SearchAttributes.LD_BOOKPAGE);
        module.getFunction(3).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
        module.getFunction(4).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
        
        module.setSaKey(6, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		module.setSaKey(7, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
        
        module.addIterator(instrumentIterator);
        modules.add(module);
                
    	//search with references (instrument number) from AO/Tax like documents	
    	InstrumentGenericIterator instrumentNumberGenericIterator = new InstrumentGenericIterator(searchId) {
			private static final long	serialVersionUID	= -3928557047611571435L;

			@Override
			protected String cleanInstrumentNo(String inst, int year) {
				if (!StringUtils.isEmpty(inst) && inst.length()<=8) {
					inst = org.apache.commons.lang.StringUtils.leftPad(inst, 8, "0");
					if (year!=SimpleChapterUtils.UNDEFINED_YEAR) {
						inst = Integer.toString(year) + inst;
					}
				}
				return inst;
			}
			
			@Override
			protected List<DocumentI> getDocumentsWithInstrumentsFlexible(DocumentsManagerI manager, InstrumentI instrumentI) {
				List<DocumentI> result = manager.getDocumentsWithInstrumentsFlexible(false, instrumentI);
				if (!result.isEmpty()) {
					return result;
				} else {
					String instno = instrumentI.getInstno();
					int year = instrumentI.getYear();
					if (year!=SimpleChapterUtils.UNDEFINED_YEAR) {
						String yearString = Integer.toString(year);
						int len = yearString.length();
						if (instno.length()>len && instno.startsWith(yearString)) {
							instno = instno.substring(len);
							InstrumentI clone = instrumentI.clone();
							clone.setInstno(instno);
							return manager.getDocumentsWithInstrumentsFlexible(false, clone);
						}	
					}
					return result;
				}
			}
			
		};
    	
		instrumentNumberGenericIterator.enableInstrumentNumber();
    	module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
    	module.clearSaKeys();
    	module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			"Search with references from AO/Tax like documents");
    	module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
    	module.setSaKey(6, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		module.setSaKey(7, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
    	if (isUpdate()) {
    		module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module));
    	}
    	module.addIterator(instrumentNumberGenericIterator);
    	modules.add(module);
    	
    	//search with references (book and page) from AO/Tax like documents	
		InstrumentGenericIterator bookPageGenericIterator = new InstrumentGenericIterator(searchId);
		bookPageGenericIterator.enableBookPage();
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
		module.clearSaKeys();
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				"Search with references from AO/Tax like documents");
		module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
		module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
		module.setSaKey(6, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		module.setSaKey(7, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
		if (isUpdate()) {
			module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module));
		}
		module.addIterator(bookPageGenericIterator);
		modules.add(module);
		
		//search with PIN
		if(hasPin()) {
			String parcelNo = getSearchAttribute(SearchAttributes.LD_PARCELNO_GENERIC_TR);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(2).forceValue(parcelNo);
			module.setSaKey(0, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			module.setSaKey(1, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			module.addFilterForNext(new ParcelNumberFilterResponseForNext(parcelNo, searchId));
			modules.add(module);
		}
		
		ArrayList<NameI> searchedNames = null;
		
		//search with owners
		if (hasOwner()) {
			searchedNames = addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS, null,
					new FilterResponse[]{pinFilter, defaultNameFilter, defaultLegalFilter, addressHighPassFilter, lastTransferDateFilter, rejectSavedDocumentsFilter},
					new DocsValidator[]{},
					new DocsValidator[]{pinFilter.getValidator(),
										defaultLegalFilter.getValidator(),
					                    addressHighPassFilter.getValidator(),
										lastTransferDateFilter.getValidator(),
										rejectSavedDocumentsFilter.getValidator()});
		}
		
		//OCR last transfer - instrument number search
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
		module.clearSaKeys();
		module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
		module.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
		module.setSaKey(6, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		module.setSaKey(7, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
		OcrOrBootStraperIterator ocrInstNoIteratoriterator = new OcrOrBootStraperIterator(searchId);
		ocrInstNoIteratoriterator.setInitAgain(true);
		module.addIterator(ocrInstNoIteratoriterator);
		if (isUpdate()) {
			module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module));
		}
		module.addFilter(pinFilter);
		module.addFilter(defaultLegalFilter);
		module.addFilter(addressHighPassFilter);
		module.addFilter(rejectSavedDocumentsFilter);
		modules.add(module);
		
		//OCR last transfer - book and page search
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
		module.clearSaKeys();
		module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
		module.getFunction(3).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
		module.getFunction(4).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
		module.setSaKey(6, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		module.setSaKey(7, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
		OcrOrBootStraperIterator ocrBPIteratoriterator = new OcrOrBootStraperIterator(searchId);
		ocrBPIteratoriterator.setInitAgain(true);
	    module.addIterator(ocrBPIteratoriterator);
		if (isUpdate()) {
			module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module));
		}
		module.addFilter(pinFilter);
		module.addFilter(defaultLegalFilter);
		module.addFilter(addressHighPassFilter);
		module.addFilter(rejectSavedDocumentsFilter);
		modules.add(module);
		
		//search with extra names from search page (for example added by OCR)	
		searchedNames = addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS, searchedNames,
				new FilterResponse[]{defaultNameFilter, pinFilter, defaultLegalFilter, addressHighPassFilter, lastTransferDateFilter, rejectSavedDocumentsFilter},
				new DocsValidator[]{},
				new DocsValidator[]{pinFilter.getValidator(),
									defaultLegalFilter.getValidator(),
									addressHighPassFilter.getValidator(),
									lastTransferDateFilter.getValidator(),
									rejectSavedDocumentsFilter.getValidator()});
	    	
		//search with buyers
		if(hasBuyer()) {
			FilterResponse nameFilterBuyer = NameFilterFactory.getDefaultNameFilter(SearchAttributes.BUYER_OBJECT, getSearch().getID(), null);
			
			addNameSearch(modules, serverInfo, SearchAttributes.BUYER_OBJECT, TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS, null,
					new FilterResponse[]{nameFilterBuyer, pinFilter, defaultLegalFilter, addressHighPassFilter, 
										 DoctypeFilterFactory.getDoctypeBuyerFilter(searchId), lastTransferDateFilter, rejectSavedDocumentsFilter},
					new DocsValidator[]{},
					new DocsValidator[]{pinFilter.getValidator(),
										defaultLegalFilter.getValidator(),
										addressHighPassFilter.getValidator(),
										lastTransferDateFilter.getValidator(),
										rejectSavedDocumentsFilter.getValidator()});
		}
		
		serverInfo.setModulesForAutoSearch(modules);
	
	}
	
	protected ArrayList<NameI> addNameSearch(
			List<TSServerInfoModule> modules, 
			TSServerInfo serverInfo,
			String key, 
			String extraInformation,
			ArrayList<NameI> searchedNames, 
			FilterResponse[] filters,
			DocsValidator[] docsValidators,
			DocsValidator[] docsValidatorsCrossref) {
			
		List<NameI> newNames = new ArrayList<NameI>();
		
		//person name
		{
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, extraInformation);
			module.setSaObjKey(key);
			module.clearSaKeys();
			module.setIteratorType(9, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(10, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			module.setSaKey(6, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			module.setIteratorType(6, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			module.setSaKey(7, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			module.addFilterForNext(new NameFilterForNext(searchId));
			
			if(filters != null) {
				for (FilterResponse filterResponse : filters) {
					module.addFilter(filterResponse);
				}
			}
			addFilterForUpdate(module, true);
			if(docsValidators != null) {
				for (DocsValidator docsValidator : docsValidators) {
					module.addValidator(docsValidator);
				}
			}
			if(docsValidatorsCrossref != null) {
				for (DocsValidator docsValidator : docsValidatorsCrossref) {
					module.addCrossRefValidator(docsValidator);
				}
			}
			module.addCrossRefValidator(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module).getValidator());
			
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, false, searchId, new String[] {"L;F;" });
			nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
			nameIterator.setInitAgain(true);		//initialize again after all parameters are set
			
			if (searchedNames!=null) {
				nameIterator.setSearchedNames(searchedNames);
			}
			newNames.addAll(nameIterator.getSearchedNames()) ;
			
			module.addIterator(nameIterator);
			
			modules.add(module);
			
		}
		
		//company name
		{
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, extraInformation);
			module.setSaObjKey(key);
			module.clearSaKeys();
			module.setIteratorType(12, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
			module.setSaKey(6, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			module.setIteratorType(6, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			module.setSaKey(7, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			
			if(filters != null) {
				for (FilterResponse filterResponse : filters) {
					module.addFilter(filterResponse);
				}
			}
			addFilterForUpdate(module, true);
			if(docsValidators != null) {
				for (DocsValidator docsValidator : docsValidators) {
					module.addValidator(docsValidator);
				}
			}
			if(docsValidatorsCrossref != null) {
				for (DocsValidator docsValidator : docsValidatorsCrossref) {
					module.addCrossRefValidator(docsValidator);
				}
			}
			module.addCrossRefValidator(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module).getValidator());
			
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, false, searchId, new String[] {"L;F;" });
			nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.COMPANY_NAME);
			nameIterator.setInitAgain(true);		//initialize again after all parameters are set
			
			if (searchedNames!=null) {
				nameIterator.setSearchedNames(searchedNames);
			}
			newNames.addAll(nameIterator.getSearchedNames()) ;
			
			module.addIterator(nameIterator);
			modules.add(module);
		}
		
		if(searchedNames == null) {
			searchedNames = new ArrayList<NameI>();
			searchedNames.addAll(newNames);
		}
		
		return searchedNames;
	}
	
	@Override
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {

		String dateFormat = "MM/dd/yyyy";
		FilterResponse pinFilter = PINFilterFactory.getPinFilter(searchId, true, true);
		GenericMultipleLegalFilter defaultLegalFilter = new GenericMultipleLegalFilter(searchId);
		defaultLegalFilter.setAdditionalInfoKey(AdditionalInfoKeys.RO_LOOK_UP_DATA);
		defaultLegalFilter.setUseLegalFromSearchPage(true);
		FilterResponse addressHighPassFilter = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d);
		FilterResponse[] filters = new FilterResponse[]{pinFilter, defaultLegalFilter, addressHighPassFilter};
		
		ConfigurableNameIterator nameIterator = null;
		String endDate = new SimpleDateFormat(dateFormat).format(new Date());

		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
		GBManager gbm = (GBManager) sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

		for (String id : gbm.getGbTransfers()) {

			String date = gbm.getDateForSearch(id, dateFormat, searchId);
			
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.setIndexInGB(id);
			module.setTypeSearchGB("grantor");
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
			
			if (date != null) {
				module.getFunction(6).forceValue(date);
			}
			module.forceValue(7, endDate);
				
			module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
			for (FilterResponse filterResponse : filters) {
				module.addFilter(filterResponse);
			}
			module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				
			module.setIteratorType(9, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(10, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId,	new String[] { "L;F;" });
			module.addIterator(nameIterator);
			module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
			modules.add(module);
			
			if (gbm.getNamesForBrokenChain(id, searchId).size() > 0) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.setIndexInGB(id);
				module.setTypeSearchGB("grantee");
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				
				date = gbm.getDateForSearchBrokenChain(id, dateFormat, searchId);
				if (date != null) {
					module.getFunction(6).forceValue(date);
				}
				module.forceValue(7, endDate);
				
				module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				for (FilterResponse filterResponse : filters) {
					module.addFilter(filterResponse);
				}
				module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				
				module.setIteratorType(9, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(10, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId,	new String[] { "L;F;" });
				module.addIterator(nameIterator);
				module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
				module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				modules.add(module);
			}
		}
		serverInfo.setModulesForGoBackOneLevelSearch(modules);
	}
	
	@Override
	public List<TSServerInfoModule> getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		
		if(restoreDocumentDataI == null) {
			return null;
		}
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		TSServerInfoModule module = null;
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		String instno = restoreDocumentDataI.getInstrumentNumber();
		Date today = new Date();
		String date = (new SimpleDateFormat("M/d/yyyy")).format(today);
		
		if (!StringUtils.isEmpty(instno)) {
			module = new TSServerInfoModule(getDefaultServerInfo().getModule(TSServerInfo.NAME_MODULE_IDX));
			module.forceValue(1, instno);
			module.forceValue(6, date);
			module.forceValue(7, date);
			module.forceValue(14, "-1");
			module.forceValue(15, "-1");
			module.forceValue(16, "-1");
			module.forceValue(17, "-1");
			module.forceValue(18, "-1");
			module.forceValue(19, "1");
			module.forceValue(20, "Begin Search");
			module.getFilterList().clear();
			if (StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
				HashMap<String, String> filterCriteria = new HashMap<String, String>();
				filterCriteria.put("Book", book);
				filterCriteria.put("Page", page);
				GenericInstrumentFilter filter = new GenericInstrumentFilter(searchId, filterCriteria);
				module.addFilter(filter);
			}
			modules.add(module);
		}
		
		if (StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			module = new TSServerInfoModule(getDefaultServerInfo().getModule(TSServerInfo.NAME_MODULE_IDX));
			module.forceValue(3, book);
			module.forceValue(4, page);
			module.forceValue(6, date);
			module.forceValue(7, date);
			module.forceValue(14, "-1");
			module.forceValue(15, "-1");
			module.forceValue(16, "-1");
			module.forceValue(17, "-1");
			module.forceValue(18, "-1");
			module.forceValue(19, "1");
			module.forceValue(20, "Begin Search");
			module.getFilterList().clear();
			if (!StringUtils.isEmpty(instno)) {
				HashMap<String, String> filterCriteria = new HashMap<String, String>();
				filterCriteria.put("InstrumentNumber", instno);
				GenericInstrumentFilter filter = new GenericInstrumentFilter(searchId, filterCriteria);
				module.addFilter(filter);
			}
			modules.add(module);
		}
		
		return modules;
	}
	
	@Override
	protected NameI getNameFromModule(TSServerInfoModule module) {
		NameI name = new Name();
		if (module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX && module.getFunctionCount() > 12) {
			String lastName = module.getFunction(9).getParamValue();
			String firstName = module.getFunction(10).getParamValue();
			String companyName = module.getFunction(12).getParamValue();
			if (!StringUtils.isEmpty(lastName)) {
				name.setLastName(lastName);
				if (!StringUtils.isEmpty(firstName)) {
					name.setFirstName(firstName);
				}
				return name;
			} else if (!StringUtils.isEmpty(companyName)) {
				name.setLastName(companyName);
				name.setCompany(true);
				return name;
			}
		}	
		return null;
	}
		
	protected void logSearchBy(TSServerInfoModule module, Map<String, String> params) {
    	
    	if(module.isVisible() || "GB_MANAGER_OBJECT".equals(module.getSaObjKey())) {//B 4511
        
    		Search search = getSearch();
	        // determine whether it's an automatic search
	        boolean automatic = (search.getSearchType() != Search.PARENT_SITE_SEARCH) 
	        		|| (GPMaster.getThread(searchId) != null);
	        boolean imageSearch = module.getLabel().equalsIgnoreCase("image search") || 
	                              module.getModuleIdx() == TSServerInfo.IMG_MODULE_IDX;
    		
    		if (automatic) {
    			if (module.getModuleIdx()==TSServerInfo.NAME_MODULE_IDX) {
    				module.getFunction(14).setLoggable(false);	//1.
        			module.getFunction(15).setLoggable(false);	//2.
        			module.getFunction(16).setLoggable(false);	//3.
        			module.getFunction(17).setLoggable(false);	//4.
        			module.getFunction(18).setLoggable(false);	//5.
        			module.getFunction(19).setLoggable(false);	//Sort Query By
    			} else if (module.getModuleIdx()==TSServerInfo.PARCEL_ID_MODULE_IDX) {
    				module.getFunction(3).setLoggable(false);	//Sort Query By
    			}
    		}
    		
	        // get parameters formatted properly
	        Map<String,String> moduleParams = params;
	        if(moduleParams == null){
	        	moduleParams = module.getParamsForLog();
	        }
	        
	        // create the message
	        StringBuilder sb = new StringBuilder();
	        SearchLogFactory sharedInstance = SearchLogFactory.getSharedInstance();
	        SearchLogPage searchLogPage = sharedInstance.getSearchLogPage(searchId);
	        sb.append("</div>");
	        
	        Object additional = GetAttribute("additional");
			if(Boolean.TRUE != additional){
	        	searchLogPage.addHR();
	        	sb.append("<hr/>");	
	        }
			int fromRemoveForDB = sb.length();
	        
			//searchLogPage.
	        sb.append("<span class='serverName'>");
	        String serverName = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getName();
			sb.append(serverName);
	        sb.append("</span> ");
	
	       	sb.append(automatic? "automatic":"manual");
	       	Object info = module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION);
	       	if(StringUtils.isNotEmpty(module.getLabel())) {
		        
		        if(info!=null){
		        	sb.append(" - " + info + "<br>");
		        }
		        sb.append(" <span class='searchName'>");
		        sb.append(module.getLabel());
	       	} else {
	       		sb.append(" <span class='searchName'>");
		        if(info!=null){
		        	sb.append(" - " + info + "<br>");
		        }
	       	}
	        sb.append("</span> by ");
	        
	        boolean firstTime = true;
	        for(Entry<String,String> entry : moduleParams.entrySet() ){
	        	String key = entry.getKey();
	        	String value = entry.getValue();
	        	if ("1.".equals(key)||"2.".equals(key)||"3.".equals(key)||"4.".equals(key)||"5.".equals(key)) {
	        		value = getDocumentType(value);
	        	} else if ("Sort Query By".equals(key)||"Sort By".equals(key)) {
	        		value = getSortBy(value);
	        	}
	        	value = value.replaceAll("(, )+$",""); 
	        	if(!firstTime){
	        		sb.append(", ");
	        	} else {
	        		firstTime = false;
	        	}
	        	sb.append(entry.getKey().replaceAll("&lt;br&gt;", "") + " = <b>" + value + "</b>");
	        } 
	        int toRemoveForDB = sb.length();
	        //log time when manual is starting        
	        if (!automatic || imageSearch){
	        	sb.append(" ");
	        	sb.append(SearchLogger.getTimeStamp(searchId));
	        }
	        sb.append(":<br/>");
	        
	        // log the message
	        SearchLogger.info(sb.toString(),searchId);   
	        ModuleShortDescription moduleShortDescription = new ModuleShortDescription();
	        moduleShortDescription.setDescription(sb.substring(fromRemoveForDB, toRemoveForDB));
	        moduleShortDescription.setSearchModuleId(module.getModuleIdx());
	        search.setAdditionalInfo(TSServerInfoConstants.TS_SERVER_INFO_MODULE_DESCRIPTION, moduleShortDescription);
	        String user=InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser().getAttribute(1).toString();
	        SearchLogger.info(StringUtils.createCollapsibleHeader(),searchId);
	        searchLogPage.addModuleSearchParameters(serverName,additional, info, moduleParams,module.getLabel(), automatic, imageSearch,user);
    	}  
        
    }
	
}
