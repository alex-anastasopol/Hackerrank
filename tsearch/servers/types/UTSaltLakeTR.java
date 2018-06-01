package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.Calendar;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.parser.HtmlParser3;

public class UTSaltLakeTR extends TSServer {
	
	private static final long serialVersionUID = 1L;
	
	public UTSaltLakeTR(long searchId) {
		super(searchId);
	}
	
	public UTSaltLakeTR(String rsRequestSolverName, String rsSitePath,
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
			if (rsResponse.indexOf("There were no parcels found with this search criteria") > -1) {
				Response.getParsedResponse().setError("No results found using this search criteria. Please try again.");
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
		case ID_SEARCH_BY_PARCEL:
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:
			//no result
			if (rsResponse.indexOf("ERROR- Parcel number is incorrect.") > -1) {
				Response.getParsedResponse().setError("ERROR- Parcel number is incorrect. Please try again.");
				return;
			}
			if (rsResponse.indexOf("ERROR- Parcel number values must be numeric.") > -1) {
				Response.getParsedResponse().setError("ERROR- Parcel number values must be numeric. Please try again.");
				return;
			}
			
			if (rsResponse.indexOf("ERROR- No Parcel Found.") > -1) {
				Response.getParsedResponse().setError("ERROR- No results found.");
				return;
			}
			
			if (rsResponse.indexOf("ERROR- As of Date must be a valid date.") > -1) {
				Response.getParsedResponse().setError("ERROR- As of Date must be a valid date. Please try again.");
				return;
			}
									
			StringBuilder parcelNumber = new StringBuilder();
			String details = getDetails(rsResponse, parcelNumber);
			String filename = parcelNumber + ".html";
			
			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data);
				if (isInstrumentSaved(parcelNumber.toString(),null,data)){
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
			
		default:
			break;
		}
		
	}
		
	protected void loadDataHash(HashMap<String, String> data) {
		if(data != null) {
			data.put("type","CNTYTAX");
		}
	}
	
	protected String getDetails(String rsResponse, StringBuilder parcelNumber) {
		try {
			
			StringBuilder details = new StringBuilder();
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
			
			/* If from memory - use it as is */
			if(!rsResponse.contains("<html")){
				NodeList parcelList = nodeList.extractAllNodesThatMatch(new TagNameFilter("td"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "width60Percent"));
				String parcelID = parcelList.elementAt(0).toPlainTextString()
					.replace("Parcel Number:", "")
					.replaceAll("-", "")
					.trim();
				parcelNumber.append(parcelID);
				return rsResponse;
			}
			
			NodeList parcelList = nodeList.extractAllNodesThatMatch(new TagNameFilter("td"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "width60Percent"));
			if(parcelList.size() == 0) 
				return null;
			else {
				String parcelID = parcelList.elementAt(0).toPlainTextString()
					.replace("Parcel Number:", "")
					.replaceAll("-", "")
					.trim();
				parcelNumber.append(parcelID);
			}
			
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "taxDue"));
			
			//last three aren't used
			if(tables.size() != 8) { 
				return null;
			}

