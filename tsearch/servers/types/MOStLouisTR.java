package ro.cst.tsearch.servers.types;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Span;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http3.HttpSite3;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.DuplicateInstrumentFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
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
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

public class MOStLouisTR extends TSServer {

	private static final long serialVersionUID = -356001205651646977L;
	
	public MOStLouisTR(long searchId) {
		super(searchId);
	}

	public MOStLouisTR(String rsRequestSolverName, String rsSitePath, String rsServerID, 
			String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	private static int seq = 0;

	protected synchronized static int getSeq() {
		return seq++;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		switch (viParseID) {
		
		case ID_SEARCH_BY_PARCEL:
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_SUBDIVISION_NAME:
			
			if (rsResponse.indexOf("Due to routine maintenance procedures this page is currently unavailable") > -1) {
				Response.getParsedResponse().setError("Due to routine maintenance procedures this page is currently unavailable");
				return;
			}
			
			if (rsResponse.indexOf("Invalid Locator ID") > -1) {
				Response.getParsedResponse().setError("Invalid Locator ID");
				return;
			}
			
			if (rsResponse.indexOf("Invalid Owner Name") > -1) {
				Response.getParsedResponse().setError("Invalid Owner Name");
				return;
			}
			
			if (rsResponse.indexOf("Invalid Address") > -1) {
				Response.getParsedResponse().setError("Invalid Address");
				return;
			}
			
			if (rsResponse.indexOf("Invalid Subdivision Name") > -1) {
				Response.getParsedResponse().setError("Invalid Subdivision Name");
				return;
			}
			
			if (rsResponse.indexOf("The specified URL cannot be found") > -1) {
				Response.getParsedResponse().setError("No data found.");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			
			if (rsResponse.indexOf("No Records Match Your Search Criteria") > -1) {
				Response.getParsedResponse().setError("No data found.");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if (smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,	outputTable.toString());
			}

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
			ParseResponse(sAction, Response, rsResponse.toLowerCase().contains("taxing address:")?ID_DETAILS:ID_SEARCH_BY_NAME);
			break;

		default:
			break;
		}

	}

	protected void loadDataHash(HashMap<String, String> data) {
		if (data != null) {
			data.put("type", "CNTYTAX");
		}
	}

	protected String getDetails(String rsResponse, StringBuilder parcelNumber) {
		try {
			Matcher ma = Pattern.compile("(?is)<span id=\"ctl00_MainContent_OwnLeg_labLocatorNum\">([^<]+)</span>").matcher(rsResponse);
//			Matcher ma2 = Pattern.compile("(?is)<span[^>]*>Locator Number\\s*:\\s*([^<]+)</span>").matcher(rsResponse);
			String detailsString = "";

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);

			/* If from memory - use it as is */
			if (!rsResponse.toLowerCase().contains("<html")) {
				if (ma.find()) {
					String parcelID = ma.group(1).trim();
					parcelNumber.append(parcelID);
				}
				
				return rsResponse;
			}

			if (ma.find()) {
				String parcelID = ma.group(1).trim();
				parcelNumber.append(parcelID);
			}

			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "divOwnLegData"));
			if (tables.size() > 0) {
				detailsString += "<h3>Assessment Data</h3>" + tables.elementAt(0).toHtml();
			}
			
			
			detailsString = detailsString.replaceAll("(?is)<abbr[^>]+>([^<]+)</abbr>","$1");
			detailsString = detailsString.replaceAll("(?is)<a class=\\\"PopupMsgLink\\\"[^>]+>\\s*<sup>\\s*\\?\\s*</sup>\\s*</a>", "");
			detailsString = detailsString.replaceAll("(?is)<span class=\\\"PopupWin\\\">\\s*<img[^>]+>\\s*(?:</img>)?\\s*(?:</span>)?","");
					
			String baseLink = getBaseLink().replaceFirst("(?i)/IAS/$", "");
			
			String salesTable = "";
			Matcher ma1 = Pattern.compile("(?is)<a[^>]+onclick=\\\"ShowDeedIndexes\\('([^']+)'\\s*,\\s*'([^']+)'\\s*,\\s*'([^']+)'\\);\\\"[^>]*>").matcher(detailsString);
			String refStr = "";
			if (ma1.find()) {
				refStr = ma1.group(2);
//				try {
//					refStr = URLEncoder.encode(refStr, "UTF-8");
//				} catch (UnsupportedEncodingException e) {
//					e.printStackTrace();
//				}
				
				String link = baseLink + "/RecorderOfDeeds/Deeds/Default.aspx" + "?LocatorNum="+ ma1.group(1) + "&RefStr=" + refStr + "&SearchType=" + ma1.group(3);
				salesTable = getLinkContents(link);
				
				org.htmlparser.Parser htmlParserSales = org.htmlparser.Parser.createParser(salesTable, null);
				NodeList salesTableList = htmlParserSales.parse(null);
				
				Node table = HtmlParser3.getNodeByID("ctl00_MainContent_panSearchResults", salesTableList, true);
				if (table != null) {
					String tableString = table.toHtml();
					tableString = tableString.replaceAll("(?is)onMouseOut=\\\"[^\\\"]+\\\"", "");
					tableString = tableString.replaceAll("(?is)onMouseOver=\\\"[^\\\"]+\\\"", "");
					tableString = tableString.replaceAll("(?is)<span class='RevSearch[^']+[^>]*>([^<]+)</span>", "$1");
					tableString = tableString.replaceAll("(?is)<p[^>]*>\\s*<a [^>]+>\\s*Show Tax Rates Information\\s*</a>\\s*[^/]+/p>", "");
//					tableString = tableString.replaceAll("(?is)<p[^>]*>([^<]+<a[^>]+>[^>]+>)*[^<]*</p>", "");
					tableString = tableString.replaceAll("(?is)<p[^>]*>([^<]+<a[^>]+>[^>]+>)*\\s*(</?br/?>\\s*)*\\s*[^<]*</p>", "");
					tableString = tableString.replaceFirst("(?is)(</?br/?>\\s*){2,}", "<br>");
					salesTable = "<br> <br> <h4>Locator Deed Search Information</h4>" + tableString;
				} else {
					salesTable = "";
				}
			}
			
			detailsString = detailsString.replaceAll("(?is)<a[A-z#\\s\\\"=]+onclick=\\\"ShowDeedIndexes[^>]+>View Deed Index Information Recorded With Locator Number [^<]+</a>", "See the table below");
			detailsString += salesTable;
			
			
			String taxAmountsDueTable = "";
			String link = getBaseLink();
			link = baseLink + "/Collection/TaxesDue.aspx" + "?LocatorNum=" + parcelNumber + "&Ref=" + refStr + "&TaxType=RE";
			
			taxAmountsDueTable = getLinkContents(link);
			
			org.htmlparser.Parser htmlParserSales = org.htmlparser.Parser.createParser(taxAmountsDueTable, null);
			NodeList salesTableList = htmlParserSales.parse(null);
			
			Node table = HtmlParser3.getNodeByTypeAndAttribute(salesTableList, "div", "style", "text-align:center; margin-top:10px;", true);
			if (table!=null) {
				taxAmountsDueTable = "<br><br><h3>Tax Amounts Due</h3>" + "<table width=\"526\"><tr><td>" + table.toHtml() + "</td></tr></table>";
				taxAmountsDueTable = taxAmountsDueTable.replaceAll("(?is)<p[^>]*>([^<]+<a[^>]+>[^>]+>)*\\s*(<\\s*/?\\s*br\\s*/?\\s*>)*\\s*[^<]*</p>", "");
				taxAmountsDueTable = taxAmountsDueTable.replaceAll("(?is)<input[^>]+>\\s*", "");
			} else {
				taxAmountsDueTable = "";
			}
			
			detailsString += taxAmountsDueTable;
			
			link = baseLink + "/Collection/RealEstateHistory.aspx" + "?LocatorNum=" + parcelNumber + "&Ref=" + refStr;
			String taxHistory = getLinkContents(link);
			
			if (!StringUtils.isEmpty(taxHistory)) {
				org.htmlparser.Parser htmlParserProperty = org.htmlparser.Parser.createParser(taxHistory, null);
				NodeList propertyTableList = htmlParserProperty.parse(null);
				table = HtmlParser3.getNodeByTypeAndAttribute(propertyTableList, "table", "id", "Table2", true);
				if (table!=null) {
					detailsString += "<br><br><h3>Tax History</h3>" + "<table width=\"526\"><tr><td>" + table.toHtml() + "</td></tr></table>";
				} 
			}
			
			return detailsString;

		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
	
	public static String getInputValue(String page, String param) {
		String result = "";
		Matcher ma = Pattern.compile("(?is)<input[^>]+name=\"" + param + "\"[^>]+value=\"([^\"]*)\"[^>]+>").matcher(page);
		if (ma.find()) {
			result = ma.group(1);
		}
		return result;
	}
	
	public static String getSelectValue(String page, String param) {
		String result = "";
		Matcher ma = Pattern.compile("(?is)<select[^>]+name=\"" + param + "\"[^>]*>.*?<option\\s+selected\\s+value=\"([^\"]*)\".*?</select>").matcher(page);
		if (ma.find()) {
			result = ma.group(1);
		}
		return result;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		TableTag resultsTable = null;
		
		//String hrefDet = "(?is)(<td[A-Z\\\"-=\\s]+)onClick=\\\"return ShowParcelData\\('([^']+)'\\s*,\\s*'([^']+)'\\s*,\\s*\\d+\\);\\\"([^>]*>)";
		String hrefDet = "(?is)(<td[A-Z\\\"-=\\s\\(\\);]+)onClick=\\\"return ShowParcelData\\('([^']+)'\\s*,\\s*'([^']+'(?:[\\da-z%]+')?)\\s*,\\s*\\d+\\);\\\"([^>]*>)";
		String hrefPrevNext = "(?is)<a (?:onclick=\\\"[^]\"]+\\\" )?(id=\\\"([^\\\"]+)\\\") class=\\\"RevNavPageNum\\\" href=[^>]+>\\s*(\\d+|(?:<b>)?\\s*(?:Next|Previous|First|Last)\\s*(?:</b>)?)\\s*</a>";
		
//		String query = response.getQuerry();
//		String searchby = StringUtils.extractParameter(query, "SearchType=([^&?]*)");
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList formList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "SearchResults"), true); 
			
