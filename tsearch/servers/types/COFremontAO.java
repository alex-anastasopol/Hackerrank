package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
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
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;

public class COFremontAO extends COGarfieldAO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -116388396220044093L;

	public COFremontAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch (viParseID) {
			case ID_SEARCH_BY_PROP_NO:
			case ID_SEARCH_BY_PARCEL:
			case ID_SEARCH_BY_NAME:
			case ID_SEARCH_BY_ADDRESS:
			case ID_SEARCH_BY_SUBDIVISION_NAME:
				// no result
				if (rsResponse.indexOf("No records found") > -1 || rsResponse.indexOf("No results") > -1) {
					Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
					return;
				}
				
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				
				if(smartParsedResponses.size() == 0) {
					return;
				}
//				String nextLink = processLink(rsResponse);
//				parsedResponse.setHeader(extractHeader(rsResponse));	
//				parsedResponse.setFooter("<tr><td>" + (nextLink != null ? nextLink : "") + "</td></tr></table>");
//				if (StringUtils.isNotEmpty(nextLink)) {
//					parsedResponse.setNextLink(nextLink);
//				}
				
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				
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
				ParseResponse(sAction, Response, rsResponse.contains("Sort Search Results")
															? ID_SEARCH_BY_NAME
															: ID_DETAILS);
				break;
			default:
				break;
		}
	}
	
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		boolean adrFlag = true;
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
			
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountId);
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"AO");
			
			
			String ownerName = "";
			String legal = "";
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
						resultMap.put(PropertyIdentificationSetKey.PARCEL_ID_PARCEL.getKeyName(), parcelIDParcel);
					}
					
				} else if(plainText.startsWith("Situs Address")) {
					String[] addressParts = row.toHtml().trim().replace("Situs Address", "").trim().split("<br>|,");
					String addr = addressParts[0].replaceAll("<[^>]*>", "").replaceAll("\\s+"," ").trim().replaceAll("^\\s+", "").replaceAll("^0+", "");
					if(addr.matches("(\\d+) (\\d+) ([^\\s]+) ([^\\s]*)")){
						addr = addr.replaceAll("(\\d+) (\\d+) ([^\\s]+) ([^\\s]*)","$1 $3 $4 $2");
					}					
					resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(addr));
					resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(addr));
					
//				} else if(plainText.startsWith("City")) {
//					resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), plainText.replace("City", "").trim());
//					
//				} else if(plainText.startsWith("ZipCode")) {
//					resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), plainText.replace("ZipCode", "").trim());
					
				} else if(plainText.startsWith("Owner Name")) {
						ownerName = plainText.replace("Owner Name", "").trim();
						
				} else if(plainText.startsWith("Owner Address")) {
					plainText = row.getChildrenHTML();
					plainText = plainText.replaceAll("</?td>", "");
					plainText = plainText.replaceAll("</?b>", "");
					Matcher matchAddr = Pattern.compile("(?is)Owner Address\\s+(.*)<\\s*/?\\s*br\\s*>([\\w\\s]+),\\s*\\bCO\\b\\s*([\\d-]+)")
							.matcher(plainText);
					if (matchAddr.find()) {
						resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), matchAddr.group(2).trim());
						resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), matchAddr.group(3).trim());
						plainText = matchAddr.group(1).trim();
						plainText = plainText.replaceAll("(?is)(?:\\bP\\s*\\.?\\s*O\\s*\\.?\\s*\\b\\s+BOX|\\d+)\\s+[\\w\\s]+(:AVENUE|AVE|DRIVE|DR|ROAD|RD|LANE|LN|CT|COURT|HWGY|HIGHWAY)", "");
					}
					
					if (StringUtils.isNotEmpty(plainText)) {
						ownerName += "<br>" + plainText.replace("Owner Address", "").replaceAll("\\s{2,}", " ").trim();
					} else { 
						adrFlag = false;
					}
					
					resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), ownerName);
					
				} else if (plainText.startsWith("In Care Of Name")) {
					plainText = row.getChildrenHTML();
					plainText = plainText.replaceAll("</?td>", "");
					plainText = plainText.replaceAll("</?b>", "");
					ownerName += "<br>" + plainText.replace("In Care Of Name", "").replaceAll("\\s{2,}", " ").trim();
					resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), ownerName);
					
				} else if(plainText.startsWith("Legal Summary")) {
					plainText = row.getChildrenHTML();
					plainText = plainText.replaceAll("<br>", " ");
					plainText = plainText.replaceAll("</?td>", "");
					plainText = plainText.replaceAll("</?b>", "");
					legal = plainText.replace("Legal Summary", "").replaceAll("\\s{2,}", " ").trim();
					resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal);
					
				} else if(plainText.startsWith("Primary Taxable")) {
					resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), plainText.replace("Primary Taxable", "").replaceAll("[,$]","").trim());
					
				} else if(plainText.startsWith("Actual")) {
					resultMap.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(), plainText.replaceAll("Actual \\(\\d+\\)", "").replaceAll("[,$]","").trim());
				} 
			}
			
			
			
			List<List> body = new ArrayList<List>();
			List<String> line = null;
			HashSet<String> foundBookPage = new HashSet<String>();
			HashSet<String> foundInstrNo = new HashSet<String>();
			NodeList tableSalesHistory = nodeList
				.extractAllNodesThatMatch(new TagNameFilter("table"), true)
