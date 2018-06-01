
package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.NotFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.searchsites.client.TaxSiteData;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.InstanceManager;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * @author vladb
 *
 */
public class OHLorainTR extends TSServer {
	
	private static final long serialVersionUID = 1L;

	public OHLorainTR(long searchId) {
		super(searchId);
	}

	public OHLorainTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch (viParseID) 
		{
			case ID_SEARCH_BY_NAME:
			case ID_SEARCH_BY_PARCEL:	
			case ID_SEARCH_BY_ADDRESS:
			case ID_SEARCH_BY_SALES:
				
				// no result
				if (rsResponse.contains("The search did not return any results")) {
					Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
					return;
				}
				
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				if(smartParsedResponses.size() == 0) {
					return;
				}
				
				StringBuilder footer = new StringBuilder();
				String nextLink = processNavigationLinks(rsResponse, footer, Response.getLastURI().toString());
				parsedResponse.setFooter("<tr><td colspan='6' align='center'>" + footer.toString() + "</td></tr></table>");
				parsedResponse.setNextLink(nextLink);
				
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				
				break;
			case ID_GET_LINK:
				if (rsResponse.contains("Owner Information")) {
					ParseResponse(sAction, Response, ID_DETAILS);
				} else {
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				}
				break;
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:
				
				StringBuilder accountId = new StringBuilder();
				String details = getDetails(rsResponse, accountId);
				
				if(viParseID == ID_DETAILS) {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
	
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

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String page, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		ParsedResponse parsedResponse = response.getParsedResponse();
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag interTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "searchResultTable"), true).elementAt(0);
			if(interTable == null) {
				return intermediaryResponse;
			}
			TableRow[] rows = interTable.getRows();
			
			String url = response.getLastURI().toString();
			
			for(int i = 0; i < rows.length; i++) {
				TableRow row = rows[i];
				String rowText = row.toPlainTextString();
				
				if(rowText.contains("Parcel Number")) { // table header
					parsedResponse.setHeader("<table style='border-collapse: collapse' border='2' width='80%' align='center'>" + 
							row.toHtml().replaceAll("(?is)<th[^>]*>", "<th>")); // perform some cleaning
					continue;
				}
				
				// process links
				StringBuilder rowHtml = new StringBuilder();
				String link = processIntermediaryLink(row, rowHtml, url);
				
				ParsedResponse currentResponse = new ParsedResponse();
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml.toString());
				currentResponse.setOnlyResponse(rowHtml.toString());
				currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
				
				ResultMap m = ro.cst.tsearch.servers.functions.OHLorainTR.parseIntermediaryRow(row);
				Bridge bridge = new Bridge(currentResponse, m, searchId);
				
				DocumentI document = (TaxDocumentI)bridge.importData();				
				currentResponse.setDocument(document);
				