			Map<String, String> params = new HashMap<String, String>();			 			
			params = HttpSite3.fillConnectionParams(table, ro.cst.tsearch.connection.http3.MOStLouisTR.getTargetArgumentParameters(), "SearchResults");
			
			NodeList mainTableList = formList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "tableData"), true);
			
			NodeList previousNextList = formList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "panelPages"), true);

			if (mainTableList.size()>0) {
				resultsTable = (TableTag)mainTableList.elementAt(0);
			}
			
			if (resultsTable != null && resultsTable.getRowCount() > 0) {
				TableRow[] rows  = resultsTable.getRows();
				
				String headerRow = "";
				boolean hasSubdivName = false;
				int len = rows.length;
				if (len > 1 && rows[1].getColumnCount() == 5) {
					String colSubdivAddress = rows[0].toHtml().replaceFirst("(?is).*<input type=\"submit\" name=\"butAddress\" value=\\\"([A-Z\\s]+)[^\\\"]*\\\".*", "$1").trim();
					if ("Subdivision Name".equals(colSubdivAddress)) {
						hasSubdivName = true;
					}
					headerRow = "<tr><td><b> # </b></td>   <td><b>Locator Number</b></td>   <td><b>" + colSubdivAddress + "</b></td>   <td><b>Owner Name</b></td></tr>";
					for (int i = 1; i < len; i++) {
						TableRow row = rows[i];
						if (row.getChildCount() == row.getColumnCount() + 2) {
							String link = "";
							int seq = getSeq();
							
							row.removeChild(2); //remove 'Map' column
							String htmlRow = row.toHtml();
							htmlRow = htmlRow.replaceAll("(?is)onMouseOver=\\\"HighlightCells\\(event\\);\\\"", "");
							htmlRow = htmlRow.replaceAll("(?is)onMouseOut=\\\"UnHighlightCells\\(event\\);\\\"", "");
							Matcher ma2 = Pattern.compile(hrefDet).matcher(htmlRow);
							if (ma2.find()) {
								String ref = ma2.group(3);
								ref = ref.substring(0, ref.length()-1);
								ref = ref.replaceAll("\\\\?'", "'");
								
								if (isParentSite()) {
									try {
										ref = URLEncoder.encode(ref, "UTF-8");
									} catch (UnsupportedEncodingException e) {
										e.printStackTrace();
									}
								}
								
								mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
								
								link = CreatePartialLink(TSConnectionURL.idPOST) + "/IAS/AsmtInfo.aspx" +  
									"?Locator="+ ma2.group(2) + "&hidLocatorNum=" + ma2.group(2) +  "&hidELN=" +  ref + "&hidINL=true" + 
									"&seq=" + seq;
								
								
								htmlRow = htmlRow.replaceAll("(?is)(<td[A-Z\\\"-=\\s]+)onClick=\\\"return ShowParcelData[^\\\"]+\\\"([^>]*>[^<]+</td>)", "$1$2");
								htmlRow = htmlRow.replaceAll("(?is)(<td[A-Z\\\"-=\\s]+)onClick=\\\"return ShowParcelData[^\\\"]+\\\"([^>]*>)(?:&nbsp;)*\\s*<img[^>]+>(?:&nbsp;)*\\s*([^<]+)(</td>)", 
										"$1" + "$2" + "<a href=\"" +  link + "\">" + "$3" + "</a> " + "$4");
								
								ParsedResponse currentResponse = new ParsedResponse();
								
								currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
								currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, htmlRow);
								currentResponse.setOnlyResponse(htmlRow);
								
								ResultMap m = ro.cst.tsearch.servers.functions.MOStLouisTR.parseIntermediaryRow(row, searchId, hasSubdivName); 
								Bridge bridge = new Bridge(currentResponse, m, searchId);
								
								DocumentI document = (TaxDocumentI)bridge.importData();				
								currentResponse.setDocument(document);
								
								intermediaryResponse.add(currentResponse);
							}
						}
					}
				}
				
				
				String previousNextString = "";
				if (previousNextList.size() > 0) {
					previousNextList = previousNextList.elementAt(0).getChildren();
					for (int i=0; i < previousNextList.size(); i++) {
						String line = previousNextList.elementAt(i).toPlainTextString().trim();
						if (line.startsWith("<span")) {
							previousNextString += "&nbsp; &nbsp; ";
						} else if (line.startsWith("<a")) {
							Matcher matPg = Pattern.compile(hrefPrevNext).matcher(line);
							if (matPg.find()) {
								String link = CreatePartialLink(TSConnectionURL.idPOST) + "/IAS/SearchResults.aspx" +
										"?Page=" + matPg.group(2) + "&seq=" + seq;
								line = line.replaceAll(hrefPrevNext, "<a " + "$1" + " href=\"" +  link + "\">" + "$3" + "</a>");
								previousNextString += line;
							}
							 
						}
					}
				}
				
				String header = "<table border=\"1\"><tr><td><table>" + headerRow;
				String footer = "</table>" + previousNextString + "</td></tr></table>";
				
				response.getParsedResponse().setHeader(header);
				response.getParsedResponse().setFooter(footer);
												
				outputTable.append(table);
			}

		} catch (Throwable t) {
			logger.error("Error while parsing intermediary data", t);
		}

		return intermediaryResponse;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"TR");
				
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			List<List> tablebodyRef = new ArrayList<List>();
			List<String> list;
							
			NodeList list1 = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			
			String name = "";
			if (list1.size()>0) {
				NodeList list11 = list1;
				
				if (list11.size()>0) {
					TableTag table = (TableTag)list11.extractAllNodesThatMatch(new HasAttributeFilter("class", "CatDataTable")).elementAt(0);
					
					if (table.getRowCount() > 14) {
						TableRow row = table.getRow(0);
						if (row != null) {
							if (row.getColumnCount() > 0) {
								TableColumn col = row.getColumns()[0];
								Span info = (Span) col.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_OwnLeg_labLocatorNum"), true).elementAt(0);
								if (info != null) {
									String pid = info.toPlainTextString().trim();
									if (StringUtils.isNotEmpty(pid)) {
										resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pid);
									}
								}
								
								info = (Span) col.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_OwnLeg_labTaxYear"), true).elementAt(0);
								if (info != null) {
									String taxYear = info.getChildrenHTML().trim();
									if (StringUtils.isNotEmpty(taxYear)) {
										resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
									}
								}
							}
							
							row = table.getRow(1); //owner's name
							if (row != null) {
								if (row.getColumnCount() == 2 && row.getColumns()[0].toPlainTextString().contains("Owner") && row.getColumns()[0].toPlainTextString().contains("Name")) {
									name = row.getColumns()[1].toPlainTextString().trim();
								}
							}
							
							row = table.getRow(2); //taxing address
							if (row != null) {
								if (row.getColumnCount() == 2 && (row.getColumns()[0].toPlainTextString().contains("Taxing Address") || row.getColumns()[0].toPlainTextString().contains("Address"))) {
									String address = row.getColumns()[1].getChildrenHTML().trim();
									address = address.replaceAll("(?is)</?span[^>]*>", "");
									address = address.replaceAll("(?is)<\\s*/?\\s*br\\s*/?\\s*>", "<br>");
									address = address.replaceAll("(?is)&nbsp;?", " ").trim();
									if (StringUtils.isNotEmpty(address)) {
										resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
									}
								}
							}
							
							row = table.getRow(3); //co-owner
							if (row != null) {
								if (row.getColumnCount() == 2 && ("Care-Of Name:".equalsIgnoreCase(row.getColumns()[0].toPlainTextString().trim()))) {
									String coOwn = row.getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;?", " ").trim();
									if (StringUtils.isNotEmpty(coOwn))  {
										name += "<br>" +  coOwn;
									}
								}
							}
							if (StringUtils.isNotEmpty(name)) {
								resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name);
							}
							
							row = table.getRow(8); //subdiv name
							if (row != null) {
								if (row.getColumnCount() == 2 && ("Subdivision Name:".equalsIgnoreCase(row.getColumns()[0].toPlainTextString().trim()))) {
									String subdivName = row.getColumns()[1].toPlainTextString().trim();
									if (StringUtils.isNotEmpty(subdivName)) {
										ro.cst.tsearch.servers.functions.MOStLouisTR.putSubdivision(resultMap, subdivName);
									}
								}
							}
							
							row = table.getRow(9); //legal desc
							if (row != null) {
								if (row.getColumnCount() == 2 && ("Legal Description:".equalsIgnoreCase(row.getColumns()[0].toPlainTextString().trim()))) {
									String legal = row.getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;?", " ").trim();
									if (StringUtils.isNotEmpty(legal)) {
										resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legal);
									}
								}
							}
							
							row = table.getRow(10); //Lot && Block
							if (row != null) {
								if (row.getColumnCount() == 4) {
									if ("Lot Number:".equalsIgnoreCase(row.getColumns()[0].toPlainTextString().trim()) || "Lot No:".equalsIgnoreCase(row.getColumns()[0].toPlainTextString().trim())) {
										ro.cst.tsearch.servers.functions.MOStLouisTR.putValue(resultMap,
												PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), row.getColumns()[1].toPlainTextString().trim());
									}
									if ("Block Number:".equalsIgnoreCase(row.getColumns()[2].toPlainTextString().trim()) || "Block No:".equalsIgnoreCase(row.getColumns()[2].toPlainTextString().trim())) {
										ro.cst.tsearch.servers.functions.MOStLouisTR.putValue(resultMap,
												PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), row.getColumns()[3].toPlainTextString().trim());
									}
								}
							}
							
							list = new ArrayList<String>();
							
							row = table.getRow(13); //Deed ref docNo & deed type
							if (row != null) {
								if (row.getColumnCount() == 4) {
									String colName1 = row.getColumns()[0].toPlainTextString().trim();
									String colName2 = row.getColumns()[2].toPlainTextString().trim();
									if (("Deed Document Number:".equalsIgnoreCase(colName1) || "Deed Document No:".equalsIgnoreCase(colName1)) && ("Deed Type:".equalsIgnoreCase(colName2))) {
										String documentNumber = row.getColumns()[1].toPlainTextString().trim();
										String recordedDate = "";
										if (org.apache.commons.lang.StringUtils.isNotBlank(documentNumber)) {
											if (documentNumber.length() > 8) {
												recordedDate = documentNumber.substring(0, 8);
												recordedDate = recordedDate.replaceAll("(?is)(\\d{4})(\\d{2})(\\d{2})", "$2/$3/$1");
												documentNumber = org.apache.commons.lang.StringUtils.stripStart(documentNumber.substring(8), "0");
											}
											list.add(recordedDate);	//recorded date
											list.add(documentNumber);	//instrument number
											list.add("");	//book
											list.add("");	//page
											list.add(row.getColumns()[3].toPlainTextString().trim());	//document type											
										}
									}
								}
							}
							
							row = table.getRow(14); //Deed ref bk-pg
							if (row != null) {
								if (row.getColumnCount() == 4) {
									if ("Deed Book and Page:".equalsIgnoreCase(row.getColumns()[0].toPlainTextString().trim())) {
										String bkPgRef = row.getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;?", " ").trim();
										Matcher ma = Pattern.compile("(?is)Book:\\s*(\\d+)\\s+Page:\\s*(\\d+)").matcher(bkPgRef);
										if (ma.find()) {
											if (list.size() == 0) {
												list.add(""); //recorded date
												list.add(""); //instrument number
												list.add(org.apache.commons.lang.StringUtils.stripStart(ma.group(1), "0"));	//book
												list.add(org.apache.commons.lang.StringUtils.stripStart(ma.group(2), "0"));	//page
												list.add("");
												
											} else if (list.size() == 5) {
												list.set(2, org.apache.commons.lang.StringUtils.stripStart(ma.group(1), "0"));	//book
												list.set(3, org.apache.commons.lang.StringUtils.stripStart(ma.group(2), "0"));	//page
											}
										}
									}
								}
							}
							
							if (list.size() > 0) {
								tablebodyRef.add(list);
							}
						}
					}
					
					table = (TableTag) list11.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_tabNavData"), true).elementAt(0);
					if (table != null) {
						for (int i=0; i < table.getRowCount(); i++) {
							TableRow row = table.getRow(i);
							
							if (row.getColumnCount() == 9) {
								String tmp =  "";
								String recordedDate = "";
								String instrumentNumber = "";
								
								tmp = row.getColumns()[3].getChildrenHTML().replaceAll("(?is)&nbsp;?", " ").trim();
								String[] split = tmp.split("(?is)<br(\\s*/)?>");
								if (split.length == 2) {
									recordedDate = split[0].trim();
									if ("00/00/0000".equals(recordedDate)) {
										recordedDate = "";
									}
									instrumentNumber = split[1].trim();
									if (instrumentNumber.matches("0+")) {
										instrumentNumber = "";
									}
								} else if (split.length == 1 && split[0].trim().matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
									recordedDate = split[0].trim();
									if ("00/00/0000".equals(recordedDate)) {
										recordedDate = "";
									}
								} else if (split[0].trim().matches("\\d+")) {
									instrumentNumber = split[0].trim();
								}
								
								tmp = row.getColumns()[5].getChildrenHTML().replaceAll("(?is)&nbsp;?", " ").trim();
								String book = "";
								String page = "";
								split = tmp.split("(?is)<br(\\s*/)?>");
								if (split.length == 2) {
									book = split[0].trim();
									if (book.matches("0+")) {
										book = "";
									}
									page = split[1].trim();
									if (page.matches("0+")) {
										page = "";
									}
								}
								
								String documentType = row.getColumns()[7].toPlainTextString().replaceAll("(?is)&nbsp;?", " ").trim();
								
								boolean found = false;
								
								for (int j=0;j<tablebodyRef.size();j++) {
									List l = tablebodyRef.get(j);
									
									if (l.size()==5 && book.equals(l.get(2)) && page.equals(l.get(3))) {
										found = true;
										if (!StringUtils.isEmpty(recordedDate) && StringUtils.isEmpty((String)l.get(0))) {
											l.set(0, recordedDate);
										}
										if (!StringUtils.isEmpty(instrumentNumber) && StringUtils.isEmpty((String)l.get(1))) {
											l.set(1, instrumentNumber);
										}
										if (!StringUtils.isEmpty(documentType) && StringUtils.isEmpty((String)l.get(4))) {
											l.set(4, documentType);
										}
									}
								}
								if (!found) {
									list = new ArrayList<String>();
									list.add(recordedDate);
									list.add(instrumentNumber);
									list.add(book);
									list.add(page);
									list.add(documentType);
									tablebodyRef.add(list);
								}
							}
						}
					}
				}
			}
			
			
			if (tablebodyRef.size() > 0) {
				String[] headerRef = {SaleDataSetKey.RECORDED_DATE.getShortKeyName(),	SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(), 
						SaleDataSetKey.BOOK.getShortKeyName(), SaleDataSetKey.PAGE.getShortKeyName(), SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(), };
				ResultTable crossRef = GenericFunctions2.createResultTable(tablebodyRef, headerRef);
				if (crossRef != null){
					resultMap.put("SaleDataSet", crossRef);
				}
			}
			
			ro.cst.tsearch.servers.functions.MOStLouisTR.parseNames(resultMap, searchId);
			ro.cst.tsearch.servers.functions.MOStLouisTR.parseAddress(resultMap);
			ro.cst.tsearch.servers.functions.MOStLouisTR.parseLegalSummary(resultMap);
			ro.cst.tsearch.servers.functions.MOStLouisTR.parseTaxes(nodeList, resultMap, searchId);
		
		} catch (ParserException e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;

		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);
		DuplicateInstrumentFilterResponse duplicateInstrFilter = new DuplicateInstrumentFilterResponse(searchId) { 
			private static final long serialVersionUID = 3801993155906031119L;

			@Override
			public String getFilterCriteria() {
				return "Instrument duplicates";
			}
		};
		
		if (hasPin()) {
			FilterResponse nameFilter = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			//module.setSaKey(0, SearchAttributes.LD_PARCELNO);
			module.setSaKey(0, SearchAttributes.LD_PARCELNO_GENERIC_TR);
			module.addFilter(addressFilter);
			module.addFilter(nameFilter);
			module.addFilter(duplicateInstrFilter);
			moduleList.add(module);
		}

		if (hasStreet()) {
			FilterResponse nameFilter = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(0, SearchAttributes.P_STREETNO);
			module.setSaKey(2, SearchAttributes.P_STREETNAME);
			module.addFilter(addressFilter);
			module.addFilter(nameFilter);
			module.addFilter(duplicateInstrFilter);
			moduleList.add(module);
		}

		if (hasOwner()) {
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addFilter(duplicateInstrFilter);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] { "L;F;", "L;M;" }));
			moduleList.add(module);
		}

		serverInfo.setModulesForAutoSearch(moduleList);
	}
	
}