//				.extractAllNodesThatMatch(new HasAttributeFilter("class", "accountSummary"));
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "SaleHistoryTable"));
			
			if(tableSalesHistory.size() > 0) {
				TableTag mainTable = (TableTag) tableSalesHistory.elementAt(0);				
				TableRow[] rows = mainTable.getRows();
				
				if (rows.length > 1) {
					for (int i = 1; i < rows.length; i++) {
						TableColumn[] columns = rows[i].getColumns();
						if(columns.length == 8) {
							line = new ArrayList<String>();
							line.add(columns[3].toPlainTextString().replaceAll("[,$]", "").replaceAll("&nbsp;", "").trim()); //Sale price
							line.add(columns[4].toPlainTextString().replaceAll("&nbsp;", "").trim()); //Sale Date
							
							String instrOrBookPage = columns[1].toPlainTextString().replaceAll("&nbsp;", "").trim();
							Matcher matcher = TRANFERS_BOOK_PAGE_PATTERN.matcher(instrOrBookPage);
								
							if(matcher.find()) {
								line.add("");//InstrNo
								line.add(matcher.group(1).replaceAll("^0+", ""));
								line.add(matcher.group(2).replaceAll("^0+", ""));
								foundBookPage.add(matcher.group(0));
								
							} else {
								line.add(instrOrBookPage.replaceAll("^0+", ""));//InstrNo
								line.add("");
								line.add("");
								foundInstrNo.add(instrOrBookPage);
							}
							
							line.add(columns[2].toPlainTextString().replaceAll("&nbsp;", "").trim());
							body.add(line);
						}		
					}
				}
			}
			
			someNodeList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id","ats_all_transfers"), true);
			if(someNodeList.size() > 0) {
				String tmpAllTransfers =  someNodeList.elementAt(0).toPlainTextString().replaceAll("ALL TRANSFERS:", "").replaceAll("&nbsp;", "").trim();
				String[] tmpAllTransfersArray = tmpAllTransfers.split("\\|");
				for (String transferString : tmpAllTransfersArray) {
					transferString = transferString.replaceAll("\\s*:\\s*","").trim();
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
						if(!foundInstrNo.contains(transferString)) {
							foundInstrNo.add(transferString);
							body.add(line);
						}
					}
				}
				
			}
			
			
			ro.cst.tsearch.servers.functions.COFremontAO.parseNames(resultMap, ownerName, adrFlag);
			ro.cst.tsearch.servers.functions.COFremontAO.parseLegalSummary(resultMap, legal);
			
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
	
	
	protected String getSalesHistory(String extractLink) {
		String taxHistoryHtml = getLinkContents(extractLink);
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(taxHistoryHtml, null);
			NodeList divList = htmlParser.parse(null)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "middle"), true)
				.extractAllNodesThatMatch(new TagNameFilter("div"),true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "iDoc"));
			if(divList.size() > 0) {
				taxHistoryHtml = divList.elementAt(0).getChildren().toHtml();
				taxHistoryHtml = taxHistoryHtml.replaceAll("(?is)<!--[^-]+-->", "");
				taxHistoryHtml = taxHistoryHtml.replaceAll("(?is)<(?:no)?script[^>]*>(?:[\\s\\w\\d',\\(\\)\\.{}/\\?=&;]+)?<\\s*/\\s*(?:no)?script>", "");
				taxHistoryHtml = taxHistoryHtml.replaceAll("(?is)</?a[^>]*>", "");
				taxHistoryHtml = taxHistoryHtml.replaceAll("(?is)<(?:\\s*/)?\\s*div[^>]*>", "");
				taxHistoryHtml = taxHistoryHtml.replaceAll("(?is)<(?:\\s*/\\s*)?(?:input|fieldset)[^>]*>", "");
				taxHistoryHtml = taxHistoryHtml.replaceFirst("(?is)<table\\s+id\\s*=\\s*'[^']+'[^>]*>", "<table id=\"SaleHistoryTable\">");
			}
			
			
		} catch (Exception e) {
			logger.error("Error while getting getTaxHistory " + extractLink, e);
		}
		return taxHistoryHtml;
	}
	
	@Override
	protected String getDetails(String rsResponse, StringBuilder accountId) {
		try {
			StringBuilder details = new StringBuilder();
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
			
			/* If from memory - use it as is */
			if(!rsResponse.contains("<html")){
				NodeList headerList = nodeList.extractAllNodesThatMatch(new TagNameFilter("h1"), true);
				if(headerList.size() == 0) {
					return null;
				} else {
					String account = headerList.elementAt(0).toPlainTextString()
						.replace("Account:", "").replace("&nbsp;", "").replaceAll("\\s+", "");
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
					.replace("Account:", "").replace("&nbsp;", "").replaceAll("\\s+", "");
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
			details.append("<table align=\"center\" border=\"1\"><tr><td align=\"center\">")
				.append(headerList.elementAt(0).toHtml())
				.append("</td></tr><tr><td align=\"center\">")
				.append(mainTable.toHtml().replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1"))
				.append("</td></tr>");
			
			NodeList linkList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "left"),true)
				.extractAllNodesThatMatch(new TagNameFilter("a"), true);
			
			DataSite site = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), getServerID());
			String serverHomeLink = site.getServerHomeLink();
			StringBuilder fullTransferList = new StringBuilder();
			StringBuilder detailedSaleHistory = new StringBuilder();
			
			for (int i = 0; i < linkList.size(); i++) {
				LinkTag link = (LinkTag) linkList.elementAt(i);
				String linkPlainText = link.toPlainTextString().trim();
				if(linkPlainText.equals("Tax History")) {
					details.append("<tr><td align=\"center\">")
						.append(getTaxHistory(serverHomeLink + "assessor/taxweb/" + link.extractLink()))
						.append("</td></tr>");
					
				} else if (linkPlainText.equals("Sale History")) {
					detailedSaleHistory.append("<tr><td align=\"center\"> <h4> Sales History </h4> <br>")
						.append(getSalesHistory(serverHomeLink + "assessor/taxweb/" + link.extractLink()))
						.append("</td></tr>");
							
					
				} else if (linkPlainText.startsWith("B:")) {
					Matcher matcher = TRANFERS_BOOK_PAGE_PATTERN.matcher(linkPlainText);
					if(matcher.find()) {
						if(fullTransferList.length() != 0) {
							fullTransferList.append("&nbsp;|&nbsp;");
						}
						fullTransferList.append(linkPlainText);
					}
					
				} else if (linkPlainText.matches("\\d{4,}")) {
					if(fullTransferList.length() != 0) {
						fullTransferList.append("&nbsp;|&nbsp;");
					}
					fullTransferList.append(linkPlainText);
					
				}
			}
			
			if(fullTransferList.length() != 0) {
				details.append("<tr><td align=\"center\" id=\"ats_all_transfers\"> ALL TRANSFERS: ").append(fullTransferList).append("</td></tr>");
				if (detailedSaleHistory != null) {
					details.append(detailedSaleHistory.toString());
				}
			}
			
			details.append("</table>");
			return details.toString();
			
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}	
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();		
		TSServerInfoModule module;
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		
		//search by PIN as Parcel Number
		if(hasPin()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.forceValue(0, "RealAccount");
			module.getFunction(2).setSaKey(SearchAttributes.LD_PARCELNO_GENERIC_AO);
			modules.add(module);
		}
		
		// search by Address
		String strName = getSearchAttribute(SearchAttributes.P_STREETNAME);
		String strNo = getSearchAttribute(SearchAttributes.P_STREETNO);
		if(!isEmpty(strName) && !isEmpty(strNo)){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.forceValue(0, "RealAccount");
			module.getFunction(1).forceValue(strNo);
			module.getFunction(3).forceValue(strName);
			module.addFilter(addressFilter);
			modules.add(module);			
		}
		
		// search by name - filter by address
		if(hasOwner()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.forceValue(0, "RealAccount");
			module.setIteratorType(1,FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			module.addFilter(NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , module));
			if (hasStreet()) {
				module.addFilter(addressFilter);
			}
			
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] {"L;F;","L;M;"});
			module.addIterator(nameIterator);
			
			modules.add(module);			
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
}
