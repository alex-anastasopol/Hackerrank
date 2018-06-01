package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
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

public class COGarfieldAO extends COEagleTR {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	public COGarfieldAO(long searchId) {
		super(searchId);
	}
	
	
	public COGarfieldAO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);
	}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTableList = nodeList
				.extractAllNodesThatMatch(new HasAttributeFilter("id","searchResultsTable"), true);
			
			if(mainTableList.size() != 1) {
				return intermediaryResponse;
			}
			TableTag tableTag = (TableTag) mainTableList.elementAt(0);
			TableRow[] rows  = tableTag.getRows();
			String footer = "";
			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				if(row.getColumnCount() == 3) {
	
					LinkTag linkTag = ((LinkTag)row.getColumns()[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0));
					
					String link = CreatePartialLink(TSConnectionURL.idGET) + "/assessor/taxweb/" + 
						linkTag.extractLink().trim().replaceAll("\\s", "%20");
					
					linkTag.setLink(link);
					
					linkTag = ((LinkTag)row.getColumns()[1].getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0));
					linkTag.setLink(link);
					if(row.getChildCount() == 7) {
						row.removeChild(5);
					}
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(row.toHtml());
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
					
					ResultMap m = ro.cst.tsearch.servers.functions.COGarfieldAO.parseIntermediaryRow( row, searchId ); 
					Bridge bridge = new Bridge(currentResponse, m, searchId);
					
					currentResponse.setDocument(bridge.importData());
					
					intermediaryResponse.add(currentResponse);
				} 
			}
			footer = processLinks(response,nodeList);
			response.getParsedResponse().setHeader("<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + "<tr><th>Account#</th><th>Summary</th></tr>");
			response.getParsedResponse().setFooter(footer + "</table>");

			
			outputTable.append(table);
			
			
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}
	
	
	protected String getTaxHistory(String extractLink) {
		String taxHistoryHtml = getLinkContents(extractLink);
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(taxHistoryHtml, null);
			NodeList divList = htmlParser.parse(null)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "middle"), true)
				.extractAllNodesThatMatch(new TagNameFilter("div"),true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "iDoc"));
			if(divList.size() > 0) {
				taxHistoryHtml = divList.elementAt(0).getChildren().toHtml();
			}
			
			
		} catch (Exception e) {
			logger.error("Error while getting getTaxHistory " + extractLink, e);
		}
		return taxHistoryHtml;
	}


	
	
	protected String getDetails(String rsResponse, StringBuilder accountId) {
		try {
			
			StringBuilder details = new StringBuilder();
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
			
			
			/* If from memory - use it as is */
			if(!rsResponse.contains("<html")){
				NodeList headerList = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("h1"), true);
				if(headerList.size() == 0) {
					return null;
				} else {
					String account = headerList.elementAt(0).toPlainTextString()
						.replace("Account:", "")
						.replace("&nbsp;", "")
						.replaceAll("\\s+", "");
					accountId.append(account);	
				}
				return rsResponse;
			}
			
			NodeList tableAccountSummaryList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "middle"),true)
				.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "accountSummary"));
			if(tableAccountSummaryList.size() != 1) {
				return null;
			}
			NodeList headerList = nodeList
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "middle"),true)
				.extractAllNodesThatMatch(new TagNameFilter("h1"), true);
			if(headerList.size() == 0) {
				return null;
			} else {
				String account = headerList.elementAt(0).toPlainTextString()
					.replace("Account:", "")
					.replace("&nbsp;", "")
					.replaceAll("\\s+", "");
				accountId.append(account);	
			}
			
			
			
			
			TableTag mainTable = (TableTag) tableAccountSummaryList.elementAt(0);
			
			int posLastHeader = -1;
			int posLastRow = -1;
			TableRow[] rows = mainTable.getRows();
			
			for (int i = 0; i < rows.length; i++) {
				if(posLastHeader < 0) {
					if(rows[i].toPlainTextString().trim().equals("Images")) {
						posLastHeader = mainTable.findPositionOf(rows[i]);
					}
				} else {
					posLastRow = mainTable.findPositionOf(rows[i]);
					break;
				}
			}
			
			if(posLastRow > 0) {
				mainTable.removeChild(posLastRow);
			}
			if(posLastHeader > 0) {
				mainTable.removeChild(posLastHeader);
			}
			details.append("<table align=\"center\" border=\"1\" style=\"max-width:900px;\"><tr><td align=\"center\">")
				.append(headerList.elementAt(0).toHtml())
				.append("</td></tr><tr><td align=\"center\">")
				.append(mainTable.toHtml().replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1"))
				.append("</td></tr>");
			
			NodeList linkList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "left"),true)
				.extractAllNodesThatMatch(new TagNameFilter("a"), true);
			
			DataSite site = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), getServerID());
			String serverHomeLink = site.getServerHomeLink();
			StringBuilder fullTransferList = new StringBuilder();
			for (int i = 0; i < linkList.size(); i++) {
				LinkTag link = (LinkTag) linkList.elementAt(i);
				String linkPlainText = link.toPlainTextString().trim();
				if(linkPlainText.equals("Tax History")) {
					details
						.append("<tr><td align=\"center\">")
						.append(getTaxHistory(serverHomeLink + "assessor/taxweb/" + link.extractLink()))
						.append("</td></tr>");
				} else if (linkPlainText.startsWith("B:")) {
					Matcher matcher = TRANFERS_BOOK_PAGE_PATTERN.matcher(linkPlainText);
					if(matcher.find()) {
						if(fullTransferList.length() != 0) {
							fullTransferList.append(" | ");
						}
						fullTransferList.append(linkPlainText);
					}
				} else if(linkPlainText.matches("\\d{4,}")) {
					if(fullTransferList.length() != 0) {
						fullTransferList.append(" | ");
					}
					fullTransferList.append(linkPlainText);
				}
			}
			if(fullTransferList.length() != 0) {
				details.append("<tr><td align=\"center\" id=\"ats_all_transfers\"> ALL TRANSFERS: ").append(fullTransferList).append("</td></tr>");
			}
			details.append("</table>");
			return details.toString().replaceFirst("(?is)<div[^>]*id=\"map\"[^>]*>\\s*</div>", "");
			
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList headerList = nodeList.extractAllNodesThatMatch(new TagNameFilter("h1"), true);
			String accountId = null;
			if(headerList.size() == 0) {
				return null;
			} else {
				accountId = headerList.elementAt(0).toPlainTextString()
					.replace("Account:", "")
					.replace("&nbsp;", "")
					.replaceAll("\\s+", "");
					
			}
			
			resultMap.put("PropertyIdentificationSet.ParcelID", accountId);
			resultMap.put("OtherInformationSet.SrcType","AO");
			
			NodeList someNodeList = nodeList
				.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new TagNameFilter("td"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "40%"))
				.extractAllNodesThatMatch(new TagNameFilter("tr"), true);
			
			for (int i = 0; i < someNodeList.size(); i++) {
				TableRow row = (TableRow) someNodeList.elementAt(i);
				String plainText = row.toPlainTextString().trim();
				if(plainText.startsWith("Parcel Number")) {
					String parcelIDParcel = plainText.replace("Parcel Number", "").trim().replaceAll("-", "");
					if (!parcelIDParcel.matches("0+")) {
						resultMap.put("PropertyIdentificationSet.ParcelIDParcel", parcelIDParcel);
					}
				} else if(plainText.startsWith("Situs Address")) {
					String[] addressParts = row.toHtml().trim().replace("Situs Address", "").trim().split("<br>|,");
					String addr = addressParts[0].replaceAll("<[^>]*>", "").replaceAll("\\s+"," ").trim().replaceAll("^\\s+", "").replaceAll("^0+", "");
					if(addr.matches("(\\d+) (\\d+) ([^\\s]+) ([^\\s]*)")){
						addr = addr.replaceAll("(\\d+) (\\d+) ([^\\s]+) ([^\\s]*)","$1 $3 $4 $2");
					}					
					resultMap.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(addr));
					resultMap.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(addr));
				} else if(plainText.startsWith("City")) {
					resultMap.put("PropertyIdentificationSet.City",
							plainText.replace("City", "").trim());
				} else if(plainText.startsWith("ZipCode")) {
					resultMap.put("PropertyIdentificationSet.Zip",
							plainText.replace("ZipCode", "").trim());
				} else if(plainText.startsWith("Owner Name")) {
					resultMap.put("PropertyIdentificationSet.NameOnServer", 
							plainText.replace("Owner Name", "").trim());
				} else if(plainText.startsWith("Legal Summary")) {
					plainText = row.getChildrenHTML();
					plainText = plainText.replaceAll("<br>", " ");
					plainText = plainText.replaceAll("</?td>", "");
					plainText = plainText.replaceAll("</?b>", "");
					resultMap.put("PropertyIdentificationSet.PropertyDescription", 
							plainText.replace("Legal Summary", "").replaceAll("\\s{2,}", " ").trim());
				} else if(plainText.startsWith("Primary Taxable")) {
					resultMap.put("PropertyAppraisalSet.TotalAssessment", 
							plainText.replace("Primary Taxable", "").replaceAll("[,$]","").trim());
				} else if(plainText.startsWith("Actual")) {
					resultMap.put("PropertyAppraisalSet.TotalAppraisal", 
							plainText.replaceAll("Actual \\(\\d+\\)", "").replaceAll("[,$]","").trim());
				} 
			}
			
			
			
			List<List> body = new ArrayList<List>();
			List<String> line = null;
			HashSet<String> foundBookPage = new HashSet<String>();
			NodeList tableAccountSummaryList = nodeList
				.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "accountSummary"));
			if(tableAccountSummaryList.size() > 0) {
				TableTag mainTable = (TableTag) tableAccountSummaryList.elementAt(0);				
				TableRow[] rows = mainTable.getRows();
				
				for (int i = 0; i < rows.length; i++) {
					if(rows[i].toPlainTextString().trim().equals("Transfers")) {
						i++;
						if (i < rows.length) {
							NodeList tranferRows = rows[i].getChildren().extractAllNodesThatMatch(new TagNameFilter("tr"), true);
							
							for (int j = 1; j < tranferRows.size(); j++) {
								TableColumn[] columns = ((TableRow)tranferRows.elementAt(j)).getColumns();
								if(columns.length == 4) {
									line = new ArrayList<String>();
									line.add(columns[0].toPlainTextString().replaceAll("[,$]", "").trim());
									line.add(columns[1].toPlainTextString().trim());
									line.add("");
									String bookPage = columns[3].toPlainTextString().trim();
									Matcher matcher = TRANFERS_BOOK_PAGE_PATTERN.matcher(bookPage);
									
									if(matcher.find()) {
										line.add(matcher.group(1).replaceAll("^0+", ""));
										line.add(matcher.group(2).replaceAll("^0+", ""));
										line.add(columns[2].toPlainTextString().trim());
										foundBookPage.add(matcher.group(0));
										body.add(line);
									}
								}
							}
							
						}
					}
					
				}
			
			}
			
			someNodeList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id","ats_all_transfers"), true);
			if(someNodeList.size() > 0) {
				String tmpAllTransfers =  
						someNodeList.elementAt(0).toPlainTextString().replaceAll("ALL TRANSFERS:", "").replaceAll("&nbsp;", "").trim();
				String[] tmpAllTransfersArray = tmpAllTransfers.split("\\|");
				for (String transferString : tmpAllTransfersArray) {
					transferString = transferString.trim();
					Matcher matcher = TRANFERS_BOOK_PAGE_PATTERN.matcher(transferString);
					if (matcher.find()) {
						line = new ArrayList<String>();
						line.add("");
						line.add("");
						line.add("");
						line.add(matcher.group(1).replaceAll("^0+", ""));
						line.add(matcher.group(2).replaceAll("^0+", ""));
						line.add("");
						if(!foundBookPage.contains(matcher.group(0))) {
							foundBookPage.add(matcher.group(0));
							body.add(line);
						}
					} else if(transferString.matches("\\d{4,}")) {
						line = new ArrayList<String>();
						line.add("");
						line.add("");
						line.add(transferString);
						line.add("");
						line.add("");
						line.add("");
						if(!foundBookPage.contains(transferString)) {
							foundBookPage.add(transferString);
							body.add(line);
						}
					}
				}
				
			}
			
			
			ro.cst.tsearch.servers.functions.COGarfieldAO.parseLegalSummary(resultMap, body, searchId);
			ro.cst.tsearch.servers.functions.COGarfieldAO.parseNames(resultMap, searchId);
			
			//adding all cross references - should contain transfer table and info parsed from legal description
			if(body != null && body.size() > 0) {
				ResultTable rt = new ResultTable();
				String[] header = {"SalesPrice", "InstrumentDate", "InstrumentNumber", "Book", "Page" , "DocumentType"};
				rt = GenericFunctions2.createResultTable(body, header);
				resultMap.put("SaleDataSet", rt);
			}
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
	
	@Override
	protected void loadDataHash(HashMap<String, String> data) {
		if(data != null) {
			data.put("type","ASSESSOR");
			data.put("dataSource","AO");
		}
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
		
		TSServerInfoModule module = null;
		
		if(hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(0, SearchAttributes.LD_PARCELNO);
			moduleList.add(module);
		}
		if(hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			module.clearSaKeys();
			module.forceValue(1, getSearchAttribute(SearchAttributes.LD_PARCELNO).replaceAll("-", ""));
			moduleList.add(module);
		}
		if(hasPinParcelNo()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			module.clearSaKeys();
			module.forceValue(1, getSearchAttribute(SearchAttributes.LD_PARCELNO_PARCEL).replaceAll("-", ""));
			moduleList.add(module);
		}
		
		boolean hasOwner = hasOwner();
		FilterResponse addressFilter 	= AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d , true);
		if(hasStreet()) {
			FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , module );
			
			if(hasStreetNo()) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
				module.clearSaKeys();
				module.setSaKey(11, SearchAttributes.P_STREETNO);
				module.setSaKey(13, SearchAttributes.P_STREETNAME);
				module.addFilter(addressFilter);
				module.addFilter(nameFilterHybrid);
				moduleList.add(module);
			}
			if(hasOwner) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
				module.setSaKey(13, SearchAttributes.P_STREETNAME);
				module.addFilter(nameFilterHybrid);
				module.addFilter(addressFilter);
				module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(module, searchId, new String[] {"L;F;","L;M;"})
						);
				moduleList.add(module);
			}
		}
		if(hasOwner) {
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter( 
					SearchAttributes.OWNER_OBJECT , searchId , module );
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
			
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addFilter(addressFilter);
			module.addFilter(PINFilterFactory.getDefaultPinFilter(searchId));
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] {"L;F;","L;M;"})
					);
			moduleList.add(module);
		}
		serverInfo.setModulesForAutoSearch(moduleList);
	}
}
