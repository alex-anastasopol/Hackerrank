package ro.cst.tsearch.servers.types;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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

import org.apache.commons.lang.ArrayUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.bean.recoverdocument.ModuleShortDescription;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.data.StateCountyManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.log.SearchLogFactory;
import ro.cst.tsearch.log.SearchLogPage;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericMultipleLegalFilter;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterForNext;
import ro.cst.tsearch.search.filter.newfilters.name.SubdivisionFilter;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.module.OcrOrBootStraperIterator;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.parentsite.ModuleWrapperManager;
import ro.cst.tsearch.servers.response.CrossRefSet;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.threads.GPMaster;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.address.Address;
import com.stewart.ats.base.address.AddressI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.Subdivision;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;

/**
 *Anne Arundel, Baltimore, Charles, Frederick, Harford, Howard, Prince George's, St. Mary's, Washington
 */

@SuppressWarnings("deprecation")
public class MDGenericRO extends TSServerROLike {

	private static final long serialVersionUID = 7746904263125035399L;
	
	private static Map<String, String> AS_MAP = new HashMap<String, String>();
	static {
		AS_MAP.put("B", "Both");
		AS_MAP.put("R", "Grantor");
		AS_MAP.put("E", "Grantee");
	}
	
	private static Map<String, String> SORT_BY_MAP = new HashMap<String, String>();
	static {
		SORT_BY_MAP.put("D_Date", "Date Descending");
		SORT_BY_MAP.put("A_Date", "Date Ascending");
		SORT_BY_MAP.put("book/page", "Book / Page");
		SORT_BY_MAP.put("Instrument", "Instrument Type");
		SORT_BY_MAP.put("Surname", "Name");
		SORT_BY_MAP.put("Corporation", "Corporation");
	}
	
	private static Map<String, String> INDIVIDUAL_NAME_TYPE_MAP = new HashMap<String, String>();
	static {
		INDIVIDUAL_NAME_TYPE_MAP.put("Is", "Exact Last Name");
		INDIVIDUAL_NAME_TYPE_MAP.put("Contains", "Last Name Contains String");
		INDIVIDUAL_NAME_TYPE_MAP.put("Begins", "Last Name Begins With");
		INDIVIDUAL_NAME_TYPE_MAP.put("Ends", "Last Name Ends With");
	}
	
	private static Map<String, String> CORPORATION_NAME_TYPE_MAP1 = new HashMap<String, String>();
	static {
		CORPORATION_NAME_TYPE_MAP1.put("Is", "Exact Name");
		CORPORATION_NAME_TYPE_MAP1.put("Contains", "Contains String");
	}
	
	private static Map<String, String> CORPORATION_NAME_TYPE_MAP2 = new HashMap<String, String>();
	static {
		CORPORATION_NAME_TYPE_MAP2.put("Is", "Exact Corporation Name");
		CORPORATION_NAME_TYPE_MAP2.put("Contains", "Corporation Name Contains String");
		CORPORATION_NAME_TYPE_MAP2.put("Begins", "Corporation Name Begins With");
		CORPORATION_NAME_TYPE_MAP2.put("Ends", "Corporation Name Ends With");
	}
	
	public static final String BOOK_PAGE_PATT1 = "(?is)<a[^>]+href=\"([^\"]+)\"[^>]*>\\s*Book\\s+([^,]+),\\s*p?p\\.\\s+([^<]+)<";
	public static final String BOOK_PAGE_PATT2 = "(?is)\\bBook\\s+([^,]+),\\s*p?p\\.\\s+([^<]+)";
	public static final String DETAILS_PAGE_PATT = "(?is)<a[^>]+href=\"javascript:loadwindow\\('([^']+)'[^)]+\\)\">";
	
	public MDGenericRO(long searchId) {
		super(searchId);
	}

	public MDGenericRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	public void setFieldNote(TSServerInfo msiServerInfoDefault, int module, int index, ModuleWrapperManager moduleWrapperManager, String siteName) {
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(module);
		if(tsServerInfoModule != null) {
			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
				
			}
			PageZone pageZone = (PageZone)tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if(StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, index, nameToIndex.get(functionName));
					if(comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {

		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		
		ModuleWrapperManager moduleWrapperManager = ModuleWrapperManager.getInstance();
		DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID);
		String siteName = StateCountyManager.getInstance().getSTCounty(dataSite.getCountyId()) + dataSite.getSiteType();
		
		setFieldNote(msiServerInfoDefault, TSServerInfo.BOOK_AND_PAGE_MODULE_IDX, 0, moduleWrapperManager, siteName);
		setFieldNote(msiServerInfoDefault, TSServerInfo.NAME_MODULE_IDX, 1, moduleWrapperManager, siteName);
		setFieldNote(msiServerInfoDefault, TSServerInfo.MODULE_IDX38, 2, moduleWrapperManager, siteName);
		setFieldNote(msiServerInfoDefault, TSServerInfo.MODULE_IDX39, 3, moduleWrapperManager, siteName);
		setFieldNote(msiServerInfoDefault, TSServerInfo.MODULE_IDX40, 4, moduleWrapperManager, siteName);
		setFieldNote(msiServerInfoDefault, TSServerInfo.MODULE_IDX41, 5, moduleWrapperManager, siteName);
		setFieldNote(msiServerInfoDefault, TSServerInfo.ADDRESS_MODULE_IDX, 6, moduleWrapperManager, siteName);
		setFieldNote(msiServerInfoDefault, TSServerInfo.TAX_BILL_MODULE_IDX, 5, moduleWrapperManager, siteName);
		
		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;

	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		if (rsResponse.indexOf("The web site you are accessing has experienced an unexpected error") > -1) {
			Response.getParsedResponse().setError(NO_DATA_FOUND);
			Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
			return;
		}
		
		if (rsResponse.indexOf("There are no exact matches for your search criteria") > -1) {
			Response.getParsedResponse().setError(NO_DATA_FOUND);
			Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
			return;
		}
		
		if (rsResponse.indexOf("There were no results for") > -1) {
			Response.getParsedResponse().setError(NO_DATA_FOUND);
			Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
			return;
		}
		
		if (rsResponse.indexOf("Please enter either a first/last name OR a corporation name") > -1) {
			Response.getParsedResponse().setError("Please enter either a first/last name OR a corporation name");
			return;
		}
		
		switch (viParseID) {
		
		case ID_SEARCH_BY_BOOK_AND_PAGE:		//Search for Instruments by Book and Page
		case ID_SEARCH_BY_NAME:					//Search for Instruments by Grantor/Grantee
		case ID_SEARCH_BY_MODULE38:				//Search for Individual Grantor/Grantee Last Name by Soundex
		case ID_SEARCH_BY_MODULE39:				//Search for Corporation Name by Soundex
		case ID_SEARCH_BY_MODULE40:				//Jump to Land Record Volume/Page
		case ID_SEARCH_BY_MODULE41:				//Search By Block and/or Street
		case ID_SEARCH_BY_ADDRESS:				//Search by Street Address
		case ID_SEARCH_BY_TAX_BIL_NO:			//Search by SDAT Tax Account Number
			
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if (smartParsedResponses.size() == 0) {
				return;
			}

			parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
			parsedResponse.setOnlyResponse(outputTable.toString());

			if (!isJumpSearch(Response.getQuerry())) {
				String header = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
				String footer = "";

				StringBuilder nextLink = new StringBuilder();
				String navigationLinks = getNavigationLinks(rsResponse, nextLink);
				if (nextLink.length()>0) {
					parsedResponse.setNextLink(nextLink.toString());
				}
					
				header += "<table><tr><td>" + navigationLinks +  "<table>" + "<tr><th>" + SELECT_ALL_CHECKBOXES + "</th>" + 
					"<th>Date Recorded</th><th>Grantor / Grantee</th><th>Instrument</th><th>Volume</th><th>Remarks</th><th>Image</th></tr>";

				Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);

				if (numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
					footer = "</table>" + navigationLinks + "</td></tr></table>"
						+ CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer) numberOfUnsavedDocument);
				} else {
					footer = "</table>" + navigationLinks + "</td></tr></table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
				}

				parsedResponse.setHeader(header);
				parsedResponse.setFooter(footer);
			}
			
			break;

		case ID_DETAILS:
		case ID_SAVE_TO_TSD:
			
