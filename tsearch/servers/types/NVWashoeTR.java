
package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * @author vladb
 *
 */
public class NVWashoeTR extends TSServer {

	private static final long serialVersionUID = 1L;

	public NVWashoeTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		Response.getQuerry();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch (viParseID) 
		{
			case ID_SEARCH_BY_NAME:
			case ID_SEARCH_BY_PARCEL:	
			case ID_SEARCH_BY_ADDRESS:
				
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				
				if(smartParsedResponses.size() == 0) {
					return;
				}

				StringBuilder footer = new StringBuilder();
				String nextLink = processFooter(rsResponse, footer);
				parsedResponse.setNextLink(nextLink);
				parsedResponse.setFooter(footer.toString());
				
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				
				break;
			case ID_GET_LINK:
				if (rsResponse.indexOf("Washoe County Parcel Information") > -1) {
					ParseResponse(sAction, Response, ID_DETAILS);
				} else {
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				}
				break;
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:
				
				StringBuilder accountId = new StringBuilder();
				String details = getDetails(Response, accountId);
				
				if(viParseID == ID_DETAILS) {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
	
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","CNTYTAX");
					if (isInstrumentSaved(accountId.toString().trim(), null, data)){
						details += CreateFileAlreadyInTSD();
					}
					else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
					}
	
					parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
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
		
	private String getDetails(ServerResponse Response, StringBuilder accountId) {

		StringBuilder details = new StringBuilder();
		
		try {
			String rsResponse = Response.getResult();
			
			org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = parser.parse(null);
			
			// Parcel Information
			TableTag table = null;
			
			if(rsResponse.toLowerCase().contains("<html")) { // page from official site
				table = (TableTag) nodeList
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "dnn_ctr528_ModuleContent"), true)
						.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
				table.setAttribute("border", "1");
				table.setAttribute("id", "parcelinfo");
				table.setAttribute("style", "border-collapse: collapse");
				details.append(table.toHtml());
				details.append("<br />");
			} else { // page from memory
				table = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "parcelinfo"), true).elementAt(0);
			}
			
			for(TableRow row : table.getRows()) {
				if(row.getColumnCount() == 3 && row.getColumns()[0].toPlainTextString().trim().matches("\\d+")) {
					accountId.append(row.getColumns()[0].toPlainTextString().trim());
					if(!rsResponse.toLowerCase().contains("<html")) {
						return rsResponse;
					} else {
						break;
					}
				}
			}
			
			// Tax Bill
			table = (TableTag) nodeList
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "dnn_ctr529_ModuleContent"), true)
					.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
			if(table == null) { // nothing left to extract
				return details.toString();
			}
			
			table.setAttribute("border", "1");
			table.setAttribute("id", "taxbill");
			table.setAttribute("style", "border-collapse: collapse");
			details.append(table.toHtml().replaceAll("(?is)<a[^>]*>(.*?)</a>", "$1"));
			details.append("<br />");
			
			DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID);
			String siteLink = dataSite.getLink();
			
			for(TableRow row : table.getRows()) {
				LinkTag linkTag = (LinkTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
				if(linkTag == null) {
					continue;
				}
				
				String year = row.getColumns()[0].toPlainTextString().trim();
				details.append("<h3>Tax Year " + year + "</h3>");
				
				String link = siteLink + linkTag.getLink().replaceFirst("/", "");
				String yearPage = getLinkContents(link);
				
				try {
					org.htmlparser.Parser parser1 = org.htmlparser.Parser.createParser(yearPage, null);
					NodeList nodeList1 = parser1.parse(null);
					
					// Payment History
					TableTag table1 = (TableTag) nodeList1
							.extractAllNodesThatMatch(new HasAttributeFilter("id", "dnn_ctr536_ModuleContent"), true)
							.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
					if(table1 != null) {
						table1.setAttribute("border", "1");
						table1.setAttribute("name", "paymenthist");
						table1.setAttribute("style", "border-collapse: collapse");
						details.append(table1.toHtml());
						details.append("<br />");
					}
					
					// Installments
					table1 = (TableTag) nodeList1
							.extractAllNodesThatMatch(new HasAttributeFilter("id", "dnn_ctr535_ModuleContent"), true)
							.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
					if(table1 != null) {
						table1.setAttribute("border", "1");
						table1.setAttribute("id", "installments"+year);
						table1.setAttribute("style", "border-collapse: collapse");
						details.append(table1.toHtml());
						details.append("<br />");
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		} catch (Exception e) {
			logger.error("ERROR while getting details: " + e.toString());
		}
		
		return details.toString();
	}
	
	/**
	 * @return nextLink
	 */
	private String processFooter(String response, StringBuilder footer) {
		String nextLink = "";
		footer.append("<tr><td colspan=\"4\" align=\"center\">");
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
			NodeList nodeList = htmlParser.parse(null);
			
			Node navigBar = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "search-results-bar"), true).elementAt(0);
			NodeList links = navigBar.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
			String baseLink = CreatePartialLink(TSConnectionURL.idGET) + "/Tabs/TaxSearch.aspx";
			
			for(int i = 0; i < links.size(); i++) {
				LinkTag linkTag = (LinkTag) links.elementAt(i);
				String link = linkTag.getLink();
				
				if(link.startsWith("?page")) {
					linkTag.setLink(baseLink + link);
					footer.append(linkTag.toHtml().replaceFirst("(?is)(<a[^>]*>)(.*?)(</a>)", "$1&nbsp;$2&nbsp;$3"));
				} else {
					footer.append(linkTag.toHtml().replaceFirst("(?is)(<a[^>]*>)(.*?)(</a>)", "&nbsp;$2&nbsp;"));
				}
				
				if(linkTag.toPlainTextString().contains("Next")) {
					nextLink = link;
				}
			}
		} catch (Exception e) {
			logger.error("Error while processing the navigation bar");
		}
		
		footer.append("</td></tr></table>");
		
		return nextLink;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		ParsedResponse parsedResponse = response.getParsedResponse();
		
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag interTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "grm-search"), true).elementAt(0);
			
			if(interTable == null) {
				return intermediaryResponse;
			}
			
			TableRow[] rows = interTable.getRows();
			
			for(int i = 0; i < rows.length; i++) {
				
				TableRow row = rows[i];
				
				// parse header
				if(row.getHeaderCount()>0){
					parsedResponse.setHeader("<table border=\"1\" width=\"80%\" align=\"center\" " +
							"style=\"border-collapse: collapse\">" + row.toHtml());
					continue;
				}
				
				// process link
				String rowHtml = row.toHtml();
				String p = row.getAttribute("p");
				String a = row.getAttribute("a");
				String link = CreatePartialLink(TSConnectionURL.idGET) + "/Tabs/TaxSearch/AccountDetail.aspx?p=" + p + "&a=" + a;
				String pin = row.getColumns()[3].toPlainTextString();
				rowHtml = rowHtml.replaceFirst(">" + pin + "<", "><a href=" + link + ">" + pin + "</a><");
						
				ParsedResponse currentResponse = new ParsedResponse();
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
				currentResponse.setOnlyResponse(rowHtml);
				currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
				
				ResultMap m = ro.cst.tsearch.servers.functions.NVWashoeTR.parseIntermediaryRow(row);
				Bridge bridge = new Bridge(currentResponse, m, searchId);
				
				DocumentI document = (TaxDocumentI)bridge.importData();				
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
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		
		try {
			String properDetails = detailsHtml.replaceAll("(?is)<br>", "\n").replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
			HtmlParser3 htmlParser3 = new HtmlParser3(properDetails);
			NodeList nodeList = htmlParser3.getNodeList();
			
			String owner = "";
			String address = "";
			String legal = "";
			
			// Parcel Information table
			TableTag table = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "parcelinfo"), true).elementAt(0);
			
			for(TableRow row : table.getRows()) {
				String rowText = row.toPlainTextString();
				
				if(row.getColumnCount() == 3 && row.getColumns()[0].toPlainTextString().trim().matches("\\d+")) {
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), row.getColumns()[0].toPlainTextString().trim());
				} else if(rowText.contains("Current Owner")) {
					TableTag ownerTable = (TableTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
					owner = ownerTable.getRow(0).getColumns()[0].toPlainTextString().replaceFirst("(?is)Current Owner:", "").trim();
					address = ownerTable.getRow(0).getColumns()[1].toPlainTextString().replaceFirst("(?is)SITUS:", "").trim();
				} else if(!rowText.contains("Legal Description") && row.getColumnCount() == 1) {
					legal = rowText.trim();
				}
			}
			
			ro.cst.tsearch.servers.functions.NVWashoeTR.parseNames(resultMap, owner, true);
			ro.cst.tsearch.servers.functions.NVWashoeTR.parseAddress(resultMap, address);
			ro.cst.tsearch.servers.functions.NVWashoeTR.parseLegal(resultMap, legal);
			
			// Tax Bill table
			table = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "taxbill"), true).elementAt(0);
			if(table == null) {
				return null;
			}
			
			boolean currentYear = true;
			double delinqAmount = 0d;
			
			for(TableRow row : table.getRows()) {
				String rowText = row.toPlainTextString();
				if(rowText.contains("Tax Year")) {
					continue;
				}
				
				TableColumn[] cols = row.getColumns();
				
				if(cols.length >= 7) {
					if(currentYear) {
						resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), cols[0].toPlainTextString().trim());
						resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), cols[1].toPlainTextString().replaceAll("[$,]", "").trim());
						resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), cols[2].toPlainTextString().replaceAll("[$,]", "").trim());
						resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), cols[6].toPlainTextString().replaceAll("[$,]", "").trim());
						currentYear = false;
					} else {
						try {
							delinqAmount += Double.parseDouble(cols[6].toPlainTextString().replaceAll("[$,]", "").trim());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
			
			resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), String.valueOf(delinqAmount));
			
			String year = resultMap.get(TaxHistorySetKey.YEAR.getKeyName()).toString();		
			double[] installmentsAmountPaid ={0.0,0.0,0.0,0.0};
			int installmentsCounter = 0;
			// Payment History tables
			NodeList tables = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("name", "paymenthist"), true);
			List<List> body = new ArrayList<List>();
			
			for(int i = 0; i < tables.size(); i++) {
				table = (TableTag) tables.elementAt(i);
				
				for(TableRow row : table.getRows()) {
					String rowText = row.toPlainTextString();
					TableColumn[] cols = row.getColumns();
					
					if (!rowText.contains("Tax Year") && cols.length >= 5) {
						List<String> line = new ArrayList<String>();
						String taxYear = cols[0].toPlainTextString().trim();
						String amountPaid = cols[3].toPlainTextString().replaceAll("[$,]", "").trim();
						if (taxYear.equals(year) && !amountPaid.isEmpty()) {
							installmentsAmountPaid[installmentsCounter] = Double.parseDouble(amountPaid);
							installmentsCounter++;
						}
						line.add(taxYear);
						line.add(cols[2].toPlainTextString().trim());
						line.add(amountPaid);
						line.add(cols[4].toPlainTextString().trim());
						body.add(line);
					}
				}
			}
			
			String[] header = {"Year", "ReceiptNumber", "ReceiptAmount", "ReceiptDate"};
			ResultTable rt = GenericFunctions2.createResultTable(body, header);
			if (rt != null){
				resultMap.put("TaxHistorySet", rt);
			}
			// Installments table
			Node installmentsTable = htmlParser3.getNodeById("installments"+year);
			if (installmentsTable != null) {
				TableTag installmentsTTag = (TableTag) installmentsTable;
				String[] installmentsHeader ={
		        		"InstallmentName",
		                "BaseAmount", 		
		                "AmountPaid",       
		                "TotalDue",			
		                "PenaltyAmount",
		                "Status"};
				List<List> installmentsBody = new ArrayList<List>();
				
				for (int i = 1; i < installmentsTTag.getRowCount(); i++) {
					TableRow r = installmentsTTag.getRow(i);
					TableColumn[] cols = r.getColumns();

					if (cols.length > 0 && cols[0].toPlainTextString().contains("INST")) {
						List<String> line = new ArrayList<String>();		
						
						line.add("Installment " + i);
						
						line.add(cols[3].toPlainTextString().trim().replaceAll("[$,]", "").trim());
						
						line.add(String.valueOf(installmentsAmountPaid[i-1]));
						
						double amountDue = Double.parseDouble(cols[6].toPlainTextString().replaceAll("[$,]", "").trim());
						line.add(String.valueOf(amountDue));
						
						line.add(cols[5].toPlainTextString().replaceAll("[$,]", "").trim());
						
						String status = "";
						if (amountDue > 0.0) {
							status = "UNPAID";
						}
						else {
							status = "PAID";
						}
						line.add(status);
						
						installmentsBody.add(line);
					}
				}
				ResultTable installmentsRT = new ResultTable();	
				installmentsRT.setHead(installmentsHeader);
				installmentsRT.setBody(installmentsBody);
				Map<String,String[]> map = new HashMap<String,String[]>();
				map.put("InstallmentName", new String[]{"InstallmentName", ""});
				map.put("BaseAmount", new String[]{"BaseAmount", ""});
				map.put("AmountPaid", new String[]{"AmountPaid", ""});
				map.put("TotalDue", new String[]{"TotalDue", ""});
				map.put("Status", new String[]{"Status", ""});
				
				installmentsRT.setMap(map);
				if (installmentsRT != null) {
				resultMap.put("TaxInstallmentSet", installmentsRT);
				}	
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();		
		TSServerInfoModule module;
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		
		// search by account number
		if(hasPin()) {
			// Convert APN from ddd-ddd-dd to dddddddd
			String pid = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO).replaceAll("(?is)\\p{Punct}", "");
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.setData(2, pid);
//			module.getFunction(2).setSaKey(SearchAttributes.LD_PARCELNO);
			modules.add(module);
		}
		
		// search by Address
		if(hasStreet() && hasStreetNo()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			String strName = getSearchAttribute(SearchAttributes.P_STREETNAME);
			String strNo = getSearchAttribute(SearchAttributes.P_STREETNO);
			String suffix = getSearchAttribute(SearchAttributes.P_STREETSUFIX);
			String key = strNo + " " + strName;
			if(StringUtils.isNotEmpty(suffix)) {
				key += " " + suffix;
			}
			module.getFunction(2).forceValue(key);
			
			module.addFilter(addressFilter);
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module));
			
			modules.add(module);			
		}
		
		// search by name - filter by address
		if(hasOwner()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(2,FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
			
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module));
			module.addFilter(addressFilter);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] { "L F M;;", "L F;;", "L, F;;"}));
			
			modules.add(module);	
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
}
