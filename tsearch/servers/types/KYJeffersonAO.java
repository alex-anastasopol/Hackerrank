package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.HeadingTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.functions.KYJeffersonAOFunctions;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;

public class KYJeffersonAO extends TSServer{

	
	private static final long serialVersionUID = 4982958486704473651L;
	
	public KYJeffersonAO(
			String rsRequestSolverName,
			String rsSitePath,
			String rsServerID,
			String rsPrmNameLink, long searchId, int mid) {
			super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	/**
	 * 
	 */
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {        
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
        
		TSServerInfoModule m;
        FilterResponse adressFilter 	= AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d , true);
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , null );
		
        // search by parcel ID
		if( hasPin() ){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			m.setSaKey(0, SearchAttributes.LD_PARCELNO);
			l.add(m);
		}
        
        // search by address - str
		if( hasStreet() ){
			
			if(hasStreetNo()) {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
				m.setSaKey(0, SearchAttributes.P_STREET_NO_NAME);
				m.addFilter(adressFilter);
				m.addFilter(nameFilterHybrid);
				l.add(m);
			} 
			
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			m.setSaKey(0, SearchAttributes.P_STREETNAME);
			m.addFilter(adressFilter);
			m.addFilter(nameFilterHybrid);
			l.add(m);
		}
		
		// search by owner
		if( hasOwner()){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.addFilter(adressFilter);
			m.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims( SearchAttributes.OWNER_OBJECT, searchId, m));
						m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(m, searchId, new String[] {"L;%F;","L;%M;"});
			m.addIterator(nameIterator);
			
