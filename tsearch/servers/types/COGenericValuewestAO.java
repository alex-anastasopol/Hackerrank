/**
 * 
 */
package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.net.URLEncoder;
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
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.CityFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;

/**
 * @author vladb
 */
public class COGenericValuewestAO extends TSServer {

	private static final long serialVersionUID = 1L;
	private static final String FORM_NAME = "frmMain";
	private static int seq = 0;
	
	public COGenericValuewestAO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
	}
	
	protected synchronized static int getSeq(){
		return seq++;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch (viParseID) 
		{
			case ID_SEARCH_BY_NAME:
			case ID_SEARCH_BY_ADDRESS:
			case ID_SEARCH_BY_PARCEL:
			case ID_SEARCH_BY_PROP_NO:
				
				// no result
				if (rsResponse.indexOf("Could not locate any accounts using the search criteria provided") > -1) {
					parsedResponse.setError("Could not locate any accounts using the search criteria provided!");
					return;
				}
				
				// parse and store parameters on search
				Form form = new SimpleHtmlParser(rsResponse).getForm(FORM_NAME);
				Map<String, String> params = form.getParams();
				int seq = getSeq();
				mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
				
				StringBuilder outputTable = new StringBuilder();
				outputTable.append(String.valueOf(seq)); // use outputTable to transmit seq
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				
				if(smartParsedResponses.size() == 0) {
					return;
				}
				
				parsedResponse.setHeader("<table width=\"100%\" border=\"1\">\n" +
						"<tr><th>Account</th><th>Parcel</th><th>Owner Name</th><th>Property Address</th></tr>");
				parsedResponse.setFooter("</table>");
				
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				
				break;
			case ID_GET_LINK:
				
				ParseResponse(sAction, Response, ID_DETAILS);
				break;
				
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:
				
				StringBuilder accountId = new StringBuilder();
				String details = getDetails(rsResponse, accountId);
				
				if(viParseID == ID_DETAILS) {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
	
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","ASSESSOR");
					if (isInstrumentSaved(accountId.toString().trim(), null, data)){
						details += CreateFileAlreadyInTSD();
					}
					else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
					}
	
					parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
					parsedResponse.setResponse(details);
				} else {
					String filename = accountId + ".html";
					smartParseDetails(Response,details);
					
					msSaveToTSDFileName = filename;
					parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
					parsedResponse.setResponse(details);
					
					msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				}
				
				break;
		}
	}
	
	protected String getDetails(String page, StringBuilder accountId) {
		
		StringBuilder details = new StringBuilder();
		
		try {
			String properPage = Tidy.tidyParse(page, null);
			org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(properPage, null);
			NodeList nodeList = parser.parse(null);
			
			// get info tables
			TableTag accountTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "tblAccountDetail"), true)
				.elementAt(0);
			accountId.append(accountTable.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("id", "lblAccountNo_Value"), true)
				.elementAt(0).toPlainTextString().trim());
			if(page.indexOf("<html") < 0) { // if from memory
				return page;
			}
			details.append("<h3 align=\"center\">Account Information</h3>");
			accountTable.setAttribute("width", "95%");
			accountTable.setAttribute("align", "center");
			accountTable.setAttribute("border", "1");
			details.append(accountTable.toHtml());
			
			TableTag salesTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "grdSales"), true)
				.elementAt(0);
			details.append("<h3 align=\"center\">Sales Information</h3>");
			salesTable.setAttribute("width", "95%");
			salesTable.setAttribute("align", "center");
			salesTable.setAttribute("border", "1");
			details.append(salesTable.toHtml());
			
			TableTag otherSalesTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "grdSalesComp"), true)
				.elementAt(0);
			details.append("<h3 align=\"center\">Other Property Sales</h3>");
			otherSalesTable.setAttribute("width", "95%");
			otherSalesTable.setAttribute("align", "center");
			otherSalesTable.setAttribute("border", "1");
			details.append(otherSalesTable.toHtml().replaceAll("(?is)<a[^>]*>(.*?)</a>", "$1")); // remove links
			
			TableTag taxableTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "grdValues"), true)
				.elementAt(0);
			details.append("<h3 align=\"center\">Taxable Values History</h3>");
			taxableTable.setAttribute("width", "95%");
			taxableTable.setAttribute("align", "center");
			taxableTable.setAttribute("border", "1");
			details.append(taxableTable.toHtml());
			
			// get property details
			TableTag infoTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "divModelsContent"), true)
					.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
			if (infoTable != null) {
				details.append("<h3 align=\"center\">Property Details</h3>");
				
				Form form = new SimpleHtmlParser(page).getForm(FORM_NAME);
				Map<String, String> params = form.getParams();
				int seq = getSeq();
				mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
				
				DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(
						HashCountyToIndex.ANY_COMMUNITY, miServerID);
				String siteLink = dataSite.getLink() + form.action;
				
				TableRow[] rows = infoTable.getRows();
				
				for(int i = 0; i < rows.length; i++) {
					
					TableRow row = rows[i];
					NodeList links = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
					String label = row.toHtml().replaceAll("(?is)<a .*?</a>", "").replaceAll("<[^>]*>", "").trim();
					String[] thRowInfo = label.split("-");
					details.append("<h4 align=\"center\">" + thRowInfo[0].trim() + "</h4>");
					
					for(int j = 0; j < links.size(); j++) {
						Matcher matcher = Pattern.compile("__doPostBack\\('([^']*)','([^']*)'\\)").matcher(links.elementAt(j).toHtml());
						String target = "";
						String argument = "";
						if (matcher.find()) {
							target = matcher.group(1);
							argument = matcher.group(2);
						}
						
						HTTPRequest reqP = new HTTPRequest(siteLink);
				    	reqP.setMethod(HTTPRequest.POST);
				    	reqP.setPostParameter("__EVENTTARGET", target);
				    	reqP.setPostParameter("__EVENTARGUMENT", argument);
				    	reqP.setPostParameter("seq", String.valueOf(seq));
				    	
				    	HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
				    	String propInfoPage = null;
			        	try {
			        		HTTPResponse resP = site.process(reqP);
			        		propInfoPage = resP.getResponseAsString();
			        	} finally {
							HttpManager.releaseSite(site);
						}	
			        	
			        	org.htmlparser.Parser parser1 = org.htmlparser.Parser.createParser(propInfoPage, null);
						NodeList nodeList1 = parser1.parse(null);
						
						TableTag table1 = (TableTag) nodeList1.extractAllNodesThatMatch(new HasAttributeFilter("id", "grdPropertyDetailsRight"), true)
							.elementAt(0);
						if (table1 != null) {
							table1.setAttribute("style", "");
							table1.setAttribute("align", "center");
							table1.setAttribute("border", "1");								
							details.append(table1.toHtml().replaceFirst("(?is)(<span id=\"grdPropertyDetailsRight_lblAttributeHeader\">)Attributes", "$1 :: " + thRowInfo[j+1].trim() + " ::  &nbsp; &nbsp; &nbsp; Attributes"));
						}
					}
				}
			}
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		
		return details.toString();
	}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		String seq = outputTable.toString();
		outputTable.delete(0, outputTable.length());
		
		try {
			
			String page = Tidy.tidyParse(table, null);
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag interTable = (TableTag) nodeList
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "grdAccountList"), true)
				.elementAt(0);
			
			Form form = new SimpleHtmlParser(table).getForm(FORM_NAME);
			
			if(interTable == null) {
				return intermediaryResponse;
			}
			
			TableRow[] rows = interTable.getRows();
			
			for(int i = 0; i < rows.length; i++) {
				
				TableRow row = rows[i];
				
				if(row.toPlainTextString().indexOf("Owner Name") > -1) {	// skip the headers row
					continue;
				}
				
				// process links
				LinkTag linkTag = (LinkTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
				Pattern pattern = Pattern.compile("__doPostBack\\('([^']*)','([^']*)'\\)");
				Matcher matcher = pattern.matcher(linkTag.toHtml());
				String target = "";
				String argument = "";
				if (matcher.find()) {
					target = matcher.group(1);
					argument = matcher.group(2);
				}
				String linkStart = CreatePartialLink(TSConnectionURL.idPOST);
				String link = linkStart + "/" + URLEncoder.encode(form.action,"UTF-8") + 
					"&seq=" + seq +
					"&__EVENTTARGET=" + target + 
					"&__EVENTARGUMENT=" + argument;
				linkTag.setLink(link);
				
				ParsedResponse currentResponse = new ParsedResponse();
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
				currentResponse.setOnlyResponse(row.toHtml());
				currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
				
				ResultMap resultMap = parseSpecificIntermediaryRow(row);
				Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
				
				DocumentI document = (AssessorDocumentI)bridge.importData();				
				currentResponse.setDocument(document);
				
				intermediaryResponse.add(currentResponse);
			}
			outputTable.append(table);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		
		resultMap.put("OtherInformationSet.SrcType","AO");
		
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			// parse general info
			TableTag accountTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "tblAccountDetail"), true)
				.elementAt(0);
			TableRow[] rows = accountTable.getRows();
