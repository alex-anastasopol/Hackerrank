package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
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

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

public class ARWashingtonTR extends TSServer {
	
	private static final long serialVersionUID = -8670045566671399311L;
	
	public ARWashingtonTR(long searchId) {
		super(searchId);
	}
	
	public ARWashingtonTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
			// no result
			if (rsResponse.indexOf("No matches found") > -1) {
				Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");	
				return;
			}
			
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
			
			if(smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
            }
			
			break;
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			StringBuilder accountId = new StringBuilder();
			String details = getDetails(rsResponse, accountId);
			String filename = accountId + ".html";
			
			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data);
				if (isInstrumentSaved(accountId.toString(),null,data)){
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
			ParseResponse(sAction, Response, rsResponse.contains("Parcel Number") || rsResponse.contains("Assessment Number")
														? ID_DETAILS
														: ID_SEARCH_BY_NAME);
			break;
		default:
			break;
		}
		
	}

	protected void loadDataHash(HashMap<String, String> data) {
		if(data != null) {
			data.put("type","CNTYTAX");
		}
	}

	protected String getDetails(String rsResponse, StringBuilder accountId) {
		try {
			
			StringBuilder details = new StringBuilder();
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
						
			/* If from memory - use it as is */
			if(!rsResponse.toLowerCase().contains("<html")){
				NodeList tableAccountSummaryList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
				if(tableAccountSummaryList.size() == 0) {
					return null;
				}
				String firstTable = tableAccountSummaryList.elementAt(0).toHtml();
				Matcher ma = Pattern.compile("(?is)Parcel Number:\\s*<B>\\s*([\\d-]+)\\s*</B>").matcher(firstTable);
				if (ma.find())
					accountId.append(ma.group(1));
				return rsResponse;
			}
			
			NodeList tableAccountSummaryList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if(tableAccountSummaryList.size() == 0) {
				return null;
			}
			
			String firstTable = tableAccountSummaryList.elementAt(0).toHtml();
			Matcher ma = Pattern.compile("(?is)Parcel Number:\\s*<B>\\s*([\\d-]+)\\s*</B>").matcher(firstTable);
			if (ma.find())
				accountId.append(ma.group(1));
			
			for (int i=0;i<tableAccountSummaryList.size();i++)
				details.append(tableAccountSummaryList.elementAt(i).toHtml());
			
			String det = details.toString();
			det = det.replaceAll("(?is)</?a[^>]+>", "");
			det = det.replaceAll("(?is)<img.*?>", "");
			det = det.replaceAll("(?is)Sketch:", "");
			
			return det;
						
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTableList  = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			
			if(mainTableList.size() != 1) {
				return intermediaryResponse;
			}
			TableTag tableTag = (TableTag) mainTableList.elementAt(0);
			TableRow[] rows  = tableTag.getRows();
			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				if(row.getColumnCount() == 2) {
	
					LinkTag linkTag = ((LinkTag)row.getColumns()[1].getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0));
					
					String link = CreatePartialLink(TSConnectionURL.idGET) + "/PropertySearch/" + 
						linkTag.extractLink().trim();
					
					linkTag.setLink(link);
					
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(row.toHtml());
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
					
					ResultMap m = ro.cst.tsearch.servers.functions.ARWashingtonTR.parseIntermediaryRow( row, searchId ); 
					Bridge bridge = new Bridge(currentResponse, m, searchId);
					
					DocumentI document = (TaxDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				} 
			}
			response.getParsedResponse().setHeader("<table border=0 cellSpacing=1 width=75%");
			response.getParsedResponse().setFooter("</table>" + processLinks(response, nodeList));
			
			outputTable.append(table);
			
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}
	
	protected String processLinks(ServerResponse response, NodeList nodeList) {
		String result = "";
		String stringResponse = response.getResult();
		int nextIndex = stringResponse.indexOf("Next Group");
		int firstIndex = -1;
		if (nextIndex>-1) {
			firstIndex = stringResponse.toLowerCase().lastIndexOf("<br>", nextIndex);
		}
		int previousIndex = stringResponse.indexOf("Previous Group");
		int lastIndex = -1;
		if (previousIndex>-1) {
			lastIndex = stringResponse.toLowerCase().indexOf("|", previousIndex);
		}
		if (firstIndex>-1 && lastIndex>-1 && firstIndex<lastIndex) {
			result = stringResponse.substring(firstIndex, lastIndex)
				.replaceAll("SearchResults.asp", "PropertySearch/SearchResults.asp");
			result = result.replaceAll("(?is)(PropertySearch[^>]+)", 
					CreatePartialLink(TSConnectionURL.idGET)+ "$1");
		}
			
		return result;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap) {
		try {
			
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			String accountId = null;
			
			NodeList tableAccountSummaryList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if(tableAccountSummaryList.size() == 0) {
				return null;
			}
			
			String firstTable = tableAccountSummaryList.elementAt(0).toHtml();
			Matcher ma1 = Pattern.compile("(?is)Parcel Number:\\s*<B>\\s*([\\d-]+)\\s*</B>").matcher(firstTable);
			if (ma1.find())
				accountId = ma1.group(1);
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountId);
			
			Matcher ma2 = Pattern.compile("(?is)Location:\\s*<B>(.*?)</B>").matcher(firstTable);
			if (ma2.find())
				resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), 
						ma2.group(1).trim().replaceAll("\\s{2,}", " "));
			
			Matcher ma3 = Pattern.compile("(?is)Owner Name:\\s*<B>(.*?)</B>").matcher(firstTable);
			if (ma3.find())
				resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), 
						ma3.group(1).trim());
			
			Matcher ma4 = Pattern.compile("(?is)Lot:\\s*<B>(.*?)</B>").matcher(firstTable);
			if (ma4.find()) {
				String lot = ma4.group(1).trim().replaceFirst("\\A0+", "");
				if (lot.length()!=0)
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
			}
						
			Matcher ma5 = Pattern.compile("(?is)Block:\\s*<B>(.*?)</B>").matcher(firstTable);
			if (ma5.find()) {
				String block = ma5.group(1).trim().replaceFirst("\\A0+", "");
				if (block.length()!=0)
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
			}
						
			Matcher ma6 = Pattern.compile("(?is)S-T-R:\\s*<B>(.*?)</B>").matcher(firstTable);
			if (ma6.find()) {
				String str = ma6.group(1).trim();
				if (str.length()!=0) {
					Matcher ma7 = Pattern.compile("(?is)(.*?)-(.*?)-(.*)").matcher(str);
					if (ma7.find()) {
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), 
								ma7.group(1).trim().replaceFirst("\\A0+", ""));
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), 
								ma7.group(2).trim().replaceFirst("\\A0+", ""));
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), 
								ma7.group(3).trim().replaceFirst("\\A0+", ""));
					}
				}
			}
				
			Matcher ma8 = Pattern.compile("(?is)Addition:\\s*<B>(.*?)</B>").matcher(firstTable);
			if (ma8.find()) {
				String subd = ma8.group(1).replaceFirst("\\A\\s*.*?-.*?-.*?(\\s+|\\z)", "").trim(); 
				if (subd.length()!=0)
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subd);
			}
				
			
			Matcher ma9 = Pattern.compile("(?is)City:\\s*<B>(.*?)</B>").matcher(firstTable);
			if (ma9.find()) {
				String city = ma9.group(1).trim();
				if (city.length()!=0 && !city.toLowerCase().equals("rural"))
					resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
			}
				
			Matcher ma10 = Pattern.compile("(?is)Legal:\\s*<B>(.*?)</B>").matcher(firstTable);
			if (ma10.find()) {
				String legal = ma10.group(1).trim();
				if (legal.length()!=0)
					resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legal);
			}
						
			ResultTable rt = new ResultTable();							//Tax Payment History
			List<List> tablebody = new ArrayList<List>();
			List<String> list;
			
			NodeList nodeTaxes = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if (nodeTaxes != null)
			{
				TableTag table = null;
				for (int i=0;i<nodeTaxes.size();i++)
					if (nodeTaxes.elementAt(i).toHtml().contains("Tax Payment History")) {
						table = (TableTag)nodeTaxes.elementAt(i);
						break;
					}
				
				if (table!=null) {
					String year = "";
					String receiptNumber = "";
					String paymentDate = "";
					String paymentAmount = "";
					for (int i=2;i<table.getRowCount();i++) {
						TableRow row = table.getRow(i);
						if (row.getColumnCount()==9) {
							year = row.getColumns()[0].toPlainTextString().trim();
							receiptNumber = row.getColumns()[1].toPlainTextString().trim();
							paymentDate = row.getColumns()[2].toPlainTextString().trim();
							paymentAmount = row.getColumns()[8].toPlainTextString().trim().replaceAll("[,\\$]", "");
							list = new ArrayList<String>();
							list.add(year);
							list.add(receiptNumber);
							list.add(paymentDate);
							list.add(paymentAmount);
							tablebody.add(list);
						}
					}
				}
			}	
											
			String[] header = {TaxHistorySetKey.YEAR.getShortKeyName(), TaxHistorySetKey.RECEIPT_NUMBER.getShortKeyName(), 
					TaxHistorySetKey.RECEIPT_DATE.getShortKeyName(), TaxHistorySetKey.RECEIPT_AMOUNT.getShortKeyName()};
			rt = GenericFunctions2.createResultTable(tablebody, header);
			if (rt != null && rt.getLength()!=0){
				resultMap.put("TaxHistorySet", rt);
			}
			
			ResultTable transactionHistory = new ResultTable();			//Deed History
			List<List> tablebodytrans = new ArrayList<List>();
			List<String> listtrans;
			
			NodeList nodeTrans = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if (nodeTaxes != null)
			{
				TableTag tableTrans = null;
				for (int i=0;i<nodeTaxes.size();i++)
					if (nodeTaxes.elementAt(i).toHtml().contains("Deed History")) {
						tableTrans = (TableTag)nodeTrans.elementAt(i);
						break;
					}
				
				if (tableTrans!=null) {
					String date = "";
					String bookPage = "";
					String grantee1 = "";
					String grantee2 = "";
					String saleAmount = "";
					for (int i=2;i<tableTrans.getRowCount();i++)	{
						TableRow row = tableTrans.getRow(i);
						if (row.getColumnCount()==7) {
							date = row.getColumns()[0].toPlainTextString().trim();
							bookPage = row.getColumns()[1].toPlainTextString().trim();
							grantee1 = row.getColumns()[2].toPlainTextString().trim();
							grantee2 = row.getColumns()[3].toPlainTextString().trim();
							saleAmount = row.getColumns()[5].toPlainTextString().trim().replaceAll("[,\\$]", "");
							listtrans = new ArrayList<String>();
							listtrans.add(date);
							String[] bookAndPage = bookPage.split("-");
							if (bookAndPage.length==2 && bookAndPage[0].matches("\\d+") && bookAndPage[1].matches("\\d+")) {
								bookAndPage[0] = bookAndPage[0].trim();
								if (bookAndPage[0].matches("9[0-9]"))
									bookAndPage[0] = "19" + bookAndPage[0];
								listtrans.add(bookAndPage[0]);
								listtrans.add(bookAndPage[1].trim());
								listtrans.add(bookAndPage[0] +  "-" + bookAndPage[1].trim());
							} else {
								listtrans.add("");
								listtrans.add("");
								listtrans.add("");
							}
							String grantee = "";
							if (grantee1.length()!=0)
								grantee += grantee1;
							if (grantee2.length()!=0)
								grantee += " " + grantee2;
							listtrans.add(grantee);
							listtrans.add(saleAmount);
							tablebodytrans.add(listtrans);
						}
					}
				}
			}	
											
			String[] headerTrans = {SaleDataSetKey.RECORDED_DATE.getShortKeyName(), SaleDataSetKey.BOOK.getShortKeyName(), 
					SaleDataSetKey.PAGE.getShortKeyName(), SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(), SaleDataSetKey.GRANTEE.getShortKeyName(),
					SaleDataSetKey.SALES_PRICE.getShortKeyName()};
			transactionHistory = GenericFunctions2.createResultTable(tablebodytrans, headerTrans);
			if (transactionHistory != null && transactionHistory.getLength()!=0){
				resultMap.put("SaleDataSet", transactionHistory);
			}
			
			ro.cst.tsearch.servers.functions.ARWashingtonTR.parseNames(resultMap, searchId);
			ro.cst.tsearch.servers.functions.ARWashingtonTR.parseAddress(resultMap);
			ro.cst.tsearch.servers.functions.ARWashingtonTR.parseLegalSummary(resultMap);
			ro.cst.tsearch.servers.functions.ARWashingtonTR.parseTaxes(detailsHtml, nodeList, resultMap, searchId);
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
		
		TSServerInfoModule module = null;
		
		if(hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(5, SearchAttributes.LD_PARCELNO_GENERIC_TR);
			moduleList.add(module);
		}
		
		boolean hasOwner = hasOwner();
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d , true);
		if(hasStreet()) {
			FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , module );
			
			if(hasStreetNo()) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.clearSaKeys();
				module.setSaKey(2, SearchAttributes.P_STREETNO);
				module.setSaKey(3, SearchAttributes.P_STREETNAME);
				module.addFilter(addressFilter);
				module.addFilter(nameFilterHybrid);
				moduleList.add(module);
			}
			if(hasOwner) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				module.setSaKey(3, SearchAttributes.P_STREETNAME);
				module.addFilter(nameFilterHybrid);
				module.addFilter(addressFilter);
				module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(module, searchId, new String[] {"L;F;","L;M;"}));
				moduleList.add(module);
			}
		}
		if(hasOwner) {
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter(
					SearchAttributes.OWNER_OBJECT , searchId , module );
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
			
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addFilter(addressFilter);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] {"L;F;","L;M;"}));
			moduleList.add(module);
		}
		serverInfo.setModulesForAutoSearch(moduleList);
	}
}
