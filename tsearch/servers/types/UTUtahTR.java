package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.CityFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.RequestParams;


public class UTUtahTR extends TSServer {

	private static final long serialVersionUID = 1L;
	
	public UTUtahTR(long searchId) {
		super(searchId);
	}
	
	public UTUtahTR(String rsRequestSolverName, String rsSitePath,
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
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_PARCEL:
		case ID_SEARCH_BY_MODULE22:			//Delinquent Tax Search
			
			if (rsResponse.indexOf("0 records found") > -1) {
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
				loadDataHash(data);
				if (isInstrumentSaved(serialNumber.toString(),null,data)){
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
			ParseResponse(sAction, Response, rsResponse.contains("Next") || rsResponse.contains("Previous") 
					? ID_SEARCH_BY_NAME								//for pagination in intermediary results
					: ID_DETAILS);
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
				NodeList parcelList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "758"))
					.extractAllNodesThatMatch(new TagNameFilter("td"), true);
				String parcelID = parcelList.elementAt(0).toPlainTextString()
					.replaceAll("Serial Number:&nbsp;", "")
					.replaceAll(":", "")
					.trim();
				parcelNumber.append(parcelID);
								
				return rsResponse;
			}
			
			NodeList parcelList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "758"))
				.extractAllNodesThatMatch(new TagNameFilter("td"), true);
			String parcelID = parcelList.elementAt(0).toPlainTextString()
				.replaceAll("Serial Number:&nbsp;", "")
				.replaceAll(":", "")
				.trim();
			parcelNumber.append(parcelID);
			
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "758"));
			
			if(tables.size() != 1) { 
				return null;
			}

			details.append("<h3>Property Information</h3><br>").append(tables.elementAt(0).toHtml()
					.replaceAll("<a href=\".*\">", "").replaceAll("</a>", "")		//remove links
					.replaceAll("<span class=\"style1\">.*</span>", ""));			//remove "Total Photos:..."
			
			NodeList tabbedPanels = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "TabbedPanelsContent"));
			
			details.append("<br><br><b>Owner Names</b>")
				.append(tabbedPanels.elementAt(0).toHtml().replaceAll("<a href=\".*\">", "")
					.replaceAll("</a>", ""))
				.append("<br><br><b>Value History</b>")
				.append(tabbedPanels.elementAt(1).toHtml().replaceAll("<a href=\".*\">", "")
						.replaceAll("</a>", ""))
				.append("<br><br><b>Tax History</b>")
				.append(tabbedPanels.elementAt(2).toHtml().replaceAll("a href=", "div href=")
						.replaceAll("</a>", "</div>"))
				.append("<br><br><b>Documents</b>")
				.append(tabbedPanels.elementAt(5).toHtml().replaceAll("<a href=\".*\">", "")
						.replaceAll("</a>", ""))
				.append("<br><br><b>Exp Legal</b>")
				.append(tabbedPanels.elementAt(6).toHtml());

			Node firstTableRow = tabbedPanels.elementAt(2).getFirstChild().getNextSibling()						 
				.getFirstChild().getNextSibling().getNextSibling().getNextSibling();
			if (firstTableRow ==null) return details.toString();	//there are no rows in the tax table
																	//so there are no last year tax details  
			Node linkTag  = firstTableRow.getFirstChild().getNextSibling().getFirstChild();
			String taxLink = "http://www.utahcountyonline.org/LandRecords/" + linkTag.toHtml()
				.replaceAll("<a href=\"", "").replaceAll("\">.*>", "");
			HttpSite taxPage = HttpManager.getSite(getCurrentServerName(), searchId);		//get last year tax details
			try {
				
				String taxDetails = ((ro.cst.tsearch.connection.http2.UTUtahTR)taxPage).getPage(taxLink);
				org.htmlparser.Parser htmlParserTaxDtails = org.htmlparser.Parser.createParser(taxDetails, null);
				NodeList nodeTaxDetails = htmlParserTaxDtails.parse(null);
				Node tableTaxDetails = nodeTaxDetails.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("border", "0")).elementAt(1);
				StringBuilder lastYearDetails = new StringBuilder(tableTaxDetails.toHtml()
													.replaceAll("<a href=.*?>", "").replaceAll("</a>", ""));
				int indexOfTableBorder = lastYearDetails.indexOf("table border=\"0\"") + 5;
				lastYearDetails.insert(indexOfTableBorder, " name=\"LastYearTaxTable\"");
				details.append("<h3>Real Property Tax Detail Information</h3>"+ lastYearDetails);
				
			} catch(Exception e) {
				e.printStackTrace();
			} finally {
				HttpManager.releaseSite(taxPage);
			}	
				
			java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("MM/dd/yyyy");
		    String date = dateFormat.format(new java.util.Date());
			String delinquentLink = "http://www.co.utah.ut.us/LandRecords/TaxPayoff.asp?av_serial=" 
				+ parcelID + "&av_date=" + date; 
			HttpSite delinquentPage = HttpManager.getSite(getCurrentServerName(), searchId);		//get delinquent details
			try {
				
				String taxDetails = ((ro.cst.tsearch.connection.http2.UTUtahTR)delinquentPage).getPage(delinquentLink);
				if (!taxDetails.contains("Requested Serial Number is either invalid or the property is not delinquent."))									//if there is a delinquent page
				{	
					org.htmlparser.Parser htmlParserTaxDtails = org.htmlparser.Parser.createParser(taxDetails, null);
					NodeList nodeDelinquentDetails = htmlParserTaxDtails.parse(null);
					Node tableDelinquentDetails = nodeDelinquentDetails.extractAllNodesThatMatch(new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("width", "497")).elementAt(0);
					details.append("<h3>Utah County Treasurer<br>Delinquent Tax Summary</h3>" 
							+ "Delinquent Tax Summary Calculated to "+ date + "<br><br>" + tableDelinquentDetails.toHtml() + "<br><br>");
				}
			} catch(Exception e) {
				e.printStackTrace();
			} finally {
				HttpManager.releaseSite(taxPage);	
			}
						
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
			NodeList mainTable = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width","730"), true);
			
			String textBeforeTable = mainTable.extractAllNodesThatMatch(new TagNameFilter("h1"), true)
			 	.elementAt(0).toHtml().replaceAll("h1", "h3");
			String addressSearchResults = mainTable.extractAllNodesThatMatch(new TagNameFilter("p"), true)
			 	.elementAt(0).toHtml();
			
			int index = 1;
			if (textBeforeTable.contains("Utah County Delinquent tax List")) index = 2;  //delinquent tax search
			TableTag resultsTable = (TableTag)mainTable.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(index); 
			String header = "";
			String footer = "";;
			String optionsToBeReplaced = "";
			
			//if there are results
			if (resultsTable != null && resultsTable.getRowCount() != 0)		
			{
				TableRow[] rows  = resultsTable.getRows();
				Node serialNumberNode;
				String serialNumber = "";
				String previousShortSerialNumber = "";
				String previousLongSerialNumber = "";
				if (rows[0].getColumnCount() == 8) header ="<table border=\"0\">";			//delinquent tax search
				else header ="<table width=\"730\" border=\"0\" cellspacing=\"0\" cellpadding=\"2\">";
				
				if (rows[0].getColumnCount() == 5)				//name search
					header = header + textBeforeTable + rows[0].toHtml();
				else if (rows[0].getColumnCount() == 4)			//address search
					header = header	+ textBeforeTable + addressSearchResults + rows[0].toHtml();
				else if (rows[0].getColumnCount() == 11)		//serial number search
					header = header	+ textBeforeTable + rows[0].toHtml();
				else if (rows[0].getColumnCount() == 8)			//delinquent tax search
					header = header	+ textBeforeTable + rows[0].toHtml();
				
				for (int i = 1; i < rows.length; i++) {
					TableRow row = rows[i];
										
					String htmlRow = row.toHtml();
					ParsedResponse currentResponse = new ParsedResponse();
																			
					if (rows[0].getColumnCount() == 11)				//serial number search
					{
						Node selectOptions = row.getColumns()[2];
						optionsToBeReplaced = selectOptions.toHtml().replace("<td>", "").replace("</td>", "");
						serialNumber = selectOptions.getChildren().elementAt(1).getChildren()
							.elementAt(3).getText();
						serialNumber = serialNumber.substring(37,serialNumber.length()-1);
					}
					else if (rows[0].getColumnCount() == 4)			//address search
					{
						Node linkTag = row.getColumns()[0].getChild(0);
						String taxLink = "http://www.utahcountyonline.org/LandRecords/" + linkTag.toHtml()
							.replaceAll("<a href=\"", "").replaceAll("\">.*>", "");
						HttpSite taxPage = HttpManager.getSite(getCurrentServerName(), searchId);		//get the intermediary address results 
						try {																			//and get the link for the most recent 
																										//record among them
							String addresDetails = ((ro.cst.tsearch.connection.http2.UTUtahTR)taxPage).getPage(taxLink);
							org.htmlparser.Parser htmlParserTaxDetails = org.htmlparser.Parser.createParser(addresDetails, null);
							NodeList nodeaddressDetails = htmlParserTaxDetails.parse(null);
							Node tableTaxDetails = nodeaddressDetails.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
								.extractAllNodesThatMatch(new HasAttributeFilter("width", "730"))
								.extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);	
								
							serialNumber = tableTaxDetails.toHtml()
								.replaceAll("<a href=\"property.asp\\?av_serial=", "")
								.replaceAll("\">.*</a>", "");
							
							
						} catch(Exception e) {
							e.printStackTrace();
						} finally {
							HttpManager.releaseSite(taxPage);
						}
					}
					else if (rows[0].getColumnCount() == 5)			//name search 
					{
						serialNumberNode = row.getColumns()[1].getChild(0);
						serialNumber = serialNumberNode.toHtml()
						        .replaceAll("<?td.*>", "")
								.replaceAll("<a.*=", "")
								.replaceAll("\">.*", "")
								.replaceAll("\\s","");
					}
					else if (rows[0].getColumnCount() == 8)			//delinquent tax search 
					{
						serialNumberNode = row.getColumns()[1].getChild(0);
						List<String> serialNumberArray = RegExUtils.getMatches("av_serial=(\\d+)", serialNumberNode.toHtml(), 1);
						if (serialNumberArray.get(0).equals(previousShortSerialNumber))
							serialNumber = previousLongSerialNumber;
						else
						{	
							String serialNumberLink = "http://www.co.utah.ut.us/LandRecords/SerialSearch.asp?av_serial="
								+  serialNumberArray.get(0)+ "&av_valid=%25&Submit=Submit+Query";  
							HttpSite serialPage = HttpManager.getSite(getCurrentServerName(), searchId);	//search by short serial 
							try {																			//and get the first long serial 
																											//from the result
								String serialDetails = ((ro.cst.tsearch.connection.http2.UTUtahTR)serialPage).getPage(serialNumberLink);
								org.htmlparser.Parser htmlParserSerialDetails = org.htmlparser.Parser.createParser(serialDetails, null);
								NodeList nodeaddressDetails = htmlParserSerialDetails.parse(null);
								TableTag tableSerialDetails = (TableTag) nodeaddressDetails.extractAllNodesThatMatch(new TagNameFilter("table"), true)
									.elementAt(1);
								String rawSerial = tableSerialDetails.getRow(1).getColumns()[2].toHtml();
								List<String> tmpSerial = RegExUtils.getMatches("av_serial=(\\d+)", rawSerial, 1);
								previousShortSerialNumber = serialNumberArray.get(0);
								serialNumber = previousShortSerialNumber = tmpSerial.get(0);
												
							} catch(Exception e) {
								e.printStackTrace();
							} finally {
								HttpManager.releaseSite(serialPage);
							}
						}	
					}
				    
					String link = CreatePartialLink(TSConnectionURL.idGET) 
						+ "/property.asp?av_serial=" + serialNumber;
					
					if (rows[0].getColumnCount() == 11)		//serial number search; replace the drop-down list with the link obtained earlier  
						htmlRow = htmlRow.replace(optionsToBeReplaced, "<a href=" + link + ">" + serialNumber  + "</a>" );
					else 									//name search, address search, or delinquent tax search; replace the link
						htmlRow = htmlRow.replaceAll("href=\".*\"", "href=" + link );
										
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
									
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,htmlRow);
					currentResponse.setOnlyResponse(htmlRow);
					
					ResultMap m = ro.cst.tsearch.servers.functions.UTUtahTR.parseIntermediaryRow( row, searchId ); 
					Bridge bridge = new Bridge(currentResponse, m, searchId);
					
					DocumentI document = (TaxDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
					 
				}
												
				footer = processLinks(response,nodeList);
				response.getParsedResponse().setHeader(header);
				response.getParsedResponse().setFooter(footer + "</table></td></tr></table>");
				
				outputTable.append(table);
			}
			
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}
			
	protected String processLinks(ServerResponse response, NodeList nodeList) {
		
		StringBuilder footer = new StringBuilder("<br><table border=\"0\"><tr><td></td><td></td><td>");
		NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("border", "0"));
		if(tableList.size() > 1	) {
			TableTag nextTable = (TableTag)tableList.elementAt(1);
			TableRow nextRow = nextTable.getRow(0);
			for (int i=0;i<nextRow.getColumnCount();i++)
			{
				Node node =  nextRow.getColumns()[i].getFirstChild().getNextSibling();
				if (node instanceof LinkTag)
				{
					LinkTag link = (LinkTag) node;
					link.setLink(CreatePartialLink(TSConnectionURL.idGET) 
							+ link.extractLink().replaceAll("/LandRecords","").trim());
					footer.append(link.toHtml()+"<td>");
				}
								
			}
		}
		//serial number search which has link only for "Next"
		else if (nodeList.toHtml().contains("<a href=\"SerialSearch.asp?av_serial=")) 
		{
			Node node =  nodeList.extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
			LinkTag link = (LinkTag) node;
			link.setLink(CreatePartialLink(TSConnectionURL.idGET) 
					+ link.extractLink().replaceAll("/LandRecords",""));
			footer.append(link.toHtml());
		}
		return footer + "</tr></table>";
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap) {
				
		try {
					
			resultMap.put("OtherInformationSet.SrcType","TR");
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			String parcelID = HtmlParser3.getNodeValue(new HtmlParser3(detailsHtml), "Serial Number:&nbsp;", 0, 0);
			resultMap.put("PropertyIdentificationSet.ParcelID", 
					parcelID.replace("<strong>Serial Number:&nbsp;</strong>", "").replaceAll(":", "").trim());
			
			String owner = "";
			//if there is a table with last year taxes take the owner from there
			NodeList lastYearTaxesTable = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("name", "LastYearTaxTable"));
			if (lastYearTaxesTable.size() == 1)
			{
				TableTag taxTable = (TableTag) lastYearTaxesTable.elementAt(0);
				TableRow nameRow = taxTable.getRow(2);
				owner = nameRow.toPlainTextString().replaceAll("Owner Name: ", "").trim();
			}
			//else take the name from the owners table
			if (owner.equals(""))
			{
				TableTag ownersTable = (TableTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "TabbedPanelsContent"))
					.elementAt(0).getChildren().elementAt(3);
				String lastYears = ownersTable.getRow(0).getColumns()[0].toHtml()
					.replaceAll("<td>", "").replaceAll("</td>", "")
					.replaceAll("<span.*?>", "").replaceAll("</span>", "");
				String currentYears = "";
				for (int i=0; i<ownersTable.getRowCount(); i++)
				{
					currentYears = ownersTable.getRow(i).getColumns()[0].toHtml()
									.replaceAll("<td>", "").replaceAll("</td>", "")
									.replaceAll("<span.*?>", "").replaceAll("</span>", "");
					if (lastYears.equals(currentYears)) 
						owner = owner + " & " + ownersTable.getRow(i).getColumns()[2].toHtml()
							.replaceAll("<td>", "").replaceAll("</td>", "");
					else break;
				}
				owner = owner.substring(3);
			}
			resultMap.put("PropertyIdentificationSet.NameOnServer", owner);
								
			String propertyAddress = HtmlParser3.getNodeValue(new HtmlParser3(detailsHtml), "Property Address:", 0, 0);
			resultMap.put("PropertyIdentificationSet.AddressOnServer", 
					propertyAddress.replace("<strong>Property Address:</strong>", "").replace("&nbsp;", "").trim());
			
			String legalDescription = HtmlParser3.getNodeValue(new HtmlParser3(detailsHtml), "Legal Description:&nbsp;", 0, 0);
			resultMap.put("PropertyIdentificationSet.LegalDescriptionOnServer",
					legalDescription.replace("<strong>Legal Description:&nbsp;</strong>", "").trim());
									
			ResultTable rt = new ResultTable();
			@SuppressWarnings("rawtypes")
			List<List> tablebody = new ArrayList<List>();
			List<String> list;
			
			TableTag taxesTable = (TableTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "TabbedPanelsContent"))
				.elementAt(2).getFirstChild().getNextSibling();
			int rowsNumber = taxesTable.getRowCount();
			for (int i=1; i<rowsNumber; i++)			//the row with index 0 is the header
			{
				TableRow row = taxesTable.getRow(i);  
				String link  = row.getColumns()[0].toHtml()
					.replaceAll("<td(.*)href=\"", "")
					.replaceAll("\">.*</td>", "");
				String taxLink = "http://www.utahcountyonline.org/LandRecords/" + link;
				HttpSite taxPage = HttpManager.getSite(getCurrentServerName(), searchId);		//get year tax details
				try {
					
					String year ="";
					String receiptAmount = "";
					String receiptDate = "";
					
					String taxDetails = ((ro.cst.tsearch.connection.http2.UTUtahTR)taxPage).getPage(taxLink);
					org.htmlparser.Parser htmlParserTaxDtails = org.htmlparser.Parser.createParser(taxDetails, null);
					NodeList nodeTaxDetails = htmlParserTaxDtails.parse(null);
					TableTag tableTaxDetails = (TableTag) nodeTaxDetails.extractAllNodesThatMatch(new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("border", "0")).elementAt(1);
					year = tableTaxDetails.getRow(0).toPlainTextString()
						.replaceAll("(Serial.*Year: )", "").trim();
					String nextToLastRow = tableTaxDetails.getRow(tableTaxDetails.getRowCount()-2).toPlainTextString();
					if (!nextToLastRow.contains("None"))
					{
						TableTag paid = (TableTag)tableTaxDetails.getRow(tableTaxDetails.getRowCount()-2)
							.getFirstChild().getNextSibling().getFirstChild();
						receiptAmount = paid.getRow(1).getColumns()[2].toPlainTextString()
							.replaceAll("\\$", "").replaceAll(",", "");;
						receiptDate = paid.getRow(1).getColumns()[0].toPlainTextString();
					}
					
					list = new ArrayList<String>();
					list.add(year);
					list.add(receiptAmount);
					list.add(receiptDate);
					tablebody.add(list);
					
				} catch(Exception e) {
					e.printStackTrace();
				} finally {
					HttpManager.releaseSite(taxPage);
				}
			}
			
			String[] header = {"Year", "ReceiptAmount", "ReceiptDate"};
			rt = GenericFunctions2.createResultTable(tablebody, header);
			if (rt != null){
				resultMap.put("TaxHistorySet", rt);
			}
						
			ro.cst.tsearch.servers.functions.UTUtahTR.parseNames(resultMap, searchId);
			ro.cst.tsearch.servers.functions.UTUtahTR.parseAddress(resultMap);
			ro.cst.tsearch.servers.functions.UTUtahTR.parseLegalSummary(resultMap);
			ro.cst.tsearch.servers.functions.UTUtahTR.parseTaxes(nodeList, resultMap, searchId);
									
		
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
			FilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(0, SearchAttributes.LD_PARCELNO);
			module.addFilter(pinFilter);
			moduleList.add(module);
		}
						
		boolean hasOwner = hasOwner();
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d , true);
		FilterResponse cityFilter = CityFilterFactory.getCityFilterDefault(searchId);
		
		if(hasStreet()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(0, SearchAttributes.P_STREETNAME);
			module.setSaKey(4, SearchAttributes.P_STREETNO);
			module.addFilter(addressFilter);
			module.addFilter(cityFilter);
			moduleList.add(module);
		}
								
		if(hasOwner) {
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter( 
					SearchAttributes.OWNER_OBJECT , searchId , module );
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
			
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
			module.addFilter(addressFilter);
			module.addFilter(cityFilter);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] {"L;F;","L;M;"})
					);
			moduleList.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(moduleList);
		
	}

}