			details.append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"width: 771px; position: relative;\">")
				.append("<tr>")
				.append("<td colspan=\"2\" valign=\"top\">")
				.append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"width: 760px;\"><tr>")
				.append("<td colspan=\"2\" valign=\"top\" id=\"content1Col\">")
				.append(tables.elementAt(0).toHtml())
				.append("<br><br>")
				.append(tables.elementAt(1).toHtml())
				.append("<br><br>")
				.append(tables.elementAt(2).toHtml())
				.append("<br>")
				.append(tables.elementAt(3).toHtml())
				.append(tables.elementAt(4).toHtml())
				.append("</td></tr></table></td></tr></table>");
			
			return details.toString();
			
		} catch (Exception e) {
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
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id","delinqall"), true);
					
			//if there are results
			if (mainTableList.size() != 0)		
			{
				TableTag tableTag = (TableTag) mainTableList.elementAt(0);
				TableRow[] rows  = tableTag.getRows();
				
				Calendar calendar = Calendar.getInstance();							//get current month, day, year 
				String month = Integer.toString(calendar.get(Calendar.MONTH) + 1);
				String day = Integer.toString(calendar.get(Calendar.DAY_OF_MONTH));
				String year = Integer.toString(calendar.get(Calendar.YEAR));
	
				
				for (int i=2; i<rows.length; i+=2) {
					TableRow row1 = rows[i];
					TableRow row2 = rows[i+1];
					
					String htmlRow1 = row1.toHtml();
					String htmlRow2 = row2.toHtml();
					ParsedResponse currentResponse = new ParsedResponse();
														
					Node parcelNode;
					/*
					//2010 Delinquent Property Taxes - Search by Name
					if (table.contains("2010 Delinquent Property Taxes"))	
						parcelNode = row1.getColumns()[1];
					//Delinquent and Unpaid Taxes from prior years  - Search by Name
					else
					*/
					parcelNode = row1.getColumns()[1].getChild(1);
					String parcelID = parcelNode.toPlainTextString().replaceAll("<?td.*>", "")
							.replaceAll("<?span.*>", "")
							.replaceAll("<?br>", "")
							.replaceAll("\\s","")
							.replaceAll("\\s*\\(.*\\)", "");
										
					String link = createPartialLink(TSConnectionURL.idPOST, TSServerInfo.PARCEL_ID_MODULE_IDX) + 
					"&ParcelNumber=" + parcelID
					+"&asOfDateMM=" + month + "&asOfDateDD=" + day + "&asOfDateYYYY=" + year;
						
					htmlRow1 = htmlRow1.replaceAll(parcelID, "<a href=" + link + ">" + parcelID  + "</a>");
					
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
									
					String htmlRow= "<tr>" + htmlRow1 + htmlRow2 + "</tr>"; 
					
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,htmlRow);
					currentResponse.setOnlyResponse(htmlRow);
					
					ResultMap m = ro.cst.tsearch.servers.functions.UTSaltLakeTR.parseIntermediaryRow( row1, searchId ); 
					Bridge bridge = new Bridge(currentResponse, m, searchId);
					
					DocumentI document = (TaxDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
					 
				}
								
				String only200records = "";
				if (table.indexOf("Only first 200 matching records returned.")>-1) 
					only200records = "<tr align=\"center\"><td colspan=\"7\">Only first 200 matching records returned.</td></tr>";
				String header;
				/*
				//2010 Delinquent Property Taxes - Search by Name
				if (table.contains("2010 Delinquent Property Taxes"))
					header = "<tr><td rowspan=2 width=\"15px\" align=\"center\">#</td>" +
							 "<td rowspan=2><strong>Parcel Number</strong></td>" +
					         "<td colspan=2 bgcolor=\"FFFFCC\"><strong>Owner Name</strong></td>" +
            				 "<td><strong>Cat. Code.</strong></td><td colspan=2>" +
            				 "<strong>Cat. Desc.</strong></td></tr><tr>" +
				  	         "<td  ><strong>Status</strong></td><td><strong>Taxes Due</strong></td>" +
					         "<td><strong>Penalty</strong></td><td><strong>In-Care-Of</strong></td>"+
					         "<td><strong>Pr. Yr. Delq ?</strong></td></tr>";
				//Delinquent and Unpaid Taxes from prior years  - Search by Name
				else
				*/
				header = "<tr align=\"center\"><td>#</td><td class=\"textAlignLeft\" >Parcel #</td>"+
						"<td class=\"textAlignLeft\">Private Sale #</td>"+
						"<td colspan=\"4\"  class=\"textAlignLeft\"  >Owner Name</td></tr>"+
						"<tr align=\"center\"><td><br></td><td class=\"textAlignLeft\">Cat. Code</td>"+
						"<td class=\"textAlignLeft\"  >Cat. Desc.</td><td>Status</td>"+
						"<td>Balance Due</td><td>1st Year Delinq.</td><td>Tax Sale Year</td></tr>";
							
				header = "<table cellspacing=\"0\" id=\"delinqall\" border=\"1\">"+
					only200records + header;
								
				response.getParsedResponse().setHeader(header);
				response.getParsedResponse().setFooter("</tbody></table>");
				
				outputTable.append(table);
			}
			
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}
			
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap) {
		
		try {
		
			resultMap.put("OtherInformationSet.SrcType","TR");
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList parcelList = nodeList.extractAllNodesThatMatch(new TagNameFilter("td"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "width60Percent"));
			String parcelID = parcelList.elementAt(0).toPlainTextString()
				.replace("Parcel Number:", "")
				.replaceAll("-", "")
				.trim();
			
			resultMap.put("PropertyIdentificationSet.ParcelID", parcelID);
						
			String owner = HtmlParser3.getNodeValue(new HtmlParser3(detailsHtml), "Owner:", 0, 0);
			resultMap.put("PropertyIdentificationSet.NameOnServer", 
					owner.replace("<strong>Owner:</strong>", "").trim());
			
			ro.cst.tsearch.servers.functions.UTSaltLakeTR.parseNames(resultMap, searchId);
			ro.cst.tsearch.servers.functions.UTSaltLakeTR.parseTaxes(nodeList, resultMap, searchId);
									
		
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
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			moduleList.add(module);
		}
				
		serverInfo.setModulesForAutoSearch(moduleList);
	}
}