//			String owner = "";
//			String address = "";
//			String legal = "";
			
			for(TableRow row : rows) {
				if(row.getColumnCount() != 3) {
					continue;
				}
				String col1 = row.getColumns()[0].toPlainTextString().replace("&nbsp;", "").trim();
				String col2 = row.getColumns()[2].toPlainTextString().replace("&nbsp;", "").trim();
				if(col1.indexOf("Account") > -1) {
					resultMap.put("PropertyIdentificationSet.ParcelID", col2);
				} else if(col1.indexOf("Parcel") > -1) {
					resultMap.put("PropertyIdentificationSet.ParcelID2", col2);
				} else if(col1.indexOf("Owner Name") > -1) {
//					owner = col2;
					resultMap.put("PropertyIdentificationSet.NameOnServer", col2);
				} else if(col1.indexOf("Property Address") > -1) {
//					address = col2;
					resultMap.put("PropertyIdentificationSet.AddressOnServer", col2);
				} else if(col1.indexOf("Legal") > -1) {
//					legal = col2;
					resultMap.put("PropertyIdentificationSet.PropertyDescription", col2);
				} else if(col1.indexOf("Subdivision") > -1) {
					resultMap.put("PropertyIdentificationSet.SubdivisionName", col2);
				} 
			}
			
			// parse sales
			TableTag salesTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "grdSales"), true)
				.elementAt(0);
			rows = salesTable.getRows();
			List<List> body = new ArrayList<List>();
			
			for(TableRow row : rows) {
				if(row.toPlainTextString().indexOf("Deed Type") > -1) {
					continue;
				}
				TableColumn[] cols = row.getColumns();
				List<String> line = new ArrayList<String>(); 
				for(TableColumn col : cols) {
					line.add(col.toPlainTextString().trim());
				}
				line.set(5, line.get(5).replaceAll(",", ""));
				String instrument = line.get(2);
				if(instrument.startsWith("B") && instrument.length() >= 7) {
					line.set(2, "");
					line.add(instrument.substring(1, 4));
					line.add(instrument.substring(4, 7));
				} else {
					line.add("");
					line.add("");
				}
				body.add(line);
			}
			if(body != null && body.size() > 0) {
				ResultTable resultTable = new ResultTable();
				String[] header = {"InstrumentDate", "DocumentType", "InstrumentNumber", 
						"Grantor", "Grantee", "SalesPrice", "Book", "Page"};
				resultTable = GenericFunctions2.createResultTable(body, header);
				resultMap.put("SaleDataSet", resultTable);
			}
			
			// parse taxable values
			TableTag taxableTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "grdValues"), true)
				.elementAt(0);
			rows = taxableTable.getRows();
			
			for(TableRow row : rows) {
				if(row.toPlainTextString().indexOf("Year") > -1) {
					continue;
				}
				resultMap.put("PropertyAppraisalSet.LandAppraisal", 
					row.getColumns()[1].toPlainTextString().replaceAll("&nbsp;|[,$]", "").trim());
				resultMap.put("PropertyAppraisalSet.ImprovementAppraisal", 
					row.getColumns()[2].toPlainTextString().replaceAll("&nbsp;|[,$]", "").trim());
				resultMap.put("PropertyAppraisalSet.TotalAppraisal", 
					row.getColumns()[3].toPlainTextString().replaceAll("&nbsp;|[,$]", "").trim());
				resultMap.put("PropertyAppraisalSet.TotalAssessment", 
					row.getColumns()[6].toPlainTextString().replaceAll("&nbsp;|[,$]", "").trim());
				break; // parse only the values for last year
			}
			
			parseSpecificDetails(resultMap);
			
		} catch (Exception e) {
			logger.error("Error while parsing details", e);
		}
		
		return null;
	}
	
	protected ResultMap parseSpecificIntermediaryRow(TableRow row) {
		
		/* Implement me in the derived class */
		return null;
	}
	
	protected void parseSpecificDetails(ResultMap resultMap) {
		
		/* Implement me in the derived class */
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();		

		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		FilterResponse cityFilter = CityFilterFactory.getCityFilterDefault(searchId);
		
		// search by PIN
		String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
		
		if(hasPin()){
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));			
			module.clearSaKeys();
			module.forceValue(0, pin);
			modules.add(module);
		}
		
		if(hasPin()){
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PROP_NO_IDX));			
			module.clearSaKeys();
			module.forceValue(0, pin.replaceAll("-", ""));
			modules.add(module);
		}
		
		// search by Address
		String strNo = getSearchAttribute(SearchAttributes.P_STREETNO);
		String strName = getSearchAttribute(SearchAttributes.P_STREETNAME);
		String city = getSearchAttribute(SearchAttributes.P_CITY);
		if(!isEmpty(strNo) && !isEmpty(strName)){
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			String addr = strNo + " " + strName;
			if(!isEmpty(city)) {
				addr += ", " + city;
			}
			module.forceValue(0, addr);	
			module.addFilter(addressFilter);
			module.addFilter(cityFilter);
			modules.add(module);			
		}
		
		// search by name - filter by address
		if(hasOwner()) {
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module));
			module.addFilter(addressFilter);
			module.addFilter(cityFilter);
			
			module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, new String[] {"L F M;;", "L, F M;;", "L F;;", "L, F;;"});
			module.addIterator(nameIterator);
			
			modules.add(module);			
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
}