			StringBuilder serialNumber = new StringBuilder();
			HashMap<String, String> data = new HashMap<String, String>();
			String details = getDetails(rsResponse, serialNumber, data, Response);
			String filename = serialNumber + ".html";
			
			if (viParseID != ID_SAVE_TO_TSD) {
				try {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + URLDecoder.decode(originalLink, "UTF-8");
					
					if (isInstrumentSaved(serialNumber.toString(), null, data, false)){
						details += CreateFileAlreadyInTSD();
					}
					else {
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
				
				//remove links
				details = details.replaceAll("(?is)<a[^>]+href=[^>]*>([^<]*)</a>", "$1");
				
				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
				Response.getParsedResponse().setResponse(details);
				
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				saveRelatedDocuments(Response);
				
			}
			break;

			case ID_GET_LINK:
				ParseResponse(sAction, Response, sAction.contains("/dsp_fullcitation.cfm") ? ID_DETAILS : ID_SEARCH_BY_BOOK_AND_PAGE);
			break;
		}
	}
	
	@SuppressWarnings({ "unchecked" })
	protected void saveRelatedDocuments(ServerResponse Response) {
		
		ParsedResponse parsedResponse = Response.getParsedResponse();
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
        	String book = instr.getBook();
        	String page = instr.getPage();
        	String link = "";
        	HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
    		try {
    			link = ((ro.cst.tsearch.connection.http2.MDGenericRO)site).searchByBookPage(book, page);
    		} catch (Exception e) {
    			e.printStackTrace();
    		} finally {
    			HttpManager.releaseSite(site);
    		}
        	if (!StringUtils.isEmpty(link)) {
        		link = CreatePartialLink(TSConnectionURL.idGET) + link;
				ParsedResponse prChild = new ParsedResponse();
				LinkInPage pl = new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD);
				prChild.setPageLink(pl);
				Response.getParsedResponse().addOneResultRowOnly(prChild);
			}
		}
    }
	
	public static String getValue(String text, String label) {
		String ret = "";
		Matcher ma = Pattern.compile("(?is)<b>\\s*" + label + "\\s*</b>(.*?)<br>").matcher(text);
		if (ma.find()) {
			ret = ma.group(1).replaceAll("(?is)&nbsp;", "").trim();
		}
		return ret;
	}
	
	protected String getDetails(String rsResponse, StringBuilder accountId, HashMap<String, String> data, ServerResponse Response) {

		String bookPage = getValue(rsResponse, "Citation:");
		String instrNo = "";
		String imageLink = "";
		String book = "";
		String page = "";
		
		String backupImageLink = "";
		String seq = "";
		LinkInPage lip = Response.getParsedResponse().getPageLink();
		if (lip!=null) {
			String link = lip.getLink();
			seq = StringUtils.extractParameter(link, "seq=([^&?]*)");
		}
		if (!StringUtils.isEmpty(seq)) {
			backupImageLink = (String)mSearch.getAdditionalInfo(getCurrentServerName() + ":imageLink:" + seq);
		}	
		
		Matcher ma = Pattern.compile(BOOK_PAGE_PATT1).matcher(bookPage);
		if (ma.find()) {
			imageLink = ma.group(1).trim();
			book = ma.group(2).trim();
			page = ma.group(3).trim();
			instrNo = book + "_" + page;
			page = page.replaceFirst("-.*", "");
			accountId.append(instrNo);
		} else {	//has no image link
			imageLink = backupImageLink;
			Matcher ma2 = Pattern.compile(BOOK_PAGE_PATT2).matcher(bookPage);
			if (ma2.find()) {
				book = ma2.group(1).trim();
				page = ma2.group(2).trim();
				instrNo = book + "_" + page;
				page = page.replaceFirst("-.*", "");
				accountId.append(instrNo);
				rsResponse = rsResponse.replaceFirst("(?is)(<b>\\s*Citation:\\s*</b>.*?)\\b(" + BOOK_PAGE_PATT2 + ")", "$1 <a href=\"" + imageLink + "\" target=\"_blank\">$2</a>");
			}
		}
		
		if (!StringUtils.isEmpty(imageLink)) {
			imageLink = CreatePartialLink(TSConnectionURL.idGET) + imageLink;
			String sFileLink = instrNo + ".pdf";
			ImageLinkInPage ilip = new ImageLinkInPage(imageLink, sFileLink);
			Response.getParsedResponse().addImageLink(ilip);
		}
		
		/* If from memory - use it as is */
		if (!rsResponse.toLowerCase().contains("<html")) {
			return rsResponse;
		}
		
		rsResponse = rsResponse.replaceFirst("(?is).*?<body>", "");
		rsResponse = rsResponse.replaceFirst("(?is)</body>.*", "");
		rsResponse = rsResponse.replaceAll("(?is)<div[^>]*>.*?</div>", "");
		
		String recordedDate = getValue(rsResponse, "Recordation Date:");
		String year = "";
		int index = recordedDate.lastIndexOf("/");
		if (index>-1 && index<recordedDate.length()-1) {
			year = recordedDate.substring(index+1);
		}
		String type = getValue(rsResponse, "Instrument:");
		
		data.put("instrno", instrNo);
		data.put("book", book);
		data.put("page", page);
		data.put("year", year);
		data.put("type", type);
		
		accountId.append(instrNo);
		
		rsResponse = rsResponse.replaceAll("(?is)(<a[^>]+href=\")[^\"]+(\")([^>]*>)", "$1" + imageLink + "$2 target=\"_blank\"$3");
		
		if (!StringUtils.isEmpty(seq)) {
			String tmpBlock = (String)mSearch.getAdditionalInfo(getCurrentServerName() + ":block:" + seq);
			String tmpRemarks = (String)mSearch.getAdditionalInfo(getCurrentServerName() + ":remarks:" + seq);
			if (!StringUtils.isEmpty(tmpBlock)) {
				tmpBlock = tmpBlock.replaceFirst("(?is)<br>", "");
				tmpBlock = tmpBlock.replaceAll("(?is)<i>\\s*BLOCK:\\s*</i>", "Block No.:");
				rsResponse = rsResponse.replaceFirst("(?is)<b>\\s*Block\\s+No\\.:.*?(<br>)", tmpBlock + "$1");
			}
			if (!StringUtils.isEmpty(tmpRemarks)) {
				tmpRemarks = tmpRemarks.replaceFirst("(?is)<br>", "");
				rsResponse = rsResponse.replaceFirst("(?is)(<td>\\s*<b>\\s*Remarks:\\s*</b>).*?(</td>)", "$1" + tmpRemarks + "$2");
			}
		}
		
		return rsResponse;

	}
	
	protected String getNavigationLinks(String response, StringBuilder nextLink) {

		try {
			
			org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(response, null);
			NodeList nodeList = parser.parse(null);
			NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("align", "center"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("border", "0"), true);
			
			for (int i=0;i<mainTableList.size();i++) {
				TableTag mainTable = (TableTag)mainTableList.elementAt(i);
				String bgcolor = mainTable.getAttribute("bgcolor");
				if (StringUtils.isEmpty(bgcolor)) {
					String table = mainTable.toHtml();
					table = table.replaceAll("(?is)<tr>\\s*<td[^>]+>\\s*\\bDisplaying\\b.*?\\bRecords\\b\\.\\s*</td>\\s*</tr>", "");
					table = table.replaceAll("(?is)\\bclass=\"td[45]\"", "align=\"center\"");
					String partialLink = CreatePartialLink(TSConnectionURL.idGET);
					table = table.replaceAll("(?is)href=\"([^\"]+)\"", "href=\"" + partialLink + "$1\"");
					Matcher ma = Pattern.compile("\\b\\d+\\b\\s*(<a[^>]+href=\"[^\"]+\"[^>]*>[^<]+</a>)").matcher(table);
					if (ma.find()) {
						nextLink.append(ma.group(1));
					}
					return table;
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return "";
	}
	
	public static String extractImageLink(String rowText, StringBuilder detailsLink, StringBuilder bookPage) {
		String[] split = rowText.split("[^<]/[^Aa]");
		if (split.length==2) {
			Matcher ma1 = Pattern.compile(DETAILS_PAGE_PATT).matcher(split[1]);
			if (ma1.find()) {
				detailsLink.append(ma1.group(1).replaceAll("(?is)&amp;", "&").replaceAll(" ", ""));
			}
			Matcher ma2 = Pattern.compile(BOOK_PAGE_PATT1).matcher(split[0]);
			if (ma2.find()) {
				bookPage.append(ma2.group(2).trim()).append("_").append(ma2.group(3).trim());
				return ma2.group(1).replaceAll("(?is)&amp;", "&");
			}
		}
		return "";
	}
	
	public static boolean isJumpSearch(String link) {
		if (!StringUtils.isEmpty(link)) {
			String clerksInitials = StringUtils.extractParameter(link, "b_ci=([^&?]*)");
			String bookNo = StringUtils.extractParameter(link, "b_bk=([^&?]*)");
			String pageNo = StringUtils.extractParameter(link, "b_sp=([^&?]*)");
			if (!StringUtils.isEmpty(clerksInitials) || !StringUtils.isEmpty(bookNo) || !StringUtils.isEmpty(pageNo)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		if (isJumpSearch(response.getQuerry())) {
			TableTag resultsTable = null;
			String header = "";
			String footer = "";

			try {
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
				NodeList nodeList = htmlParser.parse(null);
				NodeList mainTable = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "70%"), true);

				if (mainTable.size() != 0) {
					resultsTable = (TableTag) mainTable.elementAt(0);
				}
				
				// if there are results
				if (resultsTable!=null && resultsTable.getRowCount()>2) {
					TableRow[] rows = resultsTable.getRows();

					for (int i = 2; i < rows.length; i++) {
						TableRow row = rows[i];

						String htmlRow = row.toHtml();
						ParsedResponse currentResponse = new ParsedResponse();

						String link = row.getColumns()[2].toHtml();
						Matcher matcher = Pattern.compile("(?is)href=\"([^\"]+)\"").matcher(link);
						if (matcher.find()) {
							link = matcher.group(1);
						}
						
						link = CreatePartialLink(TSConnectionURL.idGET) + link;
						htmlRow = htmlRow.replaceAll("(?is)\\bhref=\"[^\"]+\"", "href=\"" + link + "\"");

						currentResponse.setPageLink(new LinkInPage(link, link, TSServer.ID_SEARCH_BY_MODULE40));

						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, htmlRow);
						currentResponse.setOnlyResponse(htmlRow);

						ResultMap m = ro.cst.tsearch.servers.functions.MDGenericRO.parseIntermediaryRow(row);
						Bridge bridge = new Bridge(currentResponse, m, searchId);
						DocumentI document = (RegisterDocumentI) bridge.importData();
						currentResponse.setDocument(document);
						intermediaryResponse.add(currentResponse);

					}

					header = "<table width=\"70%\" border=\"0\"><tr>" + "" +
						"<td><b>Series</b></td><td><b>Date</b></td><td><b>Volume</b></td><td><b>Source</b></td><td><b>Accession No.</b></td></tr>";
					
					response.getParsedResponse().setHeader(header);
					response.getParsedResponse().setFooter(footer + "</table>");

					outputTable.append(table);
				}

			} catch (Throwable t) {
				logger.error("Error while parsing intermediary data", t);
			}

			response.setDisplayMode(ServerResponse.HIDE_ALL_CONTROLS);
			
			return intermediaryResponse;
			
		}
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);

			NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "searchresults"), true);
			
			if (mainTableList.size()==0) {
				return intermediaryResponse;
			}
			
			int numberOfUncheckedElements = 0;
			
			String partialLink = CreatePartialLink(TSConnectionURL.idGET);
			
			TableTag tbl = (TableTag) mainTableList.elementAt(0);
			TableRow[] rows = tbl.getRows();

			LinkedHashMap<String, List<List<String>>> processedRows = new  LinkedHashMap<String, List<List<String>>>();
			
			int index = 1;	//current result index
			int size = rows.length-1;
			int len = 6;	//number of columns in a row
			for (int i=3;i<size;i++) {
				
				int currentLen = rows[i].getColumnCount();
				if (currentLen!=len) {
					continue;
				}
				
				String key = "";
				String imageLink = "";
				StringBuilder detailsLink = new StringBuilder();
				StringBuilder bookPage = new StringBuilder();
				
				List<List<String>> trs = new ArrayList<List<String>>();
				
				imageLink = extractImageLink(rows[i].getColumns()[4].toHtml(), detailsLink, bookPage);
				if (!StringUtils.isEmpty(imageLink)) {
					key = imageLink;
				} else {
					key = bookPage + "_" + rows[i].getColumns()[1].toPlainTextString().trim().replaceAll("/", "-");
				}
				
				if (processedRows.containsKey(key)) {	//document already inserted
					List<List<String>> existingTrs = processedRows.get(key);
					boolean replace = true;
					String tr1 = StringUtils.extractParameter(existingTrs.get(0).get(3), "tr=([^&?]*)");
					String tr2 = StringUtils.extractParameter(rows[i].getColumns()[4].toHtml(), "tr=([^&?]*)");
					if (tr1.matches("\\d+") && tr2.matches("\\d+")) {
						int tr1i = Integer.parseInt(tr1);
						int tr2i = Integer.parseInt(tr2);
						if (tr1i>tr2i) {
							replace = false;
						}
					}
					if (replace) {
						existingTrs.get(0).set(0, rows[i].getColumns()[1].toHtml().replaceAll("(?is)<a[^>]+>([^<]+)</a>", "$1"));
						existingTrs.get(0).set(2, rows[i].getColumns()[3].toHtml().replaceAll("(?is)<a[^>]+>([^<]+)</a>", "$1"));
						existingTrs.get(0).set(3, rows[i].getColumns()[4].toHtml()
							.replaceFirst("(?is)(<td[^>]*>)\\s*(<s>)?(?:<a[^>]+>)?([^<]+)(?:</a>)?(\\s*</s>)?\\s*/.*?(</td>)", 
								"$1$2<a href=\"" + partialLink + detailsLink.toString() + "\">$3</a>$4$5"));
						if (!StringUtils.isEmpty(imageLink)) {
							existingTrs.get(0).set(5, "<td><a href=\"" + partialLink + imageLink + "\" target=\"_blank\">Image</a></td>");
						} else {
							existingTrs.get(0).set(5, "<td>&nbsp;</td>");
						}
					}
					List<String> tds = new ArrayList<String>();
					tds = new ArrayList<String>(); 
					tds.add("");								//Date Recorded
					tds.add(rows[i].getColumns()[2].toHtml());	//Grantor / Grantee
					tds.add("");								//Instrument
					tds.add("");								//Volume
					tds.add(rows[i].getColumns()[5].toHtml());	//Remarks
					tds.add("");								//Image
					existingTrs.add(tds);
					
				} else  {								//document not inserted
					List<String> tds = new ArrayList<String>(); 
					for (int j=1;j<len;j++) {
						if (j==4) {
							tds.add(rows[i].getColumns()[j].toHtml()
								.replaceFirst("(?is)(<td[^>]*>)\\s*(<s>)?(?:<a[^>]+>)?([^<]+)(?:</a>)?(\\s*</s>)?\\s*/.*?(</td>)", 
									"$1$2<a href=\"" + partialLink + detailsLink.toString() + "\">$3</a>$4$5"));
						} else {
							tds.add(rows[i].getColumns()[j].toHtml().replaceAll("(?is)<a[^>]+>([^<]+)</a>", "$1"));
						}
					}
					if (!StringUtils.isEmpty(imageLink)) {
						tds.add("<td><a href=\"" + partialLink + imageLink + "\" target=\"_blank\">Image</a></td>");
					} else {
						tds.add("<td>&nbsp;</td>");
					}
					trs.add(tds);
					processedRows.put(key, trs);
				}
			}	
			
			Iterator<Entry<String, List<List<String>>>> it = processedRows.entrySet().iterator();
			while (it.hasNext()) {
			
				Map.Entry<String, List<List<String>>> pairs = (Map.Entry<String, List<List<String>>>)it.next();
				List<List<String>> trs = pairs.getValue();
				
				String detailsLinkString = "";
				
				StringBuilder sb = new StringBuilder();
				StringBuilder sbRemarksInterm = new StringBuilder();
				StringBuilder sbRemarksBlock = new StringBuilder();
				StringBuilder sbRemarksDet = new StringBuilder();
				if (trs.size()>0) {
					List<String> firstTd = trs.get(0);
					if (firstTd.size()==6) {
						
						sb.append(firstTd.get(0));
						
						ArrayList<String> grantorGrantee = new ArrayList<String>();
						for (int j=0;j<trs.size();j++) {
							List<String> each = trs.get(j);
							if (each.size()==6 && !grantorGrantee.contains(each.get(1))) {
								String s = each.get(1);
								grantorGrantee.add(s);
							}
						}	
						sb.append("<td><table>");
						for (int j=0;j<grantorGrantee.size();j++) {
							sb.append("<tr>");
							sb.append(grantorGrantee.get(j));
							sb.append("</tr>");
						}
						sb.append("</table></td>");
						
						sb.append(firstTd.get(2));
						
						int seq = getSeq();
						String s = firstTd.get(3);
						Matcher ma = Pattern.compile("(?is)\\bhref=\"([^\"]+)\"").matcher(s);
						if (ma.find()) {
							detailsLinkString = ma.group(1) + "&seq=" + seq;
						}
						s = s.replaceAll("(?is)href=\"([^\"]+)\"", "href=\"" + detailsLinkString + "\"");
						sb.append(s);
						
						String imageLink = "";
						ma = Pattern.compile("(?is)\\bhref=\"([^\"]+)\"").matcher(firstTd.get(5));
						if (ma.find()) {
							imageLink = ma.group(1);
						}
						if (!StringUtils.isEmpty(imageLink)) {
							mSearch.setAdditionalInfo(getCurrentServerName() + ":imageLink:" + seq, imageLink);
						}
												
						int noOfRemarks = 0;
						for (int j=1;j<trs.size();j++) {
							boolean hasNew = false;
							List<String> each = trs.get(j);
							if (each.size()==6) {
								String newRemarks = each.get(4);
								if (!StringUtils.isEmpty(newRemarks)) {
									String[] lines = newRemarks.replaceAll("(?is)</?td[^>]*>", "").split("(?is)<br>");
									for (int k=0;k<lines.length;k++) {
										if (!StringUtils.isEmpty(lines[k]) && sbRemarksInterm.indexOf(lines[k])==-1) {	//not already in remarks
											hasNew = true;
											sbRemarksInterm.append("<br>").append(lines[k]);
											if (lines[k].contains("BLOCK:")) {
												sbRemarksBlock.append("<br>").append(lines[k]);
											} else {
												sbRemarksDet.append("<br>").append(lines[k]);
											}
										}
									}
								}
							}
							if (hasNew) {
								noOfRemarks++;
							}
						}
						if (noOfRemarks==1) {
							sb.append(firstTd.get(4));
						} else {
							sb.append("<td>" + sbRemarksInterm.toString() + "</td>");
							if (sbRemarksBlock.length()>0) {
								mSearch.setAdditionalInfo(getCurrentServerName() + ":block:" + seq, sbRemarksBlock.toString());
							}
							if (sbRemarksDet.length()>0) {
								mSearch.setAdditionalInfo(getCurrentServerName() + ":remarks:" + seq, sbRemarksDet.toString());
							}
						}
						
						sb.append(firstTd.get(5));
					}
				}
				
				String county = getDataSite().getCountyName();
				
				ResultMap m = ro.cst.tsearch.servers.functions.MDGenericRO.parseIntermediaryRow(sb.toString(), county);
				
				String rowString = sb.toString();
					
				ParsedResponse currentResponse = new ParsedResponse();
				currentResponse.setPageLink(new LinkInPage(detailsLinkString, detailsLinkString, TSServer.REQUEST_SAVE_TO_TSD));
				
				Bridge bridge = new Bridge(currentResponse, m, searchId);
				DocumentI document = (RegisterDocumentI) bridge.importData();
				currentResponse.setDocument(document);
				
				String checkBox = "checked";
				String book = org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.BOOK.getKeyName()));
				String page = org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.PAGE.getKeyName()));
				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName())));
				data.put("book", book);
				data.put("page", page);
				
				if (isInstrumentSaved(book + "_" + page, document, data, false)) {
					checkBox = "saved";
				} else {
					numberOfUncheckedElements++;
					LinkInPage linkInPage = new LinkInPage(detailsLinkString, detailsLinkString, TSServer.REQUEST_SAVE_TO_TSD);
					checkBox = "<input type='checkbox' name='docLink' value='" + detailsLinkString + "'>";
					currentResponse.setPageLink(linkInPage);
				}
				String rowType = "1";
				if (index%2==0) {
					rowType = "2";
				}
				String rowHtml = "<tr class=\"row" + rowType + "\"><td align=\"center\">" + checkBox + "</td>" + rowString + "</tr>";
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
				currentResponse.setOnlyResponse(rowHtml);
				intermediaryResponse.add(currentResponse);
				index++;
			
			}
			
			outputTable.append(table);
			SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	@SuppressWarnings("rawtypes")
	public static boolean contains(List<List> body, String book, String page) {
		for (int i=0;i<body.size();i++) {
			List line = body.get(i);
			if (line.size()>1) {
				if (book.equals(line.get(0)) && page.equals(line.get(1))) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static String cleanPin(String pin) {
		pin = pin.replaceFirst("\\s+[A-Z]+\\s*$", "");
		pin = pin.replaceFirst("\\s", "");
		pin = pin.trim();
		return pin;
	}
	
	@SuppressWarnings("rawtypes")
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			List<List> bodyPIS = new ArrayList<List>();
			List<String> bodyLine = new ArrayList<String>();
			
			map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO");
			
			String bookPage = getValue(detailsHtml, "Citation:");
			String book = "";
			String page = "";
			Matcher ma = Pattern.compile(BOOK_PAGE_PATT1).matcher(bookPage);
			if (ma.find()) {
				book = ma.group(2).trim();
				page = ma.group(3).trim();
				page = page.replaceFirst("-.*", "");
				map.put(SaleDataSetKey.BOOK.getKeyName(), book);
				map.put(SaleDataSetKey.PAGE.getKeyName(), page);
			} else {
				ma = Pattern.compile(BOOK_PAGE_PATT2).matcher(bookPage);
				if (ma.find()) {
					book = ma.group(1).trim();
					page = ma.group(2).trim();
					page = page.replaceFirst("-.*", "");
					map.put(SaleDataSetKey.BOOK.getKeyName(), book);
					map.put(SaleDataSetKey.PAGE.getKeyName(), page);
				}
			}
			
			String recordedDate = getValue(detailsHtml, "Recordation Date:");
			if (!StringUtils.isEmpty(recordedDate)) {
				map.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recordedDate);
			}
			
			String type = getValue(detailsHtml, "Instrument:");
			type = cleanDoctype(type);
			if (!StringUtils.isEmpty(type)) {
				map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), type);
			}
			
			String pin = getValue(detailsHtml, "SDAT Tax No.:");
			if (!StringUtils.isEmpty(pin)) {
				pin = cleanPin(pin);
				bodyLine.add(pin);
			} else {
				bodyLine.add("");
			}
			
			bodyLine.add("");
			
			bodyPIS.add(bodyLine);
			
			StringBuilder sb = new StringBuilder();
			Matcher maBlock =  Pattern.compile("(?is)<b>\\s*Block\\s+No\\.:\\s*</b>\\s*(.+?)<br>").matcher(detailsHtml);
			while (maBlock.find()) {
				sb.append("Block: ").append(maBlock.group(1).trim()).append("\n");
			}
			Matcher maLegal = Pattern.compile("(?is)<td>\\s*<b>\\s*Remarks:\\s*</b>(.+?)</td>").matcher(detailsHtml);
			if (maLegal.find()) {
				String rem = maLegal.group(1).trim().replaceAll("(?is)\\s*<br>\\s*", "\n");
				sb.append(rem);
			}
			if (sb.length()>0) {
				map.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), sb.toString());
			}
			
			StringBuilder sbGrantor = new StringBuilder();
			StringBuilder sbGrantee = new StringBuilder();
			
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("border", "0"));
			for (int i=0;i<tableList.size();i++) {
				String text = tableList.elementAt(i).toPlainTextString();
				if (text.contains("Cross References")) {
					TableTag table = (TableTag) tableList.elementAt(i);
					TableRow[] rows = table.getRows();
					List<List> bodyCR = new ArrayList<List>();
					List<String> line;

					for (int j=2;j<rows.length;j++) {
						if (rows[j].getColumnCount() == 5) {
							line = new ArrayList<String>();
							String bookCR = rows[j].getColumns()[0].toPlainTextString().trim();
							String pageCR = rows[j].getColumns()[1].toPlainTextString().trim();
							String recordedDateCR = rows[j].getColumns()[2].toPlainTextString().trim();
							String year = "", month = "", day = "";
							Matcher mat = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})").matcher(recordedDateCR);
							if (mat.find()) {
								year = mat.group(1);
								month = mat.group(2);
								day = mat.group(3);
							}
							String typeCR = rows[j].getColumns()[3].toPlainTextString().trim();
							line.add(bookCR);
							line.add(pageCR);
							line.add(year);
							line.add(month);
							line.add(day);
							line.add(typeCR);
							if (!contains(bodyCR, bookCR, pageCR)) {
								bodyCR.add(line);
							}
						}
					}

					if (bodyCR.size() > 0) {
						String[] header = { CrossRefSetKey.BOOK.getShortKeyName(), CrossRefSetKey.PAGE.getShortKeyName(),
								CrossRefSetKey.YEAR.getShortKeyName(), CrossRefSetKey.MONTH.getShortKeyName(),
								CrossRefSetKey.DAY.getShortKeyName(), CrossRefSetKey.INSTRUMENT_REF_TYPE.getShortKeyName()};
						ResultTable rt = GenericFunctions2.createResultTable(bodyCR, header);
						map.put("CrossRefSet", rt);
					}
				} else if (text.contains("Interested Parties")) {
					TableTag table = (TableTag) tableList.elementAt(i);
					TableRow[] rows = table.getRows();
					for (int j=1;j<rows.length;j++) {
						if (rows[j].getColumnCount()>1) {
							String name = rows[j].getColumns()[0].toPlainTextString().replaceAll("(?is)&nbsp;", " ").replaceAll("\\s{2,}", " ").trim();
							String cap = rows[j].getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", " ").replaceAll("\\s{2,}", " ")
									.replaceFirst("\\s+ETC\\s*$", "").trim();
							cap = cap.replaceFirst("^TRUS$", "TR");
							cap = cap.replaceFirst("^TST$", "TR");
							if (cap.matches(ro.cst.tsearch.extractor.xml.GenericFunctions1.nameTypeString)) {
								name += " " + cap;
							}
							if (name.matches("(?is)\\bGrantor:.*")) {
								sbGrantor.append(name.replaceFirst("(?is)\\bGrantor:", "").trim()).append("<br>");
							} else if (name.matches("(?is)\\bGrantee:.*")) {
								sbGrantee.append(name.replaceFirst("(?is)\\bGrantee:", "").trim()).append("<br>");
							}
						}
					}
				} 
			}
			
			String grantorString = sbGrantor.toString().replaceFirst("<br>$", "");
			String granteeString = sbGrantee.toString().replaceFirst("<br>$", "");
			if (!StringUtils.isEmpty(grantorString)) {
				map.put("tmpGrantor", grantorString);
			}
			if (!StringUtils.isEmpty(granteeString)) { 
				map.put("tmpGrantee", granteeString);
			}
			
			String county = getDataSite().getCountyName();
			
			ro.cst.tsearch.servers.functions.MDGenericRO.parseNames(map, ro.cst.tsearch.servers.functions.MDGenericRO.NAME_DETAILS);
			ro.cst.tsearch.servers.functions.MDGenericRO.parseLegals(map, bodyPIS, county);
			
			String[] header = {PropertyIdentificationSetKey.PARCEL_ID.getShortKeyName(),
	           		   		   PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(),
			           		   PropertyIdentificationSetKey.CITY.getShortKeyName(),
							   PropertyIdentificationSetKey.STREET_NAME.getShortKeyName(),
			           		   PropertyIdentificationSetKey.STREET_NO.getShortKeyName(),
			           		   PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(),
			           		   PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(),
			           		   PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(),
					           PropertyIdentificationSetKey.SUBDIVISION_BLDG.getShortKeyName(),
					           PropertyIdentificationSetKey.PARCEL_ID_PARCEL.getShortKeyName(),
					           PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName(),
					           PropertyIdentificationSetKey.SUBDIVISION_COND.getShortKeyName(),
					           PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(),
					           PropertyIdentificationSetKey.PLAT_NO.getShortKeyName()};
			ResultTable rt = GenericFunctions2.createResultTable(bodyPIS, header);
			map.put("PropertyIdentificationSet", rt);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String cleanDoctype(String s) {
		s = s.replaceAll("\\s+", " ");
		s = s.trim();
		return s;
	}
	
	@Override
	protected void setCertificationDate() {
		try {

			if (CertificationDateManager.isCertificationDateInCache(dataSite)) {
				String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
			} else {
	
				String html = "";
				HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
				try {
					html = ((ro.cst.tsearch.connection.http2.MDGenericRO)site).getCertificationDateHtml(); 
				} catch (RuntimeException e) {
					e.printStackTrace();
				} finally {
					HttpManager.releaseSite(site);
				}
	
				if (StringUtils.isNotEmpty(html)) {
					Matcher ma = Pattern.compile("(?is)\\(\\s*verified\\s+through\\s+(.+?)\\s*\\)").matcher(html);
					if (ma.find()) {
						String certDate = ma.group(1).trim();
						String oldFormat = "MMM dd, yyyy";
						String newFormat = "MM/dd/yyyy";
						SimpleDateFormat dateFormat = new SimpleDateFormat(oldFormat);
						Date certificationDate = dateFormat.parse(certDate);
						dateFormat.applyPattern(newFormat);
						certDate = dateFormat.format(certificationDate);
						
						if (certificationDate != null){
							CertificationDateManager.cacheCertificationDate(dataSite, certDate);
							getSearch().getSa().updateCertificationDateObject(dataSite, certificationDate);
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}
	
	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
        if (module.getModuleIdx() ==TSServerInfo.MODULE_IDX40) {	//Jump to Land Record Volume/Page
        	StringBuilder sb = new StringBuilder();
        	sb.append(CreatePartialLink(TSConnectionURL.idPOST)).append(module.getDestinationPage()).append("?");
        	for (TSServerInfoFunction fct: module.getFunctionList()) {
        		sb.append(fct.getParamName()).append("=").append(fct.getParamValue()).append("&");
        	}
        	String s = sb.toString();
        	s = s.replaceFirst("[?&]+$", "");
        	logSearchBy(module);
        	return GetLink(s, true);
        }
		return SearchBy(true, module, sd);
    }
	
	@Override
    public ServerResponse GetLink(String vsRequest, boolean vbEncodedOrIsParentSite) throws ServerResponseException {
    	    	
    	String link = StringUtils.extractParameter(vsRequest, "Link=(.*)");
    	if (link.contains("dsp_book.cfm")) {
    		String book = StringUtils.extractParameter(vsRequest, "bk=([^&?]*)");
    		String page = StringUtils.extractParameter(vsRequest, "sp=([^&?]*)");
    		String ep = StringUtils.extractParameter(vsRequest, "ep=([^&?]*)");
    		String yr = StringUtils.extractParameter(vsRequest, "yr=([^&?]*)");
    		String fileName = book + "_" + page + "_" + ep + "_" + yr;
    		return GetImageLink(link, fileName, vsRequest, vbEncodedOrIsParentSite, false);
        } else if (link.contains("act_captureimage.cfm")) {
    		String book = StringUtils.extractParameter(vsRequest, "book=([^&?]*)");
    		book = book.replaceAll("%20", "");
    		String page = StringUtils.extractParameter(vsRequest, "b_sp=([^&?]*)");
    		String sr = StringUtils.extractParameter(vsRequest, "b_sr=([^&?]*)");
    		String fileName = book + "_" + page + "_" + sr;
        	return GetImageLink(link, fileName, vsRequest, vbEncodedOrIsParentSite, false);
        } else if (link.contains("act_bookjump.cfm")) {
        	String multipleImages = StringUtils.extractParameter(vsRequest, "multipleImages=([^&?]*)");
        	if (!"true".equals(multipleImages)) {
        		String ci = StringUtils.extractParameter(vsRequest, "b_ci=([^&?]*)");
        		String book = StringUtils.extractParameter(vsRequest, "b_bk=([^&?]*)");
        		String page = StringUtils.extractParameter(vsRequest, "b_sp=([^&?]*)");
        		String fileName = ci + "_" + book + "_" + page;
            	return GetImageLink(link, fileName, vsRequest, vbEncodedOrIsParentSite, true);
        	}
        }
    	
    	return super.GetLink(vsRequest.replaceFirst("&multipleImages=true$", ""), vbEncodedOrIsParentSite);
    	
    }
	
	public ServerResponse GetImageLink(String link, String name, String vsRequest, boolean vbEncodedOrIsParentSite, boolean isDirectJump) throws ServerResponseException {
    	
		String folderName = getImageDirectory() + File.separator + searchId + File.separator;
		new File(folderName).mkdirs();
    	
		String fileName = folderName + name + ".pdf";
		
		int imgResult = -2;
		imgResult = retrieveImage(link, fileName, isDirectJump);
		
		if (imgResult==-1) {
			return GetLink(vsRequest + "&multipleImages=true", vbEncodedOrIsParentSite);
		}
		
		// write the image to the client web-browser
		boolean imageOK = false;
		if (imgResult==1) {
			imageOK = writeImageToClient(fileName, "application/pdf");
		}
		
		// image not retrieved
		if(!imageOK){ 
	        // return error message
			ParsedResponse pr = new ParsedResponse();
			pr.setError("<br><font color=\"red\"><b>Image not found! Close this page and continue your search!</b></font> ");
			ServerResponse sr = new ServerResponse();
			sr.setParsedResponse(pr);
			sr.setDisplayMode(ServerResponse.HIDE_ALL_CONTROLS);
			throw new ServerResponseException(sr);			
		}
		// return solved response
		return ServerResponse.createSolvedResponse();  
    }
    
    protected int retrieveImage(String link, String fileName, boolean isDirectJump) throws ServerResponseException {
    	
    	byte[] imageBytes = null;
    	
    	HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		try {
			imageBytes = ((ro.cst.tsearch.connection.http2.MDGenericRO)site).getImage(link, fileName, isDirectJump);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager.releaseSite(site);
		}

		ServerResponse resp = new ServerResponse();

		if (imageBytes != null) {
			if (imageBytes.length==1 && imageBytes[0]==-1) {
				return -1;	//multiple images found
			} else {
				afterDownloadImage(true);
			}	
		} else {
			return 0;	//image not found
		}

		resp.setImageResult(new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, "application/pdf"));
		logInitialResponse(resp);

		if (!ro.cst.tsearch.utils.FileUtils.existPath(fileName)) {
			FileUtils.writeByteArrayToFile(resp.getImageResult().getImageContent(), fileName);
		}
		
		return 1;	//image found
    }
	
	@Override
    protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {
    	
		String link = image.getLink().replaceFirst("(?is).*?Link=", "");
    	String fileName = image.getPath();
    	if(1==retrieveImage(link, fileName, false)){
    		byte[] b = FileUtils.readBinaryFile(fileName);
    		return new DownloadImageResult(DownloadImageResult.Status.OK, b, image.getContentType());
    	}
    	
    	return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
    }
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		Search search = getSearch();
		
		TSServerInfoModule module = null;
		
		GenericMultipleLegalFilter defaultLegalFilter = new GenericMultipleLegalFilter(searchId);
		defaultLegalFilter.setAdditionalInfoKey(AdditionalInfoKeys.RO_LOOK_UP_DATA);
		defaultLegalFilter.setUseLegalFromSearchPage(true);
		
		SubdivisionFilter subdivisionFilter = new SubdivisionFilter(searchId);
		FilterResponse addressHighPassFilter = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d);
		FilterResponse pinFilter = PINFilterFactory.getPinFilter(searchId, true, true);
		LastTransferDateFilter lastTransferDateFilter = new LastTransferDateFilter(searchId);
    	GenericNameFilter defaultNameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
		
    	//search with references (book and page) from AO/Tax like documents	
		InstrumentGenericIterator bookPageGenericIterator = new InstrumentGenericIterator(searchId);
		bookPageGenericIterator.enableBookPage();
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				"Search with references from AO/Tax like documents");
		module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
		module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
		if (isUpdate()) {
			module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module));
		}
		module.addIterator(bookPageGenericIterator);
		modules.add(module);
		
		addTaxAccountNoSearch(modules, serverInfo);
		
		addAddressSearch(modules, serverInfo, 
				new FilterResponse[]{addressHighPassFilter, defaultLegalFilter, subdivisionFilter, pinFilter, lastTransferDateFilter},
				new DocsValidator[]{},
				new DocsValidator[]{addressHighPassFilter.getValidator(),
									defaultLegalFilter.getValidator(),
				                    subdivisionFilter.getValidator(),
				                    pinFilter.getValidator(),
									lastTransferDateFilter.getValidator()});
		
		ArrayList<NameI> searchedNames = null;
		
		//search with owners
		if (hasOwner()) {
			searchedNames = addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS, null,
					new FilterResponse[]{defaultNameFilter, defaultLegalFilter, subdivisionFilter, addressHighPassFilter, pinFilter, lastTransferDateFilter},
					new DocsValidator[]{},
					new DocsValidator[]{defaultLegalFilter.getValidator(),
					                    subdivisionFilter.getValidator(),
										addressHighPassFilter.getValidator(),
										pinFilter.getValidator(),
										lastTransferDateFilter.getValidator()});
		}
		
		//OCR last transfer - book and page search
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
		module.clearSaKeys();
		module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
		module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
		module.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
		OcrOrBootStraperIterator ocrBPIteratoriterator = new OcrOrBootStraperIterator(searchId);
		ocrBPIteratoriterator.setInitAgain(true);
	    module.addIterator(ocrBPIteratoriterator);
		if (isUpdate()) {
			module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module));
		}
		module.addFilter(defaultLegalFilter);
		module.addFilter(subdivisionFilter);
		module.addFilter(addressHighPassFilter);
		modules.add(module);
		
		//search with extra names from search page (for example added by OCR)	
		searchedNames = addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS, searchedNames,
				new FilterResponse[]{defaultNameFilter, defaultLegalFilter, subdivisionFilter, addressHighPassFilter, pinFilter, lastTransferDateFilter},
				new DocsValidator[]{},
				new DocsValidator[]{defaultLegalFilter.getValidator(),
									subdivisionFilter.getValidator(),
									addressHighPassFilter.getValidator(),
									pinFilter.getValidator(),
									lastTransferDateFilter.getValidator()});
	    	
		//search with buyers
		if(hasBuyer()) {
			FilterResponse nameFilterBuyer = NameFilterFactory.getDefaultNameFilter(SearchAttributes.BUYER_OBJECT, getSearch().getID(), null);
			
			addNameSearch(modules, serverInfo, SearchAttributes.BUYER_OBJECT, TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS, null,
					new FilterResponse[]{nameFilterBuyer, defaultLegalFilter, subdivisionFilter, addressHighPassFilter, pinFilter,
										 DoctypeFilterFactory.getDoctypeBuyerFilter(searchId), lastTransferDateFilter},
					new DocsValidator[]{},
					new DocsValidator[]{defaultLegalFilter.getValidator(),
										subdivisionFilter.getValidator(),
										addressHighPassFilter.getValidator(),
										pinFilter.getValidator(),
										lastTransferDateFilter.getValidator()});
		}
		
		serverInfo.setModulesForAutoSearch(modules);
	
	}
	
	protected void addTaxAccountNoSearch(List<TSServerInfoModule> modules, TSServerInfo serverInfo) {}
	
	protected void addAddressSearch(
			List<TSServerInfoModule> modules, 
			TSServerInfo serverInfo, 
			FilterResponse[] filters,
			DocsValidator[] docsValidators,
			DocsValidator[] docsValidatorsCrossref) {}
	
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
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			module.setSaKey(8, SearchAttributes.FROMDATE_YEAR);
			module.setIteratorType(8, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			module.setSaKey(9, SearchAttributes.TODATE_YEAR);
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
			module.setIteratorType(6, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
			module.setSaKey(8, SearchAttributes.FROMDATE_YEAR);
			module.setIteratorType(8, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			module.setSaKey(9, SearchAttributes.TODATE_YEAR);
			
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

		ConfigurableNameIterator nameIterator = null;
		String endDate = new SimpleDateFormat("yyyy").format(new Date());

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

			String date = gbm.getDateForSearch(id, "yyyy", searchId);
			if (date != null) {
				module.getFunction(8).forceValue(date);
			}
			module.forceValue(9, endDate);
			
			module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId,
					new String[] { "L;F;" });
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
				
				date = gbm.getDateForSearchBrokenChain(id, "yyyy", searchId);
				if (date != null) {
					module.getFunction(8).forceValue(date);
				}
				module.forceValue(9, endDate);
				
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId,
						new String[] { "L;F;" });
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
		
		if (StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			module = new TSServerInfoModule(getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
			module.forceValue(0, book);
			module.forceValue(1, page);
			module.forceValue(2, "B");
			module.forceValue(3, "book/page");
			module.forceValue(4, "Instrument");
			module.forceValue(5, "2");
			module.forceValue(6, "Search");
			module.getFilterList().clear();
			modules.add(module);
		}	
	
		return modules;
	}
	
	@Override
	protected int[] getModuleIdsForSavingName() {
		return new int[]{TSServerInfo.NAME_MODULE_IDX, TSServerInfo.MODULE_IDX38, TSServerInfo.MODULE_IDX39};
	}
	
	@Override
	protected NameI getNameFromModule(TSServerInfoModule module) {
		NameI name = new Name();
		if (module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX && module.getFunctionCount() > 6) {
			String lastName = module.getFunction(1).getParamValue();
			String firstName = module.getFunction(2).getParamValue();
			String corporationName = module.getFunction(6).getParamValue();
			if (!StringUtils.isEmpty(corporationName)) {
				name.setLastName(corporationName);
				name.setCompany(true);
				return name;
			} else if (!StringUtils.isEmpty(lastName) || !StringUtils.isEmpty(firstName)) {
				if (!StringUtils.isEmpty(lastName)) {
					name.setLastName(lastName);
				}
				if (!StringUtils.isEmpty(firstName)) {
					name.setFirstName(firstName);
				}
				return name;
			}
		} else if (module.getModuleIdx() == TSServerInfo.MODULE_IDX38 && module.getFunctionCount() > 1) {
			String lastName = module.getFunction(0).getParamValue();
			String firstName = module.getFunction(1).getParamValue();
			if (!StringUtils.isEmpty(lastName) || !StringUtils.isEmpty(firstName)) {
				if (!StringUtils.isEmpty(lastName)) {
					name.setLastName(lastName);
				}
				if (!StringUtils.isEmpty(firstName)) {
					name.setFirstName(firstName);
				}
				return name;
			}
		} else if (module.getModuleIdx() == TSServerInfo.MODULE_IDX39 && module.getFunctionCount() > 0) {
			String corporationName = module.getFunction(0).getParamValue();
			if (!StringUtils.isEmpty(corporationName)) {
				name.setLastName(corporationName);
				name.setCompany(true);
				return name;
			}
		}
		return null;
	}
	
	@Override
	protected int[] getModuleIdsForSavingAddress() {
		return new int[]{TSServerInfo.MODULE_IDX41, TSServerInfo.ADDRESS_MODULE_IDX};
	}
	
	@Override
	protected AddressI getAddressFromModule(TSServerInfoModule module) {
		AddressI address = new Address();
		if (module.getModuleIdx() == TSServerInfo.MODULE_IDX41 && module.getFunctionCount() > 2) {
			String streetName = module.getFunction(2).getParamValue();
			if (!StringUtils.isEmpty(streetName)) {
				address.setStreetName(streetName);
				return address;
			}
		} else if (module.getModuleIdx() == TSServerInfo.ADDRESS_MODULE_IDX && module.getFunctionCount() > 1) {
			String number = module.getFunction(0).getParamValue();
			String streetName = module.getFunction(1).getParamValue();
			if (!StringUtils.isEmpty(number) && !StringUtils.isEmpty(streetName)) {
				address.setNumber(number);
				address.setStreetName(streetName);
				return address;
			}
		}
		return null;
	}

	@Override
	protected int[] getModuleIdsForSavingLegal() {
		return new int[]{TSServerInfo.MODULE_IDX41};
	}
	
	@Override
	protected LegalI getLegalFromModule(TSServerInfoModule module) {
		LegalI legal = new Legal();

		if (module.getModuleIdx() == TSServerInfo.MODULE_IDX41 && module.getFunctionCount() > 0) {
			String block = module.getFunction(0).getParamValue();
			if (!StringUtils.isEmpty(block)) {
				SubdivisionI subdivision = new Subdivision();
				subdivision.setBlock(block);
				legal.setSubdivision(subdivision);
				return legal;
			}
		}	
		return null;
	}
	
	@Override
	public String saveSearchedParameters(TSServerInfoModule module) {
		
		String globalResult = null;
		
		if(ArrayUtils.contains(getModuleIdsForSavingLegal(), module.getModuleIdx()) 
				|| ArrayUtils.contains(getModuleIdsForSavingAddress(), module.getModuleIdx())) {
			if(ArrayUtils.contains(getModuleIdsForSavingLegal(), module.getModuleIdx())) {
				
				LegalI legalI = getLegalFromModule(module);
				StringBuilder fullLegal = new StringBuilder();
				
				if(legalI != null) {
				
					List<LegalI> alreadySavedLegals = getSearchAttributes().getForUpdateSearchLegalsNotNull(getServerID());
					
					
					ArrayList<LegalI> alreadySavedSubdividedLegal = new ArrayList<LegalI>();
					
					ArrayList<LegalI> alreadySavedTownshipLegal = new ArrayList<LegalI>(); 
					
					for (LegalI alreadySavedLegal : alreadySavedLegals) {
												
						if(alreadySavedLegal.hasSubdividedLegal()  ){
							alreadySavedSubdividedLegal.add(alreadySavedLegal);
						}
						if (alreadySavedLegal.hasTownshipLegal()){
							alreadySavedTownshipLegal.add(alreadySavedLegal);
						}
						
					}
					
					boolean addSubdivision = alreadySavedSubdividedLegal.isEmpty();
					boolean addTownship = alreadySavedTownshipLegal.isEmpty();
						
					if(legalI.hasSubdividedLegal() && !addSubdivision){
						for (LegalI alreadySavedLegal : alreadySavedSubdividedLegal)
							if(!alreadySavedLegal.getSubdivision().equals(legalI.getSubdivision())) {
								addSubdivision = true;
							} else{
								addSubdivision = false;
							}
					}
					
					if (legalI.hasTownshipLegal() && !addTownship){
						for (LegalI alreadySavedLegal : alreadySavedTownshipLegal)
							if(!alreadySavedLegal.getTownShip().equals(legalI.getTownShip())) {
								addTownship = true;
							} else{
								addTownship = false;
							}
					}
					
					if(addSubdivision && legalI.hasSubdividedLegal()) {
						LegalI toAdd = new Legal();
						toAdd.setSubdivision(legalI.getSubdivision());
						if(fullLegal.length() > 0) {
							fullLegal.append(" | ");
						}
						fullLegal.append(legalI.getSubdivision().shortFormString());
						alreadySavedLegals.add(toAdd);
						getSearchAttributes().addForUpdateSearchLegal(toAdd, getServerID());
					}
					
					if(addTownship && legalI.hasTownshipLegal()) {
						LegalI toAdd = new Legal();
						toAdd.setTownShip(legalI.getTownShip());
						if(fullLegal.length() > 0) {
							fullLegal.append(" | ");
						}
						fullLegal.append(legalI.getTownShip().shortFormString());
						alreadySavedLegals.add(toAdd);
						getSearchAttributes().addForUpdateSearchLegal(toAdd, getServerID());
					}
				}
				
				if(fullLegal.length() == 0) {
					SearchLogger.info("<br><font color='red'>NO</font> legal was saved from searched parameters for future automatic search<br>", searchId);
					globalResult = "NO legal was saved from searched parameters for future automatic search";
				} else {
					SearchLogger.info("<br><font color='green'><b>Saving</b></font> legal: [" + fullLegal.toString() + "] from searched parameters for future automatic search<br>", searchId);
					globalResult = "Saved legal: [" + fullLegal.toString() + "] from searched parameters for future automatic search";
				}
			}
			
			if(ArrayUtils.contains(getModuleIdsForSavingAddress(), module.getModuleIdx())) {
				AddressI addressI = getAddressFromModule(module);
				if(addressI != null) {
					List<AddressI> alreadySavedAddresses = getSearchAttributes().getForUpdateSearchAddressesNotNull(getServerID());
					if(alreadySavedAddresses.isEmpty()) {
						alreadySavedAddresses.add(addressI);
						SearchLogger.info("<br><font color='green'><b>Saving</b></font> address: [" + addressI.shortFormString() + "] from searched parameters for future automatic search<br>", searchId);
						if (globalResult==null) {
							globalResult = "Saving address: [" + addressI.shortFormString() + "] from searched parameters for future automatic search";
						} else {
							globalResult += "\nSaving address: [" + addressI.shortFormString() + "] from searched parameters for future automatic search";
						}
					} else {
						boolean alreadySaved = false;
						for (AddressI savedAddress : alreadySavedAddresses) {
							if(savedAddress.equals(addressI)) {
								alreadySaved = true;
								break;
							}
						}
						if(!alreadySaved) {
							alreadySavedAddresses.add(addressI);
							SearchLogger.info("<br><font color='green'><b>Saving</b></font> address: [" + addressI.shortFormString() + "] from searched parameters for future automatic search<br>", searchId);
							if (globalResult==null) { 
								globalResult = "Saving address: [" + addressI.shortFormString() + "] from searched parameters for future automatic search";
							} else {
								globalResult += "\nSaving address: [" + addressI.shortFormString() + "] from searched parameters for future automatic search";
							}
						} else {
							SearchLogger.info("<br><font color='red'>NO</font> address was saved from searched parameters for future automatic search<br>", searchId);
							if (globalResult==null) {
								globalResult = "NO address was saved from searched parameters for future automatic search";
							} else {
								globalResult += "\nNO address was saved from searched parameters for future automatic search";
							}
						}
					}
				}
			}
		} else if(ArrayUtils.contains(getModuleIdsForSavingName(), module.getModuleIdx())) {
			List<NameI> alreadySavedNames = getSearchAttributes().getForUpdateSearchGrantorNamesNotNull(getServerID());
			List<NameI> newNamesAdded = new ArrayList<NameI>();
			
			NameI candName = getNameFromModule(module);
			if(candName != null) {
				NameI toAdd = candName;
				String candString =  candName.getFirstName() + candName.getMiddleName() + candName.getSufix() + candName.getLastName();
				for (NameI reference : alreadySavedNames) {
					if(
							GenericNameFilter.isMatchGreaterThenScore(candName, reference, NameFilterFactory.NAME_FILTER_THRESHOLD)
							&&(candName.isCompany()==reference.isCompany())
					) {
						/*
						 * found same name - do not save it
						 */
						String refString = reference.getFirstName() + reference.getMiddleName() + reference.getSufix() + reference.getLastName();
						if(refString.length() <= candString.length()){
							if(newNamesAdded.contains(reference)) {
								newNamesAdded.remove(reference);
							}
							
							reference.setLastName(candName.getLastName());
							reference.setFirstName(candName.getFirstName());
							reference.setMiddleName(candName.getMiddleName());
							reference.setCompany(candName.isCompany());
							reference.setSufix(candName.getSufix());
							reference.setPrefix(candName.getPrefix());
							
							newNamesAdded.add(reference);
						}
						toAdd = null;
						break;	//no need to check other cases
					} 
				}
				if(toAdd != null) {
					alreadySavedNames.add(toAdd);
					newNamesAdded.add(toAdd);
				}
			}
			
			if(newNamesAdded.size() == 0) {
				SearchLogger.info("<br><font color='red'>NO</font> name was saved from searched parameters for future automatic search<br>", searchId);
				globalResult = "NO name was saved from searched parameters for future automatic search";
			} else {
				for (NameI nameI : newNamesAdded) {
					SearchLogger.info("<br><font color='green'><b>Saving</b></font> name: [" + nameI.getFullName() + "] from searched parameters for future automatic search<br>", searchId);
					if(globalResult == null) {
						globalResult = "Saving name: [" + nameI.getFullName() + "] from searched parameters for future automatic search";
					} else {
						globalResult += "<br> Saving name: [" + nameI.getFullName() + "] from searched parameters for future automatic search";
					}
				}
			}
			
		} 
		
		return globalResult;
	}

	protected int getNameType() {
		return 0;	//has only corporation name type (CORPORATION_NAME_TYPE_MAP1)
	}
	
	@Override
	protected void logSearchBy(TSServerInfoModule module, Map<String, String> params) {
    	
    	if(module.isVisible() || "GB_MANAGER_OBJECT".equals(module.getSaObjKey())) {//B 4511
        
	    	// get parameters formatted properly
	        Map<String,String> moduleParams = params;
	        if(moduleParams == null){
	        	moduleParams = module.getParamsForLog();
	        }
	        Search search = getSearch();
	        // determine whether it's an automatic search
	        boolean automatic = (search.getSearchType() != Search.PARENT_SITE_SEARCH) 
	        		|| (GPMaster.getThread(searchId) != null);
	        boolean imageSearch = module.getLabel().equalsIgnoreCase("image search") || 
	                              module.getModuleIdx() == TSServerInfo.IMG_MODULE_IDX;
	        
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
	        
	        boolean personAndCorporation = TSServerInfo.NAME_MODULE_IDX==module.getModuleIdx(); 
	        String lastName = moduleParams.get("Last Name");
			String firstName = moduleParams.get("First Name");
			String corporationName = moduleParams.get("Name");
			boolean firstTime = true;
	        for(Entry<String,String> entry : moduleParams.entrySet() ){
	        	String key = entry.getKey();
	        	String value = entry.getValue();
	        	value = value.replaceAll("(, )+$","");
	        	if ("As".equals(key)) {
	        		if (personAndCorporation) {
	        			String[] split = value.split(",");
	        			if ((!StringUtils.isEmpty(lastName) || !StringUtils.isEmpty(firstName)) && StringUtils.isEmpty(corporationName)) {
	        				split = new String[]{split[0]};
	        			} else if (!StringUtils.isEmpty(corporationName) && StringUtils.isEmpty(lastName) && StringUtils.isEmpty(firstName)) {
	        				if (split.length==2) {
	        					split = new String[]{split[1]};
	        				} else {
	        					split = new String[]{split[0]};
	        				}
	        			}
	        			StringBuilder ss = new StringBuilder();
		        		for (int i=0;i<split.length;i++) {
		        			value = AS_MAP.get(split[i].trim());
		        			if (value==null) {
			        			value = entry.getValue();
			        		}
		        			ss.append(value).append(", ");
		        		}
		        		value = ss.toString().replaceFirst(",\\s*$", "");
	        		} else {
	        			value = AS_MAP.get(value);
		        		if (value==null) {
		        			value = entry.getValue();
		        		}
	        		}
	        	} else if ("Sort By".equals(key)) {
	        		value = SORT_BY_MAP.get(value);
	        		if (value==null) {
	        			value = entry.getValue();
	        		}
	        	} else if ("Type".equals(key)) {
	        		if (getNameType()==0) {
	        			if (personAndCorporation && StringUtils.isEmpty(corporationName)) {
		        			continue;
		        		}
		        		value = CORPORATION_NAME_TYPE_MAP1.get(value);
		        		if (value==null) {
		        			value = entry.getValue();
		        		}
	        		} else {
	        			if (personAndCorporation) {
		        			String individualType = module.getFunction(16).getParamValue();
		        			String corporationType = module.getFunction(5).getParamValue();
	        				if ((!StringUtils.isEmpty(lastName) || !StringUtils.isEmpty(firstName)) && StringUtils.isEmpty(corporationName)) {
		        				value = INDIVIDUAL_NAME_TYPE_MAP.get(individualType);
		        				if (value==null) {
				        			value = entry.getValue();
				        		}
		        			} else if (!StringUtils.isEmpty(corporationName) && StringUtils.isEmpty(lastName) && StringUtils.isEmpty(firstName)) {
		        				value = CORPORATION_NAME_TYPE_MAP2.get(corporationType);
		        				if (value==null) {
				        			value = entry.getValue();
				        		}
		        			} else {
		        				String value1 = INDIVIDUAL_NAME_TYPE_MAP.get(individualType);
		        				if (value1==null) {
				        			value1 = entry.getValue();
				        		}
		        				String value2 = CORPORATION_NAME_TYPE_MAP2.get(corporationType);
		        				if (value2==null) {
				        			value2 = entry.getValue();
				        		}
		        				value = value1 + ", "  + value2;
		        			}
		        		} 
	        		}
	        	} 
	        	if(!firstTime){
	        		sb.append(", ");
	        	} else {
	        		firstTime = false;
	        	}
	        	sb.append(key.replaceAll("&lt;br&gt;", "") + " = <b>" + value + "</b>");
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