				intermediaryResponse.add(currentResponse);
			}
			
			parsedResponse.setFooter("</table>");
			outputTable.append(interTable.toHtml());
		} catch(Exception e) {
			logger.error("Error while parsing intermediary results", e);
		}
		
		return intermediaryResponse;
	}

	private String processIntermediaryLink(TableRow row, StringBuilder rowHtml, String url) {
		String pin = row.getAttribute("pin");
		String newUrl = url.replaceFirst("Search.aspx", "ParcelSearch.aspx")
				.replaceFirst("(^|&)page=(\\d*)", "$1page=" + pin);
		String link = CreatePartialLink(TSConnectionURL.idGET) + newUrl;
		
		rowHtml.append(row.toHtml().replaceFirst("(?is)<td>([\\d-]*)</td>", "<td><a href='" + link + "'>$1</a></td>"));
		
		return link;
	}
	
	private String processNavigationLinks(String rsResponse, StringBuilder footer, String url) {
		String nextLink = "";
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
			
			Node navPanel = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "paging"), true).elementAt(0);
			if(navPanel == null) {
				return nextLink;
			}
			NodeList navLinks = navPanel.getChildren().extractAllNodesThatMatch(new TagNameFilter("span"));
			boolean foundCurrentPage = false;
			
			for(int i = 0; i < navLinks.size(); i++) {
				TagNode navLink = (TagNode) navLinks.elementAt(i);
				String page = navLink.toPlainTextString();
				if(page.matches("\\d+")) {
					if(navLink.getAttribute("class").contains("current-page")) {
						foundCurrentPage = true;
						footer.append("<b>" + page + "</b>&nbsp;&nbsp;&nbsp;");
					} else {
						String link = CreatePartialLink(TSConnectionURL.idGET) +
							url.replaceFirst("(^|&)page=(\\d*)", "$1page=" + page);
						footer.append("<a href='" + link + "'>" + page + "</a>&nbsp;&nbsp;&nbsp;");
						if(foundCurrentPage && nextLink.equals("")) {
							nextLink = link;
						}
					}
				} else {
					footer.append(". . .&nbsp;&nbsp;&nbsp;");
				}
				
			}
		} catch (Exception e) {
			logger.error("Error while processing navigation links", e);
		}
		
		return nextLink;
	}

	private String getDetails(String rsResponse, StringBuilder accountId) {
		StringBuilder details = new StringBuilder();
		
		Matcher m = Pattern.compile("Parcel:\\s*([\\d-]+)").matcher(rsResponse);
		if(m.find()) {
			accountId.append(m.group(1));
			details.append("<div align='center'><h3>" + m.group() + "</h3></div>");
		}
		
		if(!rsResponse.contains("resultParcelMeta")) { // doc from memory
			return rsResponse;
		}
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList detailsItems = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "parcel-item"), true);
			
			for(int i = 0; i < detailsItems.size(); i++) {
				Node item = detailsItems.elementAt(i);
				NodeList tables = item.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true);
				
				for(int j = 0; j < tables.size(); j++) {
					TableTag table = (TableTag) tables.elementAt(j);
					String tableText = table.toPlainTextString();
					
					if(tableText.contains("Neighborhood")) {
						table.getChildren().keepAllNodesThatMatch(new NotFilter(
								new HasAttributeFilter("id", "neighborhood-search-form")), true);
					}
					if(tableText.contains("Residential Information")) {
						for(TableRow row : table.getRows()) {
							if(row.toPlainTextString().contains("Crawl Space")) {
								row.getColumns()[1].setChildren(new NodeList());
							}
						}
					}
					if(!tableText.contains("Property Photos") && !tableText.contains("Building Sketch")
							&& !tableText.contains("Conveyance Forms")) {
						table.setAttribute("style", "border-collapse: collapse");
						table.setAttribute("rules", "rows");
						table.setAttribute("border", "2");
						table.setAttribute("align", "center");
						table.setAttribute("width", "80%");
						// some cleaning
						String tableHtml = table.toHtml().replaceAll("(?is)<a [^>]*>(.*?)</a>", "$1");
						
						details.append(tableHtml);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		
		return details.toString();
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID);
		Calendar cal = Calendar.getInstance();
		cal.setTime(dataSite.getPayDate());
		int taxYear = cal.get(Calendar.YEAR);
		if(dataSite.getTaxYearMode() == TaxSiteData.TAX_YEAR_PD_YEAR_MINUS_1) {
			taxYear--;
		} else if(dataSite.getTaxYearMode() == TaxSiteData.TAX_YEAR_PD_YEAR_PLUS_1) {
			taxYear++;
		}
		
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), String.valueOf(taxYear));
		
		Matcher m = Pattern.compile("Parcel:\\s*([\\d-]+)").matcher(detailsHtml);
		if(m.find()) {
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), m.group(1));
		}
		
		try {
			detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", " ");
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"));
			String owner = "";
			String address = "";
			String legal = "";
			String fullTax = "";
			String amountPaid = "";
			String unpaid = "";
			List<List> body = new ArrayList<List>();
			
			for(int i = 0; i < tables.size(); i++) {
				TableTag table = (TableTag) tables.elementAt(i);
				String tableText = table.toPlainTextString();
				
				if(tableText.contains("Owner Information")) {
					for(TableRow row : table.getRows()) {
						if(row.getColumnCount() >= 2) {
							String col1 = row.getColumns()[0].toPlainTextString();
							String col2 = row.getColumns()[1].toPlainTextString();
							if(col1.contains("Owner Name")) {
								col2 = col2.replaceAll("\\s*\n\\s*", "\n").trim().split("\n")[0];
								if(!col2.matches("\\d+.*,\\s*[A-Z]{2}\\s+\\d+")){ // Parcel: 02-01-006-114-024
									owner = col2;
								}
							}
						}
					}
				} else if(tableText.contains("Property Information")) {
					for(TableRow row : table.getRows()) {
						if(row.getColumnCount() >= 2) {
							String col1 = row.getColumns()[0].toPlainTextString();
							String col2 = row.getColumns()[1].toPlainTextString();
							if(col1.contains("Location Address")) {
								address = col2;
							} else if(col1.contains("Legal")) {
								legal = col2.replaceAll("\\s*\n\\s*", "\n").trim();
							} else if(col1.contains("Instrument Number")) {
								String crossRef = col2.trim();
								if(crossRef.matches("\\d+")) {
									List<List> bodyCR = new ArrayList<List>();
									List<String> line = new ArrayList<String>();
									line.add("");
									line.add("");
									line.add(crossRef);
									line.add("");
									bodyCR.add(line);
									GenericFunctions2.saveCRInMap(resultMap, bodyCR);
								}
							}
						}
					}
				} else if(tableText.contains("Value Information")) {
					for(TableRow row : table.getRows()) {
						String rowText = row.toPlainTextString();
						if(rowText.contains("Assessed Land Value")) {
							rowText = rowText.split(":")[1].replaceAll("[$,]", "").trim();
							resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), rowText);
						} else if(rowText.contains("Assessed Total Value")) {
							rowText = rowText.split(":")[1].replaceAll("[$,]", "").trim();
							resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), rowText);
							resultMap.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(), rowText);
						}
						else if(rowText.contains("Assessed Building Value")) {
							rowText = rowText.split(":")[1].replaceAll("[$,]", "").trim();
							resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), rowText);
						}
					}
				} else if(tableText.contains("Full Year Tax Information")) {
					for(TableRow row : table.getRows()) {
						String rowText = row.toPlainTextString();
						if(rowText.contains("Annual Real Estate Tax")) {
							rowText = rowText.split(":")[1].replaceAll("[$,]", "").trim();
							resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), rowText);
						} else if(rowText.contains("Full Year Tax(includes any unpaid taxes & special assessments)")) {
							rowText = rowText.split(":")[1].replaceAll("[$,]", "").trim();
							fullTax = rowText;
						} else if(rowText.contains("Total Taxes Paid to Date")) {
							rowText = rowText.split(":")[1].replaceAll("[$,]", "").trim();
							resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), rowText);
							amountPaid = rowText;
						} else if(rowText.contains("Unpaid Taxes")) {
							rowText = rowText.split(":")[1].replaceAll("[$,]", "").trim();
							unpaid = rowText;
						}
					}
					try {
						String totalDue = String.valueOf(Double.parseDouble(fullTax) - Double.parseDouble(unpaid) - Double.parseDouble(amountPaid));
						resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue);
						
						String delinquent = String.valueOf(Double.parseDouble(unpaid) - Double.parseDouble(totalDue));
						if(!delinquent.startsWith("-")) {
							resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), delinquent);
						}
					} catch (Exception e) {
						logger.error("Error while parsing details", e);
					}
				} else if(tableText.contains("Transfer History")) {
					List<String> line = new ArrayList<String>();
					
					for(TableRow row : table.getRows()) {
						if(row.getColumnCount() == 4) {
							String col1 = row.getColumns()[0].toPlainTextString().trim();
							String col2 = row.getColumns()[1].toPlainTextString().trim();
							String col3 = row.getColumns()[2].toPlainTextString().trim();
							if(col1.matches("\\d+/\\d+/\\d+")) {
								line.add(col1); // sale date
								line.add(col2.replaceAll("[$,]", "")); // sale price
								line.add(col3); // inst no
							} else if(col1.matches("\\d{13}")) {
								line.add(col2); // grantor
								line.add(col3); // grantee
								break;
							}
						}
					}
					while(line.size() < 5) { // some transfers doesn't have grantor / grantee
						line.add("");
					}
					
					body.add(line);
				}
			}
			
			if(!body.isEmpty()) {
				ResultTable resultTable = new ResultTable();
				String[] header = {"InstrumentDate", "SalesPrice", "InstrumentNumber", "Grantor", "Grantee"};
				resultTable = GenericFunctions2.createResultTable(body, header);
				resultMap.put("SaleDataSet", resultTable);
			}
			
			ro.cst.tsearch.servers.functions.OHLorainTR.parseNames(resultMap, owner);
			ro.cst.tsearch.servers.functions.OHLorainTR.parseAddress(resultMap, address);
			ro.cst.tsearch.servers.functions.OHLorainTR.parseLegal(resultMap, legal);
		} catch (Exception e) {
			logger.error("Error while parsing details", e);
		}
		
		return resultMap;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();		
		TSServerInfoModule module;
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		
		// search by account number
		if(hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO);
			modules.add(module);
		}
		
		// search by Address
		if(hasStreet() && hasStreetNo()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(2).setSaKey(SearchAttributes.P_STREETNO);
			module.getFunction(3).setSaKey(SearchAttributes.P_STREETNAME);
			module.getFunction(4).setSaKey(SearchAttributes.P_CITY);
			
			module.addFilter(addressFilter);
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module));
			
			modules.add(module);
		}
		
		// search by name - filter by address
		if(hasOwner()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module));
			module.addFilter(addressFilter);
			
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, new String[] {"L F M;;", "L F;;"});
			module.addIterator(nameIterator);
			
			modules.add(module);			
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
}
