package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.OptionTag;
import org.htmlparser.tags.SelectTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.LinkParser;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.DocumentStateFilter;
import ro.cst.tsearch.search.filter.DocumentStateFilter.DocumentState;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.ParcelIDFilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

public class FLHernandoTR extends TSServer {
	
	static final long serialVersionUID = 10000000;
	private boolean downloadingForSave; 
	
	private static final Pattern ROW_PATTERN = Pattern.compile("(?is)Value\\s*\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"\\s*,\\s*\\\"Text\\s*\\\"\\s*:\\s*\\\"\\s*([^\\\"]*)");
	private static final Pattern listboxFileName_PATT = Pattern.compile("(?is)listboxFileName\\s*\\\"\\s*:\\s*\\\"([^\\\"]*)");
	private static final String EMPTY_INTERMEDIARIES = "(?is).*\\\"length\\\":0,.*";
	
	public void setServerID(int ServerID) {
		super.setServerID(ServerID);
	}

	public FLHernandoTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}

	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		ParsedResponse parsedResponse = Response.getParsedResponse();
	
		if (sAction.contains("?")) {
			Response.setQuerry(Response.getQuerry() + "&"
					+ sAction.substring(sAction.indexOf("?") + 1));
			sAction = sAction.substring(0, sAction.indexOf("?"));
		}
		
		String rsResponse = Response.getResult();
		rsResponse = StringEscapeUtils.unescapeHtml(rsResponse);
		rsResponse=rsResponse.replaceAll("(?is)\\u00A0", " ");// '\u00A0' is the Unicode representation for '&nbsp;'
		rsResponse = StringEscapeUtils.unescapeJava(rsResponse);// because sometimes the result contains contains \u0027 instead of ' ..etc
		
		String next = "";
		String prev = "";
		String sTmp2 = CreatePartialLink(TSConnectionURL.idPOST);
		
		if (rsResponse.indexOf("Session cannot be created, no correct connection to application service.") != -1){
			Response.getParsedResponse().setError("Session cannot be created, no correct connection to application service.");
			return;
		} else if (rsResponse.indexOf("Error message") != -1){
			Response.getParsedResponse().setError("Some error occured");
			return;
		}
		
		if (rsResponse.indexOf("Application can't re-log on") != -1){
			try {
				HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);
				NodeList mainList = htmlParser3.getNodeList();
				String message = HtmlParser3.getNodeByID("_MESSAGE", mainList, true).toHtml();
				Response.getParsedResponse().setError(message);
			} catch (Exception e) {
			}
			
			return;
		}
		
		rsResponse = rsResponse.replaceAll("(?is)\\s+(-\\d+)", "$1");
		
		// values in a row's cells are the ones separated by minimum 2 spaces;
		// e.g. search 'PICKFORD STREET'; some pages have less than 50 results
		if (viParseID == ID_SEARCH_BY_ADDRESS || viParseID == ID_SEARCH_BY_INSTRUMENT_NO || viParseID == ID_SEARCH_BY_NAME) {
			if (rsResponse.matches(EMPTY_INTERMEDIARIES)) {
				Response.getParsedResponse().setError("No results found");
				return;
			}
			rsResponse = rsResponse.replaceAll("((?:\\*\\*)?" + Pattern.quote("* DELETED *") + "(?:\\*\\*)?)", "  $1  ");
			rsResponse = rsResponse.replaceAll("((?:R|M)\\d{2}\\s\\d{3}\\s\\d{2}\\s)", "  $1");
			rsResponse = rsResponse.replaceAll("(P\\d{4}-\\d{6}-\\d{3})", "  $1");
			rsResponse = rsResponse.replaceAll("([^\\s])\\s+(ETUX)", "$1 $2");// bug: extra cells in intermediaries table
		}
		
		switch (viParseID) {
		case ID_SEARCH_BY_ADDRESS:
			
			StringBuffer tableBuff = new StringBuffer("<table id=\"SRCH_LIST_AMT\" border=\"1\"><tr><th>Location</th><th>Owner</th><th>Parcel</th><th>Alt Key</th></tr>\n");
			String tableHeader = tableBuff.toString();
			
			String listboxFileName = "";
			Matcher listBoxMat = listboxFileName_PATT.matcher(rsResponse);
			if (listBoxMat.find()){
				listboxFileName = listBoxMat.group(1);
			}
			
			Matcher rowMat = ROW_PATTERN.matcher(rsResponse);
			while (rowMat.find()){
				String linkDetails = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + sAction + "?Value=" + rowMat.group(1) 
										+ "&listboxFileName=" + listboxFileName + "\">";
				String rowData = rowMat.group(2);
				rowData = testAndMarkEmptyCell(rowData);
				
				String[] cellsData = rowData.split("\\s{2,}");
				tableBuff.append("<tr>");
				for (String cell : cellsData){
					tableBuff.append("<td>").append(linkDetails).append(cell).append("</a></td>");
				}
				tableBuff.append("</tr>\n");
				
			}
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, tableBuff.toString(), outputTable);
			
			if(smartParsedResponses.size() == 0) {
				return;
			}
			
			next = "<a href=\"" + sTmp2 + sAction + "?Type=AmtButtonGroup&Name=SRCH_DIR_AMT&Value=N&Search=Address\">Next&gt;&gt;</a>";
			prev = "<a href=\"" + sTmp2 + sAction + "?Type=AmtButtonGroup&Name=SRCH_DIR_AMT&Value=B&Search=Address\">&lt;&lt;Prior</a>";
			
			parsedResponse.setHeader(prev + "&nbsp;&nbsp;&nbsp;" + next + "</br></br>" + tableHeader);
			parsedResponse.setFooter("</table></br></br>" + prev + "&nbsp;&nbsp;&nbsp;" + next);
			
			parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
			parsedResponse.setOnlyResponse(outputTable.toString());
			parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
			
			break;
		case ID_SEARCH_BY_NAME:
			
			tableBuff = new StringBuffer("<table id=\"SRCH_LIST_AMT\" border=\"1\"><tr><th>Owner</th><th>Location</th><th>Parcel</th><th>Alt Key</th></tr>\n");
			tableHeader = tableBuff.toString();
			
			listboxFileName = "";
			listBoxMat = listboxFileName_PATT.matcher(rsResponse);
			if (listBoxMat.find()){
				listboxFileName = listBoxMat.group(1);
			}
			
			rowMat = ROW_PATTERN.matcher(rsResponse);
			while (rowMat.find()){
				String linkDetails = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + sAction + "?Value=" + rowMat.group(1) 
										+ "&listboxFileName=" + listboxFileName + "\">";
				String rowData = rowMat.group(2);
				rowData = testAndMarkEmptyCell(rowData);
				
				String[] cellsData = rowData.split("\\s{2,}");
				tableBuff.append("<tr>");
				for (String cell : cellsData){
					tableBuff.append("<td>").append(linkDetails).append(cell).append("</a></td>");
				}
				tableBuff.append("</tr>\n");
				
			}
			outputTable = new StringBuilder();
			smartParsedResponses = smartParseIntermediary(Response, tableBuff.toString(), outputTable);
			
			if(smartParsedResponses.size() == 0) {
				return;
			}
			
			next = "<a href=\"" + sTmp2 + sAction + "?Type=AmtButtonGroup&Name=SRCH_DIR_AMT&Value=N&Search=Owner\">Next&gt;&gt;</a>";
			prev = "<a href=\"" + sTmp2 + sAction + "?Type=AmtButtonGroup&Name=SRCH_DIR_AMT&Value=B&Search=Owner\">&lt;&lt;Prior</a>";
			
			parsedResponse.setHeader(prev + "&nbsp;&nbsp;&nbsp;" + next + "</br></br>" + tableHeader);
			parsedResponse.setFooter("</table></br></br>" + prev + "&nbsp;&nbsp;&nbsp;" + next);
			
			parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
			parsedResponse.setOnlyResponse(outputTable.toString());
			parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
			
			break;
		case ID_SEARCH_BY_PARCEL:
			
			ParseResponse(sAction, Response, ID_DETAILS);
			break;
		case ID_SEARCH_BY_INSTRUMENT_NO:
			
			tableBuff = new StringBuffer("<table id=\"SRCH_LIST_AMT\" border=\"1\"><tr><th>Parcel</th><th>Owner</th><th>Location</th><th>Alt Key</th></tr>\n");
			tableHeader = tableBuff.toString();
			
			listboxFileName = "";
			listBoxMat = listboxFileName_PATT.matcher(rsResponse);
			if (listBoxMat.find()){
				listboxFileName = listBoxMat.group(1);
			}
			
			rowMat = ROW_PATTERN.matcher(rsResponse);
			while (rowMat.find()){
				String linkDetails = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + sAction + "?Value=" + rowMat.group(1) 
										+ "&listboxFileName=" + listboxFileName + "\">";
				String rowData = rowMat.group(2);
				rowData = testAndMarkEmptyCell(rowData);
				
				String[] cellsData = rowData.split("\\s{2,}");
				tableBuff.append("<tr>");
				for (String cell : cellsData){
					tableBuff.append("<td>").append(linkDetails).append(cell).append("</a></td>");
				}
				tableBuff.append("</tr>\n");
				
			}
			outputTable = new StringBuilder();
			smartParsedResponses = smartParseIntermediary(Response, tableBuff.toString(), outputTable);
			
			if(smartParsedResponses.size() == 0) {
				return;
			}
			
			next = "<a href=\"" + sTmp2 + sAction + "?Type=AmtButtonGroup&Name=SRCH_DIR_AMT&Value=N&Search=Property\">Next&gt;&gt;</a>";
			prev = "<a href=\"" + sTmp2 + sAction + "?Type=AmtButtonGroup&Name=SRCH_DIR_AMT&Value=B&Search=Property\">&lt;&lt;Prior</a>";
			
			parsedResponse.setHeader(prev + "&nbsp;&nbsp;&nbsp;" + next + "</br></br>" + tableHeader);
			parsedResponse.setFooter("</table></br></br>" + prev + "&nbsp;&nbsp;&nbsp;" + next);
			
			parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
			parsedResponse.setOnlyResponse(outputTable.toString());
			parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
			break;
		
		case ID_DETAILS:
			
			rsResponse = getDetails(rsResponse);

			if (!Response.isParentSiteSearch()) {
				rsResponse = rsResponse.replaceAll("(?is)<a\\s*[^>]*>[^<]*</a\\s*[^>]*>", "");
			}
			String keyCode = "File";
			String pid = "";
			
			try {		
				
				HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);
				NodeList mainList = htmlParser3.getNodeList();
				pid = org.apache.commons.lang.StringUtils.defaultString(HtmlParser3.getValueFromNextCell(mainList, "PARCEL", "", false)).trim();
			} catch(Exception e) {
				e.printStackTrace();
			}
			
			if (StringUtils.isNotEmpty(pid)){
				keyCode = pid.replaceAll("(?is)\\s+", "");
			}
			
			if(!downloadingForSave) {
				String qry_aux = Response.getRawQuerry();
				qry_aux = "dummy=" + keyCode + "&" + qry_aux;
				String originalLink = sAction + "&" + qry_aux;
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink.replace("\\s+", "");
				
				HashMap<String, String>	data = new HashMap<String, String>();
				data.put("type", "CNTYTAX");

				
				if (isInstrumentSaved(pid, null, data) || isInstrumentSaved(pid.replaceAll("(?is)\\s+", ""), null, data)) {
					rsResponse += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, rsResponse);
					rsResponse = addSaveToTsdButton(rsResponse, sSave2TSDLink, viParseID);
				}
				parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
				parsedResponse.setResponse(rsResponse);
				
			} else {// for html
				
				smartParseDetails(Response, rsResponse);
				
				@SuppressWarnings("unchecked")
				Vector<PropertyIdentificationSet> vectorPIS = (Vector<PropertyIdentificationSet>)parsedResponse.infVectorSets.get("PropertyIdentificationSet");
				String status = vectorPIS.get(0).getAtribute("Status");
				if (StringUtils.isNotEmpty(status)){
					if ((status.contains("DELETED") || status.contains("INACTIVE")) && !Response.isParentSiteSearch()){
						TaxDocumentI taxDocumentI = (TaxDocumentI) Response.getParsedResponse().getDocument();
						SearchLogger.info("<br>Document " + taxDocumentI.prettyPrint() + " will not be saved because his status is DELETED or INACTIVE.<br>", searchId);
						Response.getParsedResponse().setError("<br>Document " + taxDocumentI.prettyPrint() + " is INVALID because his status is DELETED or INACTIVE.<br>");
					}
				}
				
				msSaveToTSDFileName = keyCode + ".html";
				Response.getParsedResponse().setFileName(
						getServerTypeDirectory() + msSaveToTSDFileName);
				rsResponse = rsResponse.replaceAll("(?is)<a\\s*[^>]*>[^<]*</a\\s*[^>]*>", "");
				msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();
			}
			break;
		case ID_GET_LINK:
			LinkParser link1 = new LinkParser("www.hernandopa-fl.us?" + Response.getQuerry());
			
			if (Response.getQuerry().contains("Value") && Response.getQuerry().contains("listboxFileName")) {
				ParseResponse(sAction, Response, ID_DETAILS);
			} else {
				if (link1.getParamValue("Search").equalsIgnoreCase("Address")) {
					ParseResponse(sAction, Response, ID_SEARCH_BY_ADDRESS);
				} else if (link1.getParamValue("Search").equalsIgnoreCase("Owner")) {
						ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				} else if (link1.getParamValue("Search").equalsIgnoreCase("Property")) {
						ParseResponse(sAction, Response, ID_SEARCH_BY_INSTRUMENT_NO);
				}
			}
			break;
		case ID_SAVE_TO_TSD :
			downloadingForSave = true;
			ParseResponse(sAction, Response, ID_DETAILS);
			downloadingForSave = false;
		break;
		default:
			break;

		}
	}
	
	// if empty cells(only space) are not marked, the row is not displayed,
	// because it doesn't pass number of cells test in smartParseIntermediary
	private String testAndMarkEmptyCell(String rowData) {
		if (rowData.split("\\s{2,}").length < 4) {
			rowData = rowData.replaceFirst("\\s{25,}", "  * EMPTY *  ");
		}
		return rowData;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
				
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag mainTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "SRCH_LIST_AMT"), true).elementAt(0);
			
			if(mainTable == null) {
				return intermediaryResponse;
			}
			
			TableRow[] rows = mainTable.getRows();

			for (TableRow row : rows){
				if(row.getColumnCount() > 3) {
					
					TableColumn[] cols = row.getColumns();
					NodeList aList = cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
					String link = ((LinkTag) aList.elementAt(0)).extractLink();
					String rowHtml =  row.toHtml();
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
													
					ResultMap resultMap = new ResultMap();
					resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
					if (rows[0].getHeaders()[0].toHtml().toLowerCase().contains("parcel")){//Real Property search
						
						String tmpPIN = cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0).getChildren().toHtml();
						if (tmpPIN.toLowerCase().contains("deleted")){
							resultMap.put(PropertyIdentificationSetKey.STATUS.getKeyName(), "DELETED");
						}
						tmpPIN = tmpPIN.replaceAll("(?is)[\\*\\s\\s]+DELETED[\\*\\s]+", "");
						resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), tmpPIN.trim());
						resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), tmpPIN.trim().replaceAll("(?is)[\\s-]+", ""));
						resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), tmpPIN.trim().replaceAll("(?is)[\\s-]+", ""));
						
						if (tmpPIN.trim().startsWith("R")){
							resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "REAL ESTATE");
						}
						
						String tmpOwnersName = cols[1].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0).getChildren().toHtml();
						ro.cst.tsearch.servers.functions.FLHernandoTR.parseOwnerNames(tmpOwnersName, resultMap, searchId);
						
						String tmpAddress = cols[2].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0).getChildren().toHtml();
						if (tmpAddress.toLowerCase().contains("deleted")){
							resultMap.put(PropertyIdentificationSetKey.STATUS.getKeyName(), "DELETED");
						}
						tmpAddress = tmpAddress.replaceAll("(?is)\\*\\s*DELETED\\s*\\*", "");
						if (StringUtils.isNotEmpty(tmpAddress)){
							resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(tmpAddress));
							resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(tmpAddress));
						}
					} else {
						if (rows[0].getHeaders()[0].toHtml().toLowerCase().contains("location")){//Address search
							
							String tmpAddress = cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0).getChildren().toHtml();
							if (tmpAddress.toLowerCase().contains("deleted")){
								resultMap.put(PropertyIdentificationSetKey.STATUS.getKeyName(), "DELETED");
							}
							tmpAddress = tmpAddress.replaceAll("(?is)\\*\\s*DELETED\\s*\\*", "");
							if (StringUtils.isNotEmpty(tmpAddress)){
								tmpAddress = tmpAddress.replaceAll("(?is)\\A(\\d+)(.*)", "$1 $2");
								resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(tmpAddress));
								resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(tmpAddress));
							}
							
							String tmpOwnersName = cols[1].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0).getChildren().toHtml();
							ro.cst.tsearch.servers.functions.FLHernandoTR.parseOwnerNames(tmpOwnersName, resultMap, searchId);
							
						} else if (rows[0].getHeaders()[0].toHtml().toLowerCase().contains("owner")){ //name search
							String tmpOwnersName = cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0).getChildren().toHtml();
							ro.cst.tsearch.servers.functions.FLHernandoTR.parseOwnerNames(tmpOwnersName, resultMap, searchId);
							
							String tmpAddress = cols[1].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0).getChildren().toHtml();
							if (tmpAddress.toLowerCase().contains("deleted")){
								resultMap.put(PropertyIdentificationSetKey.STATUS.getKeyName(), "DELETED");
							}
							tmpAddress = tmpAddress.replaceAll("(?is)\\*\\s*DELETED\\s*\\*", "");
							if (StringUtils.isNotEmpty(tmpAddress)){
								resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(tmpAddress));
								resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(tmpAddress));
							}
						}
						
						String tmpPIN = cols[2].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0).getChildren().toHtml();
						if (tmpPIN.toLowerCase().contains("deleted")){
							resultMap.put(PropertyIdentificationSetKey.STATUS.getKeyName(), "DELETED");
						}
						tmpPIN = tmpPIN.replaceAll("(?is)[\\*\\s\\s]+DELETED[\\*\\s]+", "");
						resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), tmpPIN.trim());
						resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), tmpPIN.trim().replaceAll("(?is)[\\s-]+", ""));
						resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), tmpPIN.trim().replaceAll("(?is)[\\s-]+", ""));
						
						if (tmpPIN.trim().startsWith("R")){
							resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "REAL ESTATE");
						}
					}
					
					Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
					resultMap.removeTempDef();
						
					DocumentI document = (TaxDocumentI) bridge.importData();						
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				}
			}
			outputTable.append(table);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	private String getDetails(String rsResponse){

		if (!rsResponse.toLowerCase().contains("<html")) {
			return rsResponse;
		}
		
		StringBuffer details = new StringBuffer();
		try {
			HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);
			NodeList nodeList = htmlParser3.getNodeList().extractAllNodesThatMatch(new TagNameFilter("body"), true);
			FormTag formMain = (FormTag) htmlParser3.getNodeById("theform");
			if (formMain != null) {
				rsResponse = formMain.getChildrenHTML();
				rsResponse = rsResponse.replaceAll("(?is)<input type=\\\"hidden\\\"[^>]*>", "");
				rsResponse = rsResponse.replaceAll("(?is)</?script[^>]*>", "");
				rsResponse = rsResponse.replaceAll("(?is)<img[^>]*>", "");
				htmlParser3 = new HtmlParser3(rsResponse);
				nodeList = htmlParser3.getNodeList().extractAllNodesThatMatch(new TagNameFilter("div"), true);

				details.append("<table border=\"1\" align=\"center\">");
				details.append(makeTableRow("<b>STATUS:</b>", getListCont(nodeList, "ctl00_ClientContent_PANEL_0_AMT_STAT_MSG_AMT")));
				details.append(makeTableRow("<b>TAX YEAR:</b>", getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_ROLL_YEAR_AMT")));
				details.append(makeTableRow("<b>KEY:</b>", getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PANEL_2_AMT_KEY_PYMT_AMT")));
				details.append(makeTableRow("<b>PARCEL:</b>", getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PARCEL_AMT")));
				details.append(makeTableRow("<b>OWNER:</b>", getListCont(nodeList, "ctl00_ClientContent_OWNER_AMT")));
				details.append(makeTableRow("<b>LOCATION:</b>", getListCont(nodeList, "ctl00_ClientContent_LOCATION_AMT")));
				details.append(makeTableRow("<b>ADDRESS:</b>", getListCont(nodeList, "ctl00_ClientContent_ADDRESS_AMT")));
				details.append(makeTableRow("<b>DESCRIPTION:</b>", getListCont(nodeList, "ctl00_ClientContent_LGL_AMT")));
				details.append("</table>");
				
				details.append("<table border=\"1\" align=\"center\">");
				details.append(makeTableRow("<b>ZONING:</b>", getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_ZONING_AMT")));
				details.append(makeTableRow("<b>DESC:</b>", getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_ZONE_DESC_AMT")));
				details.append(makeTableRow("<b>ZONE ID:</b>", getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_ZONEID_AMT")));
				details.append(makeTableRow("<b>LEVY:</b>", getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_LEVY_AMT"),
						getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_LEVY_DESC_AMT")));
				details.append(makeTableRow("<b>AREA:</b>", getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_AREA_AMT"),
						getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_AREA_DESC_AMT")));
				details.append(makeTableRow("<b>USE:</b>", getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_DOR_DESC_AMT")));
				details.append(makeTableRow("<b>MILLAGE:</b>", getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_MILLAGE_AMT")));
				details.append(makeTableRow("<b>MARKET:</b>", getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_MKT_A_AMT"),
						getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_MKT_DESC_AMT")));
				details.append(makeTableRow("<b>PCA1:</b>", getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_PCA1_AMT"),
						getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_PCA1_DESC_AMT")));
				details.append(makeTableRow("<b>PCA2:</b>", getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_PCA2_AMT"),
						getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_PCA2_DESC_AMT")));
				details.append(makeTableRow("<b>PCA3:</b>", getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_PCA3_AMT"),
						getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_PCA3_DESC_AMT")));
				details.append(makeTableRow("<b>PCA4:</b>", getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_PCA4_AMT"),
						getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_PCA4_DESC_AMT")));
				details.append(makeTableRow("<b>LAND:</b>", getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_LAND_VALUE_AMT")));
				details.append(makeTableRow("<b>CLASS:</b>", getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_CLA_VALUE_AMT")));
				details.append(makeTableRow("<b>SQFT:</b>", getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_SQFT_AMT")));
				details.append(makeTableRow("<b>ACRES:</b>", getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_ACRES_AMT")));
				details.append(makeTableRow("<b>JUST:</b>", getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_JUST_VALUE_AMT")));
				details.append(makeTableRow("<b>ASSESSED:</b>", getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_ASD_VALUE_AMT")));
				details.append(makeTableRow("<b>EXEMPT:</b>", getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_EXM_VALUE_AMT")));
				details.append(makeTableRow("<b>TAXABLE:</b>", getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_TXB_VALUE_AMT")));
				details.append(makeTableRow("<b>EXEMPTIONS:</b>", getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_OCC0001REXM1_AMT"),
						getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_OCC0002REXM1_AMT"),
						getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_OCC0003REXM1_AMT"),
						getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_OCC0004REXM1_AMT"),
						getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PVRVW_AMT_OCC0005REXM1_AMT")));
				details.append("</table>");
				details.append("<br><br>");
				
				details.append("<h3 align=\"center\"><b>*****  SCROLL DOWN FOR ADDITIONAL INFORMATION  *****</b></h3><br><br>");
				
				details.append("<table border=\"1\" align=\"center\" id=\"idTaxesDue\">");
				details.append("<tr><th>YEAR/ASM</th><th>CERT NUM</th><th>BID</th><th>TAX AMT</th><th>BALANCE</th>"
						+ "<th>INT %</th><th>TOTAL DUE</th><th>THRU</th><th>TYPE</th></tr>");
				details.append(transformSelectInTableBody(nodeList, "ctl00_ClientContent_PANEL_3_AMT_TXD_NFO_AMT"));
				details.append("</table>");
				details.append("<br><br>");
				
				details.append("<table border=\"1\" align=\"center\">");
				details.append("<tr><td>TOTAL TAX DUE</td><td>")
						.append(getHtmlFromDiv(htmlParser3, "ctl00_ClientContent_PANEL_3_AMT_GTOT_TAXDUE_AMT")).append("</td></tr>");
				details.append("</table>");
				details.append("<br><br>");
				
				/*details.append("<table border=\"1\"><tr><td>");
				details.append(nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ClientContent_PANEL_3_AMT_DISPLAY_104_AMT")).elementAt(0).toHtml());
				details.append(nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ClientContent_PANEL_3_AMT_DISPLAY_105_AMT")).elementAt(0).toHtml());
				details.append(nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ClientContent_PANEL_3_AMT_DISPLAY_106_AMT")).elementAt(0).toHtml());
				details.append(nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ClientContent_PANEL_3_AMT_DISPLAY_108_AMT")).elementAt(0).toHtml());
				details.append(nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ClientContent_PANEL_3_AMT_DISPLAY_109_AMT")).elementAt(0).toHtml());
				details.append(nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ClientContent_PANEL_3_AMT_DISPLAY_110_AMT")).elementAt(0).toHtml());
				details.append(nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ClientContent_PANEL_3_AMT_DISPLAY_111_AMT")).elementAt(0).toHtml());
				details.append(nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ClientContent_PANEL_3_AMT_DISPLAY_112_AMT")).elementAt(0).toHtml());
				details.append("</td></tr></table>");
				details.append("<br><br>");*/

				details.append("<h3 align=\"center\"><b>PAYMENT HISTORY</b></h3><br><br>");
				details.append("<table border=\"1\" id=\"idPayments\" align=\"center\">");
				details.append("<tr><th>YEAR</th><th>ADJ</th><th>SEQ</th><th>CERTIFICATE</th><th>RECEIPT</th>"
						+ "<th>TYPE</th><th>DATE</th><th>AMT PAID</th><th>DISC/PEN</th></tr>");
				details.append(transformSelectInTableBody(nodeList, "ctl00_ClientContent_PANEL_2_AMT_PAY_NFO_AMT"));
				details.append("</table>");
				details.append("<br><br>");
				
				details.append("<h3 align=\"center\"><b>PAST TAX VALUATION</b></h3><br><br>");
				details.append("<table border=\"1\" id=\"idValuation\" align=\"center\">");
				details.append("<tr><th>YEAR</th><th>/ADJ</th><th>EI NUM</th><th>ASSESSED</th><th>EXEMTIONS</th><th>TAXABLE</th>"
						+ "<th>Gross Tax(No Pen/Disc)</th></tr>");
				details.append(transformSelectInTableBody(nodeList, "ctl00_ClientContent_PANEL_4_AMT_PTV_NFO_AMT"));
				details.append("</table>");
				details.append("<br><br>");
				
				details.append("<h3 align=\"center\"><b>SPECIAL ASSESMENTS</b></h3><br><br>");
				details.append("<table border=\"1\" id=\"idSpecialAssesments\" align=\"center\">");
				details.append("<tr><th>ASSESMENT DESC</th><th>UNITS</th><th>RATE</th><th>ASSESMENT</th><th>BALANCE</th>"
						+ "<th>TOTAL ASSESMENTS</th></tr>");
				details.append(transformSelectInTableBody(nodeList, "ctl00_ClientContent_PANEL_5_AMT_ASSM_NFO_AMT"));
				details.append("</table>");
				details.append("<br><br>");
				
				details.append("<h3 align=\"center\"><b>SALES INFORMATION</b></h3><br><br>");
				details.append("<table border=\"1\" id=\"idSalesInformation\" align=\"center\">");
				details.append("<tr><th>SALE DATE</th><th>NAME</th><th>TYPE</th><th>BOOK</th><th>PAGE</th><th>V or I</th><th>SALE PRICE</th></tr>");
				details.append(transformSelectInTableBody(nodeList, "ctl00_ClientContent_PANEL_7_AMT_SALES_NFO_AMT"));
				details.append("</table>");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return details.toString();
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);
			
			String status = HtmlParser3.getValueFromNextCell(mainList, "STATUS:", "", false).trim();
			if (StringUtils.isNotEmpty(status)){
				if (status.toLowerCase().contains("deleted")){
					resultMap.put(PropertyIdentificationSetKey.STATUS.getKeyName(), "DELETED");
				} else if (status.toLowerCase().contains("inactive") || status.toLowerCase().contains("parcel merged")){
					resultMap.put(PropertyIdentificationSetKey.STATUS.getKeyName(), "INACTIVE");
				}
			}
			
			String pid = HtmlParser3.getValueFromNextCell(mainList, "PARCEL:", "", false).trim();
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), StringUtils.isNotEmpty(pid.replaceAll("(?is)\\s+", "")) ? pid : "");
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), StringUtils.isNotEmpty(pid) ? pid : "");
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), StringUtils.isNotEmpty(pid.replaceAll("(?is)\\s+", "")) ? pid : "");
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID3.getKeyName(), StringUtils.isNotEmpty(pid.replaceAll("(?is)\\s+", "")) ? pid : "");
			if (pid.trim().startsWith("R")){
				resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "REAL ESTATE");
			}
			
			String owner = HtmlParser3.getValueFromNextCell(mainList, "OWNER:", "", true).trim();
			
			String address = HtmlParser3.getValueFromNextCell(mainList, "LOCATION:", "", false).trim();
			if (StringUtils.isNotEmpty(address)){
				resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
				resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
			}
			
			String taxYear = HtmlParser3.getValueFromNextCell(mainList, "TAX YEAR:", "", false).trim();
			if (StringUtils.isNotEmpty(taxYear)){
				resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
			}
			
			String legal = HtmlParser3.getValueFromNextCell(mainList, "DESCRIPTION:", "", true).trim();
			if (StringUtils.isNotEmpty(legal)){
				legal = legal.replaceAll("(?is)\\A\\s*<br>", "");
				resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal.trim());
			}
				
			try {
			String totalDueTable = HtmlParser3.getNodeByID("idTaxesDue", mainList, true).toHtml();
				if (StringUtils.isNotEmpty(totalDueTable)){
					
					List<List<String>> dueTableList = HtmlParser3.getTableAsList(totalDueTable, false);
					String totalDue = dueTableList.get(0).get(6).toString();
					totalDue = totalDue.replaceAll("(?is)[\\$,]+", "").trim();
					resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue);
					
				}
			} catch (Exception e) {
				logger.error("Didn't get the total due on FLHernandoTR " + searchId);
			}
			
			try {
				String totalTaxDue = HtmlParser3.getValueFromNextCell(mainList, "TOTAL TAX DUE", "", false).trim();
				if (StringUtils.isNotEmpty(totalTaxDue)) {
					totalTaxDue = totalTaxDue.replaceAll("(?is)[\\$,]+", "").trim();
					resultMap.put("tmpTotalTaxDue", totalTaxDue);
				}
			} catch (Exception e) {
				logger.error("Didn't get the total due tax on FLHernandoTR " + searchId);
			}
			
			try {
				String paymentTable = HtmlParser3.getNodeByID("idPayments", mainList, true).toHtml();
				if (StringUtils.isNotEmpty(paymentTable)) {

					ResultTable rt = new ResultTable();
					List<List> body = new ArrayList<List>();
					List<String> list = new ArrayList<String>();

					List<List<String>> paymentsTableList = HtmlParser3.getTableAsList(paymentTable, false);

					String amtPaid = "";
					for (List<String> lst : paymentsTableList) {
						String eachYear = lst.get(0).toString().trim();
						if (taxYear.equals(eachYear)) {
							String tmpAmtPaid = lst.get(7).toString();
							amtPaid += "+" + tmpAmtPaid.replaceAll("(?is)[\\$,]+", "").trim();
							list = new ArrayList<String>();
							list.add(lst.get(0).trim());
							list.add(lst.get(4).trim());
							list.add(lst.get(6).trim());
							list.add(lst.get(7).replaceAll("(?is)[\\$,]+", "").trim());
							body.add(list);
						} else {
							list = new ArrayList<String>();
							list.add(lst.get(0).trim());
							list.add(lst.get(4).trim());
							list.add(lst.get(6).trim());
							list.add(lst.get(7).replaceAll("(?is)[\\$,]+", "").trim());
							body.add(list);
						}
					}

					resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(),
							GenericFunctions.sum(amtPaid, searchId));

					if (!body.isEmpty()) {
						String[] header = { "Year", "ReceiptNumber", "ReceiptDate", "ReceiptAmount" };
						rt = GenericFunctions2.createResultTable(body, header);
						if (rt != null) {
							resultMap.put(TaxHistorySet.class.getSimpleName(), rt);
						}
					}
				}
			} catch (Exception e) {
				logger.error("An error occured on payments on FLHernandoTR " + searchId);
			}
			
			try {
				String valuationTable = HtmlParser3.getNodeByID("idValuation", mainList, true).toHtml();
				if (StringUtils.isNotEmpty(valuationTable)) {
					List<List<String>> valuationTableList = HtmlParser3.getTableAsList(valuationTable, false);
					for (List<String> lst : valuationTableList) {
						String eachYear = lst.get(0).toString().trim();
						if (taxYear.equals(eachYear)) {
							String baseAmount = lst.get(6).toString();
							baseAmount = baseAmount.replaceAll("(?is)[\\$,]+", "").trim();
							resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount.trim());
							break;
						}
					}
				}
			} catch (Exception e) {
				logger.error("Didn't get the base amount on FLHernandoTR " + searchId);
			}
			try {
				String salesTable = HtmlParser3.getNodeByID("idSalesInformation", mainList, true).toHtml();
				if (StringUtils.isNotEmpty(salesTable)) {
					ResultTable rtSale = new ResultTable();
					List<List<String>> salesTableList = HtmlParser3.getTableAsList(salesTable, false);
					List<List> newBody = new ArrayList<List>();
					List<String> list = new ArrayList<String>();
					for (List<String> lst : salesTableList) {
						if (lst.size() > 6) {
							list = new ArrayList<String>();
							list.add(lst.get(0).trim());
							list.add(lst.get(1).trim());
							list.add(lst.get(2).trim());
							list.add(lst.get(3).replaceAll("(?is)\\A0+", "").trim());
							list.add(lst.get(4).replaceAll("(?is)\\A0+", "").trim());
							list.add(lst.get(6).replaceAll("(?is)[\\$,]+", "").trim());
							newBody.add(list);
						}
					}
					if (!newBody.isEmpty()) {
						String[] headerSale = { "InstrumentDate", "Grantor", "DocumentType", "Book", "Page", "SalesPrice" };
						rtSale = GenericFunctions2.createResultTable(newBody, headerSale);
						if (rtSale != null) {
							resultMap.put(SaleDataSet.class.getSimpleName(), rtSale);
						}
					}
				}
			} catch (Exception e) {
				logger.error("An error occured on sales on FLHernandoTR " + searchId);
			}
			ro.cst.tsearch.servers.functions.FLHernandoTR.parseOwnerNames(owner, resultMap, searchId);
			ro.cst.tsearch.servers.functions.FLHernandoTR.legalFLHernandoTR(resultMap, searchId);
			
	    } catch (Exception e) {
			e.printStackTrace();
		}
			return null;
	}

	private String makeTableRow(String cellHeader, String ... cellValue){
		
		if (cellHeader == null)
			return "";
		
		StringBuffer tableRow = new StringBuffer();
		
		tableRow.append("<tr><td>").append(cellHeader).append("</td>");
		for (String cellString : cellValue){
			tableRow.append("<td>").append(cellString).append("</td>");
		}
		tableRow.append("</tr>");
		
		return tableRow.toString();
	}
	
	private String transformSelectInTableBody(NodeList nodeList, String id){
		
		if (nodeList == null)
			return "";
		
		SelectTag select = (SelectTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", id), true).elementAt(0);
		NodeList options = select.getChildren().extractAllNodesThatMatch(new TagNameFilter("option"));
		StringBuffer tableRows = new StringBuffer();
		if ("ctl00_ClientContent_PANEL_5_AMT_ASSM_NFO_AMT".equals(id)){//SPECIAL ASSESMENTS table
			for (int i = 0; i < options.size(); i++){
				tableRows.append("<tr>");
				String option = options.elementAt(i).getChildren().toHtml().trim();
				if ("D".equals(((OptionTag)options.elementAt(i)).getAttribute("value"))){
					
					String[] items = option.split("\\s{4,}");
					for (String item : items){
						tableRows.append("<td>").append(item.trim()).append("</td>");
					}
					tableRows.append("<td> </td><td> </td><td> </td><td> </td>");
				} else if ("E".equals(((OptionTag)options.elementAt(i)).getAttribute("value"))){ 
					tableRows.append("<td> </td><td> </td>");
					option = option.replaceAll("(?is)([\\$\\d+\\.]+)", "  $1");
					
					String[] items = option.trim().split("\\s{2,}");
					for (String item : items){
						tableRows.append("<td>").append(item.trim()).append("</td>");
					}
				}
				tableRows.append("</tr>");
			}
		} else {
			for (int i = 0; i < options.size(); i++){
				String option = options.elementAt(i).getChildren().toHtml().trim();
				if (!"ctl00_ClientContent_PANEL_7_AMT_SALES_NFO_AMT".equals(id)){
					option = option.replaceAll("(?is)\\s{11,}", " -  ");
				}
				option = option.replaceAll("(?is)\\s{6,}", "  ");
				option = option.replaceAll("(?is)(\\d+)\\s", "$1  ");
				option = option.replaceAll("(?is)([A-Z]+)\\s{2,}([A-Z]{2})\\s{1}([A-Z]{2})", "$1 $2 $3 ");
				tableRows.append("<tr>");
				String[] items = option.split("\\s{2,}");
				for (String item : items){
					tableRows.append("<td>").append(item.trim()).append("</td>");
				}
				tableRows.append("</tr>");
			}
		}
		
		return tableRows.toString();
	}

	private String getListCont(NodeList nodeList, String id) {
		
		StringBuffer listRows = new StringBuffer("");
		
		try {
			SelectTag select = (SelectTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", id), true).elementAt(0);
			NodeList options = select.getChildren().extractAllNodesThatMatch(new TagNameFilter("option"));
			for (int i = 0; i < options.size(); i++) {
				String option = options.elementAt(i).getChildren().toHtml().trim();

				listRows.append("<br>").append(option);

			}
		} catch (Exception e) {
			return "";
		}
		return listRows.toString();
		
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		
		FilterResponse rejectNonRealEstateFilter = new RejectNonRealEstate(SearchAttributes.PROPERTY_TYPE, searchId);
		rejectNonRealEstateFilter.setThreshold(new BigDecimal("0.99"));
		
		FilterResponse rejectDeletedDocsFilter = new DocumentStateFilter(searchId, DocumentState.DELETED);
		rejectDeletedDocsFilter.setThreshold(new BigDecimal("0.99"));
		
		TSServerInfoModule m;
		FilterResponse nameFilterHybridDoNotSkipUnique = null;
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		
		if (hasPin()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			FilterResponse pinFilter = new ParcelIDFilterResponse(
					SearchAttributes.LD_PARCELNO, searchId);
			pinFilter.setThreshold(new BigDecimal(0.99d));
			m.addFilter(pinFilter);
			m.addFilter(rejectNonRealEstateFilter);
			m.addFilter(rejectDeletedDocsFilter);
			l.add(m);
		}
		if (hasStreet()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			m.addFilter(addressFilter);
			nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter( 
					SearchAttributes.OWNER_OBJECT , searchId , m );
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
			
			m.addFilter(nameFilterHybridDoNotSkipUnique);
			m.addFilter(rejectNonRealEstateFilter);
			m.addFilter(rejectDeletedDocsFilter);
			l.add(m);
		}
		if (hasOwner()) {
			m = new TSServerInfoModule(serverInfo
					.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);

			nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter( 
					SearchAttributes.OWNER_OBJECT , searchId , m );
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
			
			m.addFilter(nameFilterHybridDoNotSkipUnique);
			m.addFilter(addressFilter);
			m.addFilter(new ParcelIDFilterResponse(
					SearchAttributes.LD_PARCELNO, searchId));
			m.addFilter(rejectNonRealEstateFilter);
			m.addFilter(rejectDeletedDocsFilter);
			
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(m, searchId,
							new String[] {"L;F;","L;f;", "L;M;", "L;m;"});
			m.addIterator(nameIterator);

			l.add(m);
		}
		serverInfo.setModulesForAutoSearch(l);
	}

	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		// upper case if necessary 
        if(module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX){
        	String name = module.getFunction(0).getParamValue();
           	module.getFunction(0).setParamValue(name.toUpperCase());
        } else if(module.getModuleIdx() == TSServerInfo.ADDRESS_MODULE_IDX){
        	String streetDir = module.getFunction(4).getParamValue();
        	String streetName = module.getFunction(1).getParamValue();
        	String loApt = module.getFunction(2).getParamValue();
        	
           	module.getFunction(4).setParamValue(streetDir.toUpperCase());
           	module.getFunction(1).setParamValue(streetName.toUpperCase());
           	module.getFunction(2).setParamValue(loApt.toUpperCase());
        } else if(module.getModuleIdx() == TSServerInfo.PARCEL_ID_MODULE_IDX){
        	
        	String parcel = module.getFunction(0).getParamValue();
        	if (!parcel.contains(" ")){
        		parcel = parcel.replaceFirst("^R", "");
        		parcel = parcel.replaceAll("-", "");
    			if(parcel.length()==19){
    				String pid = "R" + parcel.substring(0,2) + " " + parcel.substring(2,5) + " " + parcel.substring(5,7)
    						+ " " + parcel.substring(7,11)
    						+ " " + parcel.substring(11,15) + " " + parcel.substring(15,19);
    				module.getFunction(0).setParamValue(pid);
    			}
        	}
           	
        }
        
        return super.SearchBy(module, sd);
	}
	
	protected String getFileNameFromLink(String url) {
		String keyCode = "File";
		if (url.contains("dummy="))
			keyCode = org.apache.commons.lang.StringUtils.substringBetween(url, "dummy=", "&");

		return keyCode + ".html";
	}
	
	protected String getHtmlFromDiv(HtmlParser3 htmlParser3, String divId) {
		String html = "";
		Node node = htmlParser3.getNodeById(divId);
		if (node != null) {

			NodeList div = node.getChildren().extractAllNodesThatMatch(new TagNameFilter("div")).elementAt(0).getChildren();
			if (div.size() > 0) {
				html = div.toHtml();
			}
		}
		return html;
	}

}
