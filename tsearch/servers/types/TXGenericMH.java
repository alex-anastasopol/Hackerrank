package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
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
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.ManufacturedHousingDocument;

public class TXGenericMH extends TSServer {
	
	static final long serialVersionUID = 5785577994850303L;
	
	public TXGenericMH(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	public TXGenericMH(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch (viParseID) {
		case ID_SEARCH_BY_NAME:				//current records search
		case ID_SEARCH_BY_ADDRESS:			//archived records search
			
			if (rsResponse.indexOf("No records were found that match your selection criteria.") > -1) {
				Response.getParsedResponse().setError("<font color=\"red\">No results found.</font>");
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
			
			StringBuilder serialNumber = new StringBuilder();
			String details = getDetails(rsResponse, serialNumber);
			String filename = serialNumber + ".html";
			
			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type","MISCELLANEOUS");
				if (isInstrumentSaved(serialNumber.toString(),null,data)){
					details += CreateFileAlreadyInTSD();
				}
				else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					//don't allow saving for documents with invalid Certificate Number
					if (serialNumber.toString().matches("0+_.*"))	
						details += "Invalid Certificate Number";
					else details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
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
			if (sAction.contains("title_detail.jsp") || sAction.contains("dmv_detail.jsp"))		//details page for current and archived records, respectively
				ParseResponse(sAction, Response, ID_DETAILS);
			else if (sAction.contains("dmv_list.jsp"))
				ParseResponse(sAction, Response, ID_SEARCH_BY_ADDRESS);							//archived records intermediary page
			else if (sAction.contains("title_list.jsp"))
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);							//current records intermediary page
			break;	
		
		default:
			break;
		}
	}	
	
	private void getParcelNumber(NodeList nodeList, String s, StringBuilder  parcelNumber, StringBuilder date) {
		
		String parcelID = "";
		boolean isArchived = false;
		if (s.indexOf("archived records")!=-1) 
			isArchived = true;
		Matcher matcher = Pattern.compile("(?is)Certificate Detail for Certificate #\\s([^\\s]+)").matcher(s);
		if (matcher.find()) parcelID = matcher.group(1);
		if (parcelID.matches("0+") || parcelID.matches("X+")) { 	//invalid Certificate #  
			if (!isArchived) {										//parse Label instead
				NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
					.extractAllNodesThatMatch(new HasAttributeFilter("summary", "section information"));
				TableTag table = null;
				if (tables.size()>0) {
					table = (TableTag)tables.elementAt(0);
					parcelID = table.getRows()[2].getColumns()[1].toPlainTextString();
				}
			} else {												//parse Vehicle ID instead
				NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "90%"));
				TableTag table = null;
				if (tables.size()>0) {
					table = (TableTag)tables.elementAt(0);
					parcelID = table.getRows()[0].getColumns()[1].toPlainTextString();
				}
			}
		}		
		parcelNumber.append(parcelID);
		if (!isArchived) {
			matcher = Pattern.compile("(?is)ISSUE DATE:.*?(\\d{1,2}/\\d{1,2}/\\d{2}(?:\\d\\d)?)").matcher(s);
			if (matcher.find()) date.append(matcher.group(1));
		} else {
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "90%"));
			TableTag table = null;
			if (tables.size()>0) {
				table = (TableTag)tables.elementAt(0);
				date.append(table.getRows()[2].getColumns()[1].toPlainTextString());
			}	
		}
		parcelNumber.append("_");
		parcelNumber.append(date.toString().replaceAll("/", ""));
	}
	
	protected String getDetails(String rsResponse, StringBuilder parcelNumber) {
		try {
			
			StringBuilder details = new StringBuilder();
			StringBuilder date = new StringBuilder();
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
									
			/* If from memory - use it as is */
			if(!rsResponse.toLowerCase().contains("<html")){
				getParcelNumber(nodeList, rsResponse, parcelNumber, date);
				return rsResponse;
			}
			
			getParcelNumber(nodeList, rsResponse, parcelNumber, date);
			
			String det = "";
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("style", "padding-left:10px"));
			if (tables.size()>0)
				det = tables.elementAt(0).toHtml();
			
			NodeList linkList = nodeList.extractAllNodesThatMatch(new TagNameFilter("a"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "dataLink"));
			LinkTag linkTag;
			String taxLienDetails = "";
			if (linkList.size()>0) {
				linkTag = (LinkTag)linkList.elementAt(0);
				HttpSite taxLien = HttpManager.getSite(getCurrentServerName(), searchId);	
				try {
					taxLienDetails = ((ro.cst.tsearch.connection.http2.TXGenericMH)taxLien)
						.getPage("http://mhweb.tdhca.state.tx.us/mhweb/" + linkTag.getLink());
				} catch(Exception e) {
					e.printStackTrace();
				} finally {
					HttpManager.releaseSite(taxLien);
				}
				
				htmlParser = org.htmlparser.Parser.createParser(taxLienDetails, null);
				nodeList = htmlParser.parse(null);
				NodeList div = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
				 	.extractAllNodesThatMatch(new HasAttributeFilter("style", "padding-left:10px"));	
				if (nodeList.size()>0) det += div.elementAt(0).toHtml();
			}
			
			det = det.replaceFirst("(?is)Printable Version of this Page\\.", "");
			det = det.replaceFirst("(?is)You may contact.*payment is received\\.", "");
			det = det.replaceFirst("(?is)Check Tax Lien Status", "See below");
			det = det.replaceFirst("(?is)Application Received\\..*?\\(Click for Status\\.\\)", "");
			det = det.replaceFirst("(?is)Click on County Code.*in printable form\\.", "");
			det = det.replaceAll("(?is)Questions or comments, please call 1-800-500-7074", "");
			det = det.replaceAll("(?is)Search Again", "");
			det = det.replaceAll("(?is)<a.*?>", "");
			
			details.append(det);
			
			return details.toString();
			
		} catch (ParserException e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	protected String createPartialLink(int iActionType, int dispatcher) {
		StringBuilder build = new StringBuilder(msRequestSolverName);
		build.append("?");
		build.append(msServerID);
		build.append("&");
		build.append(RequestParams.SEARCH_ID);
		build.append("=");
		build.append(mSearch.getSearchID());
		build.append("&");
		build.append("dispatcher=");
		build.append(dispatcher);
		return build.toString();
	}
		
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		TableTag resultsTable = null;
		String header = "";
		String footer = "";
		boolean hasAddress = false;
		boolean isArchived = false;
		
		if (table.indexOf("archived records")!=-1) isArchived = true;
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(Tidy.tidyParse(table, null), null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTable = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width","98%"), true);
				
			if (mainTable.size() != 0)
				resultsTable = (TableTag)mainTable.elementAt(0); 
			
			//if there are results
			if (resultsTable != null && resultsTable.getRowCount() != 0)		
			{
				TableRow[] rows  = resultsTable.getRows();
				if (rows[1].getColumnCount()==5) {		//search criteria include address
					hasAddress = true;
				}
				
				//row 0 is the header
				for (int i = 1; i < rows.length; i++) {
					TableRow row = rows[i];
										
					String htmlRow = row.toHtml();
					ParsedResponse currentResponse = new ParsedResponse();
					
					// Parse the link.
					String link = "#";
					for (TableColumn column : row.getColumns()) {
						// Check the first column that contains a link.
						if (column.toHtml().contains("href")) {
							// Parse the link and make it internal.
							Matcher matcher = Pattern.compile("(?is)href=\"(.*?)\"").matcher(column.toHtml());
							if (matcher.find())
								link = CreatePartialLink(TSConnectionURL.idGET) + matcher.group(1).trim(); 
							break;
						}
					}
					
					// Replace the links.
					htmlRow = htmlRow.replaceAll("(?is)href=\".*?\"", "href=" + link);
										
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
									
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,htmlRow);
					currentResponse.setOnlyResponse(htmlRow);
					
					ResultMap m = ro.cst.tsearch.servers.functions.TXGenericMH.parseIntermediaryRow( row, searchId, isArchived ); 
					Bridge bridge = new Bridge(currentResponse, m, searchId);
					
					DocumentI document = (ManufacturedHousingDocument)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				}
				
				NodeList numberList = nodeList.extractAllNodesThatMatch(new TagNameFilter("H2"), true);
				if (numberList.size()!=0) header = numberList.elementAt(0).toHtml(); 	
				
				if (isArchived) {
					header += "<table border=\"1\" cellspacing=\"0\" cellpadding=\"2\" width=\"98%\" align = \"center\" class=\"bgTable\"" +
			          "<tr><th align=\"left\" scope=\"col\">VEHICLE ID</th>" +
			          "<th align=\"left\" scope=\"col\">CERTIFICATE NUMBER</th>" +
			          "<th align=\"left\" scope=\"col\">CERTIFICATE DATE</th>" +	   
			          "<th align=\"left\" scope=\"col\">OWNER</th>" +	
			          "</tr>";
				} 
				else {
					if (hasAddress) 
						header += "<table border=\"1\" cellspacing=\"0\" cellpadding=\"2\" width=\"98%\" align = \"center\" class=\"bgTable\"" +
				          		 "<tr><th align=\"left\" scope=\"col\">LABEL/SEAL</th>" +
				          		 "<th align=\"left\" scope=\"col\">SERIAL</th>" +
				          		 "<th align=\"left\" scope=\"col\">OWNER</th>" +	   
				          		 "<th align=\"left\" scope=\"col\">CERTIFICATE #</th>" +
				          		 "<th align=\"left\" scope=\"col\">ADDRESS</th>" +
				          		 "</tr>";
					else 
						header += "<table border=\"1\" cellspacing=\"0\" cellpadding=\"2\" width=\"98%\" align = \"center\" class=\"bgTable\"" +
						          "<tr><th align=\"left\" scope=\"col\">LABEL/SEAL</th>" +
						          "<th align=\"left\" scope=\"col\">SERIAL</th>" +
						          "<th align=\"left\" scope=\"col\">OWNER</th>" +	   
						          "<th align=\"left\" scope=\"col\">CERTIFICATE #</th>" +	
						          "</tr>";
				}
				
				footer = processLinks(response,nodeList);
				response.getParsedResponse().setHeader(header);
				response.getParsedResponse().setFooter("</table>" + footer);
																
				outputTable.append(table);
			}
			
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}

	protected String processLinks(ServerResponse response, NodeList nodeList) {
		
		String links = "";
		NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("cellspacing", "4"));
		if(tableList.size() > 0	)
			links =  tableList.elementAt(0).toHtml()
				.replaceAll("(?is)href=\"([^\"]+)\"", "href=\"" + CreatePartialLink(TSConnectionURL.idGET) + "$1\"");
		return links;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap) {
	
		try {
						
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"MH");
			resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "MANUFACTURED HOUSING");
				
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			StringBuilder parcelNumber = new StringBuilder();
			StringBuilder date = new StringBuilder();
			
			getParcelNumber(nodeList, detailsHtml, parcelNumber, date);
			
			resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), parcelNumber.toString());
			resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), date.toString());
			
			if (!detailsHtml.contains("archived records")) { //current records
				
				NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "90%"));
				if (tableList.size()>0) {
					TableTag table = (TableTag)tableList.elementAt(0);
					String instrumentDate = table.getRow(1).getColumns()[0].toPlainTextString();
					if (instrumentDate.length()!=0) resultMap.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), instrumentDate);
				}
				
				NodeList ownersList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("summary", "Owner Information"));
				if (ownersList.size()>0) {
					TableTag table = (TableTag)ownersList.elementAt(0);
					String tmpGrantor = table.getRow(2).getColumns()[1].toHtml()
						.replaceAll("(?is)</?td.*?>", "").replaceAll("(?is)<br\\s*/>", "<br/>");
					resultMap.put("tmpGrantor", tmpGrantor);
					String tmpGrantee = table.getRow(2).getColumns()[0].toHtml()
						.replaceAll("(?is)</?td.*?>", "").replaceAll("(?is)<br\\s*/>", "<br/>");
					resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), tmpGrantee);
				}
				
				NodeList addressList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("summary", "manufacturer information"));
				if (addressList.size()>0) {
					TableTag table = (TableTag)addressList.elementAt(0);
					if (table.toHtml().contains("Location of Home")) {
						String address = table.getRow(1).getColumns()[0].toPlainTextString();
						resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
					}
				}
				
				ro.cst.tsearch.servers.functions.TXGenericMH.parseNames(resultMap,false);
				ro.cst.tsearch.servers.functions.TXGenericMH.parseAddress(resultMap);
				
			} else {										//archived records
				
				NodeList ownersList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("class", "bgTable"))
						.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"));
				if (ownersList.size() > 0) {
					TableTag table = (TableTag) ownersList.elementAt(0);
					String tmpGrantee = table.getRow(2).getColumns()[0].toHtml()
						.replaceAll("(?is)</?td.*?>", "").replaceAll("(?is)<br\\s*/>", "<br/>");
					resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), tmpGrantee);
					
					ro.cst.tsearch.servers.functions.TXGenericMH.parseNames(resultMap,true);
				}
			}
			
		} catch (ParserException e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
		
		TSServerInfoModule module = null;
		
		boolean hasOwner = hasOwner();
								
		if(hasOwner) {
			{	//person name
				FilterResponse nameFilterOwner = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
				nameFilterOwner.setSkipUnique(false);
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				module.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				module.addFilter(nameFilterOwner);
				ConfigurableNameIterator nameIterator = new ConfigurableNameIterator(searchId, new String[]{"L;F;","L;M;"});
				nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
				nameIterator.setInitAgain(true);
				module.addIterator(nameIterator);
				moduleList.add(module);
			}
			{	//company name
				FilterResponse nameFilterOwner = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
				nameFilterOwner.setSkipUnique(false);
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
				module.addFilter(nameFilterOwner);
				ConfigurableNameIterator nameIterator = new ConfigurableNameIterator(searchId, new String[]{"L;F;"});
				nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.COMPANY_NAME);
				nameIterator.setInitAgain(true);
				module.addIterator(nameIterator);
				moduleList.add(module);
			}
		}
				
		serverInfo.setModulesForAutoSearch(moduleList);
	}
	
}
