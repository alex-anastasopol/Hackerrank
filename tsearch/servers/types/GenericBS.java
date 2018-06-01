package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.BaseServlet;

import com.stewart.ats.base.document.BankSuccessorDocumentI;
import com.stewart.ats.base.document.BankSuccessorDocument;
import com.stewart.ats.base.document.DocumentI;

@SuppressWarnings("deprecation")
public class GenericBS extends TSServer {
		
	private static final long serialVersionUID = -4452691490235091614L;
	
	//for State, Country and Institution Type combo boxes
	private static String STATE_SELECT = "";
	private static String COUNTRY_SELECT = "";
	private static String INSTITUTION_TYPE_SELECT = "";
	
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
			String selects1 = FileUtils.readFileToString(new File(folderPath
					+ File.separator + "GenericBSState.xml"));
			STATE_SELECT = selects1;
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			String selects2 = FileUtils.readFileToString(new File(folderPath
					+ File.separator + "GenericBSCountry.xml"));
			COUNTRY_SELECT = selects2;
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			String selects3 = FileUtils.readFileToString(new File(folderPath
					+ File.separator + "GenericBSInstitutionType.xml"));
			INSTITUTION_TYPE_SELECT = selects3;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	public static String getSTATE_SELECT() {
		return STATE_SELECT;
	}
	
	public static String getCOUNTRY_SELECT() {
		return COUNTRY_SELECT;
	}
	
	public static String getINSTITUTION_TYPE_SELECT() {
		return INSTITUTION_TYPE_SELECT;
	}
	
	private static int seq = 0;

	protected synchronized static int getSeq() {
		return seq++;
	}
	
	public GenericBS(long searchId) {
		super(searchId);
	}
	
	public GenericBS(String rsRequestSolverName, String rsSitePath,	String rsServerID, 
			String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,	miServerID);
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {

		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault
				.getModule(TSServerInfo.NAME_MODULE_IDX);

		if (tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(9), STATE_SELECT);
			setupSelectBox(tsServerInfoModule.getFunction(11), COUNTRY_SELECT);
			setupSelectBox(tsServerInfoModule.getFunction(13), INSTITUTION_TYPE_SELECT);
		}

		setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch (viParseID) {			//Name Search
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_PARCEL:		//ID Search
			
			if (rsResponse.indexOf("No institution(s) matched the specified criteria.") > -1) {
				Response.getParsedResponse().setError("No institution(s) matched the specified criteria.");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			
			if (rsResponse.indexOf("Please enter an Institution Name or a City Name") > -1) {
				Response.getParsedResponse().setError("Please enter an Institution Name or a City Name.");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			
			if (rsResponse.indexOf("Please enter only the RSSD ID, RTN, or FDIC Cert. Number.") > -1) {
				Response.getParsedResponse().setError("Please enter only the RSSD ID, RTN, or FDIC Cert. Number.");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			
			if (rsResponse.indexOf("Entry must be numeric and less than 2147483647.") > -1) {
				Response.getParsedResponse().setError("Entry must be numeric and less than 2147483647.");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			
			if (rsResponse.indexOf("Entry cannot exceed 9 numeric digits.") > -1) {
				Response.getParsedResponse().setError("Entry cannot exceed 9 numeric digits.");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
			
			if(smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
            }
			
			int seq = getSeq();
			Map<String, String> params = new HashMap<String, String>(); 
			Matcher matcher1 = Pattern.compile("(?is)id=\"__VIEWSTATE\" value=\"(.*?)\"").matcher(rsResponse);
			String viewState = "";
			if (matcher1.find()) {
				viewState = matcher1.group(1);
			}
			String eventValidation = "";
			Matcher matcher2 = Pattern.compile("(?is)id=\"__EVENTVALIDATION\" value=\"(.*?)\"").matcher(rsResponse);
			if (matcher2.find()) {
				eventValidation = matcher2.group(1);
			}
			params.put("__VIEWSTATE", viewState);
			params.put("__EVENTVALIDATION", eventValidation);
			mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
			
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
			if (sAction.indexOf("InstitutionProfile.aspx")>-1) {
				ParseResponse(sAction, Response, ID_DETAILS);
			} else {
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			} 
			break;	
			
		default:
			break;
		}
		
	}	
	
	protected void loadDataHash(HashMap<String, String> data) {
		if(data != null) {
			data.put("type","BANK SUCCESSOR");
		}
	}

	protected String getDetails(String rsResponse, StringBuilder parcelNumber) {
		try {
			
			StringBuilder details = new StringBuilder();
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
									
			/* If from memory - use it as is */
			if(!rsResponse.toLowerCase().contains("<html")){
				NodeList parcelList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "lblID_RSSD"));
				String parcelID = parcelList.elementAt(0).toPlainTextString();
				parcelNumber.append(parcelID);
								
				return rsResponse;
			}
			
			NodeList parcelList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "lblID_RSSD"));
			String parcelID = parcelList.elementAt(0).toPlainTextString();
			parcelNumber.append(parcelID);
			
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "Table2"));
			if (tables.size()>0) {
				details.append(tables.elementAt(0).toHtml().replaceAll("(?is)<a[^>]*?>([^<]*?)</a>", "$1"));
			} 
			
			tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "Table3"));
			if (tables.size()>0) {
				details.append(tables.elementAt(0).toHtml());
			}
			
			tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "Table4"));
			if (tables.size()>0) {
				details.append(tables.elementAt(0).toHtml().replaceAll("(?is)<a[^>]*?>([^<]*?)</a>", "$1"));
			}
						
			String detailsString = details.toString();
			detailsString = detailsString.replaceAll("(?is)(<input)([^>]*>)", "$1 disabled=\"disabled\"$2");
			detailsString = detailsString.replaceAll("(?is)(<select)([^>]*>)", "$1 disabled=\"disabled\"$2");
			detailsString = detailsString.replaceAll("(?is)Please verify the.*?before requesting financial reports\\.", "");
			
			NodeList historyLinkList = nodeList.extractAllNodesThatMatch(new TagNameFilter("a"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "hyplnkHistory_link_txt"));
			if (historyLinkList.size()>0) {
				
				LinkTag linktag = (LinkTag)historyLinkList.elementAt(0);
				String link = linktag.getLink();
				String historyPage = getLinkContents(link);
				htmlParser = org.htmlparser.Parser.createParser(historyPage, null);
				nodeList = htmlParser.parse(null);
				NodeList historyTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "dgOrgHistory"));
				if (historyTableList.size()>0) {
					detailsString += "<br><b>Institution History</b><br><br>";
					String tableString = historyTableList.elementAt(0).toHtml();
					tableString = tableString.replaceAll("(?is)<a[^>]*>", "");
					tableString = tableString.replaceAll("(?is)</a>", "");
					detailsString += tableString;
				}
			}
			
			return detailsString;
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
		
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		TableTag resultsTable = null;
		String header = "";
		String footer = "";
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTable = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id","dgInstitution"), true);
				
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
					
					String link = row.getColumns()[0].toHtml();
					Matcher matcher = Pattern.compile("(?i)href=\"(.*?)\"").matcher(link);
					if (matcher.find()) {
						link = matcher.group(1);
					} 
					link = CreatePartialLink(TSConnectionURL.idGET) + link;
					
					//replace the links
					htmlRow = htmlRow.replaceAll("(?i)href=\".*\"", "href=" + link);
										
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
									
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,htmlRow);
					currentResponse.setOnlyResponse(htmlRow);
					
					ResultMap m = ro.cst.tsearch.servers.functions.GenericBS.parseIntermediaryRow( row, searchId ); 
					Bridge bridge = new Bridge(currentResponse, m, searchId);
					
					String bankName = (String)m.get(SaleDataSetKey.GRANTOR.getKeyName());
					
					BankSuccessorDocumentI document = (BankSuccessorDocument)bridge.importData();
					document.setBankName(bankName);
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				}
				
				NodeList pageXofYList = nodeList.extractAllNodesThatMatch(new TagNameFilter("td"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("style","HEIGHT: 4px"), true);
				String pageXofY = "";
				if (pageXofYList.size()>0) {
					pageXofY = pageXofYList.elementAt(0).toHtml();
				}
				String totalPages = "";
				Matcher matcher = Pattern.compile("(?is)Page\\s+\\d+\\s+of\\s+(\\d+)").matcher(pageXofY);
				if (matcher.find()) {
					totalPages = matcher.group(1);
				}
				
				header = processLinks(response,nodeList, totalPages);
				header += "<table class=\"datagrid\" cellspacing=\"0\" cellpadding=\"3\" rules=\"rows\" border=\"2\" id=\"dgInstitution\"" +
					"style=\"border-width:2px;border-style:solid;font-family:Arial;font-size:X-Small;width:100%;border-collapse:collapse;\">" +
					"<tr class=\"datagridH\" style=\"color:Blue;font-weight:bold;\">" +
					"<th align=\"left\" scope=\"col\">Name (RSSD ID)</th>" + 
					"<th align=\"left\" scope=\"col\">City</th>" +
					"<th align=\"left\" scope=\"col\">State/ Country</th>" +
					"<th align=\"left\" scope=\"col\">Institution Type</th>" +
					"<th align=\"left\" scope=\"col\">As of Date</th></tr>";
				
				
				footer = "</table><table width=\"100%\"><tr>" + pageXofY + "<tr></table>";
				
				response.getParsedResponse().setHeader(header);
				response.getParsedResponse().setFooter(footer);
																
				outputTable.append(table);
			}
			
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}
	
	protected String processLinks(ServerResponse response, NodeList nodeList, String totalPages) {
		
		StringBuilder links = new StringBuilder("<table width=\"100%\"><tr>");
		NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("td"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("align", "right"));
		String currentPage = "";
		Matcher matcher = Pattern.compile("(?is)<option.*?selected=\"selected\".*?>(\\d+)</option>").matcher(tableList.elementAt(0).toHtml());
		if (matcher.find()) {
			currentPage = matcher.group(1);
		}
		String allLinks = "";
		if(tableList.size() > 0	) {
			allLinks = tableList.elementAt(0).toHtml();
			allLinks = allLinks.replaceAll("(?is)<select.*?>", "<select name=\"ddlbPage\" onchange=\"location.href='"				//select list
					+ CreatePartialLink(TSConnectionURL.idPOST)					
					+ "SearchResultForm.aspx&__EVENTTARGET=ddlbPage&seq=" + seq 
					+ "&ddlbPage=' + this.options[this.selectedIndex].value;\">");
			if (currentPage.equals("1")) {
				allLinks = allLinks.replaceFirst("(?is)<a[^>]*?(style=\"[^>]*?\")[^>]*?>(< Previous)</a>", "<span $1>$2<span>");	//previous link
			} else {
				allLinks = allLinks.replaceAll("(?is)href=\\\"[^\\\"]*__doPostBack[^\\\"]*?lkbtnPrevRec[^\\\"]*?\\\"", "href=\"" + 
						CreatePartialLink(TSConnectionURL.idPOST) +	"SearchResultForm.aspx&__EVENTTARGET=lkbtnPrevRec&ddlbPage=" + 
						currentPage + "&seq=" + seq + "\"");
			}
			if (currentPage.equals(totalPages)) {
				allLinks = allLinks.replaceFirst("(?is)<a[^>]*?(style=\"[^>]*?\")[^>]*?>(Next >)</a>", "<span $1>$2<span>");		//next link
			} else {
				allLinks = allLinks.replaceAll("(?is)href=\\\"[^\\\"]*__doPostBack[^\\\"]*?lkbtnNextRec[^\\\"]*?\\\"", "href=\"" + 
						CreatePartialLink(TSConnectionURL.idPOST) +	"SearchResultForm.aspx&__EVENTTARGET=lkbtnNextRec&ddlbPage=" + 
						currentPage + "&seq=" + seq + "\"");
			}
			
		}
				
		links.append(allLinks);		
		links.append("</tr></table>");

		return links.toString() ;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap) {
	
		try {
						
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"BS");
			resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "BANK SUCCESSOR");
				
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
							
			NodeList instrList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "lblID_RSSD"));
			String instr = instrList.elementAt(0).toPlainTextString().trim();
			resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instr);
			
			NodeList nameList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "lblNm_lgl"));
			String name = nameList.elementAt(0).toPlainTextString().trim();
			resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), name);
						
			ro.cst.tsearch.servers.functions.GenericBS.parseNames(resultMap, searchId);
					
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
	
	public DocumentI smartParseDetails(ServerResponse response, String detailsHtml, boolean fillServerResponse){
		BankSuccessorDocumentI document = null;
		try {
			ResultMap map = new ResultMap();
			parseAndFillResultMap(response,detailsHtml, map);
			map.removeTempDef();//this is for removing tmp items. we remove them here to not remove them in every place when we parse something.
			Bridge bridge = new Bridge(response.getParsedResponse(),map,searchId);
			document = (BankSuccessorDocumentI)bridge.importData();
			document.setBankName((String)map.get(SaleDataSetKey.GRANTOR.getKeyName()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(fillServerResponse) {
			response.getParsedResponse().setResponse(detailsHtml);
			if(document!=null) {
				response.getParsedResponse().setDocument(document);
			}
		}
		response.getParsedResponse().setSearchId(this.searchId);
		return document;
	}
}
