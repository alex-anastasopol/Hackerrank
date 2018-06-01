package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.CityFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;

public class MOCassAO extends TSServer {
		
	private static final long serialVersionUID = -4281532459371967590L;
	
	private static int seq = 0;
	
	protected synchronized static int getSeq() {
		return seq++;
	}
	
	public MOCassAO(long searchId) {
		super(searchId);
	}
	
	public MOCassAO(String rsRequestSolverName, String rsSitePath, String rsServerID, 
			String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, 
				rsPrmNameLink, searchId, miServerID);
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) 
			throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		if (rsResponse.indexOf("No results match your search criteria.") > -1) {
			Response.getParsedResponse().setError("No data found.");
			return;
		}
		
		if (viParseID==ID_SEARCH_BY_ADDRESS && rsResponse.indexOf("Summary")>-1) {
			viParseID = ID_DETAILS;			//if searching by address and only one result is found, the details are displayed
		}
		
		switch (viParseID) {
								
		case ID_SEARCH_BY_ADDRESS:				
			
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
			
			if(smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
            }
			
			break;	
				
		case ID_SEARCH_BY_PARCEL:
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:
			
			StringBuilder serialNumber = new StringBuilder();
			String details = getDetails(rsResponse, serialNumber);
			String filename = serialNumber + ".html";
			
			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type","ASSESSOR");
				data.put("dataSource","AO");
				if (isInstrumentSaved(serialNumber.toString(),null,data)){
					details += CreateFileAlreadyInTSD();
				}
				else {
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
		
	protected String getDetails(String rsResponse, StringBuilder parcelNumber) {
		try {
			
			StringBuilder details = new StringBuilder();
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
									
			/* If from memory - use it as is */
			if(!rsResponse.contains("<html")){
				NodeList parcelIDNodes = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)	
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctlBodyPane_ctl02_lblTaxID"));
				if (parcelIDNodes.size()>0) {
					parcelNumber.append(parcelIDNodes.elementAt(0).toPlainTextString().trim());
				}
				return rsResponse;
			}
			
			NodeList parcelIDNodes = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctlBodyPane_ctl02_lblTaxID"));
			if (parcelIDNodes.size()>0) {
				parcelNumber.append(parcelIDNodes.elementAt(0).toPlainTextString().trim());
			}
			
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "Table5"));
			
			if (tables.size()>0) {
				details.append(tables.elementAt(0).toHtml());
			}
			
			String stringDetails = details.toString();
			
			stringDetails = stringDetails.replaceAll("(?is)<input[^>]+>", "");
			stringDetails = stringDetails.replaceAll("(?is)<img[^>]+>", "");
			//Photos and Sketches tables
			stringDetails = stringDetails.replaceAll("(?is)<table[^>]+id=\"Table1\"[^>]+cellpadding=\"0\"[^>]*>[^<]*</table>", "");
			//Report table
			stringDetails = stringDetails.replaceAll("(?is)<table[^>]+id=\"Table1\"[^>]+cellspacing=\"4\"[^>]*>.*?</table>", "");
			//No data table
			stringDetails = stringDetails.replaceAll("(?is)<div>\\s*<table[^>]+id=\"tblNoData\"[^>]*>.*?</table>\\s*</div>", "");
			//link 'Search across multiple counties with Guidepost!'
			stringDetails = stringDetails.replaceAll("(?is)<span[^>]+id=\"" + 
					"ctlBodyPane_ctl45_ctlAnnouncements_lstAnnouncements_ctl02_lblSubject\"[^>]*>.*?</span>", "");
			//Photos label
			stringDetails = stringDetails.replaceAll("(?is)<div>\\s*<div[^>]+id=\"ctlBodyPane_ctl34_pnlDisplay\"[^>]*>.*?</div>\\s*</div>", "");
			//Sketches label
			stringDetails = stringDetails.replaceAll("(?is)<div>\\s*<div\\s*id=\"ctlBodyPane_ctl37_pnlDisplay\"[^>]*>.*?</div>\\s*</div>", "");
			//Report label
			stringDetails = stringDetails.replaceAll("(?is)<div>\\s*<div\\s*id=\"ctlBodyPane_ctl40_pnlDisplay\"[^>]*>.*?</div>\\s*</div>", "");
			//clean empty lines from the end
			stringDetails = stringDetails.replaceAll("(?is)<br>\\s*<p>\\s*<table[^>]+id=\"Table1\"[^>]+cellspacing=\"0\"[^>]*>.*?</table>\\s*</p>", "");
			stringDetails = stringDetails.replaceAll("(?is)<br>\\s*<br>\\s*(</div>\\s*</div>)", "$1");
			//remove link from owner (if any)
			stringDetails = stringDetails.replaceAll("(?is)<a[^>]+id=(\"ctlBodyPane_ctl05_lstDeed_ctl01_lnkDeedName\")[^>]+>([^<]*)</a>", 
					"<span id=$1>$2</span>");
			
			return stringDetails;
			
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
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTable = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id","ctlBodyPane_ctl03_gvwParcelResults"), true);
			
			if (mainTable.size()>0) {
				resultsTable = (TableTag)mainTable.elementAt(0);
			}
			
			//if there are results
			if (resultsTable != null) 			
			{
				TableRow[] rows  = resultsTable.getRows();
				Map<String, String> params = ro.cst.tsearch.connection.http2.MOCassAO.isolateParams(response.getResult(), "Form1");
				params.put("SelectionChangeMode", "NewSelection");
				params.put("ToolBar1$ReportTools1$ddlLayerList", "1529");
				
				for (int i = 1; i < rows.length; i++)
				{
					TableRow row = rows[i];
										
					String link = response.getLastURI().toString();
					String htmlRow = row.toHtml();
					htmlRow = htmlRow.replaceAll("(?is)<input[^>]*>", "");
					htmlRow = htmlRow.replaceFirst("(?is)<td[^>]*>[^<]*</td>", "");
					
					ParsedResponse currentResponse = new ParsedResponse();
					
					String column = row.getColumns()[1].toHtml().replaceAll("&#39;", "'");
					String eventTarget = "";
					String eventArgument = "";
					Matcher matcher = Pattern.compile("(?is)href=\"javascript:__doPostBack\\('([^']*)','([^']*)'\\)\"").matcher(column);
					if (matcher.find()) {
						Map<String, String> new_params = new HashMap<String,String>();
						for (Map.Entry<String, String> entry : params.entrySet()) {
						    new_params.put(entry.getKey(), entry.getValue());
						}
						eventTarget = matcher.group(1);
						eventArgument = matcher.group(2);
						new_params.put("__EVENTTARGET", eventTarget);
						new_params.put("__EVENTARGUMENT", eventArgument);
						int seq = getSeq();
						mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, new_params);
						link = CreatePartialLink(TSConnectionURL.idPOST) + link + "&seq=" + seq;
						htmlRow = htmlRow.replaceFirst("(?is)<a[^>]+>",	"<a href=\"" + link + "\">");
					}
					
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
									
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,htmlRow);
					currentResponse.setOnlyResponse(htmlRow);
					
					ResultMap m = ro.cst.tsearch.servers.functions.MOCassAO.parseIntermediaryRow( row, searchId ); 
					Bridge bridge = new Bridge(currentResponse, m, searchId);
					
					DocumentI document = (AssessorDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
					 
				}
				
				String header = "<table border=\"1\" align=\"center\">" +
					"<tr><td align=\"center\"><strong>Parcel ID</strong></td>" +
					"<td align=\"center\"><strong>Owner</strong></td>" +
					"<td align=\"center\"><strong>Property Address</strong></td>" +
					"<td align=\"center\"><strong>City</strong></td>" +
					"</tr>";
				response.getParsedResponse().setHeader(header);
				response.getParsedResponse().setFooter("</table>");
												
				outputTable.append(table);
			}
			
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap) {
				
		try {
					
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"AO");
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList parcelIDNodes = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctlBodyPane_ctl02_lblParcelID"));
			if (parcelIDNodes.size()>0) {
				String parcelID = parcelIDNodes.elementAt(0).toPlainTextString().trim();
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), parcelID);
			}
				
			NodeList addressNodes = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctlBodyPane_ctl02_lblPropertyAddress"));
			if (addressNodes.size()>0) {
				String address = addressNodes.elementAt(0).toPlainTextString().trim();
				resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
			}	
				
			NodeList strNodes = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctlBodyPane_ctl02_lblSecTwpRng"));
			if (strNodes.size()>0) {
				String str = strNodes.elementAt(0).toPlainTextString().trim();
				Matcher matcher1 = Pattern.compile("(\\d+)/(\\d+)/(\\d+)").matcher(str);
				if (matcher1.find()) {
					String sec = matcher1.group(1).replaceFirst("^0+", "");
					String twp = matcher1.group(2).replaceFirst("^0+", "");
					String rng = matcher1.group(3).replaceFirst("^0+", "");
					if (!StringUtils.isEmpty(sec)) {
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), sec);
					}
					if (!StringUtils.isEmpty(twp)) {
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), twp);
					}
					if (!StringUtils.isEmpty(rng)) {
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), rng);
					}
				}
			}
						
			NodeList legalNodes = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)	
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctlBodyPane_ctl02_lblLegalDescription"));
			if (legalNodes.size()>0) {
				String legal = legalNodes.elementAt(0).toPlainTextString().trim();
				resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legal);
			}
			
			NodeList taxIDNodes = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)	
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctlBodyPane_ctl02_lblTaxID"));
			if (taxIDNodes.size()>0) {
				String taxID = taxIDNodes.elementAt(0).toPlainTextString().trim();
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), taxID);
			}
			
			String owner = "";
			NodeList ower1Nodes = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)	
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctlBodyPane_ctl05_lstDeed_ctl01_lnkDeedName"));
			if (ower1Nodes.size()>0) {
				String owner1 = ower1Nodes.elementAt(0).toPlainTextString().trim();
				if (!StringUtils.isEmpty(owner1)) {
					owner += owner1 + "<br>";
				}
			}
			NodeList ower2Nodes = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)	
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctlBodyPane_ctl05_lstDeed_ctl01_lblDeedName"));
			if (ower2Nodes.size()>0) {
				String owner2 = ower2Nodes.elementAt(0).toPlainTextString().trim();
				if (!StringUtils.isEmpty(owner2)) {
					owner += owner2;
				}
			}
			owner = owner.replaceFirst("<br>$", "");
			if (!StringUtils.isEmpty(owner)) {
				resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owner);
			}
			
			List<List> tablebody = null;
			NodeList salesNodes = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctlBodyPane_ctl26_gvwSales"));
			if (salesNodes.size()>0 ) {
				TableTag salesTable = (TableTag) salesNodes.elementAt(0);
				ResultTable salesHistory = new ResultTable();
				tablebody = new ArrayList<List>();
				List<String> list;
				
				for (int i=1;i<salesTable.getRowCount(); i++)
				{
					TableRow row = salesTable.getRow(i);
					if (row.getColumnCount()>=8) {
						String bookAndPage = row.getColumns()[3].toPlainTextString().trim();	//book/page
						String book = "", page = "";
						Matcher matcher3 = Pattern.compile("(\\d+)/(\\d+)").matcher(bookAndPage);
						if (matcher3.find()) {
							book = matcher3.group(1);
							page = matcher3.group(2);
						}
						list = new ArrayList<String>();
						list.add(row.getColumns()[0].toPlainTextString().trim());	//date
						list.add(row.getColumns()[1].toPlainTextString().replaceAll("(?i)&amp;", "&").trim());	//grantor
						list.add(row.getColumns()[2].toPlainTextString().replaceAll("(?i)&amp;", "&").trim());	//grantee
						list.add(book);	//book
						list.add(page);	//page
						list.add(row.getColumns()[5].toPlainTextString().trim());	//type
						list.add(row.getColumns()[7].toPlainTextString().replaceAll("[,\\$]", "").trim());	//amount
						tablebody.add(list);
					}
				}
				
				String[] header = {SaleDataSetKey.RECORDED_DATE.getShortKeyName(), SaleDataSetKey.GRANTOR.getShortKeyName(), 
						SaleDataSetKey.GRANTEE.getShortKeyName(), SaleDataSetKey.BOOK.getShortKeyName(), 
						SaleDataSetKey.PAGE.getShortKeyName(), SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(),
						SaleDataSetKey.SALES_PRICE.getShortKeyName()};
				salesHistory = GenericFunctions2.createResultTable(tablebody, header);
				if (salesHistory != null && tablebody.size()>0){
					resultMap.put("SaleDataSet", salesHistory);
				}
			}
			
			Matcher matcher2 = Pattern.compile("(?is)<td[^>]*>\\s*<strong>Book & Page:</strong>\\s*(\\d+)/(\\d+)\\s*</td>")
					.matcher(detailsHtml);
			if (matcher2.find()) {
				String newBook = matcher2.group(1);
				String newPage = matcher2.group(2);
				boolean found = false;
				if (tablebody!=null) {
					for (int i=0;i<tablebody.size();i++) {
						if (tablebody.get(i).size()>4) {
							String oldBook = (String)tablebody.get(i).get(3);
							String oldPage = (String)tablebody.get(i).get(4);
							if (newBook.equals(oldBook) && newPage.equals(oldPage)) {
								found = true;
								break;
							}
						}
					}
				}
				if (!found) {	//if not already in sales table
					resultMap.put(SaleDataSetKey.BOOK.getKeyName(), newBook);
					resultMap.put(SaleDataSetKey.PAGE.getKeyName(), newPage);
				}
			}
			
			ro.cst.tsearch.servers.functions.MOCassAO.parseNames(resultMap, searchId);
			ro.cst.tsearch.servers.functions.MOCassAO.parseAddress(resultMap);
			ro.cst.tsearch.servers.functions.MOCassAO.parseLegalSummary(resultMap);
		
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
		
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;

		if (hasPin()) {
			String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
			if (pin.matches("\\d{18}")) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.forceValue(0, pin);
				moduleList.add(module);
			}
		}
		
		String pin2 = getSearchAttribute(SearchAttributes.LD_PARCELNO2);
		if (!StringUtils.isEmpty(pin2)) {
			if (pin2.matches("\\d{18}")) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.forceValue(0, pin2);
				moduleList.add(module);
			}
		}
		
		if (hasStreet()) {
			FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);
			FilterResponse cityFilter = CityFilterFactory.getCityFilterDefault(searchId);
			FilterResponse nameFilter = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(0, SearchAttributes.P_STREET_NO_NAME);
			module.addFilter(addressFilter);
			module.addFilter(cityFilter);
			module.addFilter(nameFilter);
			moduleList.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(moduleList);
	}
}
