package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.URI;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
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
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentI.SearchType;

public class FLBrowardAO extends TSServer {
		
	private static final long serialVersionUID = 1728463522132729770L;
	
	public FLBrowardAO(long searchId) {
		super(searchId);
	}
	
	public FLBrowardAO(String rsRequestSolverName, String rsSitePath, String rsServerID, 
			String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, 
				rsPrmNameLink, searchId, miServerID);
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) 
			throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		if (rsResponse.indexOf("No records found.") > -1) {
			Response.getParsedResponse().setError("No data found.");
			Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
			return;
		}
		
		if (rsResponse.indexOf("Name must have at least 3 characters.") > -1) {
			Response.getParsedResponse().setError("Name must have at least 3 characters.");
			return;
		}
		
		if (rsResponse.indexOf("Incorrect Folio length.") > -1) {
			Response.getParsedResponse().setError("Incorrect Folio length.");
			return;
		}
		
		if (rsResponse.indexOf("No records found with subdivision") > -1) {
			Response.getParsedResponse().setError("No data found.");
			return;
		}
		
		if (viParseID==ID_SEARCH_BY_NAME || viParseID==ID_SEARCH_BY_ADDRESS 
			|| viParseID==ID_SEARCH_BY_SUBDIVISION_NAME || viParseID==ID_SEARCH_BY_PARCEL) {
				URI lastURI = Response.getLastURI();
				if (lastURI!=null && lastURI.toString().contains("RecInfo.asp?URL_Folio=")) {
					viParseID = ID_DETAILS;			//if only one result is found, the details are displayed
				}
		}
		
		switch (viParseID) {
								
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_SUBDIVISION_NAME:
		case ID_SEARCH_BY_PARCEL:
			
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
				if(!isParentSite()) {
					smartParseDetails(Response,details);
				} else {
					Response.getParsedResponse().setResponse(details);
				}
				
			} else {
				smartParseDetails(Response,details);
				
				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				
			}
			break;
		
		case ID_GET_LINK :
			ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			break;	
			
		default:
			break;
		}
	}	
		
	protected String getDetails(String rsResponse, StringBuilder parcelNumber) {
		try {
			
			String details = "";
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
									
			/* If from memory - use it as is */
			if(!rsResponse.toLowerCase().contains("<html")){
				NodeList parcelIDNodes = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)	
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "BodyCopyBold9"));
				if (parcelIDNodes.size()>0) {
					parcelNumber.append(parcelIDNodes.elementAt(0).toPlainTextString().replaceAll("(?is)&nbsp;", "").replaceAll("\\s", ""));
				}
				return rsResponse;
			}
			
			NodeList parcelIDNodes = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "BodyCopyBold9"));
			if (parcelIDNodes.size()>0) {
				parcelNumber.append(parcelIDNodes.elementAt(0).toPlainTextString().replaceAll("(?is)&nbsp;", "").replaceAll("\\s", ""));
			}
			
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("td"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("align", "Center"));
			
			if (tables.size()>0) {
				details = tables.elementAt(0).toHtml();
			}
			
			details = details.replaceAll("(?is)<a[^>]+>[^<]*Click[^<]*</a>", "");
			details = details.replaceAll("(?is)\\(See\\s*<a[^>]+>Sketch</a>\\)", "");
			details = details.replaceAll("(?is)or\\s*&nbsp;\\s*<a[^>]+>\\s*Act\\. Year Built</a>", "");
			details = details.replaceAll("(?is)<a[^>]+>(.*?)</a>", "$1");
			details = details.replaceAll("(?is)<img[^>]+>", "");
			details = details.replaceAll("(?is)</SPAN></TD></TR></TABLE>\\z", "");
			details = details.replaceAll("(?is)<script[^>]+>(.*?)</script>", "");
			return details;
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
		
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		TableTag resultsTable = null;
		String hrefPattern = "(?is)href\\s*=\\s*\"([^\"]+)\"";
		boolean isSubdivisionNameIntermediary = table.indexOf("records found with subdivision matching")>-1;
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTable;
			if (isSubdivisionNameIntermediary) {
				mainTable = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("width","575"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("border","2"), true);
			} else {
				mainTable = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id","Table8"), true);
			} 
			
			if (mainTable.size()>0) {
				resultsTable = (TableTag)mainTable.elementAt(0);
			}
			
			if (resultsTable != null) 			
			{
				SearchType searchType = null;
				TableRow[] rows  = resultsTable.getRows();
				int len = rows.length;
				if (isSubdivisionNameIntermediary) {
					len--;
				}		
				for (int i = 1; i < len; i++)			//first row is the header
				{
					TableRow row = rows[i];
										
					String link = "";
					String htmlRow = row.toHtml();
					
					if (isSubdivisionNameIntermediary) {
						String folio = row.getColumns()[0].toPlainTextString().trim();
						link = CreatePartialLink(TSConnectionURL.idPOST) + 
								"RecSearch.asp?SubFolio=" + folio;
					} else {
						Matcher matcher = Pattern.compile(hrefPattern).matcher(row.getColumns()[0].toHtml());
						if (matcher.find()) {
							link = matcher.group(1);
							link = CreatePartialLink(TSConnectionURL.idGET) + link;
						}
					}
					
					htmlRow = htmlRow.replaceAll(hrefPattern, "href=\"" + link + "\"");
					
					ParsedResponse currentResponse = new ParsedResponse();
					
					if (isSubdivisionNameIntermediary) {
						currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_GO_TO_LINK));
					} else {
						currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
					}
									
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,htmlRow);
					currentResponse.setOnlyResponse(htmlRow);
					
					ResultMap m = ro.cst.tsearch.servers.functions.FLBrowardAO.parseIntermediaryRow( row, searchId ); 
					Bridge bridge = new Bridge(currentResponse, m, searchId);
					
					DocumentI document = (AssessorDocumentI)bridge.importData();
					currentResponse.setDocument(document);
					currentResponse.setUseDocumentForSearchLogRow(true);
					
					intermediaryResponse.add(currentResponse);
					
					
					if(searchType == null) {
						searchType = getDocumentSearchType(document, false);
					}
					
					if(searchType != null && document.getSearchType().equals(DocumentI.SearchType.NA)) {
						document.setSearchType(searchType);
					}
				}
				
				String header = "";
				if (isSubdivisionNameIntermediary) {
					header = "<table border=\"1\" align=\"center\">" +
							 "<tr><td align=\"center\"><strong>Folio Number</strong></td>" +
							 "<td align=\"center\"><strong>Subdivision Name</strong></td></tr>";
				} else {
					String headerLinks = "";
					NodeList headerList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("id","Table7"), true);
					if (headerList.size()>0) {
						headerLinks = headerList.elementAt(0).toHtml();
						headerLinks = headerLinks.replaceAll("(?is)<img[^>]+>", "");
						headerLinks = headerLinks.replaceAll("(?is)href\\s*=\\s*\"([^\"]+)\">", "href=\"" + 
								CreatePartialLink(TSConnectionURL.idGET) + "$1\">");
					}
					header = "<table align=\"center\"><tr><td>" + headerLinks + "</td></tr>" +  
							 "<tr><td><table border=\"1\" align=\"center\">" +
							 "<tr><td align=\"center\"><strong>Folio Number</strong></td>" +
							 "<td align=\"center\"><strong>Owner Name</strong></td>" +
							 "<td align=\"center\"><strong>Property Address</strong></td></tr>";
				}
				response.getParsedResponse().setHeader(header);
				
				String footer = "";
				if (isSubdivisionNameIntermediary) {
					footer = "</table>";
				} else {
					String footerLinks = "";
					NodeList footerList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("id","Table9"), true);
					if (footerList.size()>0) {
						footerLinks = footerList.elementAt(0).toHtml();
						footerLinks = footerLinks.replaceAll("(?is)<img[^>]+>", "");
						footerLinks = footerLinks.replaceAll("(?is)href\\s*=\\s*\"([^\"]+)\">", "href=\"" + 
								CreatePartialLink(TSConnectionURL.idGET) + "$1\">");
					}
					footer = "</table></td></tr><tr><td>" + footerLinks + "</td></tr></table>"; 
				}
				response.getParsedResponse().setFooter(footer);
												
				outputTable.append(table);
			}
			
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,	String detailsHtml, ResultMap resultMap) {
				
		try {
					
			detailsHtml = Tidy.tidyParse(detailsHtml, null);
			
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"AO");
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList parcelTableNodes = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "155"));
			if (parcelTableNodes.size()>0) {
				TableTag table = (TableTag)parcelTableNodes.elementAt(0);
				if (table.getRowCount()>0) {
					TableRow row = table.getRow(0);
					if (row.getColumnCount()>1) {
						resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), 
								row.getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", "")
									.replaceAll("\\s", ""));
					}
				}
			}
			
			NodeList addressOwnerTableNodes = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "450"));
			if (addressOwnerTableNodes.size()>0) {
				TableTag table = (TableTag)addressOwnerTableNodes.elementAt(0);
				if (table.getRowCount()>0) {
					TableRow row = table.getRow(0);
					if (row.getColumnCount()>1) {
						resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), 
								row.getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", " ")
									.replaceAll("\\s{2,}", " ").trim());
					}
				}
				if (table.getRowCount()>1) {
					TableRow row = table.getRow(1);
					if (row.getColumnCount()>1) {
						resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), 
								row.getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", " ")
									.replaceAll("%", " ").replaceAll("[\r\n]+", "<br>")
									.replaceAll("\\s{2,}", " ").trim());
					}
				}
			}
			
			NodeList legalTableNodes = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "612"));
			if (legalTableNodes.size()>0) {
				TableTag table = (TableTag)legalTableNodes.elementAt(0);
				if (table.getRowCount()>0) {
					TableRow row = table.getRow(0);
					if (row.getColumnCount()>1) {
						String legal = row.getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", " ")
								.replaceAll("\\s{2,}", " ").trim(); 
						resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal);
					}
				}
			}
			
			for (int i=1;i<legalTableNodes.size();i++) {
				if (legalTableNodes.elementAt(i).toPlainTextString().toLowerCase().contains("property assessment values")) {
					TableTag table = (TableTag)legalTableNodes.elementAt(i);
					if (table.getRowCount()>2) {
						TableRow row = table.getRow(2);
						if (row.getColumnCount()>1) {
							resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), 
									row.getColumns()[1].toPlainTextString().replaceAll("[,$]", "").replaceAll("(?is)&nbsp;", "").trim());
						}
						if (row.getColumnCount()>2) {
							resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), 
									row.getColumns()[2].toPlainTextString().replaceAll("[,$]", "").replaceAll("(?is)&nbsp;", "").trim());
						}
						if (row.getColumnCount()>3) {
							resultMap.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(), 
									row.getColumns()[3].toPlainTextString().replaceAll("[,$]", "").replaceAll("(?is)&nbsp;", "").trim());
						}
						if (row.getColumnCount()>4) {
							resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), 
									row.getColumns()[4].toPlainTextString().replaceAll("[,$]", "").replaceAll("(?is)&nbsp;", "").trim());
						}
					}
					break;
				}
			}
			
			NodeList salesTableNodes = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "352"));
			if (salesTableNodes.size()>0) {
				TableTag table = (TableTag)salesTableNodes.elementAt(0);
				List<List> tablebody = null;
				ResultTable salesHistory = new ResultTable();
				tablebody = new ArrayList<List>();
				List<String> list;
				for (int i=2;i<table.getRowCount(); i++)
				{
					TableRow row = table.getRow(i);
					if (row.getColumnCount()>=5) {
						list = new ArrayList<String>();
						list.add(row.getColumns()[0].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim());	//date
						list.add(row.getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim());	//type
						list.add(row.getColumns()[2].toPlainTextString().replaceAll("(?is)&nbsp;", "")
								.replaceAll("[,\\$]", "").trim());												//price
						list.add(row.getColumns()[3].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim());	//book
						list.add(row.getColumns()[4].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim());	//page
						boolean listIsEmpty = true;
						for (int j=0;j<list.size();j++) {
							if (StringUtils.isNotEmpty(list.get(j))) {
								listIsEmpty = false;
								break;
							}
						}
						if (!listIsEmpty) {
							tablebody.add(list);
						}
					}
				}
				
				String[] header = {SaleDataSetKey.RECORDED_DATE.getShortKeyName(), SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(), 
						SaleDataSetKey.SALES_PRICE.getShortKeyName(), SaleDataSetKey.BOOK.getShortKeyName(), 
						SaleDataSetKey.PAGE.getShortKeyName()};
				salesHistory = GenericFunctions2.createResultTable(tablebody, header);
				if (salesHistory != null && tablebody.size()>0){
					resultMap.put("SaleDataSet", salesHistory);
				}
			}
			
			ro.cst.tsearch.servers.functions.FLBrowardAO.parseNames(resultMap, searchId);
			ro.cst.tsearch.servers.functions.FLBrowardAO.parseAddress(resultMap);
			ro.cst.tsearch.servers.functions.FLBrowardAO.parseLegalSummary(resultMap, searchId);
		
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
		
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;

		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		
		if(searchType == Search.AUTOMATIC_SEARCH) {
		
			FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);
			
			if (hasPin()) {
				String parcelID = getSearchAttribute(SearchAttributes.LD_PARCELNO);
				parcelID = parcelID.replaceAll("\\s", "");
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.forceValue(0, parcelID);
				if (isUpdate()) {
					module.addFilter(addressFilter);
				}
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
				moduleList.add(module);
			}
	
			if (hasOwner()) {
				FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory
						.getHybridNameFilter(SearchAttributes.OWNER_OBJECT,	searchId, module);
				nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
				module.addFilter(addressFilter);
				module.addFilter(nameFilterHybridDoNotSkipUnique);
				module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" }));
				moduleList.add(module);
			}
		
		}

		serverInfo.setModulesForAutoSearch(moduleList);
	}
}