			l.add(m);
		}
		
        serverInfo.setModulesForAutoSearch(l);	
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponce = Response.getResult();
        ParsedResponse parsedResponse = Response.getParsedResponse();
    	
    	
    	switch (viParseID) {
        case ID_SEARCH_BY_ADDRESS:
        case ID_SEARCH_BY_NAME:
        case ID_SEARCH_BY_PARCEL:
        case ID_SEARCH_BY_SUBDIVISION_NAME:
        case ID_SEARCH_BY_SERIAL_ID:
        case ID_SEARCH_BY_SALES:        	
        	
        	// no matches
            if(rsResponce.indexOf("<td>Your search has produced zero records. Please try your search again.</td>") != -1){
            	return;
            }
        	
            
            if(!rsResponce.contains("<h1>Property Listings</h1>") && rsResponce.toUpperCase().indexOf("PROPERTY DETAILS") != -1 && rsResponce.toUpperCase().indexOf("ASSESSED VALUE") != -1){
            	ParseResponse(sAction, Response, ID_DETAILS);
            	return;
            }
        	
            
            StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponce, outputTable, viParseID);
			
			if(smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
            }
			
			break;
            
        case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			StringBuilder accountId = new StringBuilder();
			String details = getDetails(rsResponce, accountId);
			if(StringUtils.isEmpty(details)) {
				Response.getParsedResponse().setError("Detailed page is not available. Probably a site or login problem.");
				return;
			}
			String filename = accountId + ".html";
			
			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type","ASSESSOR");
				if (isInstrumentSaved(accountId.toString(),null,data)){
					details += CreateFileAlreadyInTSD();
				} else {
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
			if(rsResponce.contains("<h1>Property Listings</h1>")) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_ADDRESS);
			} else if(rsResponce.toUpperCase().indexOf("PROPERTY DETAILS") != -1 && rsResponce.toUpperCase().indexOf("ASSESSED VALUE") != -1) {
				ParseResponse(sAction, Response, ID_DETAILS);
			}
			break;
        }
	}
	
	private String getDetails(String rsResponse, StringBuilder accountId) {
		try {
			
			StringBuilder details = new StringBuilder();
			HtmlParser3 parser = new HtmlParser3(rsResponse);

			TableTag tableRight = (TableTag) parser.getNodeByTypeAndAttribute("table", "class", "tableRight", true);
			if(tableRight == null) {
				// the account might be inactive (Task 8718)
				org.htmlparser.Parser parser1 = org.htmlparser.Parser.createParser(rsResponse, null);
				NodeList nodeList = parser1.parse(null);
				
				// address
				Matcher m = Pattern.compile("(?is)<h1>[^<>]*?</h1>").matcher(rsResponse);
				if(m.find()) {
					details.append(m.group());
				}
				
				TableTag table = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "property_data"), true).elementAt(0);
				if(table != null) {
					accountId.append(table.getRow(1).getColumns()[1].toPlainTextString().trim());
					if(!rsResponse.contains("<html")) {
						return rsResponse;
					}

					details.append(table.toHtml().replaceAll("(?is)<a [^>]*>.*?</a>", ""));
					
					return details.toString();
				} else {
					logger.error("Site is not logged in");
					return null;
				}
			}
			tableRight.setAttribute("id", "ats_tableRight");
			
			for (TableRow row : tableRight.getRows()) {
				TableColumn[] columns = row.getColumns();
				if(columns[0].toPlainTextString().trim().equalsIgnoreCase("Parcel ID")){
					details.append("<input type=\"hidden\" id=\"ats_parcelId\" name=\"ats_parcelId\" value=\"" + 
							columns[1].toPlainTextString().trim() + "\">");
					accountId.append(columns[1].toPlainTextString().trim());	
				}
				
			}
			
			/* If from memory - use it as is */
			if(!rsResponse.contains("<html")){
				Node node = parser.getNodeById("ats_parcelId");
				if(node == null) {
					return null;
				} else {
					accountId.append(((InputTag)node).getAttribute("value"));	
				}
				return rsResponse;
			}
			
			TableTag tableLeft = (TableTag) parser.getNodeByTypeAndAttribute("table", "class", "tableLeft", true);
			tableLeft.setAttribute("id", "ats_tableLeft");
			
			TableTag tableSalesHistory = (TableTag) parser.getNodeById("tableSalesHistory");
			TableTag tableAssessmentHistory = (TableTag) parser.getNodeById("tableAssessmentHistory");
			TableTag tableLegalLines = (TableTag) parser.getNodeById("tableLegalLines");
			
			NodeList headerList = parser.getNodeById("container").getChildren().extractAllNodesThatMatch(new TagNameFilter("h1"));
			((HeadingTag)headerList.elementAt(0)).setAttribute("align", "center");
			details.append(headerList.elementAt(0).toHtml());
			details.append("<input type=\"hidden\" id=\"ats_address\" name=\"ats_address\" value=\"" + 
					headerList.elementAt(0).toPlainTextString().trim() + "\">");
			
			details.append("<table align=\"center\" border=\"1\"><tr><td>")
				.append(tableRight.toHtml().replaceAll("<a[^>]+>([^<]+)<\\s*/\\s*a\\s*>", "$1"))
				.append("</td><td>")
				.append(tableLeft.toHtml())
				.append("</td></tr>");
			
			if(tableSalesHistory != null) {
				details
					.append("<tr><td colspan=\"2\">")
					.append(tableSalesHistory.toHtml().replaceAll("<a[^>]+>([^<]+)<\\s*/\\s*a\\s*>", "$1"))
					.append("</td></tr>");
				
			}
			if(tableAssessmentHistory != null) {
				details
					.append("<tr><td colspan=\"2\">")
					.append(tableAssessmentHistory.toHtml())
					.append("</td></tr>");
			}
			if(tableLegalLines != null) {
				details
					.append("<tr><td colspan=\"2\">")
					.append(tableLegalLines.toHtml())
					.append("</td></tr>");
			}
			details.append("</table>");
			
			return details.toString();
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap map) {
		try {
			HtmlParser3 parser = new HtmlParser3(detailsHtml);
			
			map.put("OtherInformationSet.SrcType", "AO");
			Node ats_parcelId = parser.getNodeById("ats_parcelId");
			
			if(ats_parcelId == null) {
				// the account might be inactive (Task 8718)
				org.htmlparser.Parser parser1 = org.htmlparser.Parser.createParser(detailsHtml, null);
				NodeList nodeList = parser1.parse(null);
				
				// address
				Matcher m = Pattern.compile("(?is)<h1>([^<>]*?)</h1>").matcher(detailsHtml);
				if(m.find()) {
					String address = m.group(1);
					map.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
					map.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
				}
				
				TableTag table = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "property_data"), true).elementAt(0);
				if(table != null) {
					TableRow[] rows = table.getRows();
					
					for(TableRow row : rows) {
						if(row.getColumnCount() >= 2) {
							String col1 = row.getColumns()[0].toPlainTextString().trim().toUpperCase();
							String col2 = row.getColumns()[1].toPlainTextString().trim().replaceAll("(?is)&amp;", "&");
							
							if(col1.contains("OWNER")) {
								map.put(PropertyIdentificationSetKey.OWNER_LAST_NAME.getKeyName(), col2);
							} else if(col1.contains("PARCEL ID")) {
								map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), col2);
							} else if(col1.contains("ASSESSED VALUE")) {
								map.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), col2.replaceAll("[$,]", ""));
							}
						}
					}
				}
			} else {
				map.put("PropertyIdentificationSet.ParcelID", ((InputTag)ats_parcelId).getAttribute("value"));
				Node ats_address = parser.getNodeById("ats_address");
				String addressOnServer = KYJeffersonAOFunctions.cleanAddress(((InputTag)ats_address).getAttribute("value"));
				map.put("PropertyIdentificationSet.AddressOnServer", addressOnServer);
				map.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(addressOnServer.trim()));
				map.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(addressOnServer.trim()));
				
				KYJeffersonAOFunctions.parseLeftTable(map, parser.getNodeById("ats_tableLeft"));
				KYJeffersonAOFunctions.parseRightTable(map, parser.getNodeById("ats_tableRight"));
				KYJeffersonAOFunctions.parseSalesHistoryTable(map, parser.getNodeById("tableSalesHistory"));
				KYJeffersonAOFunctions.parseAssessmentHistoryTable(map, parser.getNodeById("tableAssessmentHistory"));
				KYJeffersonAOFunctions.parseLegalLinesTable(map, parser.getNodeById("tableLegalLines"));
				
				GenericFunctions.legalJeffersonAO(map, searchId);
			}
			
			GenericFunctions.partyNamesKYJeffersonAO(map, searchId);
			
		} catch (Exception e) {
			logger.error("Problem while parsing detailed document", e);
		}
		
		return null;
	}


	
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable, int viParseID) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			HtmlParser3 parser = new HtmlParser3(table);
			TableTag tableTag = (TableTag) parser.getNodeByAttribute("class", "searchResultsTable", true);
			if(tableTag == null) {
				return intermediaryResponse;
			}
			
			TableRow[] rows  = tableTag.getRows();
			String footer = "";
			String linkPrefix = CreatePartialLink(TSConnectionURL.idGET);
			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				if(row.getColumnCount() == 5) {
					
					ResultMap m = KYJeffersonAOFunctions.parseIntermediaryRow( row, linkPrefix, searchId ); 
					
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(row.toHtml());
					String link = (String) m.get("tmpLink");
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
					
					m.removeTempDef();
					Bridge bridge = new Bridge(currentResponse, m, searchId);
					
					DocumentI document = (AssessorDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				} 
			}
			
			
			String headerStart = "";
			NodeList header3List = parser.getNodeList()
				.extractAllNodesThatMatch(new TagNameFilter("div"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "container"))
				.extractAllNodesThatMatch(new TagNameFilter("h3"), true);
			if(header3List.size() > 0) {
				HeadingTag h3Tag = (HeadingTag) header3List.elementAt(0);
				headerStart = h3Tag.toHtml();
			}
			
			
			
			footer = processLinks(response,parser, linkPrefix);
			
			if(viParseID == ID_SEARCH_BY_SALES) {
				response.getParsedResponse().setHeader(headerStart + "<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + 
					"<tr><th>Address / Owner</th><th>Sales Price / Date</th><th>Type</th><th>Parcel ID</th></tr>");
			} else {
				response.getParsedResponse().setHeader(headerStart + "<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + 
						"<tr><th>Address</th><th>Owner</th><th>Type</th><th>Parcel ID</th></tr>");
			}
			
			response.getParsedResponse().setFooter(footer + "</table>");
			
			
			outputTable.append(table);
			
			
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
		
	}

	private String processLinks(ServerResponse response, HtmlParser3 parser, String linkPrefix) {
		NodeList divPaginationList = parser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("div"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("class", "pagination"));
		if(divPaginationList.size() > 0) {
			Div divPagination = (Div) divPaginationList.elementAt(0);
			NodeList linkList = divPagination.getChildren();
			if(linkList != null && linkList.size() > 0) {
				String result = "<tr><td colspan=\"4\">";
				for (int i = 0; i < linkList.size(); i++) {
					Node node = linkList.elementAt(i);
					if(node instanceof LinkTag) {
						LinkTag linkTag = (LinkTag) linkList.elementAt(i);
						linkTag.setLink(linkPrefix + "/property-search/property-listings/" + linkTag.extractLink().trim().replaceAll("\\s", "%20").replaceAll("&amp;", "&"));
						if(linkTag.toPlainTextString().contains("Next")) {
							response.getParsedResponse().setNextLink("<a href=" + linkTag.extractLink()+ ">Next</a>");
						}
					}
					result += node.toHtml() + "&nbsp;&nbsp;";
				}
				return result + "</td></tr>";
			}
			return divPagination.toHtml();
		}
		return "";
	}


	/**
	 * 
	 * @param p
	 * @param pr
	 * @param htmlString
	 * @param pageId
	 * @param linkStart
	 * @param action
	 * @throws ro.cst.tsearch.exceptions.ServerResponseException
	 */
    public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart,
            int action) throws ro.cst.tsearch.exceptions.ServerResponseException {
        
        p.splitResultRows(
                pr,
                htmlString,
                pageId,
                "<tr",
                "</tr>", linkStart, action);
    }
    

}
